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

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi băm mật khẩu", e);
        }
    }

    // 1. API LẤY DANH SÁCH NGƯỜI DÙNG TÍCH HỢP PHÂN TRANG (10 NGƯỜI / TRANG)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        String baseWhere = " WHERE 1=1";
        if (keyword != null && !keyword.trim().isEmpty()) {
            baseWhere += " AND (DisplayName LIKE '%" + keyword.trim() + "%' OR Email LIKE '%" + keyword.trim() + "%')";
        }
        if (role != null && !role.trim().isEmpty() && !role.equalsIgnoreCase("Tất cả")) {
            baseWhere += " AND Role = '" + role.trim() + "'";
        }

        String countSql = "SELECT COUNT(*) FROM Users" + baseWhere;
        long totalItems = ((Number) entityManager.createNativeQuery(countSql).getSingleResult()).longValue();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        int offset = page * size;
        String selectSql = "SELECT * FROM Users" + baseWhere + " ORDER BY CreatedAt DESC LIMIT " + size + " OFFSET " + offset;

        @SuppressWarnings("unchecked")
        List<User> users = entityManager.createNativeQuery(selectSql, User.class).getResultList();
        users.forEach(u -> u.setPassword(null));

        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }

    // 2. ĐÃ SỬA: API THÊM MỚI NGƯỜI DÙNG (Dùng @RequestParam nhận chuỗi sạch thuần túy, loại bỏ hoàn toàn lỗi trùng ảo)
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> adminCreateUser(
            @RequestParam("displayName") String displayName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") String role,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
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
            user.setRole(role);
            user.setStatus("Active");
            user.setCreatedAt(LocalDateTime.now());

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
            saved.setPassword(null);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi hệ thống khi tạo tài khoản: " + e.getMessage());
        }
    }

    // 3. ĐÃ SỬA: API CẬP NHẬT MULTIPART (Dùng @RequestParam dứt điểm hoàn toàn lỗi sập mạng cấu trúc timestamp)
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
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi hệ thống khi cập nhật: " + e.getMessage());
        }
    }

    // 4. API CẬP NHẬT JSON BODY (Phục vụ Dialog sửa nhanh của màn hình danh sách)
    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> adminUpdateUser(@PathVariable Integer id, @RequestBody User userDetails) {
        Optional<User> userOpt = userRepository.findById(id);
        if (!userOpt.isPresent()) return ResponseEntity.notFound().build();

        User user = userOpt.get();

        Optional<User> checkEmail = userRepository.findByEmail(userDetails.getEmail());
        if (checkEmail.isPresent() && !checkEmail.get().getUserId().equals(id)) {
            return ResponseEntity.badRequest().body("Email mới đã tồn tại trên hệ thống!");
        }
        Optional<User> checkName = userRepository.findByDisplayName(userDetails.getDisplayName());
        if (checkName.isPresent() && !checkName.get().getUserId().equals(id)) {
            return ResponseEntity.badRequest().body("Tên hiển thị mới đã tồn tại!");
        }

        user.setEmail(userDetails.getEmail());
        user.setDisplayName(userDetails.getDisplayName());
        user.setRole(userDetails.getRole());

        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(hashPassword(userDetails.getPassword()));
        }

        User updated = userRepository.save(user);
        updated.setPassword(null);
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