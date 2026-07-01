package com.yuhbui.comicapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloaded_images")
public class DownloadedImage {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int chapterId;
    private String localFilePath;
    private int position;

    public DownloadedImage() {}

    // Getters và Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getChapterId() { return chapterId; }
    public void setChapterId(int chapterId) { this.chapterId = chapterId; }

    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}