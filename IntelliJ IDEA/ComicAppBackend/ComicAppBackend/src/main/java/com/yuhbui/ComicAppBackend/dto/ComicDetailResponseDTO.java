package com.yuhbui.ComicAppBackend.dto;

import com.yuhbui.ComicAppBackend.entity.Comic;
import lombok.Data;

@Data
public class ComicDetailResponseDTO {
    private Comic comic;
    private String genres;        // Chuỗi thể loại (VD: "Hành động, Hài hước")
    private int favoriteCount;    // Lấy tổng số dòng trong bảng Follows của truyện này
    private boolean isFavorite;   // Trạng thái người dùng hiện tại đã thích chưa

    // BỔ SUNG CÁC TRƯỜNG DƯỚI ĐÂY
    private String latestChapter;          // Số chương mới nhất (VD: "Chương 12")
    private String latestChapterUpdatedAt; // Thời gian cập nhật chương mới nhất
    private int commentCount;              // Tổng số bình luận của truyện

    private String latestChapterNumber;
    private String timeUpdated;
}