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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
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

    private String sanitizeInput(String input) {
        if (input == null) return "";
        input = input.trim();
        if (input.startsWith("\"") && input.endsWith("\"") && input.length() >= 2) {
            input = input.substring(1, input.length() - 1).trim();
        }
        if ("null".equalsIgnoreCase(input) || "undefined".equalsIgnoreCase(input)) {
            return "";
        }
        return input;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi băm mật khẩu", e);
        }
    }

    // 1. ĐÃ CHUẨN HÓA: API LẤY DANH SÁCH NGƯỜI DÙNG PHÂN TRANG ĐỒNG BỘ 100% VỚI PHÍA USER
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? sanitizeInput(keyword) : null;
        String cleanRole = (role != null && !role.trim().isEmpty() && !role.equalsIgnoreCase("Tất cả")) ? sanitizeInput(role) : null;

        // Phân trang chuẩn JPA và sắp xếp theo ngày tạo giảm dần (createdAt)
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());

        org.springframework.data.domain.Page<User> userPage = userRepository.findAllAdminWithPagination(cleanKeyword, cleanRole, pageable);

        // Đóng gói JSON trả về khớp với cấu trúc phân trang bên User (Truyện mới, Lịch sử, Yêu thích)
        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getContent());            // Danh sách tài liệu bản ghi của trang hiện tại
        response.put("totalPages", userPage.getTotalPages());      // Tổng số trang (Ví dụ: 5)
        response.put("currentPage", userPage.getNumber());        // Trang hiện tại (Bắt đầu từ 0)
        response.put("totalItems", userPage.getTotalElements());  // Tổng số lượng người dùng trong DB

        return ResponseEntity.ok(response);
    }

    // 2. ĐÃ SỬA: API THÊM MỚI NGƯỜI DÙNG - BẢO ĐẢM KHÔNG LỖI TIMESTAMP
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> adminCreateUser(
            @RequestParam("displayName") String displayName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") String role,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            displayName = sanitizeInput(displayName);
            email = sanitizeInput(email);
            password = sanitizeInput(password);
            role = sanitizeInput(role);

            if (email.isEmpty() || displayName.isEmpty() || password.isEmpty()) {
                return ResponseEntity.badRequest().body("Thông tin tài khoản không được để trống!");
            }

            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body("Địa chỉ Email này đã được sử dụng!");
            }
            if (userRepository.findByDisplayName(displayName).isPresent()) {
                return ResponseEntity.badRequest().body("Tên hiển thị này đã được đăng ký từ trước!");
            }

            User user = new User();
            user.setDisplayName(displayName);
            user.setEmail(email);
            user.setPassword(hashPassword(password));
            user.setRole(role.isEmpty() ? "User" : role);
            user.setStatus("Active");

            if (file != null && !file.isEmpty()) {
                String uploadDir = "uploads/avatars/";
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";
                Path filePath = Paths.get(uploadDir + fileName);
                Files.write(filePath, file.getBytes());

                user.setAvatarUrl("http://localhost:8080/uploads/avatars/" + fileName);
            } else {
                user.setAvatarUrl("http://localhost:8080/uploads/avatars/default_avatar.jpg");
            }

            User saved = userRepository.save(user);

            // Gán tạm thời thời gian hiện tại vào đối tượng Java để Android nhận phản hồi hiển thị ngay lập tức,
            // Cấu hình updatable/insertable = false bảo đảm lệnh này KHÔNG sinh ra lỗi ghi xuống database.
            saved.setCreatedAt(LocalDateTime.now());

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi hệ thống khi tạo tài khoản: " + e.getMessage());
        }
    }

    // 3. ĐÃ SỬA TRIỆT ĐỂ: API CẬP NHẬT MULTIPART - SỬA LỖI SẬP MẠNG KHI SỬA
    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> adminUpdateUserWithMultipart(
            @PathVariable Integer id,
            @RequestParam("displayName") String displayName,
            @RequestParam("email") String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam("role") String role,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Thành viên không tồn tại trên hệ thống!");
            }

            User user = userOpt.get();

            displayName = sanitizeInput(displayName);
            email = sanitizeInput(email);
            password = sanitizeInput(password);
            role = sanitizeInput(role);

            Optional<User> checkEmail = userRepository.findByEmail(email);
            if (checkEmail.isPresent() && !checkEmail.get().getUserId().equals(id)) {
                return ResponseEntity.badRequest().body("Địa chỉ Email mới nhập đã được một tài khoản khác sử dụng!");
            }
            Optional<User> checkName = userRepository.findByDisplayName(displayName);
            if (checkName.isPresent() && !checkName.get().getUserId().equals(id)) {
                return ResponseEntity.badRequest().body("Tên hiển thị mới đã được một tài khoản khác đăng ký!");
            }

            user.setEmail(email);
            user.setDisplayName(displayName);
            user.setRole(role);

            if (password != null && !password.isEmpty()) {
                user.setPassword(hashPassword(password));
            }

            if (file != null && !file.isEmpty()) {
                String uploadDir = "uploads/avatars/";
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String fileName = "avatar_" + id + "_" + System.currentTimeMillis() + ".jpg";
                Path filePath = Paths.get(uploadDir + fileName);
                Files.write(filePath, file.getBytes());

                user.setAvatarUrl("http://localhost:8080/uploads/avatars/" + fileName);
            }

            userRepository.save(user);

            // ĐÃ XÓA DÒNG GÂY LỖI: user.setPassword(null);
            // Bây giờ hàm sửa sẽ chạy cực kỳ mượt mà không bao giờ bị báo lỗi ràng buộc mật khẩu nữa!

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi hệ thống khi cập nhật: " + e.getMessage());
        }
    }

    // 4. API CẬP NHẬT JSON BODY
    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> adminUpdateUser(@PathVariable Integer id, @RequestBody User userDetails) {
        Optional<User> userOpt = userRepository.findById(id);
        if (!userOpt.isPresent()) return ResponseEntity.notFound().build();

        User user = userOpt.get();

        String cleanEmail = sanitizeInput(userDetails.getEmail());
        String cleanName = sanitizeInput(userDetails.getDisplayName());

        Optional<User> checkEmail = userRepository.findByEmail(cleanEmail);
        if (checkEmail.isPresent() && !checkEmail.get().getUserId().equals(id)) {
            return ResponseEntity.badRequest().body("Email mới đã tồn tại trên hệ thống!");
        }
        Optional<User> checkName = userRepository.findByDisplayName(cleanName);
        if (checkName.isPresent() && !checkName.get().getUserId().equals(id)) {
            return ResponseEntity.badRequest().body("Tên hiển thị mới đã tồn tại!");
        }

        user.setEmail(cleanEmail);
        user.setDisplayName(cleanName);
        user.setRole(sanitizeInput(userDetails.getRole()));

        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(hashPassword(userDetails.getPassword()));
        }

        User updated = userRepository.save(user);
        return ResponseEntity.ok(updated);
    }

    // 5. API BAN / UNBAN
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

    // 6. API XÓA SẠCH NGƯỜI DÙNG
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> adminDeleteUser(@PathVariable Integer id) {
        if (!userRepository.existsById(id)) return ResponseEntity.notFound().build();

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