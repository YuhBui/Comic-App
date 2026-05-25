package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.data.model.ReadingHistory;
import com.yuhbui.comicapp.ui.adapters.ComicAdapter; // Có thể tái sử dụng ComicAdapter hoặc tạo Adapter riêng tùy cấu trúc DB
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistory;
    private ComicAdapter historyAdapter; // Giả định dùng chung định dạng hiển thị truyện hoặc bạn thay bằng HistoryAdapter riêng

    // Các thành phần của Header
    private View layoutHeader;
    private ImageView headerMenu, headerSearch, headerNotification, headerAvatar;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 1. Ánh xạ Header và đăng ký sự kiện thanh điều hướng chung
        layoutHeader = findViewById(R.id.layoutHeaderHistory);
        headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        headerSearch = layoutHeader.findViewById(R.id.headerSearch);
        headerNotification = layoutHeader.findViewById(R.id.headerNotification);
        headerAvatar = layoutHeader.findViewById(R.id.headerAvatar);

        headerMenu.setOnClickListener(v -> showHeaderPopupMenu(v));
        headerLogo.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // 2. Cấu hình RecyclerView hiển thị danh sách truyện đã đọc
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new ComicAdapter();
        recyclerViewHistory.setAdapter(historyAdapter);

        // 3. Lấy UserId từ SharedPrefs và tải danh sách lịch sử từ Backend
        int userId = SharedPrefsManager.getUserId(this);
        if (userId != -1) {
            loadReadingHistory(userId);
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập để xem lịch sử!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadReadingHistory(int userId) {
        // Thực hiện cuộc gọi mạng kết nối đến Database qua API mới thêm
        ApiClient.getApiService().getReadingHistoryByUserId(userId).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comic> readComicsList = response.body();

                    if (readComicsList.isEmpty()) {
                        Toast.makeText(HistoryActivity.this, "Bạn chưa đọc bộ truyện nào!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Nạp danh sách Comic nhận được thật từ DB vào adapter để hiển thị ra màn hình
                        historyAdapter.setComics(readComicsList);
                    }
                } else {
                    Toast.makeText(HistoryActivity.this, "Không thể tải dữ liệu lịch sử!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi kết nối database lịch sử: " + t.getMessage());
                Toast.makeText(HistoryActivity.this, "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Gắn PopupMenu để đồng bộ hành vi chuyển trang toàn app
    private void showHeaderPopupMenu(View anchorView) {
        androidx.appcompat.widget.PopupMenu popupMenu = new androidx.appcompat.widget.PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_header_options, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.menu_history) {
                // Đang ở màn hình lịch sử, chỉ cần đóng menu thông thường
                return true;
            }
            // Khai báo các màn hình khác tương tự...
            return false;
        });
        popupMenu.show();
    }
}