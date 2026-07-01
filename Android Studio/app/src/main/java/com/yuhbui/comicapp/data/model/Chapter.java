package com.yuhbui.comicapp.data.model;

import com.google.gson.annotations.SerializedName;

public class Chapter {
    @SerializedName("chapterId")
    private int chapterId;

    @SerializedName("comicId")
    private int comicId;

    @SerializedName("chapterNumber")
    private float chapterNumber;

    @SerializedName("title")
    private String title;

    public Chapter() {
    }

    public int getChapterId() {
        return chapterId;
    }

    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }

    public int getComicId() {
        return comicId;
    }

    public void setComicId(int comicId) {
        this.comicId = comicId;
    }

    public float getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(float chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


}