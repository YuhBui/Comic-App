package com.yuhbui.ComicAppBackend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentResponseDTO {
    private Integer commentId;
    private Integer userId;
    private Integer comicId;
    private Integer chapterId;
    private Integer parentCommentId;
    private String content;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer replyCount;
    private Integer reportCount;
    private Boolean isDeleted;
    private LocalDateTime createdAt;

    private String userDisplayName;
    private String userAvatarUrl;
    private String chapterName;

    @com.fasterxml.jackson.annotation.JsonProperty("isLiked")
    private boolean isLiked;

    @com.fasterxml.jackson.annotation.JsonProperty("isDisliked")
    private boolean isDisliked;
}