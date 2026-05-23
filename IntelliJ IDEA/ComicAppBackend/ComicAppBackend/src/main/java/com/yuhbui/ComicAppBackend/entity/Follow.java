package com.yuhbui.ComicAppBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Follows")
@IdClass(FollowId.class)
@Data
public class Follow {

    @Id
    @Column(name = "UserID")
    private Integer userId;

    @Id
    @Column(name = "ComicID")
    private Integer comicId;

    @Column(name = "IsNotificationOn")
    private Boolean isNotificationOn = true;

    @Column(name = "FollowedAt", insertable = false, updatable = false)
    private LocalDateTime followedAt;
}