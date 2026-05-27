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

/**
 * FavoritesActivity - Màn hình Truyện Yêu Thích
 * Hiển thị danh sách truyện user đã nhấn ❤ (yêu thích), grid 2 cột 5 hàng + phân trang
 */
public class FollowActivity extends AppCompatActivity {

    private RecyclerView recyclerViewFavorites;
    private ComicAdapter favoritesAdapter;
    private Button btnPrevPageFavorites, btnNextPageFavorites;
    private LinearLayout layoutPageNumbersFavorites;
    private LinearLayout layoutEmptyFavorites;
    private TextView tvFavoritesCount;

    private int currentPage = 0;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 10; // 2 cột x 5 hàng = 10 truyện/trang
    private int currentUserId = -1;

    // Lưu toàn bộ danh sách để phân trang ở client (tránh gọi API nhiều lần)
    private List<Comic> allFavorites = null;

    // Header
    private View layoutHeader;
    private ImageView headerMenu, headerSearch, headerNotification, headerAvatar;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);

        initViews();
        setupHeader();
        setupRecyclerView();
        setupPagination();
        loadData();
    }

    private void initViews() {
        // Header
        layoutHeader           = findViewById(R.id.layoutHeaderFavorites);
        headerMenu             = layoutHeader.findViewById(R.id.headerMenu);
        headerLogo             = layoutHeader.findViewById(R.id.headerLogo);
        headerSearch           = layoutHeader.findViewById(R.id.headerSearch);
        headerNotification     = layoutHeader.findViewById(R.id.headerNotification);
        headerAvatar           = layoutHeader.findViewById(R.id.headerAvatar);

        // Content
        recyclerViewFavorites      = findViewById(R.id.recyclerViewFavorites);
        btnPrevPageFavorites       = findViewById(R.id.btnPrevPageFavorites);
        btnNextPageFavorites       = findViewById(R.id.btnNextPageFavorites);
        layoutPageNumbersFavorites = findViewById(R.id.layoutPageNumbersFavorites);
        layoutEmptyFavorites       = findViewById(R.id.layoutEmptyFavorites);
        tvFavoritesCount           = findViewById(R.id.tvFavoritesCount);

        currentUserId = SharedPrefsManager.getUserId(this);
    }

    private void setupHeader() {
        headerMenu.setOnClickListener(v -> showHeaderPopupMenu(v));
        headerLogo.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        headerSearch.setOnClickListener(v ->
                Toast.makeText(this, "Tìm kiếm truyện", Toast.LENGTH_SHORT).show());
        headerNotification.setOnClickListener(v ->
                Toast.makeText(this, "Thông báo", Toast.LENGTH_SHORT).show());
        headerAvatar.setOnClickListener(v ->
                Toast.makeText(this, "Hồ sơ cá nhân", Toast.LENGTH_SHORT).show());
    }

    private void setupRecyclerView() {
        favoritesAdapter = new ComicAdapter();
        recyclerViewFavorites.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewFavorites.setAdapter(favoritesAdapter);
        recyclerViewFavorites.setNestedScrollingEnabled(false);
    }

    private void setupPagination() {
        btnPrevPageFavorites.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                showPage(currentPage);
            }
        });
        btnNextPageFavorites.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                showPage(currentPage);
            }
        });
    }

    private void loadData() {
        if (currentUserId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem truyện yêu thích!", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        ApiClient.getApiService().getFavoriteComics(currentUserId).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allFavorites = response.body();

                    if (allFavorites.isEmpty()) {
                        showEmptyState();
                    } else {
                        // Tính tổng số trang
                        totalPages = (int) Math.ceil((double) allFavorites.size() / PAGE_SIZE);
                        tvFavoritesCount.setText(allFavorites.size() + " truyện");
                        layoutEmptyFavorites.setVisibility(View.GONE);
                        recyclerViewFavorites.setVisibility(View.VISIBLE);
                        showPage(0);
                    }
                } else {
                    Toast.makeText(FollowActivity.this, "Không thể tải danh sách yêu thích!", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi kết nối tải yêu thích: " + t.getMessage());
                Toast.makeText(FollowActivity.this, "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    /**
     * Hiển thị trang hiện tại từ danh sách đã tải (phân trang client-side)
     */
    private void showPage(int page) {
        if (allFavorites == null) return;
        currentPage = page;

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allFavorites.size());

        if (start < allFavorites.size()) {
            favoritesAdapter.setComics(allFavorites.subList(start, end));
        }
        updatePageNumbers(page, totalPages);
    }

    private void showEmptyState() {
        layoutEmptyFavorites.setVisibility(View.VISIBLE);
        recyclerViewFavorites.setVisibility(View.GONE);
        tvFavoritesCount.setText("0 truyện");
        totalPages = 1;
        updatePageNumbers(0, 1);
    }

    private void updatePageNumbers(int currentPage, int totalPages) {
        layoutPageNumbersFavorites.removeAllViews();

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
                tvPage.setBackgroundColor(Color.parseColor("#E91E63")); // Màu hồng cho favorites
                tvPage.setTextColor(Color.WHITE);
                tvPage.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tvPage.setBackgroundResource(R.drawable.bg_page_btn);
                tvPage.setBackgroundColor(Color.parseColor("#EEEEEE"));
                tvPage.setTextColor(Color.parseColor("#333333"));
                tvPage.setOnClickListener(v -> showPage(pageIndex));
            }
            layoutPageNumbersFavorites.addView(tvPage);
        }

        btnPrevPageFavorites.setEnabled(currentPage > 0);
        btnNextPageFavorites.setEnabled(currentPage < totalPages - 1);
        btnPrevPageFavorites.setAlpha(currentPage > 0 ? 1.0f : 0.4f);
        btnNextPageFavorites.setAlpha(currentPage < totalPages - 1 ? 1.0f : 0.4f);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showHeaderPopupMenu(View anchorView) {
        androidx.appcompat.widget.PopupMenu popupMenu =
                new androidx.appcompat.widget.PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_header_options, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.menu_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                finish();
                return true;
            } else if (id == R.id.menu_follow) {
                // Đã ở trang này
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
}
