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

    // =========================================================================
    // HÀM BỔ TRỢ: TỰ ĐỘNG GỬI THÔNG BÁO TỚI USER YÊU THÍCH (FOLLOW) TRUYỆN
    // =========================================================================
    private void sendNotificationToFollowers(Integer comicId, String title, String message) {
        try {
            // 1. Lấy danh sách UserID đang follow bộ truyện này
            String sqlFollowers = "SELECT UserID FROM Follows WHERE ComicID = :comicId";
            @SuppressWarnings("unchecked")
            List<Number> followerIds = entityManager.createNativeQuery(sqlFollowers)
                    .setParameter("comicId", comicId)
                    .getResultList();

            // 2. Nếu có người follow, tiến hành nạp thông báo tuần tự vào bảng Notifications
            if (followerIds != null && !followerIds.isEmpty()) {
                String sqlInsertNotif = "INSERT INTO Notifications (UserID, Title, Message, ComicID, IsRead, CreatedAt) " +
                        "VALUES (:userId, :title, :message, :comicId, FALSE, NOW())";

                for (Number userId : followerIds) {
                    entityManager.createNativeQuery(sqlInsertNotif)
                            .setParameter("userId", userId.intValue())
                            .setParameter("title", title)
                            .setParameter("message", message)
                            .setParameter("comicId", comicId)
                            .executeUpdate();
                }
            }
        } catch (Exception e) {
            // Ghi nhận log nếu lỗi hệ thống xảy ra nhưng không làm gián đoạn luồng chính của Chapter
            System.err.println("Lỗi gửi thông báo tự động: " + e.getMessage());
        }
    }

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

    // 2. THÊM CHƯƠNG MỚI (ĐÃ TÍCH HỢP THÔNG BÁO)
    @PostMapping("/comic/{comicId}")
    @Transactional
    public ResponseEntity<?> createChapter(@PathVariable Integer comicId, @RequestParam Double chapterNumber, @RequestParam String title) {
        String sql = "INSERT INTO Chapters (ComicID, ChapterNumber, Title, CreatedAt) VALUES (:comicId, :num, :title, NOW())";
        entityManager.createNativeQuery(sql)
                .setParameter("comicId", comicId)
                .setParameter("num", chapterNumber)
                .setParameter("title", title)
                .executeUpdate();

        // Lấy tên truyện phục vụ nội dung thông báo sinh động hơn
        String comicTitle = "Truyện";
        try {
            comicTitle = (String) entityManager.createNativeQuery("SELECT Title FROM Comics WHERE ComicID = :comicId")
                    .setParameter("comicId", comicId).getSingleResult();
        } catch (Exception e) { /* Bỏ qua nếu truyện chưa có tiêu đề */ }

        // Kích hoạt gửi thông báo tới độc giả yêu thích truyện
        sendNotificationToFollowers(comicId,
                "Truyện theo dõi có chương mới!",
                "Bộ truyện '" + comicTitle + "' bạn thích vừa ra mắt Chương " + chapterNumber + (title != null && !title.isEmpty() ? ": " + title : "") + ". Đọc ngay thôi!"
        );

        return ResponseEntity.ok(Map.of("success", true));
    }

    // 3. SỬA THÔNG TIN CHƯƠNG (ĐÃ TÍCH HỢP THÔNG BÁO)
    @PutMapping("/{chapterId}")
    @Transactional
    public ResponseEntity<?> updateChapter(@PathVariable Integer chapterId, @RequestParam Double chapterNumber, @RequestParam String title) {
        // Truy vấn tìm ComicID của chương hiện tại trước khi update nội dung mới
        Integer comicId = null;
        try {
            comicId = ((Number) entityManager.createNativeQuery("SELECT ComicID FROM Chapters WHERE ChapterID = :chapterId")
                    .setParameter("chapterId", chapterId).getSingleResult()).intValue();
        } catch (Exception e) { /* Bỏ qua */ }

        // Thực hiện cập nhật chương truyện
        String sql = "UPDATE Chapters SET ChapterNumber = :num, Title = :title WHERE ChapterID = :chapterId";
        entityManager.createNativeQuery(sql)
                .setParameter("num", chapterNumber)
                .setParameter("title", title)
                .setParameter("chapterId", chapterId)
                .executeUpdate();

        // Nếu xác định được truyện gốc, thực hiện gửi thông báo chỉnh sửa
        if (comicId != null) {
            String comicTitle = "Truyện";
            try {
                comicTitle = (String) entityManager.createNativeQuery("SELECT Title FROM Comics WHERE ComicID = :comicId")
                        .setParameter("comicId", comicId).getSingleResult();
            } catch (Exception e) { /* Bỏ qua */ }

            sendNotificationToFollowers(comicId,
                    "Chương truyện đã được chỉnh sửa",
                    "Chương " + chapterNumber + " của bộ truyện '" + comicTitle + "' vừa được Admin cập nhật lại nội dung mới."
            );
        }

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

            // Tính toán tự động số trang tiếp theo (PageNumber = Max + 1)
            String maxPageSql = "SELECT COALESCE(MAX(PageNumber), 0) + 1 FROM ChapterImages WHERE ChapterID = :chapterId";
            Integer nextPageNumber = ((Number) entityManager.createNativeQuery(maxPageSql).setParameter("chapterId", chapterId).getSingleResult()).intValue();

            String insertSql = "INSERT INTO ChapterImages (ChapterID, ImageURL, PageNumber) VALUES (:chapterId, :url, :page)";
            entityManager.createNativeQuery(insertSql)
                    .setParameter("chapterId", chapterId)
                    .setParameter("url", imageUrl)
                    .setParameter("page", nextPageNumber)
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

    // 8. API TẠO CHƯƠNG MỚI VÀ TRANH TRUYỆN CÙNG LÚC (ĐÃ TÍCH HỢP THÔNG BÁO)
    @PostMapping(value = "/comic/{comicId}/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> createChapterWithImages(
            @PathVariable Integer comicId,
            @RequestParam Double chapterNumber,
            @RequestParam String title,
            @RequestParam("files") MultipartFile[] files) {
        try {
            // Chèn chương truyện vào DB
            String insertChapterSql = "INSERT INTO Chapters (ComicID, ChapterNumber, Title, CreatedAt) VALUES (:comicId, :num, :title, NOW())";
            entityManager.createNativeQuery(insertChapterSql)
                    .setParameter("comicId", comicId)
                    .setParameter("num", chapterNumber)
                    .setParameter("title", title)
                    .executeUpdate();

            // Lấy ID vừa sinh ra của chương truyện
            Integer chapterId = ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).intValue();

            // Lưu tuần tự từng file ảnh để tránh xung đột luồng xử lý dữ liệu bất đồng bộ
            String uploadDir = "uploads/chapters/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                String fileName = "page_" + chapterId + "_" + i + "_" + System.currentTimeMillis() + ".jpg";
                Path filePath = Paths.get(uploadDir + fileName);
                Files.write(filePath, file.getBytes());

                String imageUrl = "http://localhost:8080/uploads/chapters/" + fileName;
                int pageNumber = i + 1; // Số trang tăng tuần tự: 1, 2, 3...

                String insertImageSql = "INSERT INTO ChapterImages (ChapterID, ImageURL, PageNumber) VALUES (:chapterId, :url, :page)";
                entityManager.createNativeQuery(insertImageSql)
                        .setParameter("chapterId", chapterId)
                        .setParameter("url", imageUrl)
                        .setParameter("page", pageNumber)
                        .executeUpdate();
            }

            // Tiến hành kích hoạt gửi thông báo tự động sau khi lưu chương kèm ảnh hoàn tất
            String comicTitle = "Truyện";
            try {
                comicTitle = (String) entityManager.createNativeQuery("SELECT Title FROM Comics WHERE ComicID = :comicId")
                        .setParameter("comicId", comicId).getSingleResult();
            } catch (Exception e) { /* Bỏ qua */ }

            sendNotificationToFollowers(comicId,
                    "Truyện theo dõi có chương mới!",
                    "Bộ truyện '" + comicTitle + "' bạn thích vừa ra mắt Chương " + chapterNumber + (title != null && !title.isEmpty() ? ": " + title : "") + ". Khám phá ngay!"
            );

            return ResponseEntity.ok(Map.of("success", true, "message", "Đã tạo chương và nạp toàn bộ ảnh thành công!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống khi tạo chương: " + e.getMessage());
        }
    }

    // 9. API THAY ĐỔI VỊ TRÍ SẮP XẾP CÁC TRANG TRUYỆN TRANH (REORDER)
    @PutMapping("/pages/reorder")
    @Transactional
    public ResponseEntity<?> reorderChapterPages(@RequestBody List<Integer> imageIds) {
        try {
            // Sắp xếp lại PageNumber dựa trên trật tự ID mảng nhận được từ thiết bị di động gửi lên
            for (int i = 0; i < imageIds.size(); i++) {
                entityManager.createNativeQuery("UPDATE ChapterImages SET PageNumber = :page WHERE ImageID = :id")
                        .setParameter("page", i + 1)
                        .setParameter("id", imageIds.get(i))
                        .executeUpdate();
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã cập nhật vị trí trang truyện mới!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi sắp xếp tệp tin: " + e.getMessage());
        }
    }



    // 10. API LẤY TOÀN BỘ NỘI DUNG LỜI BÁO CÁO CỦA MỘT BÌNH LUẬN TRUYỆN (kèm thông tin người báo cáo)
    @GetMapping("/comments/{commentId}/reports")
    public ResponseEntity<List<Map<String, Object>>> getCommentReportReasons(@PathVariable Integer commentId) {
        try {
            String sql = "SELECT u.DisplayName, u.AvatarUrl, cr.Reason, cr.CreatedAt " +
                    "FROM Comment_Reports cr " +
                    "JOIN Users u ON cr.UserID = u.UserID " +
                    "WHERE cr.CommentID = :commentId " +
                    "ORDER BY cr.CreatedAt DESC";
            @SuppressWarnings("unchecked")
            List<Object[]> rawData = entityManager.createNativeQuery(sql)
                    .setParameter("commentId", commentId)
                    .getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rawData) {
                Map<String, Object> map = new HashMap<>();
                map.put("displayName", row[0]);
                map.put("avatarUrl", row[1]);
                map.put("reason", row[2]);
                map.put("createdAt", row[3] != null ? row[3].toString() : "");
                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 11. API LẤY DANH SÁCH BÌNH LUẬN CỦA RIÊNG CHƯƠNG ĐÓ CHO ADMIN
    @GetMapping("/{chapterId}/comments")
    public ResponseEntity<List<Map<String, Object>>> getChapterCommentsForAdmin(@PathVariable Integer chapterId) {
        String sql = "SELECT c.CommentID, u.DisplayName, u.AvatarUrl, c.Content, " +
                "CAST(COALESCE(SUM(CASE WHEN ci.InteractionType = 1 THEN 1 ELSE 0 END), 0) AS SIGNED) as Likes, " +
                "CAST(COALESCE(SUM(CASE WHEN ci.InteractionType = -1 THEN 1 ELSE 0 END), 0) AS SIGNED) as Dislikes, " +
                "CAST(COUNT(DISTINCT cr.ReportID) AS SIGNED) as Reports " +
                "FROM Comments c " +
                "JOIN Users u ON c.UserID = u.UserID " +
                "LEFT JOIN Comment_Interactions ci ON c.CommentID = ci.CommentID " +
                "LEFT JOIN Comment_Reports cr ON c.CommentID = cr.CommentID " +
                "WHERE c.ChapterID = :chapterId " +
                "GROUP BY c.CommentID, u.DisplayName, u.AvatarUrl, c.Content, c.CreatedAt " +
                "ORDER BY c.CreatedAt DESC";
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rawData = entityManager.createNativeQuery(sql).setParameter("chapterId", chapterId).getResultList();
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
}