package com.yuhbui.ComicAppBackend.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.yuhbui.ComicAppBackend.dto.GoogleLoginRequest;
import com.yuhbui.ComicAppBackend.dto.LoginRequest;
import com.yuhbui.ComicAppBackend.dto.ProfileRequest;
import com.yuhbui.ComicAppBackend.dto.RegisterRequest;
import com.yuhbui.ComicAppBackend.dto.ResetPasswordRequest;
import com.yuhbui.ComicAppBackend.entity.User;
import com.yuhbui.ComicAppBackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // Nhúng dịch vụ gửi thư tự động được đồng bộ từ application.properties
    @Autowired
    private JavaMailSender mailSender;

    // Giữ chính xác chuỗi Web Client ID hệ thống của bạn
    private final String GOOGLE_WEB_CLIENT_ID = "904461562945-dddi1tckgjg94f2m0n0d9n5t4140j8tg.apps.googleusercontent.com";

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

    // 5. API Đăng nhập bằng Google
    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
        try {
            // 1. Cấu hình bộ xác thực Token của Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(GOOGLE_WEB_CLIENT_ID))
                    .build();

            // 2. Kiểm tra tính hợp lệ của chuỗi Token nhận được
            GoogleIdToken idToken = verifier.verify(request.getIdToken());

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                // Lấy thông tin tài khoản Google sau khi giải mã thành công
                String email = payload.getEmail();
                String displayName = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");

                // 3. Tra cứu User trong Database dựa theo Email
                Optional<User> userOpt = userRepository.findByEmail(email);
                User user;

                if (userOpt.isPresent()) {
                    // Nếu tài khoản đã tồn tại từ trước, tiến hành lấy ra để đăng nhập
                    user = userOpt.get();
                } else {
                    // Nếu là lần đầu tiên đăng nhập, tự động tạo mới tài khoản User
                    user = new User();
                    user.setEmail(email);
                    user.setDisplayName(displayName);
                    user.setAvatarUrl(pictureUrl);
                    user.setRole("User");       // Gán quyền mặc định đọc truyện
                    user.setPassword(null);      // Vì đăng nhập bằng Google nên không cần mật khẩu hệ thống

                    user = userRepository.save(user);
                }

                // Loại bỏ hiển thị password khi trả về client để bảo mật dữ liệu
                user.setPassword(null);
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Xác thực thất bại: ID Token không hợp lệ.");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi trong quá trình xử lý: " + e.getMessage());
        }
    }

    // ==========================================
    // CHỨC NĂNG QUÊN MẬT KHẨU & GỬI OTP GMAIL
    // ==========================================

    // 6. API Yêu cầu lấy mã OTP khôi phục -> Tạo mã OTP và gửi thư trực tiếp về Gmail người dùng
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam("email") String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email này không tồn tại trong hệ thống!");
        }

        User user = userOpt.get();

        // 1. Tạo ngẫu nhiên mã số OTP gồm 6 chữ số
        String otp = String.format("%06d", new Random().nextInt(999999));

        // 2. Cập nhật OTP và đặt thời hạn hết hiệu lực là 5 phút tính từ bây giờ
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // 3. Kết nối với hệ thống Gmail SMTP của Google để gửi thư trực tiếp đi
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email); // Gửi trực tiếp tới Gmail của tài khoản yêu cầu
            message.setSubject("[Comic App] Mã OTP Khôi Phục Mật Khẩu");
            message.setText("Chào bạn,\n\n"
                    + "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản trên ứng dụng đọc truyện Comic App.\n"
                    + "Mã OTP xác nhận của bạn là: " + otp + "\n"
                    + "Mã này có hiệu lực sử dụng trong vòng 5 phút. Để bảo mật, vui lòng tuyệt đối không chia sẻ mã này cho bất kỳ ai khác.\n\n"
                    + "Trân trọng,\n"
                    + "Đội ngũ phát triển Comic App.");

            mailSender.send(message); // Kích hoạt lệnh gửi mail

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Hệ thống không thể gửi email do trục trặc máy chủ: " + e.getMessage());
        }

        return ResponseEntity.ok("Mã OTP đã được gửi thành công! Bạn hãy mở hộp thư Gmail lên để kiểm tra.");
    }

    // 7. API Nhận mã OTP và mật khẩu mới -> Đối chiếu kiểm tra -> Đồng bộ băm mật khẩu cập nhật vào DB
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy thông tin tài khoản người dùng!");
        }

        User user = userOpt.get();

        // 1. Kiểm tra đối chiếu xem mã OTP người dùng nhập vào trên app có đúng không
        if (user.getOtpCode() == null || !user.getOtpCode().equals(request.getOtp())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mã OTP nhập vào không chính xác!");
        }

        // 2. Kiểm tra xem mã OTP đã quá hạn sử dụng (5 phút) hay chưa
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mã OTP này đã hết hiệu lực, vui lòng ấn nút gửi lại mã mới!");
        }

        // 3. Sử dụng chính xác hàm hashPassword SHA-256 (Base64) sẵn có của bạn để mã hóa mật khẩu mới tinh
        String hashedPassword = hashPassword(request.getNewPassword());
        user.setPassword(hashedPassword);

        // 4. Reset trống hoàn toàn thông tin OTP trong Database để mã không thể bị tái sử dụng bừa bãi
        user.setOtpCode(null);
        user.setOtpExpiry(null);

        userRepository.save(user);

        return ResponseEntity.ok("Chúc mừng! Đặt lại mật khẩu thành công. Hãy thử đăng nhập ngay bằng mật khẩu mới.");
    }
}