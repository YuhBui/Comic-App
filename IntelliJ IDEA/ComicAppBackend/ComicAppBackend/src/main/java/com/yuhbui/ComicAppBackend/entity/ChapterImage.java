package com.yuhbui.ComicAppBackend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "ChapterImages")
@Data
public class ChapterImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ImageID")
    private Integer imageId;

    @Column(name = "ChapterID", nullable = false)
    private Integer chapterId;

    @Column(name = "ImageUrl", nullable = false)
    private String imageUrl;

    @Column(name = "PageNumber", nullable = false)
    private Integer pageNumber;
}