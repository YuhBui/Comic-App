package com.yuhbui.comicapp.data.model;

import com.google.gson.annotations.SerializedName;

public class ReadingHistory {

    @SerializedName("historyId")
    private int historyId;

    @SerializedName("userId")
    private int userId;

    @SerializedName("comicId")
    private int comicId;

    @SerializedName("lastChapterId")
    private int lastChapterId;

    @SerializedName("lastPage")
    private int lastPage;

    @SerializedName("updatedAt")
    private String updatedAt;

    // --- Getter và Setter ---

    public int getHistoryId() {
        return historyId;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getComicId() {
        return comicId;
    }

    public void setComicId(int comicId) {
        this.comicId = comicId;
    }

    public int getLastChapterId() {
        return lastChapterId;
    }

    public void setLastChapterId(int lastChapterId) {
        this.lastChapterId = lastChapterId;
    }

    public int getLastPage() {
        return lastPage;
    }

    public void setLastPage(int lastPage) {
        this.lastPage = lastPage;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}