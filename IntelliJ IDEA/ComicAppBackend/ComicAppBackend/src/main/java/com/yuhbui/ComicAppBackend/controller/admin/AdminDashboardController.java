package com.yuhbui.ComicAppBackend.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/access-stats")
    public ResponseEntity<?> getAccessStats(@RequestParam(defaultValue = "day") String type) {
        String sql = "";

        // Chuyển đổi toàn bộ sang các hàm xử lý chuỗi và thời gian chuẩn MariaDB / MySQL
        if ("day".equalsIgnoreCase(type)) {
            // Theo Ngày: Lấy khung giờ hôm nay (Ví dụ: 1h, 2h, 15h...)
            sql = "SELECT CONCAT(HOUR(UpdatedAt), 'h') as Label, COUNT(*) as Value " +
                    "FROM ReadingHistory " +
                    "WHERE UpdatedAt >= CURDATE() " +
                    "GROUP BY HOUR(UpdatedAt) " +
                    "ORDER BY HOUR(UpdatedAt)";
        } else if ("week".equalsIgnoreCase(type)) {
            // Theo Tuần: Định dạng Ngày/Tháng của 7 ngày gần nhất
            sql = "SELECT DATE_FORMAT(UpdatedAt, '%d/%m') as Label, COUNT(*) as Value " +
                    "FROM ReadingHistory " +
                    "WHERE UpdatedAt >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                    "GROUP BY DATE_FORMAT(UpdatedAt, '%d/%m'), DATE(UpdatedAt) " +
                    "ORDER BY DATE(UpdatedAt)";
        } else {
            // Theo Tháng: Định dạng Ngày/Tháng của 30 ngày gần nhất
            sql = "SELECT DATE_FORMAT(UpdatedAt, '%d/%m') as Label, COUNT(*) as Value " +
                    "FROM ReadingHistory " +
                    "WHERE UpdatedAt >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                    "GROUP BY DATE_FORMAT(UpdatedAt, '%d/%m'), DATE(UpdatedAt) " +
                    "ORDER BY DATE(UpdatedAt)";
        }

        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rawData = entityManager.createNativeQuery(sql).getResultList();
            List<Map<String, Object>> responseList = new ArrayList<>();

            for (Object[] row : rawData) {
                Map<String, Object> map = new HashMap<>();
                map.put("label", row[0] != null ? row[0].toString() : "");
                map.put("value", row[1] != null ? ((Number) row[1]).longValue() : 0L);
                responseList.add(map);
            }
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi truy vấn MariaDB: " + e.getMessage());
        }
    }
}