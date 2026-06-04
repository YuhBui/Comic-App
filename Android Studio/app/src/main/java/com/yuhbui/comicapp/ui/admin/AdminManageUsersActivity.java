package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.User;
import com.yuhbui.comicapp.ui.adapters.AdminUserAdapter;
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

    private String currentKeyword = "";
    private String currentRoleFilter = "Tất cả";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_users);

        // Khởi tạo thanh Header Admin chung
        View layoutHeader = findViewById(R.id.layoutHeaderManageUsers);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        headerLogo.setText("QUẢN LÝ THÀNH VIÊN");
        headerLogo.setTextColor(Color.parseColor("#E74C3C"));
        headerMenu.setOnClickListener(v -> finish());

        // Ánh xạ các View điều khiển
        edtSearch = findViewById(R.id.edtUserSearch);
        spinnerRole = findViewById(R.id.spinnerRoleFilter);
        btnAdd = findViewById(R.id.btnAdminAddUser);
        rvUsers = findViewById(R.id.rvAdminManageUsers);

        // Cài đặt danh mục cho bộ lọc Spinner vai trò
        List<String> roles = Arrays.asList("Tất cả", "User", "Admin");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(spinnerAdapter);

        // Thiết lập RecyclerView danh sách
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUserAdapter(new AdminUserAdapter.OnUserAdminActionListener() {
            @Override
            public void onItemClick(User user) {
                // ĐÃ SỬA: Bấm vào người dùng sẽ chuyển thẳng sang Activity Chi tiết riêng biệt mới tinh thay vì hiện Dialog
                Intent intent = new Intent(AdminManageUsersActivity.this, AdminUserDetailActivity.class);
                intent.putExtra("USER_ID", user.getUserId());
                startActivity(intent);
            }

            @Override public void onToggleBan(User user, int pos) { executeToggleBan(user.getUserId()); }

            @Override public void onEdit(User user, int pos) { showUserFormDialog(user); }

            @Override public void onDelete(User user, int pos) { executeDeleteUser(user); }
        });
        rvUsers.setAdapter(adapter);

        // Lắng nghe sự kiện gõ ô tìm kiếm văn bản liên tục
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentKeyword = s.toString().trim();
                loadUsersDataFromServer();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Lắng nghe sự kiện chọn thay đổi mục Spinner lọc vai trò
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentRoleFilter = roles.get(position);
                loadUsersDataFromServer();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAdd.setOnClickListener(v -> showUserFormDialog(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsersDataFromServer();
    }

    private void loadUsersDataFromServer() {
        ApiClient.getApiService().adminGetUsers(currentKeyword, currentRoleFilter).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    // ĐA CHỨC NĂNG DIALOG FORM: Tích hợp Thêm mới hoặc Cập nhật thông tin nhanh gọn dạng Input popup
    private void showUserFormDialog(@Nullable User existingUser) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        final EditText edtName = new EditText(this);
        edtName.setHint("Tên hiển thị người dùng");
        layout.addView(edtName);

        final EditText edtEmail = new EditText(this);
        edtEmail.setHint("Địa chỉ Email");
        layout.addView(edtEmail);

        final EditText edtPass = new EditText(this);
        edtPass.setHint("Mật khẩu (Để trống nếu không đổi)");
        edtPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(edtPass);

        final Spinner spinnerType = new Spinner(this);
        spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Arrays.asList("User", "Admin")));
        layout.addView(spinnerType);

        boolean isEdit = existingUser != null;
        if (isEdit) {
            edtName.setText(existingUser.getDisplayName());
            edtEmail.setText(existingUser.getEmail());
            spinnerType.setSelection("Admin".equalsIgnoreCase(existingUser.getRole()) ? 1 : 0);
        }

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Chỉnh sửa người dùng" : "Thêm thành viên hệ thống")
                .setView(layout)
                .setPositiveButton("LƯU", (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    String email = edtEmail.getText().toString().trim();
                    String pass = edtPass.getText().toString().trim();
                    String role = spinnerType.getSelectedItem().toString();

                    if (name.isEmpty() || email.isEmpty()) {
                        Toast.makeText(AdminManageUsersActivity.this, "Tên và Email không được để trống!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    User u = new User();
                    u.setDisplayName(name);
                    u.setEmail(email);
                    u.setRole(role);
                    if (!pass.isEmpty()) u.setPassword(pass);

                    if (!isEdit) {
                        ApiClient.getApiService().adminCreateUser(u).enqueue(new Callback<User>() {
                            @Override public void onResponse(Call<User> call, Response<User> response) {
                                if (response.isSuccessful()) loadUsersDataFromServer();
                                else Toast.makeText(AdminManageUsersActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onFailure(Call<User> call, Throwable t) {}
                        });
                    } else {
                        ApiClient.getApiService().adminUpdateUser(existingUser.getUserId(), u).enqueue(new Callback<User>() {
                            @Override public void onResponse(Call<User> call, Response<User> response) {
                                if (response.isSuccessful()) loadUsersDataFromServer();
                            }
                            @Override public void onFailure(Call<User> call, Throwable t) {}
                        });
                    }
                })
                .setNegativeButton("HỦY", null).show();
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
                .setMessage("Bạn có chắc chắn muốn xóa tài khoản '" + user.getDisplayName() + "' khỏi hệ thống? Thao tác này sẽ dọn dẹp sạch toàn bộ lịch sử và bình luận liên quan.")
                .setPositiveButton("Đồng ý xóa", (dialog, which) -> {
                    ApiClient.getApiService().adminDeleteUser(user.getUserId()).enqueue(new Callback<Map<String, Object>>() {
                        @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminManageUsersActivity.this, "Đã dọn dẹp sạch dữ liệu và xóa thành công!", Toast.LENGTH_SHORT).show();
                                loadUsersDataFromServer();
                            }
                        }
                        @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Hủy", null).show();
    }
}