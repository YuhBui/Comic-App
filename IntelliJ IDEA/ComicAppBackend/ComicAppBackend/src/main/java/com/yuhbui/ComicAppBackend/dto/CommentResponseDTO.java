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
    private Boolean isDeleted;
    private LocalDateTime createdAt;

    // --- CÁC TRƯỜNG THÔNG TIN HIỂN THỊ MỚI ---
    private String userDisplayName;  // Tên thật người dùng
    private String userAvatarUrl;     // Link ảnh đại diện
    private String chapterName;       // Tên chapter (VD: "Chương 1", "Chương 2.5")
}