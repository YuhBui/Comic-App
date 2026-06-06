package com.yuhbui.ComicAppBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Entity
@Table(name = "Users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "Email", nullable = false, unique = true)
    private String email;

    @Column(name = "PasswordHash", nullable = false)
    // SỬA: Chỉ cho phép Frontend gửi mật khẩu lên, cấm trả về trong chuỗi JSON.
    // Giúp loại bỏ hoàn toàn các hàm setPassword(null) gây lỗi sập DB.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(name = "AvatarUrl")
    private String avatarUrl;

    @Column(name = "DisplayName", nullable = false, unique = true)
    private String displayName;

    @Column(name = "Role")
    private String role = "User";

    @Column(name = "Status")
    private String status = "Active";

    // SỬA: Cấm Hibernate tự ý thêm/sửa cột này trong SQL.
    // MySQL sẽ tự động gán CURRENT_TIMESTAMP khi thêm mới nên an toàn tuyệt đối!
    @Column(name = "CreatedAt", insertable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}