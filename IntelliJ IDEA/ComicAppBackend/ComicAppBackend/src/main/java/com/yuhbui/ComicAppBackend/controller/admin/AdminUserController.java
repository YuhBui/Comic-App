package com.yuhbui.ComicAppBackend.controller.admin;

import com.yuhbui.ComicAppBackend.entity.User;
import com.yuhbui.ComicAppBackend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Hàm mã hóa mật khẩu SHA-256 đồng bộ với hệ thống UserController cũ của bạn
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi mã hóa mật khẩu", e);
        }
    }

    // 1. API LẤY DANH SÁCH NGƯỜI DÙNG KÈM BỘ LỌC VAI TRÒ VÀ TÌM KIẾM KEYWORD
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) String role) {

        String sql = "SELECT * FROM Users WHERE 1=1";
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql += " AND (DisplayName LIKE '%" + keyword + "%' OR Email LIKE '%" + keyword + "%')";
        }
        if (role != null && !role.trim().isEmpty() && !role.equalsIgnoreCase("Tất cả")) {
            sql += " AND Role = '" + role + "'";
        }
        sql += " ORDER BY CreatedAt DESC";

        @SuppressWarnings("unchecked")
        List<User> users = entityManager.createNativeQuery(sql, User.class).getResultList();
        // Ẩn mật khẩu băm để bảo mật an toàn dữ liệu
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    // 2. API THÊM MỚI NGƯỜI DÙNG TRỰC TIẾP TỪ DASHBOARD ADMIN
    @PostMapping
    public ResponseEntity<?> adminCreateUser(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email này đã được sử dụng!");
        }
        if (userRepository.findByDisplayName(user.getDisplayName()).isPresent()) {
            return ResponseEntity.badRequest().body("Tên hiển thị đã được sử dụng!");
        }

        user.setUserId(null); // Đảm bảo tự tăng tự động
        user.setPassword(hashPassword(user.getPassword() != null ? user.getPassword() : "123456")); // Mật khẩu mặc định nếu trống
        if (user.getRole() == null) user.setRole("User");
        if (user.getStatus() == null) user.setStatus("Active");

        User saved = userRepository.save(user);
        saved.setPassword(null);
        return ResponseEntity.ok(saved);
    }

    // 3. API CHỈNH SỬA THÔNG TIN CHI TIẾT NGƯỜI DÙNG
    @PutMapping("/{id}")
    public ResponseEntity<?> adminUpdateUser(@PathVariable Integer id, @RequestBody User userDetails) {
        Optional<User> userOpt = userRepository.findById(id);
        if (!userOpt.isPresent()) return ResponseEntity.notFound().build();

        User user = userOpt.get();

        // Kiểm tra trùng lặp email/tên hiển thị khi thay đổi thông tin
        Optional<User> checkEmail = userRepository.findByEmail(userDetails.getEmail());
        if (checkEmail.isPresent() && !checkEmail.get().getUserId().equals(id)) {
            return ResponseEntity.badRequest().body("Email mới đã tồn tại hệ thống!");
        }
        Optional<User> checkName = userRepository.findByDisplayName(userDetails.getDisplayName());
        if (checkName.isPresent() && !checkName.get().getUserId().equals(id)) {
            return ResponseEntity.badRequest().body("Tên hiển thị mới đã tồn tại!");
        }

        user.setEmail(userDetails.getEmail());
        user.setDisplayName(userDetails.getDisplayName());
        user.setRole(userDetails.getRole());

        if (userDetails.getPassword() != null && !userDetails.getPassword().trim().isEmpty()) {
            user.setPassword(hashPassword(userDetails.getPassword()));
        }

        User updated = userRepository.save(user);
        updated.setPassword(null);
        return ResponseEntity.ok(updated);
    }

    // 4. API BAN / UNBAN ĐỔI TRẠNG THÁI HOẠT ĐỘNG NGƯỜI DÙNG LẬP TỨC
    @PutMapping("/{id}/toggle-ban")
    public ResponseEntity<?> toggleBanUser(@PathVariable Integer id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (!userOpt.isPresent()) return ResponseEntity.notFound().build();

        User user = userOpt.get();
        if ("Active".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("Banned");
        } else {
            user.setStatus("Active");
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "status", user.getStatus()));
    }

    // 5. API XÓA SẠCH NGƯỜI DÙNG (DỌN DẸP KHÓA NGOẠI TRƯỚC TRÁNH LỖI CRASH DATABASE)
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> adminDeleteUser(@PathVariable Integer id) {
        if (!userRepository.existsById(id)) return ResponseEntity.notFound().build();

        // Tiến hành dọn dẹp sạch các bảng phụ thuộc ràng buộc khóa ngoại của UserID trong CSDL
        entityManager.createNativeQuery("DELETE FROM Comment_Interactions WHERE UserID = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Comment_Reports WHERE UserID = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Comments WHERE UserID = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM ReadingHistory WHERE UserID = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Follows WHERE UserID = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Rating WHERE UserID = :id").setParameter("id", id).executeUpdate();

        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}