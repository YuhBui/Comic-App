package com.yuhbui.ComicAppBackend.controller;

import com.yuhbui.ComicAppBackend.entity.ChapterImage;
import com.yuhbui.ComicAppBackend.repository.ChapterImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/chapters")
public class ChapterController {

    @Autowired
    private ChapterImageRepository imageRepository;

    @GetMapping("/{chapterId}/images")
    public List<ChapterImage> getImagesByChapterId(@PathVariable Integer chapterId) {
        return imageRepository.findByChapterIdOrderByPageNumberAsc(chapterId);
    }
}