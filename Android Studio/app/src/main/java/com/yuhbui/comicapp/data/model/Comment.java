package com.yuhbui.comicapp.data.model;

import com.google.gson.annotations.SerializedName;

public class Comment {
    @SerializedName("commentId")
    private int commentId;

    @SerializedName("userId")
    private int userId;

    @SerializedName("comicId")
    private Integer comicId;

    @SerializedName("chapterId")
    private Integer chapterId;

    @SerializedName("parentCommentId")
    private Integer parentCommentId;

    @SerializedName("content")
    private String content;

    @SerializedName("likeCount")
    private int likeCount;

    @SerializedName("dislikeCount")
    private int dislikeCount;

    @SerializedName("isLiked")
    private boolean isLiked;

    @SerializedName("isDisliked")
    private boolean isDisliked;

    @SerializedName("replyCount")
    private int replyCount;

    @SerializedName("reportCount")
    private int reportCount;

    @SerializedName("userDisplayName")
    private String userDisplayName;

    @SerializedName("userAvatarUrl")
    private String userAvatarUrl;

    @SerializedName("chapterName")
    private String chapterName;
    public Comment() {

    }
    public int getCommentId() {
        return commentId;
    }

    public void setCommentId(int commentId) {
        this.commentId = commentId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Integer getComicId() {
        return comicId;
    }

    public void setComicId(Integer comicId) {
        this.comicId = comicId;
    }

    public Integer getChapterId() {
        return chapterId;
    }

    public void setChapterId(Integer chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Integer parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(int dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public boolean isDisliked() {
        return isDisliked;
    }

    public void setDisliked(boolean disliked) {
        isDisliked = disliked;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    public String getChapterName() {
        return chapterName;
    }

    public void setChapterName(String chapterName) {
        this.chapterName = chapterName;
    }
}