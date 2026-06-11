package com.yuhbui.ComicAppBackend.controller;

import com.yuhbui.ComicAppBackend.dto.ComicHomeResponseDTO;
import com.yuhbui.ComicAppBackend.entity.ReadingHistory;
import com.yuhbui.ComicAppBackend.repository.ReadingHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private ReadingHistoryRepository historyRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    /**
     * Lưu hoặc cập nhật lịch sử đọc của người dùng và tăng ViewCount
     */
    @PostMapping("/save")
    @Transactional // Cần có Transactional để chạy query cập nhật dữ liệu
    public ReadingHistory saveHistory(@RequestBody ReadingHistory request) {
        Optional<ReadingHistory> existingOpt =
                historyRepository.findByUserIdAndComicId(request.getUserId(), request.getComicId());

        boolean isNewChapterView = false;

        if (existingOpt.isPresent()) {
            ReadingHistory historyToUpdate = existingOpt.get();

            // Nếu người dùng chuyển sang một chương mới thì tính 1 lượt xem mới
            if (request.getLastChapterId() != null && !request.getLastChapterId().equals(historyToUpdate.getLastChapterId())) {
                isNewChapterView = true;
            }

            historyToUpdate.setLastChapterId(request.getLastChapterId());
            historyToUpdate.setLastPage(request.getLastPage());
            historyToUpdate.setUpdatedAt(LocalDateTime.now());
            ReadingHistory saved = historyRepository.save(historyToUpdate);

            if (isNewChapterView) {
                incrementViews(request.getComicId(), request.getLastChapterId());
            }
            return saved;
        } else {
            request.setHistoryId(null);
            request.setUpdatedAt(LocalDateTime.now());
            ReadingHistory saved = historyRepository.save(request);

            // Lần đầu tiên đọc truyện này -> Chắc chắn tăng view
            incrementViews(request.getComicId(), request.getLastChapterId());
            return saved;
        }
    }

    /**
     * Hàm phụ trợ tăng ViewCount cho Comics và Chapters
     */
    private void incrementViews(Integer comicId, Integer chapterId) {
        try {
            if (comicId != null) {
                entityManager.createNativeQuery("UPDATE Comics SET ViewCount = ViewCount + 1 WHERE ComicID = :comicId")
                        .setParameter("comicId", comicId)
                        .executeUpdate();
            }
            if (chapterId != null) {
                entityManager.createNativeQuery("UPDATE Chapters SET ViewCount = ViewCount + 1 WHERE ChapterID = :chapterId")
                        .setParameter("chapterId", chapterId)
                        .executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Lấy lịch sử đọc của người dùng kèm đầy đủ thông số:
     * mỗi truyện chỉ xuất hiện 1 lần, hiển thị chương mới nhất, follow, comment, thời gian cập nhật.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ComicHomeResponseDTO>> getReadingHistory(
            @PathVariable("userId") Integer userId,
            @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds) {

        // Câu lệnh SQL vạn năng gom nhóm theo ComicID để mỗi bộ truyện xuất hiện 1 lần độc nhất
        String sql = "SELECT c.ComicID, c.Title, c.CoverImageUrl, c.ViewCount, c.Rating, c.Status, " +
                "(SELECT ch.ChapterNumber FROM Chapters ch WHERE ch.ComicID = c.ComicID ORDER BY ch.ChapterNumber DESC LIMIT 1) AS latestChapter, " +
                "(SELECT ch.CreatedAt FROM Chapters ch WHERE ch.ComicID = c.ComicID ORDER BY ch.ChapterNumber DESC LIMIT 1) AS timeUpdate, " +
                "(SELECT COUNT(*) FROM Follows f WHERE f.ComicID = c.ComicID) AS follows, " +
                "(SELECT COUNT(*) FROM Comments cmt WHERE cmt.ComicID = c.ComicID) AS comments " +
                "FROM ReadingHistory h JOIN Comics c ON h.ComicID = c.ComicID WHERE h.UserID = :userId";

        boolean hasCategories = categoryIds != null && !categoryIds.isEmpty();

        if (hasCategories) {
            sql += " AND c.ComicID IN (SELECT cc.ComicID FROM Comic_Categories cc WHERE cc.CategoryID IN (:categoryIds) GROUP BY cc.ComicID HAVING COUNT(DISTINCT cc.CategoryID) = :categoryCount)";
        }

        // Group by để loại bỏ trùng lặp truyện và sắp xếp theo lượt đọc gần đây nhất của người dùng
        sql += " GROUP BY c.ComicID, c.Title, c.CoverImageUrl, c.ViewCount, c.Rating, c.Status " +
                " ORDER BY MAX(h.UpdatedAt) DESC";

        var query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        if (hasCategories) {
            query.setParameter("categoryIds", categoryIds);
            query.setParameter("categoryCount", categoryIds.size());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> list = query.getResultList();

        // Ánh xạ dữ liệu thô Object[] sang ComicHomeResponseDTO để hiển thị đầy đủ thông số lên Android
        List<ComicHomeResponseDTO> dtoList = list.stream()
                .map(this::mapRowToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    /**
     * Ánh xạ Object[] kết quả native query sang ComicHomeResponseDTO
     */
    private ComicHomeResponseDTO mapRowToDTO(Object[] row) {
        return new ComicHomeResponseDTO(
                (Integer) row[0],                                                  // comicId
                (String) row[1],                                                   // title
                (String) row[2],                                                   // coverImageUrl
                row[3] != null ? ((Number) row[3]).intValue() : 0,                 // viewCount
                row[4] != null ? ((Number) row[4]).floatValue() : 0f,              // rating
                (String) row[5],                                                   // status
                row[6] != null ? "Chương " + row[6].toString() : "Chương 0",       // latestChapterNumber
                convertToRelativeTime(row[7]),                                     // ĐÃ SỬA: Gọi hàm biến đổi "... trước"
                row[8] != null ? ((Number) row[8]).longValue() : 0L,               // followCount
                row[9] != null ? ((Number) row[9]).longValue() : 0L                // commentCount
        );
    }

    /**
     * Hàm chuyển đổi mốc thời gian từ database sang dạng tương đối (X ngày/giờ/phút trước)
     */
    private String convertToRelativeTime(Object timeObj) {
        if (timeObj == null) return "Đang cập nhật";

        java.time.LocalDateTime dateTime = null;

        // Kiểm tra và ép kiểu an toàn từ kết quả Native Query
        if (timeObj instanceof java.sql.Timestamp) {
            dateTime = ((java.sql.Timestamp) timeObj).toLocalDateTime();
        } else if (timeObj instanceof java.time.LocalDateTime) {
            dateTime = (java.time.LocalDateTime) timeObj;
        } else {
            try {
                // Trường hợp trả về dạng String "yyyy-MM-dd HH:mm:ss"
                String timeStr = timeObj.toString().replace(" ", "T");
                dateTime = java.time.LocalDateTime.parse(timeStr);
            } catch (Exception e) {
                return timeObj.toString(); // Nếu lỗi parse thì trả về chuỗi gốc
            }
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(dateTime, now);

        long seconds = duration.getSeconds();
        if (seconds < 0) seconds = 0; // Tránh lỗi lệch giây hệ thống hiển thị tương lai

        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long months = days / 30;
        long years = days / 365;

        if (seconds < 60) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else if (days < 30) {
            return days + " ngày trước";
        } else if (months < 12) {
            return months + " tháng trước";
        } else {
            return years + " năm trước";
        }
    }
}