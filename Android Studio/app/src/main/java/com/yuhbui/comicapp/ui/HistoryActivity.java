package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.adapters.ComicAdapter;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistory;
    private ComicAdapter historyAdapter;
    private Button btnPrevPageHistory, btnNextPageHistory;
    private LinearLayout layoutPageNumbersHistory;
    private int currentPage = 0;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 10; // 2 cột x 5 hàng = 10 truyện mỗi trang
    private int currentUserId = -1;

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

        // 2. Ánh xạ các thành phần Phân trang
        btnPrevPageHistory = findViewById(R.id.btnPrevPageHistory);
        btnNextPageHistory = findViewById(R.id.btnNextPageHistory);
        layoutPageNumbersHistory = findViewById(R.id.layoutPageNumbersHistory);

        // 3. Cấu hình RecyclerView hiển thị danh sách dạng GRID 2 CỘT giống Trang chủ
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewHistory.setLayoutManager(new GridLayoutManager(this, 2));
        historyAdapter = new ComicAdapter();
        recyclerViewHistory.setAdapter(historyAdapter);
        recyclerViewHistory.setNestedScrollingEnabled(false);

        // Lấy UserId từ SharedPrefs
        currentUserId = SharedPrefsManager.getUserId(this);

        // Cấu hình sự kiện click nút chuyển trang trái / phải
        btnPrevPageHistory.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                if (currentUserId != -1) loadReadingHistory(currentUserId, currentPage);
            }
        });

        btnNextPageHistory.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                if (currentUserId != -1) loadReadingHistory(currentUserId, currentPage);
            }
        });

        if (currentUserId != -1) {
            loadReadingHistory(currentUserId, currentPage);
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập để xem lịch sử!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadReadingHistory(int userId, int page) {
        // Lưu ý: Nếu ApiService chưa đổi cấu hình nhận Page, bạn hãy bổ sung params cho method getReadingHistoryByUserId(userId, page)
        ApiClient.getApiService().getReadingHistoryByUserId(userId).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comic> readComicsList = response.body();

                    if (readComicsList.isEmpty() && page == 0) {
                        Toast.makeText(HistoryActivity.this, "Bạn chưa đọc bộ truyện nào!", Toast.LENGTH_SHORT).show();
                        historyAdapter.setComics(readComicsList);
                        totalPages = 1;
                        updatePageNumbers(0, 1);
                    } else {
                        historyAdapter.setComics(readComicsList);

                        // Tính toán tổng số trang giống bên MainActivity
                        if (readComicsList.size() == PAGE_SIZE) {
                            totalPages = Math.max(totalPages, page + 2);
                        } else {
                            totalPages = page + 1;
                        }
                        updatePageNumbers(page, totalPages);
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

    private void updatePageNumbers(int currentPage, int totalPages) {
        layoutPageNumbersHistory.removeAllViews();

        int maxVisible = 5;
        int startPage = Math.max(0, currentPage - maxVisible / 2);
        int endPage = Math.min(totalPages - 1, startPage + maxVisible - 1);

        if (endPage - startPage < maxVisible - 1) {
            startPage = Math.max(0, endPage - maxVisible + 1);
        }

        int dpSize = dpToPx(34);
        int marginDp = dpToPx(2);

        for (int i = startPage; i <= endPage; i++) {
            final int pageIndex = i;
            TextView tvPage = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpSize, dpSize);
            params.setMargins(marginDp, 0, marginDp, 0);
            tvPage.setLayoutParams(params);
            tvPage.setText(String.valueOf(i + 1));
            tvPage.setGravity(Gravity.CENTER);
            tvPage.setTextSize(13);

            if (i == currentPage) {
                tvPage.setBackgroundResource(R.drawable.bg_page_btn);
                tvPage.setBackgroundColor(Color.parseColor("#FF9800"));
                tvPage.setTextColor(Color.WHITE);
                tvPage.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tvPage.setBackgroundResource(R.drawable.bg_page_btn);
                tvPage.setBackgroundColor(Color.parseColor("#EEEEEE"));
                tvPage.setTextColor(Color.parseColor("#333333"));
                tvPage.setOnClickListener(v -> {
                    HistoryActivity.this.currentPage = pageIndex;
                    loadReadingHistory(currentUserId, pageIndex);
                });
            }
            layoutPageNumbersHistory.addView(tvPage);
        }

        btnPrevPageHistory.setEnabled(currentPage > 0);
        btnNextPageHistory.setEnabled(currentPage < totalPages - 1);
        btnPrevPageHistory.setAlpha(currentPage > 0 ? 1.0f : 0.4f);
        btnNextPageHistory.setAlpha(currentPage < totalPages - 1 ? 1.0f : 0.4f);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

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
                // Đã ở trang này
                return true;
            } else if (id == R.id.menu_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                finish();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
}