package com.yuhbui.ComicAppBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Comment_Interactions")
@Data
public class CommentInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InteractionID")
    private Integer interactionId;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "CommentID", nullable = false)
    private Integer commentId;

    // Quy ước: 1 là Like, -1 là Dislike
    @Column(name = "InteractionType", nullable = false)
    private Integer interactionType;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}