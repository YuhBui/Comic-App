package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.ReadingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Integer> {
    // Tìm xem user này đã từng đọc truyện này chưa
    Optional<ReadingHistory> findByUserIdAndComicId(Integer userId, Integer comicId);
}