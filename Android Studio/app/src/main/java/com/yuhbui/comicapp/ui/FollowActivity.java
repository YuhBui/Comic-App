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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Category;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.adapters.ComicAdapter;
import com.yuhbui.comicapp.ui.adapters.CategoryFilterAdapter;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowActivity extends AppCompatActivity {

    private RecyclerView recyclerViewFavorites;
    private ComicAdapter favoritesAdapter;
    private Button btnPrevPageFavorites, btnNextPageFavorites;
    private LinearLayout layoutPageNumbersFavorites;
    private LinearLayout layoutEmptyFavorites;
    private ImageView imgFavoritesFilter;
    private CategoryFilterAdapter filterAdapter;
    private List<Category> masterCategoriesList = new ArrayList<>(); // Bộ nhớ đệm danh sách thể loại từ server
    private List<Integer> selectedCategoryIds = new ArrayList<>();
    private int currentPage = 0;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 10;
    private int currentUserId = -1;

    private List<Comic> allFavorites = null;
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

        // Cấu hình ban đầu: Mặc định chọn nút ảo "Tất cả" (ID = 0)
        selectedCategoryIds.add(0);
        setupFilterAdapter();

        // Lắng nghe sự kiện click nút hình phễu lọc -> Mở BottomSheetDialog
        imgFavoritesFilter.setOnClickListener(v -> showCategoryFilterDialog());

        loadData();
        loadCategoriesDataForFilter();
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

        imgFavoritesFilter         = findViewById(R.id.imgPageFilter);

        currentUserId = SharedPrefsManager.getUserId(this);
    }

    // Hàm khởi tạo và gán Callback sự kiện đa chọn cho Adapter bộ lọc
    private void setupFilterAdapter() {
        filterAdapter = new CategoryFilterAdapter(new CategoryFilterAdapter.OnCatClickListener() {
            @Override
            public void onCatClick(List<Integer> selectedIds) {
                selectedCategoryIds = selectedIds;
                currentPage = 0; // Đổi bộ lọc thì reset về trang đầu tiên
                loadData(); // Tải lại danh sách truyện yêu thích theo bộ lọc mới
            }
        });
        if (!masterCategoriesList.isEmpty()) {
            filterAdapter.setCategories(masterCategoriesList);
        }
    }

    // ĐÃ THÊM: Hàm khởi tạo hộp thoại BottomSheet trượt hiển thị danh mục dạng Lưới 3 cột mượt mà
    private void showCategoryFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_filter, null);
        dialog.setContentView(dialogView);

        RecyclerView rvPopup = dialogView.findViewById(R.id.rvCategoryPopup);
        TextView tvClear = dialogView.findViewById(R.id.tvClearFilter);

        rvPopup.setLayoutManager(new GridLayoutManager(this, 3));
        rvPopup.setAdapter(filterAdapter);

        // Sự kiện click chữ "Xóa lọc" bên trong Dialog
        tvClear.setOnClickListener(v -> {
            selectedCategoryIds.clear();
            selectedCategoryIds.add(0);
            setupFilterAdapter(); // Làm mới trạng thái Adapter về mặc định nút "Tất cả"
            rvPopup.setAdapter(filterAdapter);
            currentPage = 0;
            loadData();
            dialog.dismiss();
        });

        dialog.show();
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

    // Hàm lấy toàn bộ danh mục thể loại từ Server đổ vào bộ nhớ đệm
    private void loadCategoriesDataForFilter() {
        ApiClient.getApiService().getAllCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    masterCategoriesList.clear();

                    // Tạo ô chọn ảo "Tất cả" có ID bằng 0 đưa lên đầu lưới bộ lọc
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

    // ĐÃ SỬA: Hàm nạp dữ liệu truyện yêu thích truyền mảng danh sách ID đa chọn lên Server
    private void loadData() {
        if (currentUserId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem truyện yêu thích!", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        List<Integer> idsToSend = new ArrayList<>(selectedCategoryIds);
        // Nếu mảng đang chứa số 0 (Nút Tất cả) -> Tiến hành clear trống mảng để Server hiểu là không cần lọc cụ thể
        if (idsToSend.contains(0)) {
            idsToSend.clear();
        }

        // Gọi Endpoint API mới đã gá cổng bộ lọc đa chọn từ lượt trước
        ApiClient.getApiService().getFavoriteComicsFiltered(currentUserId, idsToSend).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allFavorites = response.body();

                    if (allFavorites.isEmpty()) {
                        showEmptyState();
                    } else {
                        // Tính toán chia trang dữ liệu (Client-side pagination mượt mà)
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
                tvPage.setBackgroundColor(Color.parseColor("#E91E63")); // Giữ màu hồng đặc trưng của trang yêu thích
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

    private void performLogout() {
        Toast.makeText(this, "Đang đăng xuất...", Toast.LENGTH_SHORT).show();
        SharedPrefsManager.logout(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showHeaderPopupMenu(View anchorView) {
        androidx.appcompat.widget.PopupMenu popupMenu =
                new androidx.appcompat.widget.PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_header_options, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (id == R.id.menu_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (id == R.id.menu_follow) {
                return true;
            } else if (id == R.id.menu_downloads) {
                Toast.makeText(this, "Truyện tải xuống", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(FollowActivity.this, ProfileActivity.class));
                return true;
            } else if (id == R.id.menu_logout) {
                performLogout();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
}