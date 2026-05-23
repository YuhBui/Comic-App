package com.yuhbui.comicapp.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Comic implements Serializable {
    @SerializedName("comicId")
    private int comicId;

    @SerializedName("title")
    private String title;

    @SerializedName("author")
    private String author;

    @SerializedName("description")
    private String description;

    @SerializedName("coverImageUrl")
    private String coverImageUrl;

    @SerializedName("viewCount")
    private int viewCount;

    @SerializedName("rating")
    private float rating;

    @SerializedName("status")
    private String status; // Ví dụ: "Ongoing", "Completed"

    @SerializedName("isHidden")
    private boolean isHidden;

    @SerializedName("createdAt")
    private String createdAt; // Dùng String để dễ dàng nhận dữ liệu ngày tháng từ API

    @SerializedName("latestChapterNumber")
    private String latestChapterNumber;

    @SerializedName("timeUpdated")
    private String timeUpdated;

    @SerializedName("followCount")
    private long followCount;

    @SerializedName("commentCount")
    private long commentCount;

    public Comic() {
    }

    public int getComicId() {
        return comicId;
    }

    public void setComicId(int comicId) {
        this.comicId = comicId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLatestChapterNumber() {
        return latestChapterNumber;
    }

    public void setLatestChapterNumber(String latestChapterNumber) {
        this.latestChapterNumber = latestChapterNumber;
    }

    public String getTimeUpdated() {
        return timeUpdated;
    }

    public void setTimeUpdated(String timeUpdated) {
        this.timeUpdated = timeUpdated;
    }

    public long getFollowCount() {
        return followCount;
    }

    public void setFollowCount(long followCount) {
        this.followCount = followCount;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }
}
