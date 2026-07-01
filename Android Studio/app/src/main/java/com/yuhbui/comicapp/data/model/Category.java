package com.yuhbui.comicapp.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Category implements Serializable {
    @SerializedName("categoryId")
    private int categoryId;

    @SerializedName("name")
    private String name;

    public Category() {
    }

    public Category(String name) {
        this.name = name;
    }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}