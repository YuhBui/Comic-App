package com.yuhbui.ComicAppBackend.controller;

import com.yuhbui.ComicAppBackend.dto.ComicHomeResponseDTO;
import com.yuhbui.ComicAppBackend.entity.ReadingHistory;
import com.yuhbui.ComicAppBackend.repository.ReadingHistoryRepository;
import com.yuhbui.ComicAppBackend.repository.FollowRepository;
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

    @Autowired
    private FollowRepository followRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    /**
     * Lưu hoặc cập nhật lịch sử đọc của người dùng và tăng ViewCount
     */
    @PostMapping("/save")
    @Transactional
    public ReadingHistory saveHistory(@RequestBody ReadingHistory request) {
        Optional<ReadingHistory> existingOpt =
                historyRepository.findByUserIdAndComicId(request.getUserId(), request.getComicId());

        boolean isNewChapterView = false;

        if (existingOpt.isPresent()) {
            ReadingHistory historyToUpdate = existingOpt.get();

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

            incrementViews(request.getComicId(), request.getLastChapterId());
            return saved;
        }
    }

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
     * Lấy lịch sử đọc của người dùng kèm đầy đủ thông số
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ComicHomeResponseDTO>> getReadingHistory(
            @PathVariable("userId") Integer userId,
            @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds) {

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

        List<ComicHomeResponseDTO> dtoList = list.stream()
                .map(this::mapRowToDTO)
                .peek(dto -> {
                    if (userId != null) {
                        dto.setFollowed(followRepository.existsByUserIdAndComicId(userId, dto.getComicId()));
                    }
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    /**
     * Ánh xạ Object[] kết quả native query sang ComicHomeResponseDTO dùng Setter
     */
    private ComicHomeResponseDTO mapRowToDTO(Object[] row) {
        ComicHomeResponseDTO dto = new ComicHomeResponseDTO();
        dto.setComicId((Integer) row[0]);
        dto.setTitle((String) row[1]);
        dto.setCoverImageUrl((String) row[2]);
        dto.setViewCount(row[3] != null ? ((Number) row[3]).intValue() : 0);
        dto.setRating(row[4] != null ? ((Number) row[4]).floatValue() : 0f);
        dto.setStatus((String) row[5]);
        dto.setLatestChapterNumber(row[6] != null ? "Chương " + row[6].toString() : "Chương 0");
        dto.setTimeUpdated(convertToRelativeTime(row[7]));
        dto.setFollowCount(row[8] != null ? ((Number) row[8]).longValue() : 0L);
        dto.setCommentCount(row[9] != null ? ((Number) row[9]).longValue() : 0L);
        dto.setFollowed(false);
        return dto;
    }

    private String convertToRelativeTime(Object timeObj) {
        if (timeObj == null) return "Đang cập nhật";

        java.time.LocalDateTime dateTime = null;

        if (timeObj instanceof java.sql.Timestamp) {
            dateTime = ((java.sql.Timestamp) timeObj).toLocalDateTime();
        } else if (timeObj instanceof java.time.LocalDateTime) {
            dateTime = (java.time.LocalDateTime) timeObj;
        } else {
            try {
                String timeStr = timeObj.toString().replace(" ", "T");
                dateTime = java.time.LocalDateTime.parse(timeStr);
            } catch (Exception e) {
                return timeObj.toString();
            }
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(dateTime, now);

        long seconds = duration.getSeconds();
        if (seconds < 0) seconds = 0;

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