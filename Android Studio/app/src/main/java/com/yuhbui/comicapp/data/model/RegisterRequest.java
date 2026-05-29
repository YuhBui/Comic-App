package com.yuhbui.comicapp.data.model;

public class RegisterRequest {
    private String email;
    private String displayName;
    private String password;
    private String confirmPassword;
    private String avatarUrl;

    public RegisterRequest(String email, String displayName, String password, String confirmPassword) {
        this.email = email;
        this.displayName = displayName;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    // Các hàm Getter và Setter
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}