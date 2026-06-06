package com.yuhbui.ComicAppBackend.controller.admin;

import com.yuhbui.ComicAppBackend.entity.Category;
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

    // 1. ĐÃ NÂNG CẤP: API lấy danh sách truyện tích hợp Phân trang, Tìm kiếm, Lọc theo thể loại đa năng
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllComicsForAdmin(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        String baseWhere = " WHERE 1=1";

        // Lọc theo từ khóa Tìm kiếm (Tiêu đề hoặc Tác giả)
        if (keyword != null && !keyword.trim().isEmpty()) {
            baseWhere += " AND (c.Title LIKE :keyword OR c.Author LIKE :keyword)";
        }

        // Lọc theo mã thể loại kết nối bảng trung gian Comic_Categories
        if (categoryId != null && categoryId > 0) {
            baseWhere += " AND c.ComicID IN (SELECT cc.ComicID FROM Comic_Categories cc WHERE cc.CategoryID = :categoryId)";
        }

        // Câu lệnh đếm tổng số phần tử để tính toán số trang
        String countSql = "SELECT COUNT(DISTINCT c.ComicID) FROM Comics c" + baseWhere;
        var countQuery = entityManager.createNativeQuery(countSql);
        if (keyword != null && !keyword.trim().isEmpty()) countQuery.setParameter("keyword", "%" + keyword.trim() + "%");
        if (categoryId != null && categoryId > 0) countQuery.setParameter("categoryId", categoryId);

        long totalItems = ((Number) countQuery.getSingleResult()).longValue();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int offset = page * size;

        // Câu lệnh truy vấn lấy dữ liệu phân trang thực tế
        String selectSql = "SELECT DISTINCT c.* FROM Comics c" + baseWhere + " ORDER BY c.CreatedAt DESC LIMIT :size OFFSET :offset";
        var selectQuery = entityManager.createNativeQuery(selectSql, Comic.class);
        if (keyword != null && !keyword.trim().isEmpty()) selectQuery.setParameter("keyword", "%" + keyword.trim() + "%");
        if (categoryId != null && categoryId > 0) selectQuery.setParameter("categoryId", categoryId);
        selectQuery.setParameter("size", size);
        selectQuery.setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Comic> comics = selectQuery.getResultList();

        // Đóng gói JSON trả về đồng nhất cấu hình phía Android
        Map<String, Object> response = new HashMap<>();
        response.put("comics", comics);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories-list")
    public ResponseEntity<List<Category>> getCategoriesListForFilter() {
        @SuppressWarnings("unchecked")
        List<Category> categories = entityManager.createQuery("FROM Category ORDER BY name ASC", Category.class).getResultList();
        List<Category> responseList = new ArrayList<>();

        // Tạo một phần tử "Tất cả" ảo có ID bằng 0 đưa lên đầu hàng
        Category allCat = new Category();
        allCat.setCategoryId(0);
        allCat.setName("Tất cả");
        responseList.add(allCat);
        responseList.addAll(categories);

        return ResponseEntity.ok(responseList);
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

    // ĐÃ THÊM: API tiếp nhận danh sách CategoryIds từ Android gửi lên để lọc nâng cao
    @GetMapping("/paged")
    public ResponseEntity<Map<String, Object>> getComicsPagedForAdmin(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        String baseWhere = " WHERE 1=1";
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasCategories = categoryIds != null && !categoryIds.isEmpty();

        if (hasKeyword) {
            baseWhere += " AND (c.Title LIKE :keyword OR c.Author LIKE :keyword)";
        }
        if (hasCategories) {
            // Gom nhóm theo từng truyện và ép buộc số lượng thể loại trùng khớp phải bằng đúng số lượng ID gửi lên
            baseWhere += " AND c.ComicID IN (SELECT cc.ComicID FROM Comic_Categories cc WHERE cc.CategoryID IN (:categoryIds) GROUP BY cc.ComicID HAVING COUNT(DISTINCT cc.CategoryID) = :categoryCount)";
        }

        // 1. Tính toán tổng số trang dữ liệu dựa trên bộ lọc hiện tại
        String countSql = "SELECT COUNT(DISTINCT c.ComicID) FROM Comics c" + baseWhere;
        var countQuery = entityManager.createNativeQuery(countSql);
        if (hasKeyword) countQuery.setParameter("keyword", "%" + keyword.trim() + "%");
        if (hasCategories) {
            countQuery.setParameter("categoryIds", categoryIds);
            countQuery.setParameter("categoryCount", categoryIds.size()); // Gửi số lượng thể loại đang chọn
        }

        long totalItems = ((Number) countQuery.getSingleResult()).longValue();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int offset = page * size;

        // 2. Truy vấn danh sách truyện thực tế theo trang phân phối
        String selectSql = "SELECT DISTINCT c.* FROM Comics c" + baseWhere + " ORDER BY c.ComicID DESC LIMIT :size OFFSET :offset";
        var selectQuery = entityManager.createNativeQuery(selectSql, Comic.class);
        if (hasKeyword) selectQuery.setParameter("keyword", "%" + keyword.trim() + "%");
        if (hasCategories) {
            selectQuery.setParameter("categoryIds", categoryIds);
            selectQuery.setParameter("categoryCount", categoryIds.size()); // Gửi số lượng thể loại đang chọn
        }
        selectQuery.setParameter("size", size);
        selectQuery.setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Comic> comics = selectQuery.getResultList();

        Map<String, Object> response = new HashMap<>();
        response.put("comics", comics);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }
}