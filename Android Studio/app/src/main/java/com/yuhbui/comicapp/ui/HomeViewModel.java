package com.yuhbui.comicapp.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.data.repository.ComicRepository;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private ComicRepository repository;

    public HomeViewModel() {
        repository = new ComicRepository();
    }

    // View (Activity) sẽ gọi hàm này để "lắng nghe" dữ liệu
    public LiveData<List<Comic>> getComics() {
        return repository.getComicsLiveData();
    }

    // View (Activity) sẽ gọi hàm này để ra lệnh tải dữ liệu
    public void loadComics() {
        repository.fetchComicsFromServer();
    }
}