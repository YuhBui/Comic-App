package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.ReadingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Integer> {
    Optional<ReadingHistory> findByUserIdAndComicId(Integer userId, Integer comicId);

    /**
     * Lấy danh sách truyện đã đọc kèm đầy đủ thông số tính toán (chương, follow, comment)
     * Trả về Object[] để ánh xạ sang ComicHomeResponseDTO giống các API Home khác
     */
    @Query(value =
        "SELECT c.ComicID, c.Title, c.CoverImageUrl, c.ViewCount, c.Rating, c.Status, " +
        "(SELECT ch.ChapterNumber FROM Chapters ch WHERE ch.ComicID = c.ComicID ORDER BY ch.ChapterNumber DESC LIMIT 1) as latestChapter, " +
        "rh.UpdatedAt as timeUpdate, " +
        "(SELECT COUNT(*) FROM Follows f WHERE f.ComicID = c.ComicID) as follows, " +
        "(SELECT COUNT(*) FROM Comments co WHERE co.ComicID = c.ComicID) as comments " +
        "FROM ReadingHistory rh " +
        "JOIN Comics c ON rh.ComicID = c.ComicID " +
        "WHERE rh.UserID = :userId AND c.IsHidden = FALSE " +
        "ORDER BY rh.UpdatedAt DESC",
        nativeQuery = true)
    List<Object[]> findReadComicsWithStatsByUserId(@Param("userId") Integer userId);
}