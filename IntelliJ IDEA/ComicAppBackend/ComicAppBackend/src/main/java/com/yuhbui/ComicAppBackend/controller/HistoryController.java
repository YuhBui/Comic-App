package com.yuhbui.ComicAppBackend.controller;

import com.yuhbui.ComicAppBackend.dto.ComicHomeResponseDTO;
import com.yuhbui.ComicAppBackend.entity.ReadingHistory;
import com.yuhbui.ComicAppBackend.repository.ReadingHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
     * Lưu hoặc cập nhật lịch sử đọc của người dùng
     */
    @PostMapping("/save")
    public ReadingHistory saveHistory(@RequestBody ReadingHistory request) {
        Optional<ReadingHistory> existingOpt =
                historyRepository.findByUserIdAndComicId(request.getUserId(), request.getComicId());

        if (existingOpt.isPresent()) {
            ReadingHistory historyToUpdate = existingOpt.get();
            historyToUpdate.setLastChapterId(request.getLastChapterId());
            historyToUpdate.setLastPage(request.getLastPage());
            historyToUpdate.setUpdatedAt(LocalDateTime.now());
            return historyRepository.save(historyToUpdate);
        } else {
            request.setHistoryId(null);
            request.setUpdatedAt(LocalDateTime.now());
            return historyRepository.save(request);
        }
    }

    /**
     * Lấy lịch sử đọc của người dùng kèm đầy đủ thông số:
     * latestChapterNumber, timeUpdated, viewCount, followCount, commentCount
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<com.yuhbui.ComicAppBackend.entity.Comic>> getReadingHistory(
            @PathVariable("userId") Integer userId,
            @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds) {

        String sql = "SELECT c.* FROM ReadingHistory h JOIN Comics c ON h.ComicID = c.ComicID WHERE h.UserID = :userId";
        boolean hasCategories = categoryIds != null && !categoryIds.isEmpty();

        if (hasCategories) {
            sql += " AND c.ComicID IN (SELECT cc.ComicID FROM Comic_Categories cc WHERE cc.CategoryID IN (:categoryIds) GROUP BY cc.ComicID HAVING COUNT(DISTINCT cc.CategoryID) = :categoryCount)";
        }
        sql += " ORDER BY h.UpdatedAt DESC";

        var query = entityManager.createNativeQuery(sql, com.yuhbui.ComicAppBackend.entity.Comic.class);
        query.setParameter("userId", userId);
        if (hasCategories) {
            query.setParameter("categoryIds", categoryIds);
            query.setParameter("categoryCount", categoryIds.size());
        }

        @SuppressWarnings("unchecked")
        List<com.yuhbui.ComicAppBackend.entity.Comic> list = query.getResultList();
        return ResponseEntity.ok(list);
    }

    /**
     * Ánh xạ Object[] kết quả native query sang ComicHomeResponseDTO
     * Thứ tự cột: 0=ComicID, 1=Title, 2=CoverImageUrl, 3=ViewCount, 4=Rating,
     *             5=Status, 6=latestChapter, 7=timeUpdate, 8=follows, 9=comments
     */
    private ComicHomeResponseDTO mapRowToDTO(Object[] row) {
        return new ComicHomeResponseDTO(
                (Integer) row[0],                                                  // comicId
                (String) row[1],                                                   // title
                (String) row[2],                                                   // coverImageUrl
                row[3] != null ? ((Number) row[3]).intValue() : 0,                 // viewCount
                row[4] != null ? ((Number) row[4]).floatValue() : 0f,              // rating
                (String) row[5],                                                   // status
                row[6] != null ? "Chương " + row[6].toString() : "Chương 0",      // latestChapterNumber
                row[7] != null ? row[7].toString() : "Vừa xong",                  // timeUpdated
                row[8] != null ? ((Number) row[8]).longValue() : 0L,               // followCount
                row[9] != null ? ((Number) row[9]).longValue() : 0L                // commentCount
        );
    }
}