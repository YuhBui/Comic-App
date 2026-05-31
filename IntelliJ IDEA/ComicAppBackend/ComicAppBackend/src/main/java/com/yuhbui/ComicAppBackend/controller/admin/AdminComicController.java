package com.yuhbui.ComicAppBackend.controller.admin;

import com.yuhbui.ComicAppBackend.entity.Comic;
import com.yuhbui.ComicAppBackend.repository.ComicRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/comics")
public class AdminComicController {

    @Autowired
    private ComicRepository comicRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping
    public ResponseEntity<List<Comic>> getAllComicsForAdmin() {
        return ResponseEntity.ok(comicRepository.findAll());
    }

    // Nâng cấp API thêm truyện: Tự động kết nối thể loại vào bảng trung gian
    @PostMapping
    @Transactional
    public ResponseEntity<Comic> createComic(@RequestBody Comic comic, @RequestParam(required = false) Integer categoryId) {
        comic.setViewCount(0);
        comic.setRating(0.0f);
        comic.setIsHidden(false);
        Comic savedComic = comicRepository.save(comic);

        // Nếu Admin có chọn thể loại, thêm bản ghi liên kết vào bảng Comic_Categories
        if (categoryId != null && categoryId > 0) {
            String sql = "INSERT INTO Comic_Categories (ComicID, CategoryID) VALUES (:comicId, :catId)";
            entityManager.createNativeQuery(sql)
                    .setParameter("comicId", savedComic.getComicId())
                    .setParameter("catId", categoryId)
                    .executeUpdate();
        }
        return ResponseEntity.ok(savedComic);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Comic> updateComic(@PathVariable Integer id, @RequestBody Comic comicDetails, @RequestParam(required = false) Integer categoryId) {
        Optional<Comic> comicOpt = comicRepository.findById(id);
        if (!comicOpt.isPresent()) return ResponseEntity.notFound().build();

        Comic comic = comicOpt.get();
        comic.setTitle(comicDetails.getTitle());
        comic.setAuthor(comicDetails.getAuthor());
        comic.setDescription(comicDetails.getDescription());
        if(comicDetails.getCoverImageUrl() != null) {
            comic.setCoverImageUrl(comicDetails.getCoverImageUrl());
        }
        comic.setStatus(comicDetails.getStatus());
        Comic updatedComic = comicRepository.save(comic);

        // Cập nhật lại thể loại truyện
        if (categoryId != null && categoryId > 0) {
            entityManager.createNativeQuery("DELETE FROM Comic_Categories WHERE ComicID = :comicId")
                    .setParameter("comicId", id).executeUpdate();
            entityManager.createNativeQuery("INSERT INTO Comic_Categories (ComicID, CategoryID) VALUES (:comicId, :catId)")
                    .setParameter("comicId", id).setParameter("catId", categoryId).executeUpdate();
        }

        return ResponseEntity.ok(updatedComic);
    }

    // THAY CHỨC NĂNG ẨN THÀNH XÓA TRUYỆN CỨNG HOÀN TOÀN KHỎI DATABASE
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteComic(@PathVariable Integer id) {
        if (!comicRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // Xóa các liên kết khóa ngoại trước để tránh lỗi ràng buộc Integrity
        entityManager.createNativeQuery("DELETE FROM Comic_Categories WHERE ComicID = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Chapters WHERE ComicID = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM ReadingHistory WHERE ComicID = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Follow WHERE ComicID = :id").setParameter("id", id).executeUpdate();

        comicRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // API TẢI ẢNH BÌA TRUYỆN QUA THIẾT BỊ ĐIỆN THOẠI
    @PostMapping("/upload-cover")
    public ResponseEntity<?> uploadComicCover(@RequestParam("file") MultipartFile file) {
        try {
            String uploadDir = "uploads/covers/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName = "cover_" + System.currentTimeMillis() + ".jpg";
            Path filePath = Paths.get(uploadDir + fileName);
            Files.write(filePath, file.getBytes());

            // Trả về địa chỉ localhost độc lập mạng phục vụ lệnh adb reverse của bạn
            String coverUrl = "http://localhost:8080/uploads/covers/" + fileName;
            return ResponseEntity.ok(Map.of("coverUrl", coverUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi lưu tệp: " + e.getMessage());
        }
    }
}