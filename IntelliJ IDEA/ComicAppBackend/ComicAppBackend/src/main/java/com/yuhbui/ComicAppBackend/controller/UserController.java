package com.yuhbui.ComicAppBackend.controller;

import com.yuhbui.ComicAppBackend.dto.LoginRequest;
import com.yuhbui.ComicAppBackend.dto.ProfileRequest;
import com.yuhbui.ComicAppBackend.dto.RegisterRequest;
import com.yuhbui.ComicAppBackend.entity.User;
import com.yuhbui.ComicAppBackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash); // Trả về chuỗi để lưu vào Database
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi hệ thống: Không thể băm mật khẩu", e);
        }
    }

    // 1. API Đăng ký tài khoản
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        // 1. Kiểm tra mật khẩu nhập 2 lần có khớp nhau không
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mật khẩu xác nhận không khớp!");
        }

        // 2. Kiểm tra xem email đã tồn tại chưa
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email đã được sử dụng!");
        }

        // 3. Kiểm tra xem display name (tên hiển thị) đã tồn tại chưa
        if (userRepository.findByDisplayName(request.getDisplayName()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tên hiển thị đã được sử dụng, vui lòng chọn tên khác!");
        }

        // 4. Băm mật khẩu
        String hashedPassword = hashPassword(request.getPassword());

        // 5. Tạo và lưu User mới
        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setDisplayName(request.getDisplayName());
        newUser.setPassword(hashedPassword); // Lưu mật khẩu đã bị băm

        User savedUser = userRepository.save(newUser);
        return ResponseEntity.ok(savedUser);
    }

    // 2. API Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // BƯỚC QUAN TRỌNG: Băm mật khẩu người dùng vừa gõ vào (VD: băm số "123")
            String hashedInput = hashPassword(loginRequest.getPassword());

            // Đem chuỗi vừa băm đối chiếu với chuỗi mã hóa đang lưu trong Database
            if (hashedInput.equals(user.getPassword())) {
                user.setPassword(null);
                return ResponseEntity.ok(user); // Trả về thông tin user nếu khớp
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sai email hoặc mật khẩu!");
    }

    // 3. API Lấy thông tin chi tiết User theo ID để hiển thị lên màn hình Profile
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") Integer id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(null); // Ẩn mật khẩu đã băm vì lý do bảo mật
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy người dùng!");
    }

    // 4. API Cập nhật thông tin hồ sơ cá nhân
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable("id") Integer id, @RequestBody ProfileRequest request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy người dùng!");
        }

        User user = userOpt.get();

        // Kiểm tra độc nhất Email (nếu người dùng thay đổi email khác)
        Optional<User> existingEmail = userRepository.findByEmail(request.getEmail());
        if (existingEmail.isPresent() && !existingEmail.get().getUserId().equals(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email đã được sử dụng bởi tài khoản khác!");
        }

        // Kiểm tra độc nhất DisplayName (tên hiển thị)
        Optional<User> existingName = userRepository.findByDisplayName(request.getDisplayName());
        if (existingName.isPresent() && !existingName.get().getUserId().equals(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tên hiển thị đã được sử dụng bởi tài khoản khác!");
        }

        // Kiểm tra nếu có ý định thay đổi mật khẩu
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mật khẩu xác nhận không khớp!");
            }
            // Thực hiện băm mật khẩu mới bằng SHA-256 (đồng bộ với hàm hashPassword sẵn có của bạn)
            user.setPassword(hashPassword(request.getPassword()));
        }

        // Cập nhật các thông tin cơ bản khác
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName());

        // Cập nhật Avatar nếu có gửi lên
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User updatedUser = userRepository.save(user);
        updatedUser.setPassword(null); // Ẩn mật khẩu trước khi trả về
        return ResponseEntity.ok(updatedUser);
    }

    // API Upload Avatar nội bộ (ĐÃ NÂNG CẤP)
    @PostMapping("/upload-avatar/{id}")
    public ResponseEntity<?> uploadAvatar(@PathVariable("id") Integer id, @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File không hợp lệ hoặc trống!");
            }

            String uploadDir = "uploads/avatars/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // SỬA TẠI ĐÂY: Thêm System.currentTimeMillis() để tên file luôn luôn thay đổi sau mỗi lần upload
            String fileName = id + "_avatar_" + System.currentTimeMillis() + ".jpg";
            Path filePath = uploadPath.resolve(fileName);

            // Ghi file vật lý vào ổ đĩa
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // URL này giờ đây sẽ liên tục thay đổi (Ví dụ: .../1_avatar_171542352.jpg)
            String avatarUrl = "http://localhost:8080/uploads/avatars/" + fileName;

            // Cập nhật CSDL ngay lập tức
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setAvatarUrl(avatarUrl);
                userRepository.save(user); // Lưu chuỗi URL mới tinh vào Database
            }

            // Trả về JSON để Android đọc được
            return ResponseEntity.ok(java.util.Map.of("avatarUrl", avatarUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi hệ thống khi lưu file: " + e.getMessage());
        }
    }
}