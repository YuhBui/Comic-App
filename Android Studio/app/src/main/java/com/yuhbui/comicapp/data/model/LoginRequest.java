package com.yuhbui.comicapp.data.model;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    // Khởi tạo (Constructor) giúp truyền dữ liệu nhanh hơn
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Bạn có thể dùng Alt + Insert để tạo Getter và Setter ở dưới này
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}