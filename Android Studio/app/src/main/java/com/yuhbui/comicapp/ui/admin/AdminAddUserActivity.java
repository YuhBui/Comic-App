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
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminAddUserActivity extends AppCompatActivity {

    private ImageView imgAvatar;
    private Button btnChooseAvatar; // BỔ SUNG: Nút chọn ảnh biệt lập dưới avatar
    private EditText edtName, edtEmail, edtPassword, edtConfirmPassword;
    private Spinner spinnerRole;
    private Button btnSubmit;
    private Uri selectedAvatarUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_user);

        // Khởi tạo thanh Header Admin
        View layoutHeader = findViewById(R.id.layoutHeaderAddUser);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        headerLogo.setText("THÊM THÀNH VIÊN MỚI");
        headerLogo.setTextColor(Color.parseColor("#E74C3C"));
        headerMenu.setOnClickListener(v -> finish());

        // Ánh xạ View điều khiển
        imgAvatar = findViewById(R.id.imgAddUserSelectAvatar);
        btnChooseAvatar = findViewById(R.id.btnChooseAvatarAdd); // BỔ SUNG
        edtName = findViewById(R.id.edtAddUserName);
        edtEmail = findViewById(R.id.edtAddUserEmail);
        edtPassword = findViewById(R.id.edtAddUserPassword);
        edtConfirmPassword = findViewById(R.id.edtAddUserConfirmPassword);
        spinnerRole = findViewById(R.id.spinnerAddUserRole);
        btnSubmit = findViewById(R.id.btnAdminAddUserSubmit);

        spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Arrays.asList("User", "Admin")));

        // ĐÃ SỬA: Nhấn vào Avatar chỉ kích hoạt chế độ xem phóng to toàn màn hình
        imgAvatar.setOnClickListener(v -> {
            if (selectedAvatarUri != null) {
                showFullAvatarDialog(selectedAvatarUri.toString());
            } else {
                Toast.makeText(this, "Chưa có ảnh đại diện nào được chọn để xem phóng to!", Toast.LENGTH_SHORT).show();
            }
        });

        // ĐÃ SỬA: Bấm vào nút "Chọn ảnh đại diện" dưới avatar mới mở thư viện chọn tệp Gallery
        btnChooseAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 501);
        });

        btnSubmit.setOnClickListener(v -> executeCreateUserForm());
    }

    // BỔ SUNG: Hàm khởi tạo Dialog chứa ImageView phóng lớn hiển thị Avatar chất lượng cao
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

        // KIỂM TRA ĐỐI CHIẾU KHỚP MẬT KHẨU CẤP 2 NHẬP LẠI
        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu xác minh nhập lại không trùng khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        // ĐÃ SỬA: Đóng gói toàn bộ bằng createFormData để loại bỏ hoàn toàn dấu ngoặc kép bọc chuỗi và lỗi timestamp
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

        // Truyền loạt biến Part sạch lỗi vào hàm gọi mạng
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
        if (requestCode == 501 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedAvatarUri = data.getData();
            // Đổ ảnh xem trước bo tròn bằng Glide để đồng bộ trải nghiệm
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
}