package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.Follow;
import com.yuhbui.ComicAppBackend.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    boolean existsByUserIdAndComicId(Integer userId, Integer comicId);
    int countByComicId(Integer comicId); // Dùng để đếm tổng số lượt yêu thích của truyện
}