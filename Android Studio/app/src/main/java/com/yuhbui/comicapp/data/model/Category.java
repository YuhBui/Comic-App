package com.yuhbui.comicapp.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Category implements Serializable {
    @SerializedName("categoryId")
    private int categoryId;

    @SerializedName("name")
    private String name;

    // Hàm khởi tạo không tham số (Bắt buộc cho Gson/Retrofit)
    public Category() {
    }

    // Hàm khởi tạo nhận tham số Tên thể loại (Giải quyết lỗi gạch đỏ)
    public Category(String name) {
        this.name = name;
    }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}