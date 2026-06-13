package com.yuhbui.ComicAppBackend.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
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
    public ResponseEntity<?> getAccessStats(
            @RequestParam(defaultValue = "day") String type,
            @RequestParam(required = false) String targetDate) {

        // Nếu client không gửi ngày, mặc định lấy ngày hôm nay
        if (targetDate == null || targetDate.trim().isEmpty()) {
            targetDate = LocalDate.now().toString();
        }

        LocalDate target = LocalDate.parse(targetDate);
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> responseList = new ArrayList<>();

        try {
            if ("day".equalsIgnoreCase(type)) {
                // 1. Thống kê theo ngày: Đếm số lượng theo giờ
                String sql = "SELECT HOUR(UpdatedAt) as HourNum, COUNT(*) as Value " +
                        "FROM ReadingHistory " +
                        "WHERE DATE(UpdatedAt) = :targetDate " +
                        "GROUP BY HOUR(UpdatedAt)";

                @SuppressWarnings("unchecked")
                List<Object[]> rawData = entityManager.createNativeQuery(sql)
                        .setParameter("targetDate", target.toString())
                        .getResultList();

                Map<Integer, Long> baseData = new HashMap<>();
                for (Object[] row : rawData) {
                    if (row[0] != null) {
                        baseData.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
                    }
                }

                // Xác định mốc giờ tối đa: Nếu là ngày hôm nay thì dừng ở giờ hiện tại, ngược lại hiện đủ 23h
                int maxHour = target.equals(today) ? LocalDateTime.now().getHour() : 23;

                for (int h = 0; h <= maxHour; h++) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("label", h + "h");
                    map.put("value", baseData.getOrDefault(h, 0L)); // Lấp đầy bằng 0 nếu không có dữ liệu
                    responseList.add(map);
                }

            } else if ("week".equalsIgnoreCase(type)) {
                // 2. Thống kê theo Tuần: Lấy ngày đầu tuần (Thứ 2) đến ngày Chủ Nhật của tuần đó
                LocalDate monday = target.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sunday = monday.plusDays(6);

                String sql = "SELECT DATE(UpdatedAt) as LogDate, COUNT(*) as Value " +
                        "FROM ReadingHistory " +
                        "WHERE DATE(UpdatedAt) >= :monday AND DATE(UpdatedAt) <= :sunday " +
                        "GROUP BY DATE(UpdatedAt)";

                @SuppressWarnings("unchecked")
                List<Object[]> rawData = entityManager.createNativeQuery(sql)
                        .setParameter("monday", monday.toString())
                        .setParameter("sunday", sunday.toString())
                        .getResultList();

                Map<String, Long> baseData = new HashMap<>();
                for (Object[] row : rawData) {
                    if (row[0] != null) {
                        String dateStr = row[0] instanceof java.util.Date ?
                                new java.text.SimpleDateFormat("yyyy-MM-dd").format((java.util.Date) row[0]) :
                                row[0].toString().substring(0, 10);
                        baseData.put(dateStr, ((Number) row[1]).longValue());
                    }
                }

                // Xác định mốc ngày dừng hiển thị: Nếu là tuần hiện tại thì chỉ chạy đến hôm nay
                LocalDate currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate limitDate = monday.equals(currentWeekMonday) ? today : sunday;

                DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("dd/MM");
                for (LocalDate date = monday; !date.isAfter(limitDate); date = date.plusDays(1)) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("label", date.format(labelFormatter));
                    map.put("value", baseData.getOrDefault(date.toString(), 0L));
                    responseList.add(map);
                }

            } else {
                // 3. Thống kê theo Tháng: Từ ngày 1 đến ngày cuối tháng đó
                LocalDate firstDay = target.withDayOfMonth(1);
                LocalDate lastDay = target.with(TemporalAdjusters.lastDayOfMonth());

                String sql = "SELECT DATE(UpdatedAt) as LogDate, COUNT(*) as Value " +
                        "FROM ReadingHistory " +
                        "WHERE DATE(UpdatedAt) >= :firstDay AND DATE(UpdatedAt) <= :lastDay " +
                        "GROUP BY DATE(UpdatedAt)";

                @SuppressWarnings("unchecked")
                List<Object[]> rawData = entityManager.createNativeQuery(sql)
                        .setParameter("firstDay", firstDay.toString())
                        .setParameter("lastDay", lastDay.toString())
                        .getResultList();

                Map<String, Long> baseData = new HashMap<>();
                for (Object[] row : rawData) {
                    if (row[0] != null) {
                        String dateStr = row[0] instanceof java.util.Date ?
                                new java.text.SimpleDateFormat("yyyy-MM-dd").format((java.util.Date) row[0]) :
                                row[0].toString().substring(0, 10);
                        baseData.put(dateStr, ((Number) row[1]).longValue());
                    }
                }

                // Xác định mốc ngày dừng: Nếu là tháng hiện tại thì dừng ở ngày hôm nay
                LocalDate limitDate = (firstDay.getMonth() == today.getMonth() && firstDay.getYear() == today.getYear()) ? today : lastDay;

                DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("dd/MM");
                for (LocalDate date = firstDay; !date.isAfter(limitDate); date = date.plusDays(1)) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("label", date.format(labelFormatter));
                    map.put("value", baseData.getOrDefault(date.toString(), 0L));
                    responseList.add(map);
                }
            }

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi xử lý dữ liệu: " + e.getMessage());
        }
    }
}