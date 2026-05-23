package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.CommentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Integer> {
    // Kiểm tra xem User này đã report comment này chưa để tránh spam report
    boolean existsByUserIdAndCommentId(Integer userId, Integer commentId);
}