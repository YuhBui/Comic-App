package com.yuhbui.ComicAppBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Comics")
@Data // Lombok tự động tạo Getter, Setter, Constructor
public class Comic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ComicID")
    private Integer comicId;

    @Column(name = "Title", nullable = false)
    private String title;

    @Column(name = "Author")
    private String author;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "CoverImageUrl")
    private String coverImageUrl;

    @Column(name = "ViewCount")
    private Integer viewCount = 0;

    @Column(name = "Rating")
    private Float rating = 0.0f;

    @Column(name = "Status")
    private String status = "Ongoing";

    @Column(name = "IsHidden")
    private Boolean isHidden = false;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}