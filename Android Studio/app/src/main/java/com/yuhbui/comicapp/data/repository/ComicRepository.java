package com.yuhbui.comicapp.data.repository;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comic;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicRepository {

    private MutableLiveData<List<Comic>> comicsLiveData = new MutableLiveData<>();

    public MutableLiveData<List<Comic>> getComicsLiveData() {
        return comicsLiveData;
    }

    public void fetchComicsFromServer() {
        ApiClient.getApiService().getAllComics().enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    comicsLiveData.setValue(response.body());
                } else {
                    Log.e("API_ERROR", "Lỗi server trả về không thành công");
                }
            }

            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                // Thất bại (VD: Mất mạng, sai IP, chưa mở tường lửa)
                Log.e("API_ERROR", "Lỗi kết nối: " + t.getMessage());
            }
        });
    }
}