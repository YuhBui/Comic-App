package com.yuhbui.comicapp.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.User;
import java.util.Arrays;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserDetailActivity extends AppCompatActivity {

    private int userId;
    private ImageView imgAvatar;
    private TextView tvName, tvRoleBadge, tvId, tvEmail, tvStatus;
    private Button btnBan, btnEdit, btnDelete;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_detail);

        userId = getIntent().getIntExtra("USER_ID", -1);

        // Khởi tạo thanh Header Admin
        View layoutHeader = findViewById(R.id.layoutHeaderUserDetail);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        headerLogo.setText("HỒ SƠ THÀNH VIÊN");
        headerLogo.setTextColor(Color.parseColor("#E74C3C"));
        headerMenu.setOnClickListener(v -> finish());

        // Ánh xạ View
        imgAvatar = findViewById(R.id.imgAdminUserDetailAvatar);
        tvName = findViewById(R.id.tvAdminUserDetailName);
        tvRoleBadge = findViewById(R.id.tvAdminUserDetailRoleBadge);
        tvId = findViewById(R.id.tvAdminUserDetailId);
        tvEmail = findViewById(R.id.tvAdminUserDetailEmail);
        tvStatus = findViewById(R.id.tvAdminUserDetailStatus);

        btnBan = findViewById(R.id.btnAdminUserDetailBan);
        btnEdit = findViewById(R.id.btnAdminUserDetailEdit);
        btnDelete = findViewById(R.id.btnAdminUserDetailDelete);

        // Đăng ký sự kiện nút bấm chức năng quản trị
        btnBan.setOnClickListener(v -> executeToggleBan());
        btnEdit.setOnClickListener(v -> showEditUserFormDialog());
        btnDelete.setOnClickListener(v -> executeDeleteUser());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfileData();
    }

    private void loadUserProfileData() {
        ApiClient.getApiService().getUserProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();

                    tvName.setText(currentUser.getDisplayName());
                    tvId.setText("🆔 Mã ID tài khoản: " + currentUser.getUserId());
                    tvEmail.setText("✉️ Địa chỉ Email: " + currentUser.getEmail());
                    tvRoleBadge.setText("VAI TRÒ: " + currentUser.getRole().toUpperCase());
                    tvStatus.setText("🚦 Trạng thái: " + currentUser.getStatus());

                    // Đổi màu text trạng thái và nhãn nút Ban động
                    if ("Banned".equalsIgnoreCase(currentUser.getStatus())) {
                        tvStatus.setTextColor(Color.RED);
                        btnBan.setText("✅ UNBAN");
                        btnBan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                    } else {
                        tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                        btnBan.setText("🚷 BAN TRONG");
                        btnBan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E91E63")));
                    }

                    if (!AdminUserDetailActivity.this.isDestroyed()) {
                        Glide.with(AdminUserDetailActivity.this)
                                .load(currentUser.getAvatarUrl())
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .circleCrop()
                                .into(imgAvatar);
                    }
                }
            }

            @Override public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    private void executeToggleBan() {
        ApiClient.getApiService().adminToggleBanUser(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminUserDetailActivity.this, "Đã cập nhật trạng thái hoạt động tài khoản!", Toast.LENGTH_SHORT).show();
                    loadUserProfileData(); // Tải lại thông tin mới tinh từ máy chủ
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void showEditUserFormDialog() {
        if (currentUser == null) return;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText edtName = new EditText(this);
        edtName.setHint("Tên hiển thị");
        edtName.setText(currentUser.getDisplayName());
        layout.addView(edtName);

        final EditText edtEmail = new EditText(this);
        edtEmail.setHint("Email");
        edtEmail.setText(currentUser.getEmail());
        layout.addView(edtEmail);

        final EditText edtPass = new EditText(this);
        edtPass.setHint("Mật khẩu mới (Để trống nếu giữ nguyên)");
        edtPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(edtPass);

        final Spinner spinnerType = new Spinner(this);
        spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Arrays.asList("User", "Admin")));
        spinnerType.setSelection("Admin".equalsIgnoreCase(currentUser.getRole()) ? 1 : 0);
        layout.addView(spinnerType);

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa tài khoản")
                .setView(layout)
                .setPositiveButton("CẬP NHẬT", (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    String email = edtEmail.getText().toString().trim();
                    String pass = edtPass.getText().toString().trim();
                    String role = spinnerType.getSelectedItem().toString();

                    if (name.isEmpty() || email.isEmpty()) return;

                    User u = new User();
                    u.setDisplayName(name);
                    u.setEmail(email);
                    u.setRole(role);
                    if (!pass.isEmpty()) u.setPassword(pass);

                    ApiClient.getApiService().adminUpdateUser(userId, u).enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminUserDetailActivity.this, "Đã lưu thay đổi!", Toast.LENGTH_SHORT).show();
                                loadUserProfileData();
                            } else {
                                Toast.makeText(AdminUserDetailActivity.this, "Cập nhật thất bại từ server!", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onFailure(Call<User> call, Throwable t) {}
                    });
                })
                .setNegativeButton("HỦY BỎ", null).show();
    }

    private void executeDeleteUser() {
        if (currentUser == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Cảnh báo xóa vĩnh viễn")
                .setMessage("Bạn có chắc chắn muốn xóa tài khoản '" + currentUser.getDisplayName() + "' không? Thao tác này không thể thu hồi!")
                .setPositiveButton("XÓA NGAY", (dialog, which) -> {
                    ApiClient.getApiService().adminDeleteUser(userId).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminUserDetailActivity.this, "Đã xóa người dùng thành công!", Toast.LENGTH_SHORT).show();
                                finish(); // Đóng Activity quay lại màn hình danh sách chính
                            }
                        }
                        @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                    });
                })
                .setNegativeButton("HỦY", null).show();
    }
}