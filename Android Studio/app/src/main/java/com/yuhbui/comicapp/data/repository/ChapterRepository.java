package com.yuhbui.comicapp.data.repository;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Chapter;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChapterRepository {

    private MutableLiveData<List<Chapter>> chaptersLiveData = new MutableLiveData<>();

    public MutableLiveData<List<Chapter>> getChaptersLiveData() {
        return chaptersLiveData;
    }

    public void fetchChapters(int comicId) {
        ApiClient.getApiService().getChaptersByComicId(comicId).enqueue(new Callback<List<Chapter>>() {
            @Override
            public void onResponse(Call<List<Chapter>> call, Response<List<Chapter>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    chaptersLiveData.setValue(response.body());
                } else {
                    Log.e("API_ERROR", "Lỗi lấy chapter");
                }
            }

            @Override
            public void onFailure(Call<List<Chapter>> call, Throwable t) {
                Log.e("API_ERROR", "Lỗi mạng: " + t.getMessage());
            }
        });
    }
}