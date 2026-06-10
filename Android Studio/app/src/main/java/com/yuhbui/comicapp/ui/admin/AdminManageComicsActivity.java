package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;       // THÊM: Quản lý nút Back
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;              // THÊM: Hỗ trợ đóng mở Drawer
import androidx.drawerlayout.widget.DrawerLayout;    // THÊM: Thành phần DrawerLayout root
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Category;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.data.model.Notification;
import com.yuhbui.comicapp.ui.adapters.AdminComicAdapter;
import com.yuhbui.comicapp.ui.adapters.AdminNotificationAdapter;
import com.yuhbui.comicapp.ui.adapters.CategoryFilterAdapter;
import com.yuhbui.comicapp.utils.HeaderUtils;          // THÊM: Lớp tiện ích Header dùng chung
import com.yuhbui.comicapp.utils.MenuUtils;            // THÊM: Lớp tiện ích Menu dùng chung

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminManageComicsActivity extends AppCompatActivity implements AdminComicAdapter.OnComicActionListener {

    private DrawerLayout drawerLayout; // THÊM: Khai báo DrawerLayout toàn cục

    private RecyclerView rvAdminManageComics;
    private AdminComicAdapter adapter;
    private FloatingActionButton fabAddComic;

    private ImageView imgComicFilter;
    private CategoryFilterAdapter filterAdapter;
    private List<Category> masterCategoriesList = new ArrayList<>();

    private EditText edtComicSearch;
    private Button btnPrevPage, btnNextPage;
    private LinearLayout layoutPageNumbersContainer;

    private String currentKeyword = "";
    private int currentPage = 0;
    private int totalPages = 0;
    private final int pageSize = 10;

    private List<Integer> selectedCategoryIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_comics);

        // Ánh xạ thành phần DrawerLayout root mới bổ sung từ XML
        drawerLayout = findViewById(R.id.drawerLayout);

        // 1. Khởi tạo Header đặc thù dành riêng cho Admin
        View layoutHeader = findViewById(R.id.layoutHeaderManageComics); //
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu); //
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo); //

        // Khởi tạo các tính năng cơ bản của Header dùng chung
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);

        // THAY ĐỔI: Kích hoạt Menu trượt Admin thay vì đóng trang (finish) như cũ
        MenuUtils.setupAdminSideMenu(this, drawerLayout, headerMenu);

        // YÊU CẦU: Khóa ẩn triệt để hai nút Tìm kiếm và Thông báo trên thanh Header Admin
        if (layoutHeader.findViewById(R.id.headerSearch) != null) {
            layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        }
        if (layoutHeader.findViewById(R.id.headerNotification) != null) {
            layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        }

        headerLogo.setText("COMIC APP");
        headerLogo.setTextColor(Color.parseColor("#E74C3C"));
        headerLogo.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // THÊM: Lắng nghe sự kiện nút Back cứng - Ưu tiên đóng Menu trượt nếu nó đang mở
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
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

        // 2. Ánh xạ các thành phần điều khiển
        edtComicSearch = findViewById(R.id.edtComicSearch); //
        imgComicFilter = findViewById(R.id.imgAdminComicFilter); //
        btnPrevPage = findViewById(R.id.btnComicPrevPage); //
        btnNextPage = findViewById(R.id.btnComicNextPage); //
        layoutPageNumbersContainer = findViewById(R.id.layoutComicPageNumbersContainer); //

        // 3. Cài đặt RecyclerView danh sách truyện hiển thị
        rvAdminManageComics = findViewById(R.id.rvAdminManageComics); //
        rvAdminManageComics.setLayoutManager(new LinearLayoutManager(this)); //
        adapter = new AdminComicAdapter(this); //
        rvAdminManageComics.setAdapter(adapter); //

        // 4. Khởi tạo trạng thái ban đầu của bộ lọc tái sử dụng
        selectedCategoryIds.add(0); //
        setupFilterAdapter(); //

        // 5. Bắt sự kiện bấm vào nút Biểu tượng Bộ lọc -> Mở hộp thoại BottomSheet
        imgComicFilter.setOnClickListener(v -> showCategoryFilterDialog()); //

        // 6. Sự kiện click nút Thêm truyện nổi
        fabAddComic = findViewById(R.id.fabAddComic); //
        fabAddComic.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminEditComicActivity.class);
            startActivity(intent);
        }); //

        setupFilterAndPaginationListeners(); //
        loadCategoriesDataForFilterList(); //
    }

    // Làm tươi Avatar trên Header mỗi lần quay lại trang quản lý truyện này
    @Override
    protected void onResume() {
        super.onResume();
        loadComicsDataFromServer(); //
        View layoutHeader = findViewById(R.id.layoutHeaderManageComics);
        if (layoutHeader != null && layoutHeader.findViewById(R.id.headerAvatar) != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
        }
    }

    private void setupFilterAdapter() {
        filterAdapter = new CategoryFilterAdapter(new CategoryFilterAdapter.OnCatClickListener() {
            @Override
            public void onCatClick(List<Integer> selectedIds) {
                selectedCategoryIds = selectedIds;
                currentPage = 0;
                loadComicsDataFromServer();
            }
        });
        if (!masterCategoriesList.isEmpty()) {
            filterAdapter.setCategories(masterCategoriesList);
        }
    } //

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
            loadComicsDataFromServer();
            dialog.dismiss();
        });

        dialog.show();
    } //

    private void setupFilterAndPaginationListeners() {
        edtComicSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentKeyword = s.toString().trim();
                currentPage = 0;
                loadComicsDataFromServer();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnPrevPage.setOnClickListener(v -> { if (currentPage > 0) { currentPage--; loadComicsDataFromServer(); } });
        btnNextPage.setOnClickListener(v -> { if (currentPage < totalPages - 1) { currentPage++; loadComicsDataFromServer(); } });
    } //

    private void loadCategoriesDataForFilterList() {
        ApiClient.getApiService().getAllCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Category> serverCategories = response.body();

                    masterCategoriesList.clear();
                    Category allCat = new Category();
                    allCat.setCategoryId(0);
                    allCat.setName("Tất cả");

                    masterCategoriesList.add(allCat);
                    masterCategoriesList.addAll(serverCategories);

                    filterAdapter.setCategories(masterCategoriesList);
                }
            }
            @Override public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(AdminManageComicsActivity.this, "Không thể tải bộ lọc thể loại!", Toast.LENGTH_SHORT).show();
            }
        });
    } //

    private void loadComicsDataFromServer() {
        List<Integer> idsToSend = new ArrayList<>(selectedCategoryIds);
        if (idsToSend.contains(0)) {
            idsToSend.clear();
        }

        ApiClient.getApiService().adminGetComicsPaged(currentKeyword, idsToSend, currentPage, pageSize)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, Object> result = response.body();

                            totalPages = ((Number) (result.get("totalPages") != null ? result.get("totalPages") : 0)).intValue();
                            currentPage = ((Number) (result.get("currentPage") != null ? result.get("currentPage") : 0)).intValue();

                            Gson gson = new Gson();
                            String jsonComics = gson.toJson(result.get("comics"));
                            List<Comic> comicsList = gson.fromJson(jsonComics, new TypeToken<List<Comic>>(){}.getType());

                            adapter.setData(comicsList);
                            renderPaginationUIControls();
                        }
                    }
                    @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        Toast.makeText(AdminManageComicsActivity.this, "Lỗi tải danh sách truyện!", Toast.LENGTH_SHORT).show();
                    }
                });
    } //

    private void renderPaginationUIControls() {
        layoutPageNumbersContainer.removeAllViews();
        btnPrevPage.setEnabled(currentPage > 0);
        btnPrevPage.setAlpha(currentPage > 0 ? 1.0f : 0.3f);
        btnNextPage.setEnabled(currentPage < totalPages - 1);
        btnNextPage.setAlpha(currentPage < totalPages - 1 ? 1.0f : 0.3f);

        if (totalPages <= 0) return;

        int maxVisible = 5;
        int startPage = Math.max(0, currentPage - maxVisible / 2);
        int endPage = Math.min(totalPages - 1, startPage + maxVisible - 1);

        if (endPage - startPage < maxVisible - 1) {
            startPage = Math.max(0, endPage - maxVisible + 1);
        }

        int btnSize = dpToPx(34);
        int btnMargin = dpToPx(3);

        for (int i = startPage; i <= endPage; i++) {
            final int targetPageIndex = i;
            TextView tvPage = new TextView(this);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(btnSize, btnSize);
            params.setMargins(btnMargin, 0, btnMargin, 0);
            tvPage.setLayoutParams(params);

            tvPage.setText(String.valueOf(i + 1));
            tvPage.setGravity(android.view.Gravity.CENTER);
            tvPage.setTextSize(13);
            tvPage.setTypeface(null, android.graphics.Typeface.BOLD);
            tvPage.setBackgroundResource(R.drawable.bg_page_btn);

            if (i == currentPage) {
                tvPage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E74C3C")));
                tvPage.setTextColor(Color.WHITE);
            } else {
                tvPage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
                tvPage.setTextColor(Color.parseColor("#333333"));
                tvPage.setOnClickListener(v -> {
                    currentPage = targetPageIndex;
                    loadComicsDataFromServer();
                });
            }
            layoutPageNumbersContainer.addView(tvPage);
        }
    } //

    private void openNotificationManager() {
        AdminNotificationAdapter adminAdapter = new AdminNotificationAdapter();

        ApiClient.getApiService().getAllNotificationsForAdmin().enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adminAdapter.setData(response.body(), new AdminNotificationAdapter.OnAdminNotifActionListener() {

                        @Override
                        public void onEdit(Notification notification) {
                            View dialogView = LayoutInflater.from(AdminManageComicsActivity.this).inflate(R.layout.dialog_report, null);
                            EditText edtNewContent = dialogView.findViewById(R.id.edtReportReason);
                            edtNewContent.setHint("Nhập nội dung chỉnh sửa...");
                            edtNewContent.setText(notification.getMessage());

                            new AlertDialog.Builder(AdminManageComicsActivity.this)
                                    .setTitle("Sửa nội dung thông báo")
                                    .setView(dialogView)
                                    .setPositiveButton("Cập nhật", (dialog, which) -> {
                                        String msg = edtNewContent.getText().toString().trim();
                                        if(!msg.isEmpty()) {
                                            notification.setMessage(msg);
                                            ApiClient.getApiService().adminUpdateNotification(notification.getNotificationId(), notification)
                                                    .enqueue(new Callback<Void>() {
                                                        @Override
                                                        public void onResponse(Call<Void> call, Response<Void> response) {
                                                            Toast.makeText(AdminManageComicsActivity.this, "Đã sửa thành công!", Toast.LENGTH_SHORT).show();
                                                        }
                                                        @Override public void onFailure(Call<Void> call, Throwable t) {}
                                                    });
                                        }
                                    })
                                    .setNegativeButton("Hủy", null)
                                    .show();
                        }

                        @Override
                        public void onDelete(Notification notification, int position) {
                            new AlertDialog.Builder(AdminManageComicsActivity.this)
                                    .setTitle("Xác nhận xóa")
                                    .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn thông báo này không?")
                                    .setPositiveButton("Xóa", (dialog, which) -> {
                                        ApiClient.getApiService().adminDeleteNotification(notification.getNotificationId())
                                                .enqueue(new Callback<Void>() {
                                                    @Override
                                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                                        Toast.makeText(AdminManageComicsActivity.this, "Đã xóa thành công!", Toast.LENGTH_SHORT).show();
                                                        openNotificationManager();
                                                    }
                                                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                                                });
                                    })
                                    .setNegativeButton("Hủy", null)
                                    .show();
                        }
                    });
                }
            }
            @Override public void onFailure(Call<List<Notification>> call, Throwable t) {}
        });
    } //

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    } //

    @Override public void onEdit(Comic comic) {}
    @Override public void onDelete(Comic comic, int position) {}
}