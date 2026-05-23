package com.yuhbui.comicapp.data.model;

import com.google.gson.annotations.SerializedName;

public class ComicDetailResponse {
    @SerializedName("comic")
    private Comic comic;

    @SerializedName("genres")
    private String genres;

    @SerializedName("favoriteCount")
    private int favoriteCount;

    @SerializedName("favorite")
    private boolean isFavorite;

    // Tạo Getter/Setter cho cả 4 trường này nhé!
    public Comic getComic() { return comic; }
    public String getGenres() { return genres; }
    public int getFavoriteCount() { return favoriteCount; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}