package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
    // Spring Data JPA cực kỳ thông minh, bạn chỉ cần đặt tên hàm đúng quy tắc
    // là nó tự động tạo câu lệnh SQL: SELECT * FROM Chapters WHERE ComicID = ?
    List<Chapter> findByComicIdOrderByChapterNumberDesc(Integer comicId);
    // (Lấy danh sách chương, sắp xếp giảm dần theo số chương - chương mới lên đầu)
}