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
@RequestMapping("/api/comics")
public class ComicController {

    @Autowired
    private ComicRepository comicRepository;

    @Autowired
    private com.yuhbui.ComicAppBackend.repository.ChapterRepository chapterRepository;

    @Autowired
    private com.yuhbui.ComicAppBackend.repository.FollowRepository followRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // 1. API lấy toàn bộ danh sách truyện
    @GetMapping
    public List<Comic> getAllComics() {
        return comicRepository.findAll();
    }

    // 2. API lấy danh sách chương của một truyện
    @GetMapping("/{comicId}/chapters")
    public List<Chapter> getChaptersByComicId(@PathVariable Integer comicId) {
        return chapterRepository.findByComicIdOrderByChapterNumberDesc(comicId);
    }

    // 3. API LẤY CHI TIẾT TRUYỆN ĐẦY ĐỦ
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
        // BỔ SUNG LẤY SỐ LƯỢNG BÌNH LUẬN
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
                dto.setTimeUpdated(convertToRelativeTime(chRow[1]));
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

    // 4. API BẤM NÚT YÊU THÍCH / HỦY YÊU THÍCH
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

    // 5. API Người dùng gửi đánh giá sao (1-5) cho truyện
    @PostMapping("/{comicId}/rate")
    @org.springframework.transaction.annotation.Transactional
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

        // Kiểm tra xem người dùng này đã từng đánh giá truyện này chưa
        String checkSql = "SELECT Score FROM Rating WHERE UserID = :userId AND ComicID = :comicId";
        List<?> existingRating = entityManager.createNativeQuery(checkSql)
                .setParameter("userId", userId)
                .setParameter("comicId", comicId)
                .getResultList();

        if (!existingRating.isEmpty()) {
            // Nếu đã tồn tại -> Thực hiện CẬP NHẬT
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

        // Tính toán lại chính xác điểm trung bình thực tế từ bảng Rating bằng hàm AVG()
        String avgSql = "SELECT AVG(CAST(Score AS DOUBLE)) FROM Rating WHERE ComicID = :comicId";
        Double avgRating = (Double) entityManager.createNativeQuery(avgSql)
                .setParameter("comicId", comicId)
                .getSingleResult();

        Comic comic = comicOpt.get();
        if (avgRating != null) {
            // Làm tròn kết quả điểm trung bình đến 1 chữ số thập phân
            float roundedRating = Math.round(avgRating * 10) / 10f;
            comic.setRating(roundedRating);
        } else {
            comic.setRating(Float.valueOf(score));
        }

        comicRepository.save(comic);

        return ResponseEntity.ok("Cảm ơn bạn đã đánh giá " + score + " sao!");
    }

    // =========================================================================
    // TRÙNG KHỚP CÁC ENDPOINT MỚI BỔ SUNG CHO TRANG CHỦ
    // =========================================================================

    // 6. API TRUYỆN ĐỀ CỬ HOT
    @GetMapping("/home/recommended")
    public ResponseEntity<List<ComicHomeResponseDTO>> getRecommendedComics(@RequestParam(required = false) Integer userId) {
        List<Object[]> rawData = comicRepository.getComicHomeDataRaw();
        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .peek(dto -> {
                    if (userId != null) {
                        dto.setFollowed(followRepository.existsByUserIdAndComicId(userId, dto.getComicId()));
                    }
                })
                .sorted(Comparator.comparing(ComicHomeResponseDTO::getRating, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    // 7. API TRUYỆN MỚI CẬP NHẬT
    @GetMapping("/home/updates")
    public ResponseEntity<List<ComicHomeResponseDTO>> getHomeUpdates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer userId) {

        List<Object[]> rawData = comicRepository.getComicHomeDataRaw();

        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .peek(dto -> {
                    if (userId != null) {
                        dto.setFollowed(followRepository.existsByUserIdAndComicId(userId, dto.getComicId()));
                    }
                })
                .collect(Collectors.toList());

        int start = page * 10;
        if (start >= dtoList.size()) {
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }
        int end = Math.min(start + 10, dtoList.size());

        return ResponseEntity.ok(dtoList.subList(start, end));
    }

    // 8. API BẢNG XẾP HẠNG TOP 10 TRUYỆN
    @GetMapping("/home/ranking")
    public ResponseEntity<List<ComicHomeResponseDTO>> getTopRanking(
            @RequestParam(defaultValue = "day") String type,
            @RequestParam(required = false) Integer userId) {
        List<Object[]> rawData = comicRepository.getComicHomeDataRaw();
        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .peek(dto -> {
                    if (userId != null) {
                        dto.setFollowed(followRepository.existsByUserIdAndComicId(userId, dto.getComicId()));
                    }
                })
                .collect(Collectors.toList());

        // Phân biệt tiêu chí sắp xếp theo từng tab thời gian
        Comparator<ComicHomeResponseDTO> comparator;
        switch (type) {
            case "week":
                comparator = Comparator.comparing(ComicHomeResponseDTO::getRating, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
            case "month":
                comparator = Comparator.comparing(ComicHomeResponseDTO::getFollowCount, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
            default: // "day"
                comparator = Comparator.comparing(ComicHomeResponseDTO::getViewCount, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
        }

        List<ComicHomeResponseDTO> ranked = dtoList.stream()
                .sorted(comparator)
                .limit(10)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ranked);
    }

    // 9. API BỘ LỌC TRUYỆN THEO THỂ LOẠI
    @GetMapping("/filter/category/{catId}")
    public ResponseEntity<List<ComicHomeResponseDTO>> filterComicsByCategory(
            @PathVariable Integer catId,
            @RequestParam(required = false) Integer userId) {
        List<Object[]> rawData = comicRepository.getComicHomeDataByCategoriesRaw(java.util.List.of(catId), 1);
        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .peek(dto -> {
                    if (userId != null) {
                        dto.setFollowed(followRepository.existsByUserIdAndComicId(userId, dto.getComicId()));
                    }
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    // 10. API BỘ LỌC TRUYỆN ĐA THỂ LOẠI (Đã cập nhật nhận diện userId)
    @GetMapping("/filter")
    public ResponseEntity<List<ComicHomeResponseDTO>> filterByCat(
            @RequestParam List<Integer> categoryIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer userId) {

        List<Object[]> rawData = comicRepository.getComicHomeDataByCategoriesRaw(categoryIds, categoryIds.size());

        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .peek(dto -> {
                    if (userId != null) {
                        dto.setFollowed(followRepository.existsByUserIdAndComicId(userId, dto.getComicId()));
                    }
                })
                .collect(Collectors.toList());

        int start = page * 10;
        if (start >= dtoList.size()) {
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }
        int end = Math.min(start + 10, dtoList.size());

        return ResponseEntity.ok(dtoList.subList(start, end));
    }

    // 12. API TÌM KIẾM TRUYỆN THEO TỪ KHÓA (Đã cập nhật nhận diện userId)
    @GetMapping("/search")
    public ResponseEntity<List<ComicHomeResponseDTO>> searchComics(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer userId) {
        List<Object[]> rawData = comicRepository.getComicHomeDataRaw();

        List<ComicHomeResponseDTO> filteredList = rawData.stream()
                .map(this::mapRowToDTO)
                .peek(dto -> {
                    if (userId != null) {
                        dto.setFollowed(followRepository.existsByUserIdAndComicId(userId, dto.getComicId()));
                    }
                })
                .filter(comic -> comic.getTitle() != null &&
                        comic.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(filteredList);
    }

    // =========================================================================
    // Hàm phụ trợ map dữ liệu an toàn, tránh lỗi thay đổi constructor
    // =========================================================================
    private ComicHomeResponseDTO mapRowToDTO(Object[] row) {
        ComicHomeResponseDTO dto = new ComicHomeResponseDTO();
        dto.setComicId((Integer) row[0]);
        dto.setTitle((String) row[1]);
        dto.setCoverImageUrl((String) row[2]);
        dto.setViewCount((Integer) row[3]);
        dto.setRating((Float) row[4]);
        dto.setStatus((String) row[5]);
        dto.setLatestChapterNumber(row[6] != null ? "Chương " + row[6].toString() : "Chương 0");
        dto.setTimeUpdated(convertToRelativeTime(row[7]));
        dto.setFollowCount(row[8] != null ? ((Number) row[8]).longValue() : 0L);
        dto.setCommentCount(row[9] != null ? ((Number) row[9]).longValue() : 0L);
        dto.setFollowed(false); // Mặc định khởi tạo false, Peeking ở trên sẽ gán lại nếu có userId
        return dto;
    }


    // API lấy danh sách truyện Yêu thích có tích hợp bộ lọc đa chọn AND Logic
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