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

    // 5. API Người dùng gửi đánh giá sao (1-5) cho truyện
    @PostMapping("/{comicId}/rate")
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

        Comic comic = comicOpt.get();

        float currentRating = comic.getRating();
        if (currentRating == 0) {
            comic.setRating(Float.valueOf(score));
        } else {
            float newRating = (currentRating + score) / 2f;
            newRating = Math.round(newRating * 10) / 10f;
            comic.setRating(newRating);
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
        List<Object[]> rawData = comicRepository.getComicHomeDataByCategory(catId);
        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    // 10. API BỘ LỌC TRUYỆN THEO THỂ LOẠI (Query param version - dùng bởi Android)
    @GetMapping("/filter")
    public ResponseEntity<List<ComicHomeResponseDTO>> filterByCat(@RequestParam Integer catId) {
        List<Object[]> rawData = comicRepository.getComicHomeDataByCategory(catId);
        List<ComicHomeResponseDTO> dtoList = rawData.stream()
                .map(this::mapRowToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
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
    // HÀM PHỤ TRỢ: Chuyển đổi Object[] sang DTO
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
                "Mới cập nhật",
                row[8] != null ? ((Number) row[8]).longValue() : 0L,
                row[9] != null ? ((Number) row[9]).longValue() : 0L
        );
    }
}