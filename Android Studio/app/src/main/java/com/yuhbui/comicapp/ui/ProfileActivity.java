package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.RegisterRequest;
import com.yuhbui.comicapp.data.model.User;
import com.yuhbui.comicapp.utils.HeaderUtils;
import com.yuhbui.comicapp.utils.MenuUtils;
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    private EditText edtName, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnSave, btnCancel;
    private ImageView imgProfileAvatar;
    private ProgressBar progressBar;

    // Nút bấm mắt xem ẩn/hiện mật khẩu đồng bộ màn hình Login
    private ImageView btnToggleNewPassword, btnToggleConfirmPassword;

    private ActivityResultLauncher<String> pickImageLauncher;
    private String initialName = "";
    private String initialEmail = "";
    private String currentAvatarUrl = "";
    private boolean isAvatarChanged = false;
    private Uri selectedImageUri = null;
    private int userId;

    // Các thành phần của Header dùng chung hệ thống
    private View layoutHeader;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userId = SharedPrefsManager.getUserId(this);

        // Ánh xạ DrawerLayout root quản lý menu trượt
        drawerLayout = findViewById(R.id.drawerLayout);

        initViews();
        initImagePicker();
        initPasswordVisibilityToggles(); // Đổi trạng thái Icon mắt đóng/mở chuẩn Login
        fetchUserProfile(); // Tự động cấu hình và nạp thanh Header chung bên trong hàm này

        btnCancel.setOnClickListener(v -> resetFields());
        btnSave.setOnClickListener(v -> saveChanges());

        // Bắt sự kiện nút Quay lại của hệ thống: Ưu tiên đóng Menu trượt nếu đang mở
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

    @Override
    protected void onResume() {
        super.onResume();
        if (layoutHeader != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
            HeaderUtils.loadUnreadNotificationCount(this, layoutHeader.findViewById(R.id.tvNotificationBadge));
        }
    }

    private void initViews() {
        edtName = findViewById(R.id.edtProfileFullName);
        edtEmail = findViewById(R.id.edtProfileEmail);
        edtPassword = findViewById(R.id.edtProfileNewPassword);
        edtConfirmPassword = findViewById(R.id.edtProfileConfirmPassword);

        btnSave = findViewById(R.id.btnSaveProfile);
        btnCancel = findViewById(R.id.btnCancelProfile);
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);

        progressBar = findViewById(R.id.progressBarProfile);
        if (progressBar == null) {
            progressBar = new ProgressBar(this);
        }

        // Ánh xạ các nút Mắt xem mật khẩu mới từ XML
        btnToggleNewPassword = findViewById(R.id.btnToggleNewPassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);

        // Gắn bộ lắng nghe sự kiện thay đổi dữ liệu chữ
        edtName.addTextChangedListener(profileTextWatcher);
        edtEmail.addTextChangedListener(profileTextWatcher);
        edtPassword.addTextChangedListener(profileTextWatcher);
        edtConfirmPassword.addTextChangedListener(profileTextWatcher);

        // Gắn sự kiện click mở bộ chọn ảnh
        View.OnClickListener pickImageClick = v -> pickImageLauncher.launch("image/*");
        if (findViewById(R.id.layoutChangeAvatar) != null) {
            findViewById(R.id.layoutChangeAvatar).setOnClickListener(pickImageClick);
        }
        if (findViewById(R.id.tvChangeAvatarText) != null) {
            findViewById(R.id.tvChangeAvatarText).setOnClickListener(pickImageClick);
        }
        imgProfileAvatar.setOnClickListener(pickImageClick);
    }

    private void initImagePicker() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                isAvatarChanged = true;
                Glide.with(this).load(uri).circleCrop().into(imgProfileAvatar);
                checkDataChanges();
            }
        });
    }

    /**
     * 🔄 SỬA ĐỔI: Thay đổi hình nguyên mẫu hiển thị mắt đóng/mở chuẩn xác theo màn hình Login
     */
    private void initPasswordVisibilityToggles() {
        // Đặt mặc định trạng thái ban đầu là Mắt Đóng (Ẩn mật khẩu)
        if (btnToggleNewPassword != null) btnToggleNewPassword.setImageResource(R.drawable.ic_eye);
        if (btnToggleConfirmPassword != null) btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye);

        setupSingleToggle(btnToggleNewPassword, edtPassword);
        setupSingleToggle(btnToggleConfirmPassword, edtConfirmPassword);
    }

    private void setupSingleToggle(ImageView toggleButton, EditText editText) {
        if (toggleButton == null || editText == null) return;
        toggleButton.setOnClickListener(v -> {
            if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // Hiện mật khẩu -> Đổi sang icon Mắt Mở giống Login
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleButton.setImageResource(R.drawable.ic_eye_off);
            } else {
                // Ẩn mật khẩu -> Đổi về icon Mắt Đóng giống Login
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleButton.setImageResource(R.drawable.ic_eye);
            }
            editText.setSelection(editText.getText().length()); // Giữ con trỏ ở cuối dòng
        });
    }

    private final TextWatcher profileTextWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { checkDataChanges(); }
    };

    private void checkDataChanges() {
        String currentName = edtName.getText().toString().trim();
        String currentEmail = edtEmail.getText().toString().trim();
        String currentPassword = edtPassword.getText().toString();
        String currentConfirm = edtConfirmPassword.getText().toString();

        boolean hasChanges = !currentName.equals(initialName) ||
                !currentEmail.equals(initialEmail) ||
                !currentPassword.isEmpty() ||
                !currentConfirm.isEmpty() ||
                isAvatarChanged;

        btnSave.setEnabled(hasChanges);
        btnCancel.setEnabled(hasChanges);
    }

    private void fetchUserProfile() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        ApiClient.getApiService().getUserProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    initialName = user.getDisplayName();
                    initialEmail = user.getEmail();
                    currentAvatarUrl = user.getAvatarUrl();

                    // Khởi tạo Header hệ thống dùng chung
                    setupDynamicHeaderAndMenu(user);

                    if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(currentAvatarUrl)
                                .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                                .circleCrop()
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .into(imgProfileAvatar);
                    }
                    resetFields();
                }
            }

            @Override public void onFailure(Call<User> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, "Không thể tải hồ sơ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 🛠️ ĐÃ NÂNG CẤP: Nạp trực tiếp layout_header chung thay thế cho thanh điều hướng quay lại cũ
     */
    private void setupDynamicHeaderAndMenu(User user) {
        FrameLayout drawerMenuContainer = findViewById(R.id.drawerMenuContainer);
        layoutHeader = findViewById(R.id.layoutHeaderProfile); // Kết nối với @layout/layout_header trong XML
        if (drawerMenuContainer == null || layoutHeader == null) return;

        drawerMenuContainer.removeAllViews();
        headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);

        boolean isAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole());

        // Khởi chạy bộ tiện ích nạp ảnh đại diện góc phải và sự kiện click Menu Hamburger chung
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);

        if (isAdmin) {
            getLayoutInflater().inflate(R.layout.layout_admin_side_menu, drawerMenuContainer, true);
            MenuUtils.setupAdminSideMenu(this, drawerLayout, headerMenu);

            if (headerLogo != null) {
                headerLogo.setText("COMIC APP");
                headerLogo.setTextColor(Color.parseColor("#E74C3C"));
            }
            View hSearch = layoutHeader.findViewById(R.id.headerSearch);
            View hNoti = layoutHeader.findViewById(R.id.headerNotification);
            if (hSearch != null) hSearch.setVisibility(View.GONE);
            if (hNoti != null) hNoti.setVisibility(View.GONE);

            if (headerLogo != null) {
                headerLogo.setOnClickListener(v -> {
                    Intent intent = new Intent(this, com.yuhbui.comicapp.ui.admin.AdminDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                });
            }
        } else {
            getLayoutInflater().inflate(R.layout.layout_side_menu, drawerMenuContainer, true);
            MenuUtils.setupSideMenu(this, drawerLayout, headerMenu);

            if (headerLogo != null) {
                headerLogo.setText("COMIC APP");
                headerLogo.setTextColor(Color.parseColor("#FFB77D")); // Màu cam sữa chuẩn Manga Noir
            }
            View hSearch = layoutHeader.findViewById(R.id.headerSearch);
            View hNoti = layoutHeader.findViewById(R.id.headerNotification);
            if (hSearch != null) hSearch.setVisibility(View.VISIBLE);
            if (hNoti != null) hNoti.setVisibility(View.VISIBLE);

            if (headerLogo != null) {
                headerLogo.setOnClickListener(v -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                });
            }
        }
    }

    private void resetFields() {
        edtName.setText(initialName);
        edtEmail.setText(initialEmail);
        edtPassword.setText("");
        edtConfirmPassword.setText("");
        isAvatarChanged = false;
        selectedImageUri = null;

        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentAvatarUrl)
                    .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                    .circleCrop()
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .into(imgProfileAvatar);
        } else {
            imgProfileAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        btnSave.setEnabled(false);
        btnCancel.setEnabled(false);
    }

    private void saveChanges() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Tên hiển thị và Email không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Địa chỉ Email không đúng định dạng chuẩn!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.isEmpty() && !password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        if (isAvatarChanged && selectedImageUri != null) {
            uploadAvatarFileThenText(name, email, password, confirmPassword);
        } else {
            sendProfileTextData(name, email, password, confirmPassword, currentAvatarUrl);
        }
    }

    private void uploadAvatarFileThenText(String name, String email, String password, String confirmPassword) {
        File file = getFileFromUri(selectedImageUri);

        if (file == null || !file.exists()) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            Toast.makeText(this, "Không thể xử lý tệp ảnh này!", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedImageUri)), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        ApiClient.getApiService().uploadAvatar(userId, body).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (file.exists()) {
                    file.delete();
                }

                if (response.isSuccessful() && response.body() != null) {
                    String newAvatarUrl = response.body().get("avatarUrl");
                    currentAvatarUrl = newAvatarUrl;

                    if (layoutHeader != null && layoutHeader.findViewById(R.id.headerAvatar) != null) {
                        HeaderUtils.loadHeaderAvatar(ProfileActivity.this, layoutHeader.findViewById(R.id.headerAvatar));
                    }

                    sendProfileTextData(name, email, password, confirmPassword, newAvatarUrl);
                } else {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Lỗi khi upload ảnh lên server!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onFailure(Call<Map<String, String>> call, Throwable t) {
                if (file.exists()) file.delete();
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(ProfileActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendProfileTextData(String name, String email, String password, String confirmPassword, String avatarUrl) {
        RegisterRequest updateRequest = new RegisterRequest(email, name, password, confirmPassword);
        updateRequest.setAvatarUrl(avatarUrl);

        ApiClient.getApiService().updateProfile(userId, updateRequest).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    User updatedUser = response.body();
                    Toast.makeText(ProfileActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();

                    initialName = updatedUser.getDisplayName();
                    initialEmail = updatedUser.getEmail();
                    SharedPrefsManager.saveUser(ProfileActivity.this, updatedUser);

                    resetFields();
                } else {
                    btnSave.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Cập nhật thông tin thất bại!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onFailure(Call<User> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private File getFileFromUri(Uri uri) {
        try {
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(uri));
            if (extension == null) extension = "jpg";

            File tempFile = new File(getCacheDir(), "avatar_upload_" + System.currentTimeMillis() + "." + extension);

            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(tempFile)) {

                byte[] buffer = new byte[4096];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();
            }
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}