package com.yuhbui.ComicAppBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Comments")
@Data
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CommentID")
    private Integer commentId;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "ComicID")
    private Integer comicId;

    @Column(name = "ChapterID")
    private Integer chapterId;

    // Nếu là bình luận phản hồi, trường này sẽ lưu ID của bình luận cha
    @Column(name = "ParentCommentID")
    private Integer parentCommentId;

    @Column(name = "Content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "ReplyCount")
    private Integer replyCount = 0;

    @Column(name = "LikeCount")
    private Integer likeCount = 0;

    @Column(name = "DislikeCount")
    private Integer dislikeCount = 0;

    @Column(name = "ReportCount")
    private Integer reportCount = 0;

    @Column(name = "IsDeleted")
    private Boolean isDeleted = false;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;
}