package com.yuhbui.ComicAppBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; // Thêm cái này để hỗ trợ tạo object trống nếu cần

@Data
@AllArgsConstructor
@NoArgsConstructor // Thêm annotation này cho chuẩn bài DTO
public class ComicHomeResponseDTO {
    private Integer comicId;
    private String title;
    private String coverImageUrl;
    private Integer viewCount;
    private Float rating;
    private String status;
    private String latestChapterNumber; // Số chương mới nhất
    private String timeUpdated;         // Thời gian vừa cập nhật
    private Long followCount;           // Số lượt yêu thích
    private Long commentCount;          // Số lượt bình luận

    @com.fasterxml.jackson.annotation.JsonProperty("isFollowed")
    private boolean isFollowed;
}