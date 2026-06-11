package com.yuhbui.ComicAppBackend.controller;

import com.yuhbui.ComicAppBackend.entity.Chapter;
import com.yuhbui.ComicAppBackend.entity.Comic;
import com.yuhbui.ComicAppBackend.dto.ComicDetailResponseDTO;
import com.yuhbui.ComicAppBackend.dto.ComicHomeResponseDTO;
import com.yuhbui.ComicAppBackend.repository.ComicRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comics") // Đường dẫn gốc cho các API liên quan đến truyện
public class ComicController {

    @Autowired
    private ComicRepository comicRepository;

    @Autowired
    private com.yuhbui.ComicAppBackend.repository.ChapterRepository chapterRepository;

    @Autowired
    private com.yuhbui.ComicAppBackend.repository.FollowRepository followRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // 1. API lấy toàn bộ danh sách truyện (Cơ bản)
    @GetMapping
    public List<Comic> getAllComics() {
        return comicRepository.findAll();
    }

    // 2. API lấy danh sách chương của một truyện
    @GetMapping("/{comicId}/chapters")
    public List<Chapter> getChaptersByComicId(@PathVariable Integer comicId) {
        return chapterRepository.findByComicIdOrderByChapterNumberDesc(comicId);
    }

    // 3. API LẤY CHI TIẾT TRUYỆN ĐẦY ĐỦ (KẾT NỐI DATABASE THỰC TẾ)
    @GetMapping("/{comicId}")
    public ResponseEntity<?> getComicDetail(
            @PathVariable Integer comicId,
            @RequestParam(required = false) Integer userId) {

        Optional<Comic> comicOpt = comicRepository.findById(comicId);
        if (!comicOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Comic comic = comicOpt.get();

        ComicDetailResponseDTO dto = new ComicDetailResponseDTO();
        dto.setComic(comic);
        dto.setFavoriteCount(followRepository.countByComicId(comicId));

        if (userId != null) {
            dto.setFavorite(followRepository.existsByUserIdAndComicId(userId, comicId));
        } else {
            dto.setFavorite(false);
        }

        // ==========================================
        // BỔ SUNG LẤY SỐ LƯỢNG BÌNH LUẬN (COMMENT)
        // ==========================================
        try {
            String cmtSql = "SELECT COUNT(*) FROM Comments WHERE ComicID = :comicId";
            long cmtCount = ((Number) entityManager.createNativeQuery(cmtSql)
                    .setParameter("comicId", comicId)
                    .getSingleResult()).longValue();
            dto.setCommentCount((int) cmtCount);
        } catch (Exception e) {
            dto.setCommentCount(0);
        }

        // ======================================================================
        // BỔ SUNG LẤY CHƯƠNG MỚI NHẤT & THỜI GIAN CẬP NHẬT CHƯƠNG MỚI NHẤT
        // ======================================================================
        try {
            String chSql = "SELECT ChapterNumber, CreatedAt FROM Chapters WHERE ComicID = :comicId ORDER BY ChapterNumber DESC LIMIT 1";
            @SuppressWarnings("unchecked")
            List<Object[]> chData = entityManager.createNativeQuery(chSql)
                    .setParameter("comicId", comicId)
                    .getResultList();
            if (!chData.isEmpty()) {
                Object[] chRow = chData.get(0);
                dto.setLatestChapterNumber("Chương " + chRow[0].toString());
                dto.setTimeUpdated(convertToRelativeTime(chRow[1])); // ĐÃ SỬA: Áp dụng định dạng "... trước"
            } else {
                dto.setLatestChapterNumber("Chưa có chương");
                dto.setTimeUpdated("Chưa cập nhật");
            }
        } catch (Exception e) {
            dto.setLatestChapterNumber("Đang cập nhật");
            dto.setTimeUpdated("Đang cập nhật");
        }

        try {
            String sql = "SELECT c.Name FROM Categories c " +
                    "JOIN Comic_Categories cc ON c.CategoryID = cc.CategoryID " +
                    "WHERE cc.ComicID = :comicId";

            @SuppressWarnings("unchecked")
            List<String> genreList = entityManager.createNativeQuery(sql)
                    .setParameter("comicId", comicId)
                    .getResultList();

            if (genreList.isEmpty()) {
                dto.setGenres("Đang cập nhật");
            } else {
                dto.setGenres(String.join(", ", genreList));
            }
        } catch (Exception e) {
            dto.setGenres("Đang cập nhật");
        }

        return ResponseEntity.ok(dto);
    }

    // 4. API BẤM NÚT YÊU THÍCH / HỦY YÊU THÍCH (TOGGLE FAVORITE)
    @PostMapping("/{comicId}/toggle-favorite")
    public ResponseEntity<?> toggleFavorite(
            @PathVariable Integer comicId,
            @RequestParam Integer userId) {

        com.yuhbui.ComicAppBackend.entity.FollowId id = new com.yuhbui.ComicAppBackend.entity.FollowId();
        id.setUserId(userId);
        id.setComicId(comicId);

        Optional<com.yuhbui.ComicAppBackend.entity.Follow> followOpt = followRepository.findById(id);

        if (followOpt.isPresent()) {
            followRepository.delete(followOpt.get());
            return ResponseEntity.ok(false);
        } else {
            com.yuhbui.ComicAppBackend.entity.Follow newFollow = new com.yuhbui.ComicAppBackend.entity.Follow();
            newFollow.setUserId(userId);
            newFollow.setComicId(comicId);
            followRepository.save(newFollow);
            return ResponseEntity.ok(true);
        }
    }

    // 5. API Người dùng gửi đánh giá sao (1-5) cho truyện - ĐÃ SỬA CHUẨN LOGIC VÀ THUẬT TOÁN
    @PostMapping("/{comicId}/rate")
    @org.springframework.transaction.annotation.Transactional // Thêm transaction để thực hiện chỉnh sửa dữ liệu DB
    public ResponseEntity<?> rateComic(
            @PathVariable Integer comicId,
            @RequestParam Integer userId,
            @RequestParam Integer score) {

        if (score < 1 || score > 5) {
            return ResponseEntity.badRequest().body("Số sao không hợp lệ!");
        }

        Optional<Comic> comicOpt = comicRepository.findById(comicId);
        if (!comicOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Truyện không tồn tại!");
        }

        // Bước 1: Kiểm tra xem người dùng này đã từng đánh giá truyện này chưa
        String checkSql = "SELECT Score FROM Rating WHERE UserID = :userId AND ComicID = :comicId";
        List<?> existingRating = entityManager.createNativeQuery(checkSql)
                .setParameter("userId", userId)
                .setParameter("comicId", comicId)
                .getResultList();

        if (!existingRating.isEmpty()) {
            // Nếu đã tồn tại -> Thực hiện CẬP NHẬT (Sửa đánh giá cũ)
            String updateRatingSql = "UPDATE Rating SET Score = :score, UpdatedAt = CURRENT_TIMESTAMP WHERE UserID = :userId AND ComicID = :comicId";
            entityManager.createNativeQuery(updateRatingSql)
                    .setParameter("score", score)
                    .setParameter("userId", userId)
                    .setParameter("comicId", comicId)
                    .executeUpdate();
        } else {
            // Nếu chưa tồn tại -> Thực hiện THÊM MỚI bản ghi đánh giá
            String insertRatingSql = "INSERT INTO Rating (UserID, ComicID, Score, CreatedAt, UpdatedAt) VALUES (:userId, :comicId, :score, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
            entityManager.createNativeQuery(insertRatingSql)
                    .setParameter("userId", userId)
                    .setParameter("comicId", comicId)
                    .setParameter("score", score)
                    .executeUpdate();
        }

        // Bước 2: Tính toán lại chính xác điểm trung bình thực tế từ bảng Rating bằng hàm AVG()
        String avgSql = "SELECT AVG(CAST(Score AS DOUBLE)) FROM Rating WHERE ComicID = :comicId";
        Double avgRating = (Double) entityManager.createNativeQuery(avgSql)
                .setParameter("comicId", comicId)
                .getSingleResult();

        Comic comic = comicOpt.get();
        if (avgRating != null) {
            // Làm tròn kết quả điểm trung bình đến 1 chữ số thập phân (Ví dụ: 4.367 -> 4.4)
            float roundedRating = Math.round(avgRating * 10) / 10f;
            comic.setRating(roundedRating);
        } else {
            comic.setRating(Float.valueOf(score));
        }

        comicRepository.save(comic);

        return ResponseEntity.ok("Cảm ơn bạn đã đánh giá " + score + " sao!");
    }

    // =========================================================================
    // TRÙNG KHỚP CÁC ENDPOINT MỚI BỔ SUNG CHO TRANG CHỦ (HOME NÂNG CẤP)
    // =========================================================================

    // 6. API TRUYỆN ĐỀ CỬ HOT (Lấy top 6 bộ có điểm rating cao nhất kèm đầy đủ thông số)
    @GetMapping("/home/recommended")
    public ResponseEntity<List<ComicHomeResponseDTO>> getRecommendedComics() {
        List<Object[]> rawData = comicRepository.getComicHomeDataRaw();
        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .sorted(Comparator.comparing(ComicHomeResponseDTO::getRating, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    // 7. API TRUYỆN MỚI CẬP NHẬT + TÍNH TOÁN ĐẦY ĐỦ THÔNG SỐ (PHÂN TRANG LŨY TIẾN 10 TRUYỆN)
    @GetMapping("/home/updates")
    public ResponseEntity<List<ComicHomeResponseDTO>> getHomeUpdates(@RequestParam(defaultValue = "0") int page) {
        // Gọi câu lệnh Query vạn năng từ ComicRepository đã thiết kế ở bước trước
        List<Object[]> rawData = comicRepository.getComicHomeDataRaw();

        // Ánh xạ dữ liệu thô (Object Array) sang định dạng đối tượng DTO chuyên biệt gửi cho Android
        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .collect(Collectors.toList());

        // Phân đoạn phân trang (Cắt mảng 10 phần tử dựa theo tham số ?page=)
        int start = page * 10;
        if (start >= dtoList.size()) {
            return ResponseEntity.ok(new java.util.ArrayList<>()); // Trả về list rỗng nếu lướt quá trang cuối cùng
        }
        int end = Math.min(start + 10, dtoList.size());

        return ResponseEntity.ok(dtoList.subList(start, end));
    }

    // 8. API BẢNG XẾP HẠNG TOP 10 TRUYỆN (HỖ TRỢ THAY ĐỔI TAB: NGÀY / TUẦN / THÁNG)
    @GetMapping("/home/ranking")
    public ResponseEntity<List<ComicHomeResponseDTO>> getTopRanking(@RequestParam(defaultValue = "day") String type) {
        List<Object[]> rawData = comicRepository.getComicHomeDataRaw();
        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .collect(Collectors.toList());

        // Phân biệt tiêu chí sắp xếp theo từng tab thời gian
        Comparator<ComicHomeResponseDTO> comparator;
        switch (type) {
            case "week":
                // Tuần: Sắp xếp theo điểm Rating cao nhất
                comparator = Comparator.comparing(ComicHomeResponseDTO::getRating, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
            case "month":
                // Tháng: Sắp xếp theo số Follow nhiều nhất
                comparator = Comparator.comparing(ComicHomeResponseDTO::getFollowCount, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
            default: // "day"
                // Ngày: Sắp xếp theo ViewCount nhiều nhất
                comparator = Comparator.comparing(ComicHomeResponseDTO::getViewCount, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
        }

        List<ComicHomeResponseDTO> ranked = dtoList.stream()
                .sorted(comparator)
                .limit(10)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ranked);
    }

    // 9. API BỘ LỌC TRUYỆN THEO THỂ LOẠI (Path variable version)
    @GetMapping("/filter/category/{catId}")
    public ResponseEntity<List<ComicHomeResponseDTO>> filterComicsByCategory(@PathVariable Integer catId) {
        List<Object[]> rawData = comicRepository.getComicHomeDataByCategoriesRaw(java.util.List.of(catId), 1);
        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    // 10. ĐÃ SỬA: API BỘ LỌC TRUYỆN ĐA THỂ LOẠI (AND LOGIC) + PHÂN TRANG Y HỆT TRUYỆN MỚI CẬP NHẬT
    @GetMapping("/filter")
    public ResponseEntity<List<ComicHomeResponseDTO>> filterByCat(
            @RequestParam List<Integer> categoryIds,
            @RequestParam(defaultValue = "0") int page) {

        // Gọi câu lệnh Query vạn năng nhận mảng ID và kích thước mảng để chạy logic AND
        List<Object[]> rawData = comicRepository.getComicHomeDataByCategoriesRaw(categoryIds, categoryIds.size());

        // Ánh xạ dữ liệu thô (Object Array) sang định dạng đối tượng DTO chuyên biệt gửi cho Android
        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .collect(Collectors.toList());

        // Cơ chế phân trang lũy tiến cắt mảng (Cắt cụm 10 phần tử dựa theo tham số ?page=) y hệt getHomeUpdates
        int start = page * 10;
        if (start >= dtoList.size()) {
            return ResponseEntity.ok(new java.util.ArrayList<>()); // Trả về danh sách rỗng nếu lướt quá trang cuối
        }
        int end = Math.min(start + 10, dtoList.size());

        return ResponseEntity.ok(dtoList.subList(start, end));
    }

    // 11. API LẤY DANH SÁCH TRUYỆN YÊU THÍCH CỦA NGƯỜI DÙNG (kèm đầy đủ thông số)
    @GetMapping("/favorites/{userId}")
    public ResponseEntity<List<ComicHomeResponseDTO>> getFavoriteComics(
            @PathVariable Integer userId) {
        List<Object[]> rawData = followRepository.findFavoriteComicsWithStatsByUserId(userId);
        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    // =============================================
    // HÀM PHỤ TRỢ: Chuyển đổi Object[] sang DTO ngoài trang chủ
    // =============================================
    private ComicHomeResponseDTO mapRowToDTO(Object[] row) {
        return new ComicHomeResponseDTO(
                (Integer) row[0],
                (String) row[1],
                (String) row[2],
                (Integer) row[3],
                (Float) row[4],
                (String) row[5],
                row[6] != null ? "Chương " + row[6].toString() : "Chương 0",
                convertToRelativeTime(row[7]), // ĐÃ SỬA: Gọi hàm convert thời gian tương đối
                row[8] != null ? ((Number) row[8]).longValue() : 0L,
                row[9] != null ? ((Number) row[9]).longValue() : 0L
        );
    }

    // 12. API TÌM KIẾM TRUYỆN THEO TỪ KHÓA (Kèm đầy đủ thông số tương tác cho danh sách dọc)
    @GetMapping("/search")
    public ResponseEntity<List<ComicHomeResponseDTO>> searchComics(@RequestParam String keyword) {
        // Lấy toàn bộ danh sách dữ liệu thô kèm stats từ Repository
        List<Object[]> rawData = comicRepository.getComicHomeDataRaw();

        // Chuyển đổi sang DTO và lọc các truyện có tiêu đề chứa từ khóa (không phân biệt chữ hoa/thường)
        List<ComicHomeResponseDTO> filteredList = rawData.stream()
                .map(this::mapRowToDTO)
                .filter(comic -> comic.getTitle() != null &&
                        comic.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(filteredList);
    }

    // ĐÃ SỬA: API lấy danh sách truyện Yêu thích có tích hợp bộ lọc đa chọn AND Logic (NÂNG CẤP ĐẦY ĐỦ THÔNG SỐ)
    @GetMapping("/user-favorites/{userId}")
    public ResponseEntity<List<ComicHomeResponseDTO>> getFavoriteComicsFiltered(
            @PathVariable("userId") Integer userId,
            @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds) {

        // Câu truy vấn vạn năng lấy đầy đủ 10 cột dữ liệu thống kê tương tự trang chủ
        String sql = "SELECT c.ComicID, c.Title, c.CoverImageUrl, c.ViewCount, c.Rating, c.Status, " +
                "(SELECT ch.ChapterNumber FROM Chapters ch WHERE ch.ComicID = c.ComicID ORDER BY ch.ChapterNumber DESC LIMIT 1) AS latestChapter, " +
                "(SELECT ch.CreatedAt FROM Chapters ch WHERE ch.ComicID = c.ComicID ORDER BY ch.ChapterNumber DESC LIMIT 1) AS timeUpdate, " +
                "(SELECT COUNT(*) FROM Follows fl WHERE fl.ComicID = c.ComicID) AS follows, " +
                "(SELECT COUNT(*) FROM Comments cmt WHERE cmt.ComicID = c.ComicID) AS comments " +
                "FROM Follows f JOIN Comics c ON f.ComicID = c.ComicID WHERE f.UserID = :userId";

        boolean hasCategories = categoryIds != null && !categoryIds.isEmpty();

        if (hasCategories) {
            sql += " AND c.ComicID IN (SELECT cc.ComicID FROM Comic_Categories cc WHERE cc.CategoryID IN (:categoryIds) GROUP BY cc.ComicID HAVING COUNT(DISTINCT cc.CategoryID) = :categoryCount)";
        }
        sql += " ORDER BY f.ComicID DESC";

        var query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        if (hasCategories) {
            query.setParameter("categoryIds", categoryIds);
            query.setParameter("categoryCount", categoryIds.size());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> list = query.getResultList();

        // Ánh xạ dữ liệu thô sang danh sách DTO chứa đầy đủ thông số tương tác gửi về Android
        List<ComicHomeResponseDTO> dtoList = list.stream()
                .map(this::mapRowToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    /**
     * Hàm chuyển đổi mốc thời gian từ database sang dạng tương đối (X ngày/giờ/phút trước)
     */
    private String convertToRelativeTime(Object timeObj) {
        if (timeObj == null) return "Đang cập nhật";

        java.time.LocalDateTime dateTime = null;

        // Kiểm tra và ép kiểu an toàn từ kết quả Native Query
        if (timeObj instanceof java.sql.Timestamp) {
            dateTime = ((java.sql.Timestamp) timeObj).toLocalDateTime();
        } else if (timeObj instanceof java.time.LocalDateTime) {
            dateTime = (java.time.LocalDateTime) timeObj;
        } else {
            try {
                // Trường hợp trả về dạng String "yyyy-MM-dd HH:mm:ss"
                String timeStr = timeObj.toString().replace(" ", "T");
                dateTime = java.time.LocalDateTime.parse(timeStr);
            } catch (Exception e) {
                return timeObj.toString(); // Nếu lỗi parse thì trả về chuỗi gốc
            }
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(dateTime, now);

        long seconds = duration.getSeconds();
        if (seconds < 0) seconds = 0; // Tránh lỗi lệch giây hệ thống hiển thị tương lai

        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long months = days / 30;
        long years = days / 365;

        if (seconds < 60) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else if (days < 30) {
            return days + " ngày trước";
        } else if (months < 12) {
            return months + " tháng trước";
        } else {
            return years + " năm trước";
        }
    }
}