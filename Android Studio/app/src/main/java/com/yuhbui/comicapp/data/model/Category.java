package com.yuhbui.comicapp.data.model;

import com.google.gson.annotations.SerializedName;

public class Category {
    @SerializedName("categoryId")
    private int categoryId;
    @SerializedName("name")
    private String name;

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCategoryId() { return categoryId; }
    public String getName() { return name; }
}