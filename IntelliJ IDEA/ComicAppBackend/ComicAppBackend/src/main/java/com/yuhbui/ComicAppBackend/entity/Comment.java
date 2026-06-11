package com.yuhbui.ComicAppBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "Comments")
@Data
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CommentID")
    @JsonProperty("commentId")
    private Integer commentId;

    @Column(name = "UserID", nullable = false)
    @JsonProperty("userId")
    private Integer userId;

    @Column(name = "ComicID")
    @JsonProperty("comicId")
    private Integer comicId;

    @Column(name = "ChapterID")
    @JsonProperty("chapterId")
    private Integer chapterId;

    @Column(name = "ParentCommentID")
    @JsonProperty("parentCommentId")
    private Integer parentCommentId;

    @Column(name = "Content", nullable = false, columnDefinition = "TEXT")
    @JsonProperty("content")
    private String content;

    @Column(name = "ReplyCount")
    @JsonProperty("replyCount")
    private Integer replyCount = 0;

    @Column(name = "LikeCount")
    @JsonProperty("likeCount")
    private Integer likeCount = 0;

    @Column(name = "DislikeCount")
    @JsonProperty("dislikeCount")
    private Integer dislikeCount = 0;

    @Column(name = "ReportCount")
    @JsonProperty("reportCount")
    private Integer reportCount = 0;

    @Column(name = "IsDeleted")
    @JsonProperty("isDeleted")
    private Boolean isDeleted = false;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    @JsonProperty("createdAt")
    private java.time.LocalDateTime createdAt;
}