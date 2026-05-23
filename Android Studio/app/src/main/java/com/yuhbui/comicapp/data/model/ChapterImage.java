package com.yuhbui.comicapp.data.model;

import com.google.gson.annotations.SerializedName;

public class ChapterImage {
    @SerializedName("imageUrl")
    private String imageUrl;

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}