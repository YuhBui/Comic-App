package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.User;
import com.yuhbui.comicapp.ui.adapters.AdminUserAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminManageUsersActivity extends AppCompatActivity {

    private EditText edtSearch;
    private Spinner spinnerRole;
    private Button btnAdd;
    private RecyclerView rvUsers;
    private AdminUserAdapter adapter;

    // Các thành phần xử lý thanh điều hướng phân trang
    private Button btnPrevPage, btnNextPage;
    private LinearLayout layoutPageNumbersContainer;

    private String currentKeyword = "";
    private String currentRoleFilter = "Tất cả";

    private int currentPage = 0;
    private int totalPages = 0;
    private final int pageSize = 10; // Đọc cấu hình phân trang 10 phần tử/trang

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_users);

        View layoutHeader = findViewById(R.id.layoutHeaderManageUsers);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        headerLogo.setText("QUẢN LÝ THÀNH VIÊN");
        headerLogo.setTextColor(Color.parseColor("#E74C3C"));
        headerMenu.setOnClickListener(v -> finish());

        edtSearch = findViewById(R.id.edtUserSearch);
        spinnerRole = findViewById(R.id.spinnerRoleFilter);
        btnAdd = findViewById(R.id.btnAdminAddUser);
        rvUsers = findViewById(R.id.rvAdminManageUsers);

        // Ánh xạ thành phần giao diện điều phối trang
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

        // Sự kiện click nút mũi tên Trái
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                loadUsersDataFromServer();
            }
        });

        // Sự kiện click nút mũi tên Phải
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

    @Override
    protected void onResume() {
        super.onResume();
        loadUsersDataFromServer();
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
                            renderPaginationUIControls(); // Gọi hàm vẽ lại thanh điều hướng
                        }
                    }
                    @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                });
    }

    // ĐÃ SỬA: Thay đổi cấu trúc hiển thị số trang & Điều khiển nút < > luôn hiện, khóa bấm nếu kịch trang
    private void renderPaginationUIControls() {
        layoutPageNumbersContainer.removeAllViews();

        // 1. XỬ LÝ NÚT < >: Luôn luôn hiện diện, nếu kịch biên thì disable + đặt độ mờ nhạt (Alpha = 0.3f)
        btnPrevPage.setEnabled(currentPage > 0);
        btnPrevPage.setAlpha(currentPage > 0 ? 1.0f : 0.3f);

        btnNextPage.setEnabled(currentPage < totalPages - 1);
        btnNextPage.setAlpha(currentPage < totalPages - 1 ? 1.0f : 0.3f);

        if (totalPages <= 0) return;

        // 2. Thuật toán cửa sổ trượt (Sliding Window) vẽ tối đa 5 ô số trang liền kề giống hệt phía màn hình User
        int maxVisible = 5;
        int startPage = Math.max(0, currentPage - maxVisible / 2);
        int endPage = Math.min(totalPages - 1, startPage + maxVisible - 1);

        if (endPage - startPage < maxVisible - 1) {
            startPage = Math.max(0, endPage - maxVisible + 1);
        }

        int btnSize = dpToPx(34); // Quy đổi kích thước chuẩn 34dp giống màn hình History của User
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

            // Gán background bo góc tròn mềm mại từ resource hệ thống có sẵn của bạn
            tvPage.setBackgroundResource(R.drawable.bg_page_btn);

            if (i == currentPage) {
                // Trang hiện tại đang xem: Nền đỏ thương hiệu Admin, chữ trắng
                tvPage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E74C3C")));
                tvPage.setTextColor(Color.WHITE);
            } else {
                // Các trang thông thường khác: Nền xám nhạt dịu mắt, chữ xám đen
                tvPage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
                tvPage.setTextColor(Color.parseColor("#333333"));
                tvPage.setOnClickListener(v -> {
                    currentPage = targetPageIndex;
                    loadUsersDataFromServer();
                });
            }
            layoutPageNumbersContainer.addView(tvPage);
        }
    }

    // ĐÃ THÊM: Hàm quy đổi dp sang Pixel động phục vụ căn chỉnh kích cỡ nút số trang
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