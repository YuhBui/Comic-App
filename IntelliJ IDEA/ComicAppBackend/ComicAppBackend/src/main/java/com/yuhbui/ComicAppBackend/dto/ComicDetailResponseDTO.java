package com.yuhbui.ComicAppBackend.dto;

import com.yuhbui.ComicAppBackend.entity.Comic;
import lombok.Data;

@Data
public class ComicDetailResponseDTO {
    private Comic comic;
    private String genres;
    private int favoriteCount;
    private boolean isFavorite;

    private String latestChapter;
    private String latestChapterUpdatedAt;
    private int commentCount;

    private String latestChapterNumber;
    private String timeUpdated;
}