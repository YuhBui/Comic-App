package com.yuhbui.ComicAppBackend.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/chapters")
public class AdminChapterController {

    @PersistenceContext
    private EntityManager entityManager;

    // 1. LẤY DANH SÁCH CHƯƠNG THEO COMIC ID
    @GetMapping("/comic/{comicId}")
    public ResponseEntity<List<Map<String, Object>>> getChaptersByComic(@PathVariable Integer comicId) {
        String sql = "SELECT ChapterID, ChapterNumber, Title FROM Chapters WHERE ComicID = :comicId ORDER BY ChapterNumber DESC";
        @SuppressWarnings("unchecked")
        List<Object[]> rawData = entityManager.createNativeQuery(sql).setParameter("comicId", comicId).getResultList();
        List<Map<String, Object>> response = new ArrayList<>();
        for (Object[] row : rawData) {
            Map<String, Object> map = new HashMap<>();
            map.put("chapterId", row[0]);
            map.put("chapterNumber", row[1]);
            map.put("title", row[2]);
            response.add(map);
        }
        return ResponseEntity.ok(response);
    }

    // 2. THÊM CHƯƠNG MỚI
    @PostMapping("/comic/{comicId}")
    @Transactional
    public ResponseEntity<?> createChapter(@PathVariable Integer comicId, @RequestParam Double chapterNumber, @RequestParam String title) {
        String sql = "INSERT INTO Chapters (ComicID, ChapterNumber, Title, CreatedAt) VALUES (:comicId, :num, :title, NOW())";
        entityManager.createNativeQuery(sql)
                .setParameter("comicId", comicId)
                .setParameter("num", chapterNumber)
                .setParameter("title", title)
                .executeUpdate();
        return ResponseEntity.ok(Map.of("success", true));
    }

    // 3. SỬA THÔNG TIN CHƯƠNG
    @PutMapping("/{chapterId}")
    @Transactional
    public ResponseEntity<?> updateChapter(@PathVariable Integer chapterId, @RequestParam Double chapterNumber, @RequestParam String title) {
        String sql = "UPDATE Chapters SET ChapterNumber = :num, Title = :title WHERE ChapterID = :chapterId";
        entityManager.createNativeQuery(sql)
                .setParameter("num", chapterNumber)
                .setParameter("title", title)
                .setParameter("chapterId", chapterId)
                .executeUpdate();
        return ResponseEntity.ok(Map.of("success", true));
    }

    // 4. XÓA CHƯƠNG (DỌN SẠCH ẢNH TRƯỚC ĐỂ TRÁNH LỖI KHÓA NGOẠI)
    @DeleteMapping("/{chapterId}")
    @Transactional
    public ResponseEntity<?> deleteChapter(@PathVariable Integer chapterId) {
        entityManager.createNativeQuery("DELETE FROM ChapterImages WHERE ChapterID = :id").setParameter("id", chapterId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM ReadingHistory WHERE LastChapterID = :id").setParameter("id", chapterId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Chapters WHERE ChapterID = :id").setParameter("id", chapterId).executeUpdate();
        return ResponseEntity.ok(Map.of("success", true));
    }

    // 5. LẤY DANH SÁCH CÁC TRANG TRUYỆN THEO CHƯƠNG
    @GetMapping("/{chapterId}/pages")
    public ResponseEntity<List<Map<String, Object>>> getChapterPages(@PathVariable Integer chapterId) {
        String sql = "SELECT ImageID, ImageURL, PageNumber FROM ChapterImages WHERE ChapterID = :chapterId ORDER BY PageNumber ASC";
        @SuppressWarnings("unchecked")
        List<Object[]> rawData = entityManager.createNativeQuery(sql).setParameter("chapterId", chapterId).getResultList();
        List<Map<String, Object>> response = new ArrayList<>();
        for (Object[] row : rawData) {
            Map<String, Object> map = new HashMap<>();
            map.put("imageId", row[0]);
            map.put("imageUrl", row[1]);
            map.put("pageNumber", row[2]);
            response.add(map);
        }
        return ResponseEntity.ok(response);
    }

    // 6. THÊM MỘT TRANG TRUYỆN MỚI (UPLOAD FILE THẬT)
    @PostMapping(value = "/{chapterId}/upload-page", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> uploadChapterPage(@PathVariable Integer chapterId, @RequestParam("file") MultipartFile file) {
        try {
            String uploadDir = "uploads/chapters/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName = "page_" + chapterId + "_" + System.currentTimeMillis() + ".jpg";
            Path filePath = Paths.get(uploadDir + fileName);
            Files.write(filePath, file.getBytes());

            String imageUrl = "http://localhost:8080/uploads/chapters/" + fileName;

            // ĐG ĐÃ SỬA: Sử dụng mốc thời gian làm PageNumber để ép tính độc nhất, không lo bị nghẽn luồng trùng lặp số trang
            long uniquePageNumber = System.currentTimeMillis() % 100000000L;

            String insertSql = "INSERT INTO ChapterImages (ChapterID, ImageURL, PageNumber) VALUES (:chapterId, :url, :page)";
            entityManager.createNativeQuery(insertSql)
                    .setParameter("chapterId", chapterId)
                    .setParameter("url", imageUrl)
                    .setParameter("page", uniquePageNumber)
                    .executeUpdate();

            return ResponseEntity.ok(Map.of("success", true, "imageUrl", imageUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi lưu trang truyện: " + e.getMessage());
        }
    }

    // 7. XÓA MỘT TRANG TRUYỆN CHỈ ĐỊNH
    @DeleteMapping("/pages/{imageId}")
    @Transactional
    public ResponseEntity<?> deleteChapterPage(@PathVariable Integer imageId) {
        entityManager.createNativeQuery("DELETE FROM ChapterImages WHERE ImageID = :id").setParameter("id", imageId).executeUpdate();
        return ResponseEntity.ok(Map.of("success", true));
    }
}