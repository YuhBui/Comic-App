package com.yuhbui.ComicAppBackend.controller;

import com.yuhbui.ComicAppBackend.entity.Notification;
import com.yuhbui.ComicAppBackend.repository.NotificationRepository;
import com.yuhbui.ComicAppBackend.repository.UserRepository;
import com.yuhbui.ComicAppBackend.repository.FollowRepository; // THÊM
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    // === CHỨC NĂNG DÀNH CHO USER ===
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Integer userId) {
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/unread-count/{userId}")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Integer userId) {
        return ResponseEntity.ok(notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @PutMapping("/mark-read/{notifId}")
    public ResponseEntity<?> markAsRead(@PathVariable Integer notifId) {
        notificationRepository.findById(notifId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
        return ResponseEntity.ok("Marked as read");
    }

    // === CHỨC NĂNG CRUD CỦA ADMIN ===
    // SỬA: Hỗ trợ lấy toàn bộ thông báo kèm tìm kiếm theo tiêu đề cho Admin
    @GetMapping("/admin/all")
    public ResponseEntity<List<Notification>> getAllNotificationsForAdmin(@RequestParam(required = false) String keyword) {
        String searchKey = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return ResponseEntity.ok(notificationRepository.findUniqueNotificationsForAdmin(searchKey));
    }

    // SỬA: Tách nhóm đối tượng gửi: Gửi Toàn bộ hệ thống HOẶC chỉ gửi cho Người theo dõi truyện cụ thể
    @PostMapping("/admin/create")
    public ResponseEntity<?> adminCreateNotification(@RequestBody Notification sample) {
        if (sample.getComicId() != null && sample.getComicId() > 0) {
            // Lấy danh sách UserID đã nhấn theo dõi truyện này
            List<Integer> userIds = followRepository.findUserIdsByComicId(sample.getComicId());
            if (userIds.isEmpty()) {
                return ResponseEntity.badRequest().body("Truyện này hiện chưa có ai theo dõi để gửi thông báo!");
            }
            for (Integer uId : userIds) {
                Notification n = new Notification();
                n.setUserId(uId);
                n.setTitle(sample.getTitle());
                n.setMessage(sample.getMessage());
                n.setComicId(sample.getComicId());
                notificationRepository.save(n);
            }
            return ResponseEntity.ok("Đã phát sóng thông báo tới toàn bộ người theo dõi truyện!");
        } else {
            // Gửi tới TOÀN BỘ User hệ thống (như cũ)
            List<com.yuhbui.ComicAppBackend.entity.User> allUsers = userRepository.findAll();
            for (com.yuhbui.ComicAppBackend.entity.User user : allUsers) {
                Notification n = new Notification();
                n.setUserId(user.getUserId());
                n.setTitle(sample.getTitle());
                n.setMessage(sample.getMessage());
                n.setComicId(null);
                notificationRepository.save(n);
            }
            return ResponseEntity.ok("Đã phát sóng thông báo tới toàn hệ thống!");
        }
    }

    // 2. Chỉnh sửa nội dung thông báo
    @PutMapping("/admin/edit/{id}")
    public ResponseEntity<?> adminEditNotification(@PathVariable Integer id, @RequestBody Notification updatedData) {
        return notificationRepository.findById(id).map(n -> {
            n.setTitle(updatedData.getTitle());
            n.setMessage(updatedData.getMessage());
            n.setComicId(updatedData.getComicId());
            notificationRepository.save(n);
            return ResponseEntity.ok("Sửa thông báo thành công!");
        }).orElse(ResponseEntity.notFound().build());
    }

    // 3. Xóa thông báo
    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<?> adminDeleteNotification(@PathVariable Integer id) {
        notificationRepository.deleteById(id);
        return ResponseEntity.ok("Xóa thông báo thành công!");
    }
}