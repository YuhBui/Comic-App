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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Category;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.adapters.ComicAdapter;
import com.yuhbui.comicapp.ui.adapters.CategoryFilterAdapter;
import com.yuhbui.comicapp.utils.HeaderUtils;
import com.yuhbui.comicapp.utils.MenuUtils;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    private RecyclerView recyclerViewFavorites;
    private ComicAdapter favoritesAdapter;
    private Button btnPrevPageFavorites, btnNextPageFavorites;
    private LinearLayout layoutPageNumbersFavorites;
    private LinearLayout layoutEmptyFavorites;
    private ImageView imgFavoritesFilter;
    private CategoryFilterAdapter filterAdapter;
    private List<Category> masterCategoriesList = new ArrayList<>();
    private List<Integer> selectedCategoryIds = new ArrayList<>();
    private int currentPage = 0;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 10;
    private int currentUserId = -1;

    private List<Comic> allFavorites = null;
    private View layoutHeader;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);

        initViews();
        setupHeader();
        setupRecyclerView();
        setupPagination();

        // Cấu hình ban đầu bộ lọc thể loại
        selectedCategoryIds.add(0);
        setupFilterAdapter();

        imgFavoritesFilter.setOnClickListener(v -> showCategoryFilterDialog());

        // Bắt sự kiện phím BACK hệ thống để ưu tiên đóng DrawerLayout trước khi thoát trang
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

        loadData();
        loadCategoriesDataForFilter();
    }

    // Đồng bộ và tự động cập nhật Avatar mới nhất cùng số thông báo chưa đọc khi quay lại màn hình
    @Override
    protected void onResume() {
        super.onResume();
        if (layoutHeader != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
            HeaderUtils.loadUnreadNotificationCount(this, layoutHeader.findViewById(R.id.tvNotificationBadge));
        }
    }

    private void initViews() {
        // Ánh xạ thành phần DrawerLayout
        drawerLayout               = findViewById(R.id.drawerLayout);

        // Header
        layoutHeader               = findViewById(R.id.layoutHeaderFavorites);
        headerLogo                 = layoutHeader.findViewById(R.id.headerLogo);

        // Content
        recyclerViewFavorites      = findViewById(R.id.recyclerViewFavorites);
        btnPrevPageFavorites       = findViewById(R.id.btnPrevPageFavorites);
        btnNextPageFavorites       = findViewById(R.id.btnNextPageFavorites);
        layoutPageNumbersFavorites = findViewById(R.id.layoutPageNumbersFavorites);
        layoutEmptyFavorites       = findViewById(R.id.layoutEmptyFavorites);
        imgFavoritesFilter         = findViewById(R.id.imgPageFilter);

        headerLogo.setText(android.text.Html.fromHtml("<font color='#D97707'>h</font><font color='#FFFFFF'>ay</font><font color='#D97707'>c</font><font color='#FFFFFF'>omic</font>", android.text.Html.FROM_HTML_MODE_COMPACT));

        currentUserId = SharedPrefsManager.getUserId(this);
    }

    private void setupHeader() {
        // 1. Khởi tạo chức năng cốt lõi của Header (Menu, Thông báo, Avatar).
        // Hàm này trong HeaderUtils đã tích hợp sẵn logic xử lý ô Tìm kiếm toàn cục cho các trang con rồi.
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);

        // 2. Liên kết điều hướng các nút bấm bên trong Menu trượt trái
        MenuUtils.setupSideMenu(this, drawerLayout, layoutHeader.findViewById(R.id.headerMenu));

        // CHỖ CẦN SỬA: Đã XÓA đoạn code ép ẩn nút Search (setVisibility(View.GONE)).
        // Giờ đây nút tìm kiếm sẽ luôn hiện và hoạt động như một công cụ Tìm kiếm chung toàn app.

        headerLogo.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void setupFilterAdapter() {
        filterAdapter = new CategoryFilterAdapter(selectedIds -> {
            selectedCategoryIds = selectedIds;
            currentPage = 0;
            loadData();
        });
        if (!masterCategoriesList.isEmpty()) {
            filterAdapter.setCategories(masterCategoriesList);
        }
    }

    private void showCategoryFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_filter, null);
        dialog.setContentView(dialogView);

        RecyclerView rvPopup = dialogView.findViewById(R.id.rvCategoryPopup);
        TextView tvClear = dialogView.findViewById(R.id.tvClearFilter);

        rvPopup.setLayoutManager(new GridLayoutManager(this, 3));
        rvPopup.setAdapter(filterAdapter);

        tvClear.setOnClickListener(v -> {
            selectedCategoryIds.clear();
            selectedCategoryIds.add(0);
            setupFilterAdapter();
            rvPopup.setAdapter(filterAdapter);
            currentPage = 0;
            loadData();
            dialog.dismiss();
        });

        dialog.show();
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

    private void loadCategoriesDataForFilter() {
        ApiClient.getApiService().getAllCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    masterCategoriesList.clear();

                    Category allCat = new Category();
                    allCat.setCategoryId(0);
                    allCat.setName("Tất cả");

                    masterCategoriesList.add(allCat);
                    masterCategoriesList.addAll(response.body());
                    filterAdapter.setCategories(masterCategoriesList);
                }
            }
            @Override public void onFailure(Call<List<Category>> call, Throwable t) {}
        });
    }

    private void loadData() {
        if (currentUserId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem truyện yêu thích!", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        List<Integer> idsToSend = new ArrayList<>(selectedCategoryIds);
        if (idsToSend.contains(0)) {
            idsToSend.clear();
        }

        ApiClient.getApiService().getFavoriteComicsFiltered(currentUserId, idsToSend).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allFavorites = response.body();

                    if (allFavorites.isEmpty()) {
                        showEmptyState();
                    } else {
                        totalPages = (int) Math.ceil((double) allFavorites.size() / PAGE_SIZE);
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

        // Định hình kích thước chuẩn 40dp x 40dp đồng bộ toàn hệ thống
        int density = (int) getResources().getDisplayMetrics().density;
        int size = 40 * density;
        int margin = 4 * density;

        for (int i = startPage; i <= endPage; i++) {
            final int pageIndex = i;
            Button btnPage = new Button(this); // Chuyển từ TextView sang Button
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            btnPage.setLayoutParams(params);
            btnPage.setPadding(0, 0, 0, 0);
            btnPage.setText(String.valueOf(i + 1));
            btnPage.setTextSize(14);
            btnPage.setAllCaps(false);

            if (i == currentPage) {
                // Trang hiện tại: Nền màu cam hổ phách (#FFB77D), chữ nâu đen (#4D2600)
                btnPage.setBackgroundResource(R.drawable.bg_nav_btn);
                btnPage.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFB77D")));
                btnPage.setTextColor(Color.parseColor("#4D2600"));
                btnPage.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                // Các trang khác: Nền đen xám (#1E1E1E), chữ hạt dẻ mờ (#DBC2B0)
                btnPage.setBackgroundResource(R.drawable.bg_nav_btn);
                btnPage.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E1E1E")));
                btnPage.setTextColor(Color.parseColor("#DBC2B0"));
                btnPage.setOnClickListener(v -> showPage(pageIndex));
            }
            layoutPageNumbersFavorites.addView(btnPage);
        }

        // Đồng bộ màu sắc 2 nút mũi tên điều hướng < và >
        btnPrevPageFavorites.setEnabled(currentPage > 0);
        btnNextPageFavorites.setEnabled(currentPage < totalPages - 1);

        btnPrevPageFavorites.setBackgroundResource(R.drawable.bg_nav_btn);
        btnPrevPageFavorites.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E1E1E")));
        btnPrevPageFavorites.setTextColor(Color.parseColor(currentPage > 0 ? "#DBC2B0" : "#555555"));

        btnNextPageFavorites.setBackgroundResource(R.drawable.bg_nav_btn);
        btnNextPageFavorites.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E1E1E")));
        btnNextPageFavorites.setTextColor(Color.parseColor(currentPage < totalPages - 1 ? "#DBC2B0" : "#555555"));
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}