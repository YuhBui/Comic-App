package com.yuhbui.comicapp.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.yuhbui.comicapp.data.model.Chapter;
import com.yuhbui.comicapp.data.repository.ChapterRepository;
import java.util.List;

public class ComicDetailViewModel extends ViewModel {

    private ChapterRepository repository;

    public ComicDetailViewModel() {
        repository = new ChapterRepository();
    }

    public LiveData<List<Chapter>> getChapters() {
        return repository.getChaptersLiveData();
    }

    public void loadChapters(int comicId) {
        repository.fetchChapters(comicId);
    }
}