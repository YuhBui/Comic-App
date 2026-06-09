package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.Follow;
import com.yuhbui.ComicAppBackend.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    boolean existsByUserIdAndComicId(Integer userId, Integer comicId);
    int countByComicId(Integer comicId);

    /**
     * Lấy danh sách truyện yêu thích của user kèm đầy đủ thông số tính toán
     * Trả về Object[] để ánh xạ sang ComicHomeResponseDTO
     */
    @Query(value =
        "SELECT c.ComicID, c.Title, c.CoverImageUrl, c.ViewCount, c.Rating, c.Status, " +
        "(SELECT ch.ChapterNumber FROM Chapters ch WHERE ch.ComicID = c.ComicID ORDER BY ch.ChapterNumber DESC LIMIT 1) as latestChapter, " +
        "c.CreatedAt as timeUpdate, " +
        "(SELECT COUNT(*) FROM Follows f WHERE f.ComicID = c.ComicID) as follows, " +
        "(SELECT COUNT(*) FROM Comments co WHERE co.ComicID = c.ComicID) as comments " +
        "FROM Follows fv " +
        "JOIN Comics c ON fv.ComicID = c.ComicID " +
        "WHERE fv.UserID = :userId AND c.IsHidden = FALSE " +
        "ORDER BY c.Title ASC",
        nativeQuery = true)
    List<Object[]> findFavoriteComicsWithStatsByUserId(@Param("userId") Integer userId);

    @Query("SELECT f.id.userId FROM Follow f WHERE f.id.comicId = :comicId")
    List<Integer> findUserIdsByComicId(@Param("comicId") Integer comicId);
}