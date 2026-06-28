package com.yuhbui.comicapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloaded_chapters")
public class DownloadedChapter {
    @PrimaryKey
    private int chapterId;
    private int comicId;
    private float chapterNumber;
    private String title;

    public DownloadedChapter() {}

    // Getters và Setters
    public int getChapterId() { return chapterId; }
    public void setChapterId(int chapterId) { this.chapterId = chapterId; }

    public int getComicId() { return comicId; }
    public void setComicId(int comicId) { this.comicId = comicId; }

    public float getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(float chapterNumber) { this.chapterNumber = chapterNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}