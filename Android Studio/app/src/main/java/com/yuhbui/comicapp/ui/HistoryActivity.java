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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Category;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.adapters.CategoryFilterAdapter;
import com.yuhbui.comicapp.ui.adapters.ComicAdapter;
import com.yuhbui.comicapp.utils.HeaderUtils;          // THÊM: Nhúng lớp tiện ích Header dùng chung
import com.yuhbui.comicapp.utils.MenuUtils;
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    private RecyclerView recyclerViewHistory;
    private ComicAdapter historyAdapter;
    private Button btnPrevPageHistory, btnNextPageHistory;
    private LinearLayout layoutPageNumbersHistory;
    private int currentPage = 0;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 10;
    private int currentUserId = -1;

    // Các thành phần của Header và bộ lọc
    private View layoutHeader;
    private TextView headerLogo;
    private ImageView imgHistoryFilter;
    private CategoryFilterAdapter filterAdapter;
    private List<Integer> selectedCategoryIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 1. Ánh xạ DrawerLayout và Header layout
        drawerLayout = findViewById(R.id.drawerLayout);
        layoutHeader = findViewById(R.id.layoutHeaderHistory);
        headerLogo   = layoutHeader.findViewById(R.id.headerLogo);

        // 2. TỐI ƯU: Khởi tạo các tính năng cốt lõi của Header (Menu, Chuông, Avatar, Badge) bằng 1 dòng duy nhất
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);

        // 3. Khởi tạo chức năng điều hướng cho các nút bấm bên trong Menu trượt
        MenuUtils.setupSideMenu(this, drawerLayout, layoutHeader.findViewById(R.id.headerMenu));

        headerLogo.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // 4. Cấu hình xử lý khi nhấn nút Back hệ thống (Ưu tiên đóng menu trượt)
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        // 5. Ánh xạ các thành phần Phân trang và Danh sách truyện
        btnPrevPageHistory = findViewById(R.id.btnPrevPageHistory);
        btnNextPageHistory = findViewById(R.id.btnNextPageHistory);
        layoutPageNumbersHistory = findViewById(R.id.layoutPageNumbersHistory);

        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewHistory.setLayoutManager(new GridLayoutManager(this, 2));
        historyAdapter = new ComicAdapter();
        recyclerViewHistory.setAdapter(historyAdapter);
        recyclerViewHistory.setNestedScrollingEnabled(false);

        currentUserId = SharedPrefsManager.getUserId(this);

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

        imgHistoryFilter = findViewById(R.id.imgPageFilter);
        selectedCategoryIds.add(0);

        filterAdapter = new CategoryFilterAdapter(selectedIds -> {
            selectedCategoryIds = selectedIds;
            currentPage = 0;
            if (currentUserId != -1) loadReadingHistory(currentUserId, currentPage);
        });

        imgHistoryFilter.setOnClickListener(v -> showCategoryFilterDialog());
    }

    // THÊM: Đồng bộ và làm tươi Avatar, Số thông báo mới mỗi khi người dùng quay lại trang này
    @Override
    protected void onResume() {
        super.onResume();
        if (layoutHeader != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
            HeaderUtils.loadUnreadNotificationCount(this, layoutHeader.findViewById(R.id.tvNotificationBadge));
        }
    }

    private void showCategoryFilterDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_filter, null);
        dialog.setContentView(dialogView);

        RecyclerView rvPopup = dialogView.findViewById(R.id.rvCategoryPopup);
        TextView tvClear = dialogView.findViewById(R.id.tvClearFilter);

        rvPopup.setLayoutManager(new GridLayoutManager(this, 3));
        rvPopup.setAdapter(filterAdapter);

        ApiClient.getApiService().getAllCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Category> displayList = new ArrayList<>();
                    Category all = new Category(); all.setCategoryId(0); all.setName("Tất cả");
                    displayList.add(all);
                    displayList.addAll(response.body());
                    filterAdapter.setCategories(displayList);
                }
            }
            @Override public void onFailure(Call<List<Category>> call, Throwable t) {}
        });

        tvClear.setOnClickListener(v -> {
            selectedCategoryIds.clear(); selectedCategoryIds.add(0);
            filterAdapter.notifyDataSetChanged();
            currentPage = 0;
            if (currentUserId != -1) loadReadingHistory(currentUserId, 0);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void loadReadingHistory(int userId, int page) {
        List<Integer> idsToSend = new ArrayList<>(selectedCategoryIds);
        if (idsToSend.contains(0)) idsToSend.clear();

        ApiClient.getApiService().getReadingHistoryByUserId(userId, idsToSend).enqueue(new Callback<List<Comic>>() {
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
}