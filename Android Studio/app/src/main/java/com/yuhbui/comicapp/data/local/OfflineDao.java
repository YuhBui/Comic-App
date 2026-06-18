package com.yuhbui.comicapp.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.yuhbui.comicapp.data.model.DownloadedChapter;
import com.yuhbui.comicapp.data.model.DownloadedComic;
import com.yuhbui.comicapp.data.model.DownloadedImage;

import java.util.List;

@Dao
public interface OfflineDao {

    // ========== TRUYỆN (COMIC) ==========
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComic(DownloadedComic comic);

    @Query("SELECT * FROM downloaded_comics")
    List<DownloadedComic> getAllDownloadedComics();

    @Query("SELECT * FROM downloaded_comics WHERE comicId = :comicId")
    DownloadedComic getComicById(int comicId);

    @Delete
    void deleteComic(DownloadedComic comic);


    // ========== CHƯƠNG (CHAPTER) ==========
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChapter(DownloadedChapter chapter);

    @Query("SELECT * FROM downloaded_chapters WHERE comicId = :comicId ORDER BY chapterId DESC")
    List<DownloadedChapter> getChaptersByComic(int comicId);

    @Query("SELECT * FROM downloaded_chapters WHERE chapterId = :chapterId")
    DownloadedChapter getChapterById(int chapterId);

    @Query("SELECT COUNT(*) FROM downloaded_chapters WHERE comicId = :comicId")
    int getChapterCountByComic(int comicId);

    @Query("DELETE FROM downloaded_chapters WHERE chapterId = :chapterId")
    void deleteChapterById(int chapterId);


    // ========== TRANG ẢNH (IMAGE) ==========
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertImages(List<DownloadedImage> images);

    @Query("SELECT * FROM downloaded_images WHERE chapterId = :chapterId ORDER BY position ASC")
    List<DownloadedImage> getImagesByChapter(int chapterId);

    @Query("DELETE FROM downloaded_images WHERE chapterId = :chapterId")
    void deleteImagesByChapter(int chapterId);
}