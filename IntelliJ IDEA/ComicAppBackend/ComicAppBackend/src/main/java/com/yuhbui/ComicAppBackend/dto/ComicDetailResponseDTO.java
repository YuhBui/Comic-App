package com.yuhbui.ComicAppBackend.dto;

import com.yuhbui.ComicAppBackend.entity.Comic;
import lombok.Data;

@Data
public class ComicDetailResponseDTO {
    private Comic comic;
    private String genres;        // Chuỗi thể loại (VD: "Hành động, Hài hước")
    private int favoriteCount;    // Lấy tổng số dòng trong bảng Follows của truyện này
    private boolean isFavorite;   // Trạng thái người dùng hiện tại đã thích chưa
}