package com.yuhbui.ComicAppBackend.controller;

import com.yuhbui.ComicAppBackend.dto.LoginRequest;
import com.yuhbui.ComicAppBackend.dto.RegisterRequest;
import com.yuhbui.ComicAppBackend.entity.User;
import com.yuhbui.ComicAppBackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}