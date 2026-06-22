package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback; // THÊM: Quản lý nút Back hệ thống
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;              // THÊM: Hỗ trợ đóng mở DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout;    // THÊM: Khai báo thành phần DrawerLayout Root

import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.User;
import com.yuhbui.comicapp.utils.HeaderUtils;          // THÊM: Tiện ích Header dùng chung
import com.yuhbui.comicapp.utils.MenuUtils;            // THÊM: Tiện ích Menu Admin dùng chung

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminAddUserActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 501;

    private DrawerLayout drawerLayout; // THÊM: Khai báo thành phần DrawerLayout quản lý menu trượt đè lên

    private ImageView imgAvatar;
    private Button btnChooseAvatar;
    private EditText edtName, edtEmail, edtPassword, edtConfirmPassword;
    private Spinner spinnerRole;
    private Button btnSubmit;
    private Uri selectedAvatarUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_user);

        // 1. Ánh xạ DrawerLayout Root mới từ file XML
        drawerLayout = findViewById(R.id.drawerLayout);

        // 2. Khởi tạo thanh Header Admin và Menu trượt dùng chung
        View layoutHeader = findViewById(R.id.layoutHeaderAddUser);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);

        // Chạy khởi tạo chức năng Header lõi & liên kết DrawerLayout
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);
        MenuUtils.setupAdminSideMenu(this, drawerLayout, headerMenu);

        // Ẩn triệt để hai nút Tìm kiếm và Thông báo không thuộc phận sự của Admin
        if (layoutHeader.findViewById(R.id.headerSearch) != null) {
            layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        }
        if (layoutHeader.findViewById(R.id.headerNotification) != null) {
            layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        }

        // ĐỒNG BỘ: Luôn hiện tên App cố định màu đỏ cam và nhấn vào tự động quay về Dashboard
        if (headerLogo != null) {
            headerLogo.setText("COMIC APP");
            headerLogo.setTextColor(Color.parseColor("#E74C3C"));
            headerLogo.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }

        // 3. Ánh xạ các View điều khiển form nhập liệu
        imgAvatar = findViewById(R.id.imgAddUserSelectAvatar);
        btnChooseAvatar = findViewById(R.id.btnChooseAvatarAdd);
        edtName = findViewById(R.id.edtAddUserName);
        edtEmail = findViewById(R.id.edtAddUserEmail);
        edtPassword = findViewById(R.id.edtAddUserPassword);
        edtConfirmPassword = findViewById(R.id.edtAddUserConfirmPassword);
        spinnerRole = findViewById(R.id.spinnerAddUserRole);
        btnSubmit = findViewById(R.id.btnAdminAddUserSubmit);

        spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Arrays.asList("User", "Admin")));

        // Nhấn vào Avatar để mở dialog xem phóng to ảnh chất lượng cao
        imgAvatar.setOnClickListener(v -> {
            if (selectedAvatarUri != null) {
                showFullAvatarDialog(selectedAvatarUri.toString());
            } else {
                Toast.makeText(this, "Chưa có ảnh đại diện nào được chọn để xem phóng to!", Toast.LENGTH_SHORT).show();
            }
        });

        // Bấm vào nút dưới avatar để mở thư viện Gallery chọn ảnh bìa
        btnChooseAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        btnSubmit.setOnClickListener(v -> executeCreateUserForm());

        // 4. CẤU HÌNH: Khóa nút Back cứng hệ thống - Ưu tiên đóng Menu trượt nếu nó đang mở
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
    }

    // Làm mới, hiển thị ảnh đại diện Admin góc trên bên phải mỗi khi quay lại trang này
    @Override
    protected void onResume() {
        super.onResume();
        View layoutHeader = findViewById(R.id.layoutHeaderAddUser);
        if (layoutHeader != null && layoutHeader.findViewById(R.id.headerAvatar) != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
        }
    }

    private void showFullAvatarDialog(String imageUrlOrUri) {
        ImageView imagePopup = new ImageView(this);
        imagePopup.setAdjustViewBounds(true);
        imagePopup.setPadding(24, 24, 24, 24);

        Glide.with(this).load(imageUrlOrUri).into(imagePopup);

        new AlertDialog.Builder(this)
                .setView(imagePopup)
                .setPositiveButton("ĐÓNG", null)
                .show();
    }

    private void executeCreateUserForm() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();
        String confirmPass = edtConfirmPassword.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu xác minh nhập lại không trùng khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        MultipartBody.Part partName = MultipartBody.Part.createFormData("displayName", name);
        MultipartBody.Part partEmail = MultipartBody.Part.createFormData("email", email);
        MultipartBody.Part partPass = MultipartBody.Part.createFormData("password", pass);
        MultipartBody.Part partRole = MultipartBody.Part.createFormData("role", role);

        MultipartBody.Part avatarPart = null;
        if (selectedAvatarUri != null) {
            File file = getFileFromUri(selectedAvatarUri);
            if (file != null) {
                RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedAvatarUri)), file);
                avatarPart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            }
        }

        btnSubmit.setEnabled(false);
        Toast.makeText(this, "Đang xử lý khởi tạo thành viên...", Toast.LENGTH_SHORT).show();

        ApiClient.getApiService().adminCreateUser(partName, partEmail, partPass, partRole, avatarPart)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        btnSubmit.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminAddUserActivity.this, "Tạo tài khoản thành viên thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            try {
                                String serverErr = response.errorBody().string();
                                Toast.makeText(AdminAddUserActivity.this, serverErr, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(AdminAddUserActivity.this, "Tên hiển thị hoặc Email đã được sử dụng!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        btnSubmit.setEnabled(true);
                        Toast.makeText(AdminAddUserActivity.this, "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedAvatarUri = data.getData();
            Glide.with(this).load(selectedAvatarUri).circleCrop().into(imgAvatar);
        }
    }

    private File getFileFromUri(Uri uri) {
        try {
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(uri));
            File tempFile = new File(getCacheDir(), "user_av_" + System.currentTimeMillis() + "." + (extension != null ? extension : "jpg"));
            try (InputStream is = getContentResolver().openInputStream(uri); OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buf = new byte[4096]; int len;
                while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
                os.flush();
            }
            return tempFile;
        } catch (Exception e) { return null; }
    }

    public void finishActivity(View view) {
        finish();
    }
}