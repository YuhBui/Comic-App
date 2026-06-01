package com.yuhbui.ComicAppBackend.controller.admin;

import com.yuhbui.ComicAppBackend.entity.Comic;
import com.yuhbui.ComicAppBackend.repository.ComicRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
    public ResponseEntity<Comic> createComic(@RequestBody Comic comic, @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds) {

        // BỔ SUNG DÒNG NÀY: Sửa lỗi 500 do ID mặc định từ Android gửi lên bằng 0
        comic.setComicId(null);

        comic.setViewCount(0);
        comic.setRating(0.0f);
        comic.setIsHidden(false);
        Comic savedComic = comicRepository.save(comic);

        // Đồng bộ chèn danh sách đa thể loại vào bảng trung gian Comic_Categories
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Integer catId : categoryIds) {
                entityManager.createNativeQuery("INSERT INTO Comic_Categories (ComicID, CategoryID) VALUES (:comicId, :catId)")
                        .setParameter("comicId", savedComic.getComicId())
                        .setParameter("catId", catId)
                        .executeUpdate();
            }
        }
        return ResponseEntity.ok(savedComic);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Comic> updateComic(@PathVariable Integer id, @RequestBody Comic comicDetails, @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds) {
        Optional<Comic> comicOpt = comicRepository.findById(id);
        if (!comicOpt.isPresent()) return ResponseEntity.notFound().build();

        Comic comic = comicOpt.get();
        comic.setTitle(comicDetails.getTitle());
        comic.setAuthor(comicDetails.getAuthor());
        comic.setDescription(comicDetails.getDescription());
        if (comicDetails.getCoverImageUrl() != null && !comicDetails.getCoverImageUrl().isEmpty()) {
            comic.setCoverImageUrl(comicDetails.getCoverImageUrl());
        }
        comic.setStatus(comicDetails.getStatus());
        Comic updatedComic = comicRepository.save(comic);

        // Làm sạch và cập nhật lại mối liên kết đa thể loại
        entityManager.createNativeQuery("DELETE FROM Comic_Categories WHERE ComicID = :comicId")
                .setParameter("comicId", id).executeUpdate();

        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Integer catId : categoryIds) {
                entityManager.createNativeQuery("INSERT INTO Comic_Categories (ComicID, CategoryID) VALUES (:comicId, :catId)")
                        .setParameter("comicId", id)
                        .setParameter("catId", catId)
                        .executeUpdate();
            }
        }
        return ResponseEntity.ok(updatedComic);
    }

    // ĐÃ SỬA LỖI XÓA 500: Dọn dẹp sạch sẽ 100% Khóa ngoại theo đúng tên bảng trong file comicapp.sql của bạn
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteComic(@PathVariable Integer id) {
        if (!comicRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        // 1. Dọn dẹp các bảng tương tác bình luận con sâu nhất liên quan đến truyện này
        entityManager.createNativeQuery("DELETE FROM Comment_Interactions WHERE CommentID IN (SELECT CommentID FROM Comments WHERE ComicID = :id)").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Comment_Reports WHERE CommentID IN (SELECT CommentID FROM Comments WHERE ComicID = :id)").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Comments WHERE ComicID = :id").setParameter("id", id).executeUpdate();

        // 2. Dọn dẹp các bảng lịch sử, theo dõi, đánh giá
        entityManager.createNativeQuery("DELETE FROM ReadingHistory WHERE ComicID = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Follows WHERE ComicID = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Rating WHERE ComicID = :id").setParameter("id", id).executeUpdate();

        // 3. Dọn dẹp ảnh chương truyện và chương truyện cha
        entityManager.createNativeQuery("DELETE FROM ChapterImages WHERE ChapterID IN (SELECT ChapterID FROM Chapters WHERE ComicID = :id)").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Chapters WHERE ComicID = :id").setParameter("id", id).executeUpdate();

        // 4. Dọn dẹp bảng trung gian thể loại truyện
        entityManager.createNativeQuery("DELETE FROM Comic_Categories WHERE ComicID = :id").setParameter("id", id).executeUpdate();

        // 5. Xóa thực thể cha cuối cùng
        comicRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ĐÃ SỬA LỖI 405: Khai báo chuẩn định dạng nhận dữ liệu tệp tin đa phần từ Android gửi lên
    @PostMapping(value = "/upload-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadComicCover(@RequestParam("file") MultipartFile file) {
        try {
            String uploadDir = "uploads/covers/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName = "cover_" + System.currentTimeMillis() + ".jpg";
            Path filePath = Paths.get(uploadDir + fileName);
            Files.write(filePath, file.getBytes());

            // Đường link kết nối an toàn tương thích lệnh adb reverse cổng 8080 của bạn
            String coverUrl = "http://localhost:8080/uploads/covers/" + fileName;
            return ResponseEntity.ok(Map.of("coverUrl", coverUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi lưu tệp ảnh bìa Admin: " + e.getMessage());
        }
    }

    // 5. API LẤY DANH SÁCH BÌNH LUẬN KÈM ĐẦY ĐỦ THỐNG KÊ CHO ADMIN (ĐÃ SỬA TÊN CỘT DISPLAYNAME)
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<Map<String, Object>>> getComicCommentsForAdmin(@PathVariable Integer id) {
        String sql = "SELECT c.CommentID, u.DisplayName, u.AvatarUrl, c.Content, " +
                "CAST(COALESCE(SUM(CASE WHEN ci.InteractionType = 1 THEN 1 ELSE 0 END), 0) AS SIGNED) as Likes, " +
                "CAST(COALESCE(SUM(CASE WHEN ci.InteractionType = -1 THEN 1 ELSE 0 END), 0) AS SIGNED) as Dislikes, " +
                "CAST(COUNT(DISTINCT cr.ReportID) AS SIGNED) as Reports " +
                "FROM Comments c " +
                "JOIN Users u ON c.UserID = u.UserID " +
                "LEFT JOIN Comment_Interactions ci ON c.CommentID = ci.CommentID " +
                "LEFT JOIN Comment_Reports cr ON c.CommentID = cr.CommentID " +
                "WHERE c.ComicID = :comicId " +
                "GROUP BY c.CommentID, u.DisplayName, u.AvatarUrl, c.Content, c.CreatedAt " +
                "ORDER BY c.CreatedAt DESC";
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rawData = entityManager.createNativeQuery(sql).setParameter("comicId", id).getResultList();
            List<Map<String, Object>> responseList = new ArrayList<>();
            for (Object[] row : rawData) {
                Map<String, Object> map = new HashMap<>();
                map.put("commentId", row[0]);
                map.put("username", row[1]); // Trả về DisplayName của User
                map.put("avatarUrl", row[2]);
                map.put("content", row[3]);
                map.put("likes", row[4]);
                map.put("dislikes", row[5]);
                map.put("reports", row[6]);
                responseList.add(map);
            }
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 6. API XÓA BÌNH LUẬN BẤT KỲ CỦA USER
    @DeleteMapping("/comments/{commentId}")
    @Transactional
    public ResponseEntity<?> adminDeleteComment(@PathVariable Integer commentId) {
        entityManager.createNativeQuery("DELETE FROM Comment_Interactions WHERE CommentID = :id").setParameter("id", commentId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Comment_Reports WHERE CommentID = :id").setParameter("id", commentId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Comments WHERE CommentID = :id").setParameter("id", commentId).executeUpdate();
        return ResponseEntity.ok(Map.of("success", true));
    }
}