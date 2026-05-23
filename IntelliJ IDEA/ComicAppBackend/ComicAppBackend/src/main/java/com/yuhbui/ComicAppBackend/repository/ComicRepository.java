package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.Comic;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComicRepository extends JpaRepository<Comic, Integer> {

    // Câu truy vấn vạn năng: Lấy thông tin truyện và tính toán tổng số chương, follow, comment
    @Query(value = "SELECT c.ComicID, c.Title, c.CoverImageUrl, c.ViewCount, c.Rating, c.Status, " +
            "(SELECT ch.ChapterNumber FROM Chapters ch WHERE ch.ComicID = c.ComicID ORDER BY ch.ChapterNumber DESC LIMIT 1) as latestChapter, " +
            "c.CreatedAt as timeUpdate, " +
            "(SELECT COUNT(*) FROM Follows f WHERE f.ComicID = c.ComicID) as follows, " +
            "(SELECT COUNT(*) FROM Comments co WHERE co.ComicID = c.ComicID) as comments " +
            "FROM Comics c WHERE c.IsHidden = FALSE", nativeQuery = true)
    List<Object[]> getComicHomeDataRaw();

    // API Lọc truyện theo danh mục CategoryID
    @Query(value = "SELECT c.* FROM Comics c JOIN Comic_Categories cc ON c.ComicID = cc.ComicID WHERE cc.CategoryID = :catId AND c.IsHidden = FALSE", nativeQuery = true)
    List<Comic> findByCategoryId(@Param("catId") Integer catId);
}