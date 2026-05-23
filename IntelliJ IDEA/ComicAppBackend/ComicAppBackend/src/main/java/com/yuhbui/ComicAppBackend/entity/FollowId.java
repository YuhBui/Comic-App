package com.yuhbui.ComicAppBackend.entity;

import java.io.Serializable;
import lombok.Data;

@Data
public class FollowId implements Serializable {
    private Integer userId;
    private Integer comicId;
}