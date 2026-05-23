package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.ChapterImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChapterImageRepository extends JpaRepository<ChapterImage, Integer> {
    // Tự động sinh SQL: SELECT * FROM ChapterImages WHERE ChapterID = ? ORDER BY PageNumber ASC
    List<ChapterImage> findByChapterIdOrderByPageNumberAsc(Integer chapterId);
}