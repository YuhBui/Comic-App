package com.yuhbui.ComicAppBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Chapters")
@Data
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ChapterID")
    private Integer chapterId;

    // Chỉ lưu ID của truyện để dễ dàng truy xuất (tránh lỗi vòng lặp JSON)
    @Column(name = "ComicID", nullable = false)
    private Integer comicId;

    @Column(name = "ChapterNumber", nullable = false)
    private Float chapterNumber;

    @Column(name = "Title")
    private String title;

    @Column(name = "ViewCount")
    private Integer viewCount = 0;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}