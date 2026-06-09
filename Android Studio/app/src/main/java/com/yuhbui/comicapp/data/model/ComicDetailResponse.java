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

    @SerializedName("latestChapterNumber")
    private String latestChapterNumber;

    @SerializedName("timeUpdated")
    private String timeUpdated;

    public Comic getComic() { return comic; }
    public String getGenres() { return genres; }
    public int getFavoriteCount() { return favoriteCount; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public String getLatestChapterNumber() { return latestChapterNumber; }
    public String getTimeUpdated() { return timeUpdated; }
}