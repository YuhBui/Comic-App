package com.yuhbui.ComicAppBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
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
    private String password; // Trong đồ án thực tế, bạn nên mã hóa cột này (như MD5 hoặc BCrypt)

    @Column(name = "AvatarUrl")
    private String avatarUrl;

    @Column(name = "DisplayName", nullable = false)
    private String displayName;

    @Column(name = "Role")
    private String role = "User";

    @Column(name = "Status")
    private String status = "Active";

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}