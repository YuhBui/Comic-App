package com.yuhbui.ComicAppBackend.controller;

import com.yuhbui.ComicAppBackend.entity.ReadingHistory;
import com.yuhbui.ComicAppBackend.repository.ReadingHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private ReadingHistoryRepository historyRepository;

    @PostMapping("/save")
    public ReadingHistory saveHistory(@RequestBody ReadingHistory request) {
        // 1. Tìm xem User này đã từng đọc bộ Truyện này chưa dựa vào UserId và ComicId từ Android gửi lên
        Optional<ReadingHistory> existingOpt =
                historyRepository.findByUserIdAndComicId(request.getUserId(), request.getComicId());

        if (existingOpt.isPresent()) {
            // TÌNH HUỐNG 1: Đã tồn tại bản ghi cũ -> Lấy bản ghi đó từ DB ra để sửa
            ReadingHistory historyToUpdate = existingOpt.get();

            // Cập nhật số chương mới nhất và thời gian đọc mới nhất
            historyToUpdate.setLastChapterId(request.getLastChapterId());
            historyToUpdate.setLastPage(request.getLastPage());
            historyToUpdate.setUpdatedAt(LocalDateTime.now());

            // Lưu đè lại (Lúc này Hibernate sẽ sinh lệnh UPDATE chuẩn xác vì đối tượng này đã có ID thật từ DB)
            return historyRepository.save(historyToUpdate);
        } else {
            // TÌNH HUỐNG 2: Chưa từng đọc truyện này -> Thêm mới hoàn toàn
            // Đảm bảo ép ID về null để Hibernate biết đường sinh lệnh INSERT tạo dòng mới
            request.setHistoryId(null);
            request.setUpdatedAt(LocalDateTime.now());

            return historyRepository.save(request);
        }
    }
}