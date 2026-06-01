package com.yuhbui.ComicAppBackend.controller;

import com.yuhbui.ComicAppBackend.entity.Category;
import com.yuhbui.ComicAppBackend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository; // Inject Repository xịn vào đây

    @GetMapping
    public List<Category> getAllCategories() {
        // Hàm findAll() sẽ tự động chạy lệnh: SELECT * FROM Categories và trả về danh sách
        return categoryRepository.findAll();
    }

    @PostMapping
    public Category createCategory(@RequestBody Category category) {
        category.setCategoryId(null);
        return categoryRepository.save(category);
    }
}