package com.yuhbui.ComicAppBackend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Categories")
@Data // Lombok tự sinh tự động các hàm Getter, Setter, toString, equals, hashCode...
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryID")
    private Integer categoryId;

    @Column(name = "Name", nullable = false, unique = true, length = 100)
    private String name;
}