package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.ReadingHistory;
import com.yuhbui.ComicAppBackend.entity.Comic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Integer> {
    Optional<ReadingHistory> findByUserIdAndComicId(Integer userId, Integer comicId);

    // Câu lệnh HQL: Lấy danh sách thông tin Truyện (Comic) từ bảng Lịch sử, sắp xếp theo thời gian đọc gần nhất
    @Query("SELECT c FROM ReadingHistory rh JOIN Comic c ON rh.comicId = c.comicId WHERE rh.userId = :userId ORDER BY rh.updatedAt DESC")
    List<Comic> findReadComicsByUserId(@Param("userId") Integer userId);
}