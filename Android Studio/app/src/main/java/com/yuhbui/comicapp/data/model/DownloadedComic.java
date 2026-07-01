package com.yuhbui.comicapp.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloaded_comics")
public class DownloadedComic {
    @PrimaryKey
    private int comicId;
    private String title;
    private String localCoverPath;
    private String author;
    private String description;
    private String genres;

    public DownloadedComic() {}

    // Getters và Setters
    public int getComicId() { return comicId; }
    public void setComicId(int comicId) { this.comicId = comicId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocalCoverPath() { return localCoverPath; }
    public void setLocalCoverPath(String localCoverPath) { this.localCoverPath = localCoverPath; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }
}