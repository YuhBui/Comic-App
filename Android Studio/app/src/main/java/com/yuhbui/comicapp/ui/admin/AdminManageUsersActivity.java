package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;       // THÊM: Quản lý nút Back hệ thống
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;              // THÊM: Thư viện điều hướng DrawerLayout trượt trái
import androidx.drawerlayout.widget.DrawerLayout;    // THÊM: Thành phần DrawerLayout root
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.User;
import com.yuhbui.comicapp.ui.adapters.AdminUserAdapter;
import com.yuhbui.comicapp.utils.HeaderUtils;          // THÊM: Khởi tạo tiện ích Header
import com.yuhbui.comicapp.utils.MenuUtils;            // THÊM: Gọi Menu trượt Admin dùng chung
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminManageUsersActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout; // THÊM: Khai báo thành phần DrawerLayout quản lý Menu trượt

    private EditText edtSearch;
    private Spinner spinnerRole;
    private View btnAdd;
    private RecyclerView rvUsers;
    private AdminUserAdapter adapter;

    // Các thành phần xử lý thanh điều hướng phân trang
    private Button btnPrevPage, btnNextPage;
    private LinearLayout layoutPageNumbersContainer;

    private String currentKeyword = "";
    private String currentRoleFilter = "Tất cả";

    private int currentPage = 0;
    private int totalPages = 0;
    private final int pageSize = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_users);

        // Ánh xạ DrawerLayout mới
        drawerLayout = findViewById(R.id.drawerLayout);

        // 1. SỬA ĐỔI: Khởi tạo Header và đăng ký Menu trượt Admin dùng chung
        View layoutHeader = findViewById(R.id.layoutHeaderManageUsers);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);

        // Khởi tạo các tiện ích lõi dùng chung
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);
        MenuUtils.setupAdminSideMenu(this, drawerLayout, headerMenu);

        // YÊU CẦU: Khóa ẩn triệt để hai nút Tìm kiếm và Thông báo đối với không gian Admin
        if (layoutHeader.findViewById(R.id.headerSearch) != null) {
            layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        }
        if (layoutHeader.findViewById(R.id.headerNotification) != null) {
            layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        }

        headerLogo.setText(android.text.Html.fromHtml("<font color='#D97707'>h</font><font color='#FFFFFF'>ay</font><font color='#D97707'>c</font><font color='#FFFFFF'>omic</font>", android.text.Html.FROM_HTML_MODE_COMPACT));
        headerLogo.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 2. THÊM: Quản lý nút Quay lại (Back cứng) - Ưu tiên đóng Menu trượt nếu đang mở
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

        // --- Giữ nguyên toàn bộ logic thiết lập danh sách và phân trang cũ bên dưới ---
        edtSearch = findViewById(R.id.edtUserSearch);
        spinnerRole = findViewById(R.id.spinnerRoleFilter);
        btnAdd = findViewById(R.id.btnAdminAddUser);
        rvUsers = findViewById(R.id.rvAdminManageUsers);

        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        layoutPageNumbersContainer = findViewById(R.id.layoutPageNumbersContainer);

        List<String> roles = Arrays.asList("Tất cả", "User", "Admin");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(spinnerAdapter);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUserAdapter(new AdminUserAdapter.OnUserAdminActionListener() {
            @Override
            public void onItemClick(User user) {
                Intent intent = new Intent(AdminManageUsersActivity.this, AdminUserDetailActivity.class);
                intent.putExtra("USER_ID", user.getUserId());
                startActivity(intent);
            }
            @Override public void onToggleBan(User user, int pos) { executeToggleBan(user.getUserId()); }
            @Override public void onEdit(User user, int pos) { }
            @Override public void onDelete(User user, int pos) { executeDeleteUser(user); }
        });
        rvUsers.setAdapter(adapter);

        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                loadUsersDataFromServer();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                loadUsersDataFromServer();
            }
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentKeyword = s.toString().trim();
                currentPage = 0;
                loadUsersDataFromServer();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentRoleFilter = roles.get(position);
                currentPage = 0;
                loadUsersDataFromServer();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAdd.setOnClickListener(v -> {
            startActivity(new Intent(AdminManageUsersActivity.this, AdminAddUserActivity.class));
        });
    }

    // Cập nhật làm tươi danh sách tài khoản mỗi khi Admin quay lại từ trang chi tiết
    @Override
    protected void onResume() {
        super.onResume();
        loadUsersDataFromServer();
        // Làm mới ảnh đại diện Admin trên Header (nếu có view)
        if (findViewById(R.id.layoutHeaderManageUsers) != null && findViewById(R.id.layoutHeaderManageUsers).findViewById(R.id.headerAvatar) != null) {
            HeaderUtils.loadHeaderAvatar(this, findViewById(R.id.layoutHeaderManageUsers).findViewById(R.id.headerAvatar));
        }
    }

    private void loadUsersDataFromServer() {
        ApiClient.getApiService().adminGetUsers(currentKeyword, currentRoleFilter, currentPage, pageSize)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, Object> result = response.body();

                            totalPages = ((Number) (result.get("totalPages") != null ? result.get("totalPages") : 0)).intValue();
                            currentPage = ((Number) (result.get("currentPage") != null ? result.get("currentPage") : 0)).intValue();

                            Gson gson = new Gson();
                            String jsonUsers = gson.toJson(result.get("users"));
                            List<User> usersList = gson.fromJson(jsonUsers, new TypeToken<List<User>>(){}.getType());

                            adapter.setData(usersList);
                            renderPaginationUIControls();
                        }
                    }
                    @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                });
    }

    private void renderPaginationUIControls() {
        layoutPageNumbersContainer.removeAllViews();

        btnPrevPage.setEnabled(currentPage > 0);
        btnNextPage.setEnabled(currentPage < totalPages - 1);
        btnPrevPage.setBackgroundResource(R.drawable.bg_nav_btn);
        btnPrevPage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E1E1E")));
        btnPrevPage.setTextColor(Color.parseColor(currentPage > 0 ? "#DBC2B0" : "#555555"));
        btnNextPage.setBackgroundResource(R.drawable.bg_nav_btn);
        btnNextPage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E1E1E")));
        btnNextPage.setTextColor(Color.parseColor(currentPage < totalPages - 1 ? "#DBC2B0" : "#555555"));

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
                tvPage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFB77D")));
                tvPage.setTextColor(Color.parseColor("#4D2600"));
            } else {
                tvPage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E1E1E")));
                tvPage.setTextColor(Color.parseColor("#DBC2B0"));
                tvPage.setOnClickListener(v -> {
                    currentPage = targetPageIndex;
                    loadUsersDataFromServer();
                });
            }
            layoutPageNumbersContainer.addView(tvPage);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void executeToggleBan(int userId) {
        ApiClient.getApiService().adminToggleBanUser(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) loadUsersDataFromServer();
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void executeDeleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa vĩnh viễn")
                .setMessage("Xóa tài khoản '" + user.getDisplayName() + "'?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    ApiClient.getApiService().adminDeleteUser(user.getUserId()).enqueue(new Callback<Map<String, Object>>() {
                        @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) loadUsersDataFromServer();
                        }
                        @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Hủy", null).show();
    }
}