package com.yuhbui.ComicAppBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ReadingHistory")
@Data
public class ReadingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HistoryID")
    private Integer historyId;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "ComicID", nullable = false)
    private Integer comicId;

    @Column(name = "LastChapterID")
    private Integer lastChapterId;

    @Column(name = "LastPage")
    private Integer lastPage = 0;

    @Column(name = "UpdatedAt", insertable = false, updatable = false)
    private java.time.LocalDateTime updatedAt;
}