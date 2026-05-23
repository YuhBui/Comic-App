package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    // Kế thừa toàn bộ các hàm cơ bản như findAll(), findById()... tự động
}