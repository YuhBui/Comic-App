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

    // 1. ĐÃ NÂNG CẤP: API lấy danh sách truyện tích hợp Phân trang, Tìm kiếm, Lọc theo thể loại đa năng (ĐÃ ĐỒNG BỘ KEY)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllComicsForAdmin(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        String baseWhere = " WHERE 1=1";

        if (keyword != null && !keyword.trim().isEmpty()) {
            baseWhere += " AND (c.Title LIKE :keyword OR c.Author LIKE :keyword)";
        }

        if (categoryId != null && categoryId > 0) {
            baseWhere += " AND c.ComicID IN (SELECT cc.ComicID FROM Comic_Categories cc WHERE cc.CategoryID = :categoryId)";
        }

        String countSql = "SELECT COUNT(DISTINCT c.ComicID) FROM Comics c" + baseWhere;
        var countQuery = entityManager.createNativeQuery(countSql);
        if (keyword != null && !keyword.trim().isEmpty()) countQuery.setParameter("keyword", "%" + keyword.trim() + "%");
        if (categoryId != null && categoryId > 0) countQuery.setParameter("categoryId", categoryId);

        long totalItems = ((Number) countQuery.getSingleResult()).longValue();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int offset = page * size;

        String selectSql = "SELECT DISTINCT c.* FROM Comics c" + baseWhere + " ORDER BY c.CreatedAt DESC LIMIT :size OFFSET :offset";
        var selectQuery = entityManager.createNativeQuery(selectSql, Comic.class);
        if (keyword != null && !keyword.trim().isEmpty()) selectQuery.setParameter("keyword", "%" + keyword.trim() + "%");
        if (categoryId != null && categoryId > 0) selectQuery.setParameter("categoryId", categoryId);
        selectQuery.setParameter("size", size);
        selectQuery.setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Comic> comics = selectQuery.getResultList();

        // ====================================================================
        // ĐOẠN XỬ LÝ BỔ SUNG THÔNG SỐ TƯƠNG TÁC CHO DANH SÁCH TRUYỆN ADMIN (ĐÃ SỬA KHỚP ANDROID KEY)
        // ====================================================================
        List<Map<String, Object>> enrichedComics = new ArrayList<>();
        for (Comic c : comics) {
            Map<String, Object> map = new HashMap<>();
            map.put("comicId", c.getComicId());
            map.put("title", c.getTitle());
            map.put("author", c.getAuthor());
            map.put("description", c.getDescription());
            map.put("coverImageUrl", c.getCoverImageUrl());
            map.put("viewCount", c.getViewCount() != null ? c.getViewCount() : 0);
            map.put("rating", c.getRating());
            map.put("status", c.getStatus());
            map.put("isHidden", c.getIsHidden());
            map.put("createdAt", c.getCreatedAt());

            // 1. ĐÃ ĐỒNG BỘ KEY: Lấy Chương mới nhất & Thời gian cập nhật chương mới nhất
            String chSql = "SELECT ChapterNumber, CreatedAt FROM Chapters WHERE ComicID = :comicId ORDER BY ChapterNumber DESC LIMIT 1";
            @SuppressWarnings("unchecked")
            List<Object[]> chData = entityManager.createNativeQuery(chSql).setParameter("comicId", c.getComicId()).getResultList();
            if (!chData.isEmpty()) {
                Object[] chRow = chData.get(0);
                map.put("latestChapterNumber", chRow[0].toString());
                map.put("timeUpdated", chRow[1] != null ? chRow[1].toString() : "Đang cập nhật");
            } else {
                map.put("latestChapterNumber", "");
                map.put("timeUpdated", "Chưa cập nhật");
            }

            // 2. ĐÃ ĐỒNG BỘ KEY: Lấy Số lượt yêu thích (Follows) đổi sang followCount
            String favSql = "SELECT COUNT(*) FROM Follows WHERE ComicID = :comicId";
            long favCount = ((Number) entityManager.createNativeQuery(favSql).setParameter("comicId", c.getComicId()).getSingleResult()).longValue();
            map.put("followCount", favCount);

            // 3. Lấy Số lượng bình luận (Comments)
            String cmtSql = "SELECT COUNT(*) FROM Comments WHERE ComicID = :comicId";
            long cmtCount = ((Number) entityManager.createNativeQuery(cmtSql).setParameter("comicId", c.getComicId()).getSingleResult()).longValue();
            map.put("commentCount", cmtCount);

            enrichedComics.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("comics", enrichedComics);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories-list")
    public ResponseEntity<List<Category>> getCategoriesListForFilter() {
        @SuppressWarnings("unchecked")
        List<Category> categories = entityManager.createQuery("FROM Category ORDER BY name ASC", Category.class).getResultList();
        List<Category> responseList = new ArrayList<>();

        Category allCat = new Category();
        allCat.setCategoryId(0);
        allCat.setName("Tất cả");
        responseList.add(allCat);
        responseList.addAll(categories);

        return ResponseEntity.ok(responseList);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Comic> createComic(@RequestBody Comic comic, @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds) {
        comic.setComicId(null);
        comic.setViewCount(0);
        comic.setRating(0.0f);
        comic.setIsHidden(false);
        Comic savedComic = comicRepository.save(comic);

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

        // ====================================================================
        // ĐÃ THÊM: TỰ ĐỘNG GỬI THÔNG BÁO CHO USER ĐANG THEO DÕI KHI CẬP NHẬT TRUYỆN
        // ====================================================================
        try {
            @SuppressWarnings("unchecked")
            List<Object> followerIds = entityManager.createNativeQuery("SELECT UserID FROM Follows WHERE ComicID = :id")
                    .setParameter("id", id)
                    .getResultList();
            for (Object fId : followerIds) {
                int uId = ((Number) fId).intValue();
                entityManager.createNativeQuery("INSERT INTO Notifications (UserID, Title, Message, IsRead, ComicID, CreatedAt) VALUES (:userId, :title, :message, false, :comicId, NOW())")
                        .setParameter("userId", uId)
                        .setParameter("title", "Cập nhật thông tin truyện")
                        .setParameter("message", "Truyện '" + updatedComic.getTitle() + "' bạn yêu thích vừa được cập nhật thông tin mới.")
                        .setParameter("comicId", id)
                        .executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo cập nhật truyện: " + e.getMessage());
        }

        return ResponseEntity.ok(updatedComic);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteComic(@PathVariable Integer id) {
        Optional<Comic> comicOpt = comicRepository.findById(id);
        if (!comicOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Comic comic = comicOpt.get();

        // ====================================================================
        // ĐÃ THÊM: TỰ ĐỘNG GỬI THÔNG BÁO CHO USER THEO DÕI TRƯỚC KHI XÓA TRUYỆN TRÊN DB
        // ====================================================================
        try {
            @SuppressWarnings("unchecked")
            List<Object> followerIds = entityManager.createNativeQuery("SELECT UserID FROM Follows WHERE ComicID = :id")
                    .setParameter("id", id)
                    .getResultList();
            for (Object fId : followerIds) {
                int uId = ((Number) fId).intValue();
                entityManager.createNativeQuery("INSERT INTO Notifications (UserID, Title, Message, IsRead, ComicID, CreatedAt) VALUES (:userId, :title, :message, false, :comicId, NOW())")
                        .setParameter("userId", uId)
                        .setParameter("title", "Truyện yêu thích đã gỡ bỏ")
                        .setParameter("message", "Rất tiếc, truyện tranh '" + comic.getTitle() + "' đã bị gỡ khỏi hệ thống theo yêu cầu bản quyền.")
                        .setParameter("comicId", null) // Truyện bị xóa nên Deep-link bằng null
                        .executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo xóa truyện: " + e.getMessage());
        }

        // Thực hiện tuần tự xóa sạch sẽ dữ liệu liên kết khóa ngoại
        entityManager.createNativeQuery("DELETE FROM Comment_Interactions WHERE CommentID IN (SELECT CommentID FROM Comments WHERE ComicID = :id)").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Comment_Reports WHERE CommentID IN (SELECT CommentID FROM Comments WHERE ComicID = :id)").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Comments WHERE ComicID = :id").setParameter("id", id).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM ReadingHistory WHERE ComicID = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Follows WHERE ComicID = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Rating WHERE ComicID = :id").setParameter("id", id).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM ChapterImages WHERE ChapterID IN (SELECT ChapterID FROM Chapters WHERE ComicID = :id)").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Chapters WHERE ComicID = :id").setParameter("id", id).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM Comic_Categories WHERE ComicID = :id").setParameter("id", id).executeUpdate();

        comicRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping(value = "/upload-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadComicCover(@RequestParam("file") MultipartFile file) {
        try {
            String uploadDir = "uploads/covers/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName = "cover_" + System.currentTimeMillis() + ".jpg";
            Path filePath = Paths.get(uploadDir + fileName);
            Files.write(filePath, file.getBytes());

            String coverUrl = "http://localhost:8080/uploads/covers/" + fileName;
            return ResponseEntity.ok(Map.of("coverUrl", coverUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi lưu tệp ảnh bìa Admin: " + e.getMessage());
        }
    }

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
                map.put("username", row[1]);
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

    @DeleteMapping("/comments/{commentId}")
    @Transactional
    public ResponseEntity<?> adminDeleteComment(@PathVariable Integer commentId) {
        entityManager.createNativeQuery("DELETE FROM Comment_Interactions WHERE CommentID = :id").setParameter("id", commentId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Comment_Reports WHERE CommentID = :id").setParameter("id", commentId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM Comments WHERE CommentID = :id").setParameter("id", commentId).executeUpdate();
        return ResponseEntity.ok(Map.of("success", true));
    }

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
            baseWhere += " AND c.ComicID IN (SELECT cc.ComicID FROM Comic_Categories cc WHERE cc.CategoryID IN (:categoryIds) GROUP BY cc.ComicID HAVING COUNT(DISTINCT cc.CategoryID) = :categoryCount)";
        }

        String countSql = "SELECT COUNT(DISTINCT c.ComicID) FROM Comics c" + baseWhere;
        var countQuery = entityManager.createNativeQuery(countSql);
        if (hasKeyword) countQuery.setParameter("keyword", "%" + keyword.trim() + "%");
        if (hasCategories) {
            countQuery.setParameter("categoryIds", categoryIds);
            countQuery.setParameter("categoryCount", categoryIds.size());
        }

        long totalItems = ((Number) countQuery.getSingleResult()).longValue();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int offset = page * size;

        String selectSql = "SELECT DISTINCT c.* FROM Comics c" + baseWhere + " ORDER BY c.ComicID DESC LIMIT :size OFFSET :offset";
        var selectQuery = entityManager.createNativeQuery(selectSql, Comic.class);
        if (hasKeyword) selectQuery.setParameter("keyword", "%" + keyword.trim() + "%");
        if (hasCategories) {
            selectQuery.setParameter("categoryIds", categoryIds);
            selectQuery.setParameter("categoryCount", categoryIds.size());
        }
        selectQuery.setParameter("size", size);
        selectQuery.setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Comic> comics = selectQuery.getResultList();

        List<Map<String, Object>> enrichedComics = new ArrayList<>();
        for (Comic c : comics) {
            Map<String, Object> map = new HashMap<>();
            map.put("comicId", c.getComicId());
            map.put("title", c.getTitle());
            map.put("author", c.getAuthor());
            map.put("description", c.getDescription());
            map.put("coverImageUrl", c.getCoverImageUrl());
            map.put("viewCount", c.getViewCount() != null ? c.getViewCount() : 0);
            map.put("rating", c.getRating());
            map.put("status", c.getStatus());
            map.put("isHidden", c.getIsHidden());
            map.put("createdAt", c.getCreatedAt());

            String chSql = "SELECT ChapterNumber, CreatedAt FROM Chapters WHERE ComicID = :comicId ORDER BY ChapterNumber DESC LIMIT 1";
            @SuppressWarnings("unchecked")
            List<Object[]> chData = entityManager.createNativeQuery(chSql).setParameter("comicId", c.getComicId()).getResultList();
            if (!chData.isEmpty()) {
                Object[] chRow = chData.get(0);
                map.put("latestChapterNumber", chRow[0].toString());
                map.put("timeUpdated", chRow[1] != null ? chRow[1].toString() : "Đang cập nhật");
            } else {
                map.put("latestChapterNumber", "");
                map.put("timeUpdated", "Chưa cập nhật");
            }

            String favSql = "SELECT COUNT(*) FROM Follows WHERE ComicID = :comicId";
            long favCount = ((Number) entityManager.createNativeQuery(favSql).setParameter("comicId", c.getComicId()).getSingleResult()).longValue();
            map.put("followCount", favCount);

            String cmtSql = "SELECT COUNT(*) FROM Comments WHERE ComicID = :comicId";
            long cmtCount = ((Number) entityManager.createNativeQuery(cmtSql).setParameter("comicId", c.getComicId()).getSingleResult()).longValue();
            map.put("commentCount", cmtCount);

            enrichedComics.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("comics", enrichedComics);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }
}