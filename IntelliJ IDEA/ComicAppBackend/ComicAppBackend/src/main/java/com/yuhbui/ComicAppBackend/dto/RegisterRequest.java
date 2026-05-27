package com.yuhbui.ComicAppBackend.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String displayName;
    private String password;
    private String confirmPassword;
}