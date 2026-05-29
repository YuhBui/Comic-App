package com.yuhbui.ComicAppBackend.dto;

import lombok.Data;

@Data
public class ProfileRequest {
    private String email;
    private String displayName;
    private String password;
    private String confirmPassword;
    private String avatarUrl;
}