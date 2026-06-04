package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
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

    // Khai báo các View phân trang mới bổ sung
    private Button btnPrevPage, btnNextPage;
    private LinearLayout layoutPageNumbersContainer;

    private String currentKeyword = "";
    private String currentRoleFilter = "Tất cả";

    // Các biến trạng thái quản trị phân trang
    private int currentPage = 0;
    private int totalPages = 0;
    private final int pageSize = 10; // Cố định hiển thị 10 người dùng trên 1 trang

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

        // Ánh xạ View phân trang mới
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
            @Override public void onEdit(User user, int pos) { /* Sửa qua màn Detail trực tiếp */ }
            @Override public void onDelete(User user, int pos) { executeDeleteUser(user); }
        });
        rvUsers.setAdapter(adapter);

        // Sự kiện chuyển trang lùi về sau
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                loadUsersDataFromServer();
            }
        });

        // Sự kiện bấm chuyển tiếp sang trang mới
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
                currentPage = 0; // Trở về trang đầu khi gõ tìm kiếm mới
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

                            // Trích xuất mảng tổng số trang và trang hiện tại từ Map phản hồi
                            totalPages = ((Number) (result.get("totalPages") != null ? result.get("totalPages") : 0)).intValue();
                            currentPage = ((Number) (result.get("currentPage") != null ? result.get("currentPage") : 0)).intValue();

                            // Parse danh sách User an toàn qua Gson
                            Gson gson = new Gson();
                            String jsonUsers = gson.toJson(result.get("users"));
                            List<User> usersList = gson.fromJson(jsonUsers, new TypeToken<List<User>>(){}.getType());

                            adapter.setData(usersList);
                            renderPaginationUIControls(); // Vẽ loạt nút bấm số trang
                        }
                    }
                    @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                });
    }

    // ĐÃ THÊM: Vẽ bộ điều khiển số thứ tự trang động dạng < 1 2 3 ... n > kèm tự động vô hiệu hóa nút bấm kịch biên
    private void renderPaginationUIControls() {
        layoutPageNumbersContainer.removeAllViews();

        // Ép trạng thái vô hiệu hóa nút chuyển lùi < nếu ở trang đầu tiên
        btnPrevPage.setEnabled(currentPage > 0);
        btnPrevPage.setAlpha(currentPage > 0 ? 1.0f : 0.3f);

        // Ép trạng thái vô hiệu hóa nút chuyển tiếp > nếu ở trang cuối cùng
        btnNextPage.setEnabled(currentPage < totalPages - 1);
        btnNextPage.setAlpha(currentPage < totalPages - 1 ? 1.0f : 0.3f);

        if (totalPages <= 1) return; // Chỉ có 1 trang không cần hiện số

        for (int i = 0; i < totalPages; i++) {
            // Rút gọn bớt dấu ... nếu tổng số trang hiển thị quá lớn (Giữ chuẩn UX cấu trúc hình cây)
            if (totalPages > 5) {
                if (i > 0 && i < totalPages - 1 && Math.abs(i - currentPage) > 1) {
                    if (i == 1 || i == totalPages - 2) {
                        TextView tvEllipses = new TextView(this);
                        tvEllipses.setText("...");
                        tvEllipses.setPadding(16, 4, 16, 4);
                        layoutPageNumbersContainer.addView(tvEllipses);
                    }
                    continue;
                }
            }

            final int targetPageIndex = i;
            Button btnPageNumber = new Button(this);
            btnPageNumber.setText(String.valueOf(i + 1));
            btnPageNumber.setTextSize(12);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(90, 90);
            params.setMargins(6, 0, 6, 0);
            btnPageNumber.setLayoutParams(params);

            if (i == currentPage) {
                // Làm nổi bật màu đỏ cho trang quản trị hiện tại đang mở xem
                btnPageNumber.setBackgroundColor(Color.parseColor("#E74C3C"));
                btnPageNumber.setTextColor(Color.WHITE);
            } else {
                btnPageNumber.setBackgroundColor(Color.TRANSPARENT);
                btnPageNumber.setTextColor(Color.BLACK);
                btnPageNumber.setOnClickListener(v -> {
                    currentPage = targetPageIndex;
                    loadUsersDataFromServer(); // Nhảy sang số trang đích chọn trực tiếp
                });
            }
            layoutPageNumbersContainer.addView(btnPageNumber);
        }
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