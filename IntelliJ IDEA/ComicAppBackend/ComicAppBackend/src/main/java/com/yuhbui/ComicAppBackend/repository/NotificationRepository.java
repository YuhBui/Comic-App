package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);
    long countByUserIdAndIsReadFalse(Integer userId);

    // Gom nhóm theo Tiêu đề, Nội dung, Mã truyện để Admin chỉ nhìn thấy 1 bản ghi duy nhất cho mỗi đợt phát sóng
    @Query(value = "SELECT * FROM Notifications WHERE NotificationID IN (" +
            "  SELECT MIN(NotificationID) FROM Notifications " +
            "  GROUP BY Title, Message, ComicID" +
            ") " +
            "AND (:keyword IS NULL OR LOWER(Title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY CreatedAt DESC", nativeQuery = true)
    List<Notification> findUniqueNotificationsForAdmin(@Param("keyword") String keyword);
}