package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.User;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserDetailActivity extends AppCompatActivity {

    private int userId;
    private ImageView imgAvatar;
    private Button btnChooseAvatar, btnSave, btnBan, btnDelete;
    private EditText edtName, edtEmail, edtPassword, edtConfirmPassword;
    private Spinner spinnerRole;
    private TextView tvCreatedAt;

    private User currentUser;
    private Uri localImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_detail);

        userId = getIntent().getIntExtra("USER_ID", -1);

        View layoutHeader = findViewById(R.id.layoutHeaderUserDetail);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        headerLogo.setText("HỒ SƠ THÀNH VIÊN");
        headerLogo.setTextColor(Color.parseColor("#E74C3C"));
        headerMenu.setOnClickListener(v -> finish());

        // Ánh xạ các thành phần nhập liệu sửa trực tiếp
        imgAvatar = findViewById(R.id.imgAdminUserDetailAvatar);
        btnChooseAvatar = findViewById(findViewById(R.id.btnChooseAvatarDetail) != null ? R.id.btnChooseAvatarDetail : R.id.imgAdminUserDetailAvatar);
        edtName = findViewById(R.id.edtDetailName);
        edtEmail = findViewById(R.id.edtDetailEmail);
        edtPassword = findViewById(R.id.edtDetailPassword);
        edtConfirmPassword = findViewById(R.id.edtDetailConfirmPassword);
        spinnerRole = findViewById(R.id.spinnerDetailRole);
        tvCreatedAt = findViewById(R.id.tvDetailCreatedAt);

        btnSave = findViewById(R.id.btnAdminUserDetailSave);
        btnBan = findViewById(R.id.btnAdminUserDetailBan);
        btnDelete = findViewById(R.id.btnAdminUserDetailDelete);

        spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Arrays.asList("User", "Admin")));

        // SỬA ĐỔI: Bấm vào Avatar chỉ kích hoạt xem ảnh phóng to lớn
        imgAvatar.setOnClickListener(v -> {
            if (currentUser != null && currentUser.getAvatarUrl() != null) {
                showFullAvatarDialog(currentUser.getAvatarUrl());
            }
        });

        // SỬA ĐỔI: Bấm vào nút bên dưới để chọn ảnh mới từ Gallery máy
        btnChooseAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 502);
        });

        // Sự kiện bấm nút Lưu thông tin trực tiếp
        btnSave.setOnClickListener(v -> executeSaveChangesForm());
        btnBan.setOnClickListener(v -> executeToggleBan());
        btnDelete.setOnClickListener(v -> executeDeleteUser());

        loadUserProfileData();
    }

    private void loadUserProfileData() {
        ApiClient.getApiService().getUserProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();

                    edtName.setText(currentUser.getDisplayName());
                    edtEmail.setText(currentUser.getEmail());
                    spinnerRole.setSelection("Admin".equalsIgnoreCase(currentUser.getRole()) ? 1 : 0);

                    // Hiển thị ngày tạo tài khoản lên UI
                    tvCreatedAt.setText("📅 Ngày tạo tài khoản: " + (currentUser.getCreatedAt() != null ? formatToDateOnly(currentUser.getCreatedAt()) : "Chưa cập nhật"));

                    if ("Banned".equalsIgnoreCase(currentUser.getStatus())) {
                        btnBan.setText("✅ UNBAN");
                        btnBan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                    } else {
                        btnBan.setText("🚷 BAN");
                        btnBan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E91E63")));
                    }

                    if (localImageUri == null && !AdminUserDetailActivity.this.isDestroyed()) {
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

    private String formatToDateOnly(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.trim().isEmpty()) {
            return "Chưa cập nhật";
        }
        try {
            // Tách lấy cụm ngày yyyy-MM-dd trước khoảng trắng hoặc chữ T
            String datePart = rawDateTime.contains("T") ? rawDateTime.split("T")[0] : rawDateTime.split(" ")[0];
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0]; // Trả về dd/MM/yyyy
            }
            return datePart;
        } catch (Exception e) {
            return rawDateTime;
        }
    }

    private void executeSaveChangesForm() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();
        String confirmPass = edtConfirmPassword.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Họ tên và địa chỉ Email không được để trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        // ĐÃ THÊM: Kiểm tra khớp mật khẩu cấp hai khi có gõ mật khẩu mới
        if (!pass.isEmpty() && !pass.equals(confirmPass)) {
            Toast.makeText(this, "Xác nhận mật khẩu mới gõ lại chưa trùng khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        // ĐÃ SỬA: Đồng bộ đóng gói sang dạng createFormData thuần túy không gây nhiễu gói tin
        MultipartBody.Part partName = MultipartBody.Part.createFormData("displayName", name);
        MultipartBody.Part partEmail = MultipartBody.Part.createFormData("email", email);
        MultipartBody.Part partPass = MultipartBody.Part.createFormData("password", pass);
        MultipartBody.Part partRole = MultipartBody.Part.createFormData("role", role);

        MultipartBody.Part filePart = null;
        if (localImageUri != null) {
            File file = getFileFromUri(localImageUri);
            if (file != null) {
                RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(localImageUri)), file);
                filePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            }
        }

        ApiClient.getApiService().adminUpdateUserWithAvatar(userId, partName, partEmail, partPass, partRole, filePart)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminUserDetailActivity.this, "Đã cập nhật dữ liệu thành viên thành công!", Toast.LENGTH_SHORT).show();
                            edtPassword.setText("");
                            edtConfirmPassword.setText("");
                            localImageUri = null;
                            loadUserProfileData();
                        } else {
                            try {
                                String errorMsg = response.errorBody().string();
                                Toast.makeText(AdminUserDetailActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(AdminUserDetailActivity.this, "Cập nhật dữ liệu thất bại!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    @Override public void onFailure(Call<User> call, Throwable t) {}
                });
    }

    // ĐÃ THÊM: Hàm khởi tạo Hộp thoại phóng to ảnh xem Avatar chất lượng cao độc lập
    private void showFullAvatarDialog(String imageUrlOrUri) {
        ImageView imagePopup = new ImageView(this);
        imagePopup.setAdjustViewBounds(true);
        imagePopup.setPadding(20, 20, 20, 20);

        Glide.with(this).load(imageUrlOrUri).into(imagePopup);

        new AlertDialog.Builder(this)
                .setView(imagePopup)
                .setPositiveButton("ĐÓNG XEM CHẾ ĐỘ", null)
                .show();
    }

    private void executeToggleBan() {
        ApiClient.getApiService().adminToggleBanUser(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) { loadUserProfileData(); }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void executeDeleteUser() {
        new AlertDialog.Builder(this)
                .setTitle("Cảnh báo xóa")
                .setMessage("Xóa vĩnh viễn tài khoản thành viên này khỏi hệ thống chứ?")
                .setPositiveButton("XÓA VĨNH VIỄN", (dialog, which) -> {
                    ApiClient.getApiService().adminDeleteUser(userId).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) { finish(); }
                        }
                        @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                    });
                })
                .setNegativeButton("HỦY", null).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 502 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            localImageUri = data.getData();
            Glide.with(this).load(localImageUri).circleCrop().into(imgAvatar);
        }
    }

    private File getFileFromUri(Uri uri) {
        try {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(uri));
            File tempFile = new File(getCacheDir(), "user_edit_" + System.currentTimeMillis() + "." + (ext != null ? ext : "jpg"));
            try (InputStream is = getContentResolver().openInputStream(uri); OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buf = new byte[4096]; int len;
                while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
                os.flush();
            }
            return tempFile;
        } catch (Exception e) { return null; }
    }
}