package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Category;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.adapters.CategoryFilterAdapter;
import com.yuhbui.comicapp.ui.adapters.ComicAdapter;
import com.yuhbui.comicapp.ui.adapters.RankingAdapter;
import com.yuhbui.comicapp.ui.adapters.RecommendedBannerAdapter;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    // ========== PHẦN 1: TRUYỆN ĐỀ CỬ - Banner Slider ==========
    private ViewPager2 vpRecommended;
    private RecommendedBannerAdapter bannerAdapter;
    private ImageButton btnSliderPrev, btnSliderNext;
    private LinearLayout layoutDotIndicator;

    // ========== PHẦN 2: TRUYỆN MỚI CẬP NHẬT - Grid 2 cột ==========
    private RecyclerView recyclerViewComics;
    private ComicAdapter newUpdatesAdapter;
    private ImageView btnFilterIcon;
    private TextView tvActiveFilter;

    // Phân trang
    private Button btnPrevPage, btnNextPage;
    private LinearLayout layoutPageNumbers;
    private int currentPage = 0;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 10;

    // Bộ lọc thể loại
    private CategoryFilterAdapter catFilterAdapter;
    private Integer activeFilterCategoryId = null;
    private String activeFilterCategoryName = null;

    // ========== PHẦN 3: BẢNG XẾP HẠNG TOP 10 ==========
    private RecyclerView rvTopRank;
    private RankingAdapter rankingAdapter;
    private RadioGroup rgRankFilter;

    // ========== HEADER ==========
    private View layoutHeader;
    private ImageView headerMenu, headerSearch, headerNotification, headerAvatar;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupHeader();
        setupRecommendedSlider();
        setupNewUpdatesSection();
        setupRankingSection();
        loadAllData();
    }

    // ========== KHỞI TẠO VIEW ==========

    private void initViews() {
        // Header
        layoutHeader       = findViewById(R.id.layoutHeader);
        headerMenu         = layoutHeader.findViewById(R.id.headerMenu);
        headerLogo         = layoutHeader.findViewById(R.id.headerLogo);
        headerSearch       = layoutHeader.findViewById(R.id.headerSearch);
        headerNotification = layoutHeader.findViewById(R.id.headerNotification);
        headerAvatar       = layoutHeader.findViewById(R.id.headerAvatar);

        // Phần 1: Slider
        vpRecommended    = findViewById(R.id.vpRecommended);
        btnSliderPrev    = findViewById(R.id.btnSliderPrev);
        btnSliderNext    = findViewById(R.id.btnSliderNext);
        layoutDotIndicator = findViewById(R.id.layoutDotIndicator);

        // Phần 2: Truyện mới
        recyclerViewComics = findViewById(R.id.recyclerViewComics);
        btnFilterIcon      = findViewById(R.id.btnFilterIcon);
        tvActiveFilter     = findViewById(R.id.tvActiveFilter);
        btnPrevPage        = findViewById(R.id.btnPrevPage);
        btnNextPage        = findViewById(R.id.btnNextPage);
        layoutPageNumbers  = findViewById(R.id.layoutPageNumbers);

        // Phần 3: BXH
        rvTopRank    = findViewById(R.id.rvTopRank);
        rgRankFilter = findViewById(R.id.rgRankFilter);
    }

    // ========== HEADER ==========

    private void setupHeader() {
        headerMenu.setOnClickListener(v -> showHeaderPopupMenu(v));
        headerLogo.setOnClickListener(v -> Toast.makeText(this, "Trang chủ", Toast.LENGTH_SHORT).show());
        headerSearch.setOnClickListener(v -> Toast.makeText(this, "Tìm kiếm truyện", Toast.LENGTH_SHORT).show());
        headerNotification.setOnClickListener(v -> Toast.makeText(this, "Thông báo", Toast.LENGTH_SHORT).show());
        headerAvatar.setOnClickListener(v -> showAvatarMenu(v));
    }

    // ========== PHẦN 1: SLIDER TRUYỆN ĐỀ CỬ ==========

    private void setupRecommendedSlider() {
        bannerAdapter = new RecommendedBannerAdapter();
        vpRecommended.setAdapter(bannerAdapter);

        // Nút chuyển trang slide
        btnSliderPrev.setOnClickListener(v -> {
            int cur = vpRecommended.getCurrentItem();
            if (cur > 0) vpRecommended.setCurrentItem(cur - 1, true);
        });
        btnSliderNext.setOnClickListener(v -> {
            int cur = vpRecommended.getCurrentItem();
            int max = bannerAdapter.getItemCount() - 1;
            if (cur < max) vpRecommended.setCurrentItem(cur + 1, true);
        });

        // Lắng nghe thay đổi trang để cập nhật dot indicator
        vpRecommended.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDotIndicator(position);
            }
        });
    }

    /**
     * Tạo lại dot indicator sau khi dữ liệu banner được tải
     */
    private void buildDotIndicator(int count) {
        layoutDotIndicator.removeAllViews();
        int dp6 = dpToPx(6);
        int dp8 = dpToPx(8);
        int dp3 = dpToPx(3);

        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp6, dp6);
            params.setMargins(dp3, 0, dp3, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.dot_inactive);
            layoutDotIndicator.addView(dot);
        }
        updateDotIndicator(0);
    }

    private void updateDotIndicator(int activePosition) {
        for (int i = 0; i < layoutDotIndicator.getChildCount(); i++) {
            View dot = layoutDotIndicator.getChildAt(i);
            if (i == activePosition) {
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
                p.setMargins(dpToPx(3), 0, dpToPx(3), 0);
                dot.setLayoutParams(p);
                dot.setBackgroundResource(R.drawable.dot_active);
            } else {
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dpToPx(6), dpToPx(6));
                p.setMargins(dpToPx(3), 0, dpToPx(3), 0);
                dot.setLayoutParams(p);
                dot.setBackgroundResource(R.drawable.dot_inactive);
            }
        }
    }

    // ========== PHẦN 2: TRUYỆN MỚI CẬP NHẬT ==========

    private void setupNewUpdatesSection() {
        // RecyclerView grid 2 cột
        newUpdatesAdapter = new ComicAdapter();
        recyclerViewComics.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewComics.setAdapter(newUpdatesAdapter);
        recyclerViewComics.setNestedScrollingEnabled(false);

        // Icon lọc - mở BottomSheetDialog chọn thể loại
        btnFilterIcon.setOnClickListener(v -> showCategoryFilterDialog());

        // Nút phân trang
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                if (activeFilterCategoryId != null) {
                    loadComicsByCategory(activeFilterCategoryId);
                } else {
                    loadNewUpdatesComics(currentPage);
                }
            }
        });
        btnNextPage.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                if (activeFilterCategoryId != null) {
                    loadComicsByCategory(activeFilterCategoryId);
                } else {
                    loadNewUpdatesComics(currentPage);
                }
            }
        });
    }

    /**
     * Hiển thị BottomSheetDialog chứa danh sách thể loại để lọc
     */
    private void showCategoryFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_filter, null);
        dialog.setContentView(dialogView);

        RecyclerView rvPopup = dialogView.findViewById(R.id.rvCategoryPopup);
        TextView tvClear = dialogView.findViewById(R.id.tvClearFilter);

        // Adapter thể loại với grid 3 cột
        catFilterAdapter = new CategoryFilterAdapter(category -> {
            if (category == null) {
                // Xóa bộ lọc
                activeFilterCategoryId = null;
                activeFilterCategoryName = null;
                tvActiveFilter.setVisibility(View.GONE);
                tvActiveFilter.setText("");
                currentPage = 0;
                loadNewUpdatesComics(currentPage);
            } else {
                // Áp dụng lọc theo thể loại
                activeFilterCategoryId = category.getCategoryId();
                activeFilterCategoryName = category.getName();
                tvActiveFilter.setText("Đang lọc: " + category.getName());
                tvActiveFilter.setVisibility(View.VISIBLE);
                currentPage = 0;
                loadComicsByCategory(activeFilterCategoryId);
            }
            dialog.dismiss();
        });

        rvPopup.setLayoutManager(new GridLayoutManager(this, 3));
        rvPopup.setAdapter(catFilterAdapter);

        // Tải danh sách thể loại
        ApiClient.getApiService().getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    catFilterAdapter.setCategories(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải danh mục: " + t.getMessage());
            }
        });

        // Nút xóa lọc
        tvClear.setOnClickListener(v -> {
            activeFilterCategoryId = null;
            activeFilterCategoryName = null;
            tvActiveFilter.setVisibility(View.GONE);
            tvActiveFilter.setText("");
            currentPage = 0;
            loadNewUpdatesComics(currentPage);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Cập nhật thanh phân trang: hiển thị các số trang, highlight trang hiện tại
     */
    private void updatePageNumbers(int currentPage, int totalPages) {
        layoutPageNumbers.removeAllViews();

        int maxVisible = 5; // Số trang tối đa hiển thị cùng lúc
        int startPage = Math.max(0, currentPage - maxVisible / 2);
        int endPage = Math.min(totalPages - 1, startPage + maxVisible - 1);

        // Điều chỉnh lại start nếu end bị giới hạn
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
                // Trang hiện tại - nền cam, chữ trắng
                tvPage.setBackgroundResource(R.drawable.bg_page_btn);
                tvPage.setBackgroundColor(Color.parseColor("#FF9800"));
                tvPage.setTextColor(Color.WHITE);
                tvPage.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                // Trang khác - nền xám nhạt, chữ tối
                tvPage.setBackgroundResource(R.drawable.bg_page_btn);
                tvPage.setBackgroundColor(Color.parseColor("#EEEEEE"));
                tvPage.setTextColor(Color.parseColor("#333333"));
                tvPage.setOnClickListener(v -> {
                    MainActivity.this.currentPage = pageIndex;
                    if (activeFilterCategoryId != null) {
                        loadComicsByCategory(activeFilterCategoryId);
                    } else {
                        loadNewUpdatesComics(pageIndex);
                    }
                });
            }
            layoutPageNumbers.addView(tvPage);
        }

        // Cập nhật trạng thái nút < và >
        btnPrevPage.setEnabled(currentPage > 0);
        btnNextPage.setEnabled(currentPage < totalPages - 1);
        btnPrevPage.setAlpha(currentPage > 0 ? 1.0f : 0.4f);
        btnNextPage.setAlpha(currentPage < totalPages - 1 ? 1.0f : 0.4f);
    }

    // ========== PHẦN 3: BẢNG XẾP HẠNG ==========

    private void setupRankingSection() {
        rankingAdapter = new RankingAdapter();
        rvTopRank.setLayoutManager(new LinearLayoutManager(this));
        rvTopRank.setAdapter(rankingAdapter);
        rvTopRank.setNestedScrollingEnabled(false);

        // Tab lọc ngày/tuần/tháng
        rgRankFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDay) {
                loadRankingData("day");
            } else if (checkedId == R.id.rbWeek) {
                loadRankingData("week");
            } else if (checkedId == R.id.rbMonth) {
                loadRankingData("month");
            }
        });
    }

    // ========== LOAD DỮ LIỆU TỪ API ==========

    private void loadAllData() {
        loadRecommendedComics();
        loadNewUpdatesComics(currentPage);
        loadRankingData("day");
    }

    private void loadRecommendedComics() {
        ApiClient.getApiService().getRecommendedComics().enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bannerAdapter.setComics(response.body());
                    buildDotIndicator(response.body().size());
                }
            }
            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải truyện đề cử: " + t.getMessage());
            }
        });
    }

    private void loadNewUpdatesComics(int page) {
        ApiClient.getApiService().getHomeUpdates(page).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comic> comics = response.body();
                    newUpdatesAdapter.setComics(comics);

                    // Tính tổng trang (nếu trả về đủ PAGE_SIZE thì vẫn còn trang sau)
                    if (comics.size() == PAGE_SIZE) {
                        totalPages = Math.max(totalPages, page + 2);
                    } else {
                        totalPages = page + 1;
                    }
                    updatePageNumbers(page, totalPages);
                }
            }
            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải truyện mới: " + t.getMessage());
            }
        });
    }

    private void loadComicsByCategory(int catId) {
        ApiClient.getApiService().getComicsByCategory(catId).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comic> comics = response.body();
                    newUpdatesAdapter.setComics(comics);
                    // Phân trang đơn giản khi lọc
                    totalPages = 1;
                    updatePageNumbers(0, 1);
                }
            }
            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi lọc thể loại: " + t.getMessage());
            }
        });
    }

    private void loadRankingData(String type) {
        ApiClient.getApiService().getTopRanking(type).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    rankingAdapter.setComics(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi BXH: " + t.getMessage());
            }
        });
    }

    private void showHeaderPopupMenu(View anchorView) {
        androidx.appcompat.widget.PopupMenu popupMenu =
                new androidx.appcompat.widget.PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_header_options, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                Toast.makeText(this, "Trang chủ", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (id == R.id.menu_follow) {
                startActivity(new Intent(this, FollowActivity.class));
                return true;
            } else if (id == R.id.menu_downloads) {
                Toast.makeText(this, "Truyện tải xuống", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_profile) {
                Toast.makeText(this, "Hồ sơ cá nhân", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_logout) {
                performLogout();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showAvatarMenu(View anchorView) {
        androidx.appcompat.widget.PopupMenu popupMenu =
                new androidx.appcompat.widget.PopupMenu(this, anchorView);

        // Bạn có thể tạo thêm một file menu riêng (VD: menu_avatar.xml) hoặc thêm code tay
        popupMenu.getMenu().add(0, 1, 0, "Hồ sơ cá nhân");
        popupMenu.getMenu().add(0, 2, 1, "Đăng xuất");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                Toast.makeText(this, "Mở Hồ sơ cá nhân...", Toast.LENGTH_SHORT).show();
                // TODO: Chuyển sang ProfileActivity nếu có
                return true;
            } else if (item.getItemId() == 2) {
                performLogout();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void performLogout() {
        Toast.makeText(this, "Đang đăng xuất...", Toast.LENGTH_SHORT).show();

        // 1. Xóa thông tin đã lưu trong bộ nhớ
        com.yuhbui.comicapp.utils.SharedPrefsManager.logout(this);

        // 2. Chuyển người dùng về màn hình Login
        Intent intent = new Intent(this, LoginActivity.class);
        // Xoá toàn bộ lịch sử Activity để người dùng không thể bấm phím "Back" quay lại MainActivity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}