package com.yuhbui.ComicAppBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Comment_Reports")
@Data
public class CommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReportID")
    private Integer reportId;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "CommentID", nullable = false)
    private Integer commentId;

    @Column(name = "Reason", nullable = false)
    private String reason;

    @Column(name = "IsResolved")
    private Boolean isResolved = false;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}