package com.yuhbui.comicapp.ui;

import android.content.Intent; // THÊM
import android.graphics.Color;  // THÊM
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout; // THÊM: Để chứa Menu trượt động
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;     // THÊM
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;              // THÊM
import androidx.drawerlayout.widget.DrawerLayout;    // THÊM

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.RegisterRequest;
import com.yuhbui.comicapp.data.model.User;
import com.yuhbui.comicapp.utils.HeaderUtils;          // THÊM
import com.yuhbui.comicapp.utils.MenuUtils;            // THÊM
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

    private DrawerLayout drawerLayout; // THÊM: Quản lý đóng mở menu trượt đè nền tối

    private EditText edtName, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnSave, btnCancel;
    private ImageView imgProfileAvatar;
    private ProgressBar progressBar;

    private ActivityResultLauncher<String> pickImageLauncher;
    private String initialName = "";
    private String initialEmail = "";
    private String currentAvatarUrl = "";
    private boolean isAvatarChanged = false;
    private Uri selectedImageUri = null;
    private int userId;

    // Các thành phần của Header dùng chung
    private View layoutHeader;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userId = SharedPrefsManager.getUserId(this);

        // Ánh xạ DrawerLayout root ngoài cùng
        drawerLayout = findViewById(R.id.drawerLayout);

        initViews();
        initImagePicker();
        fetchUserProfile(); // Logic Header & Menu trượt sẽ được tự động kích hoạt bên trong hàm này khi có dữ liệu Role

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
        // Tự động làm mới Avatar góc phải và số lượng thông báo khi quay lại giao diện
        if (layoutHeader != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
            HeaderUtils.loadUnreadNotificationCount(this, layoutHeader.findViewById(R.id.tvNotificationBadge));
        }
    }

    private void initViews() {
        edtName = findViewById(R.id.edtProfileName);
        edtEmail = findViewById(R.id.edtProfileEmail);
        edtPassword = findViewById(R.id.edtProfilePassword);
        edtConfirmPassword = findViewById(R.id.edtProfileConfirmPassword);
        btnSave = findViewById(R.id.btnProfileSave);
        btnCancel = findViewById(R.id.btnProfileCancel);
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        progressBar = findViewById(R.id.progressBarProfile);

        edtName.addTextChangedListener(profileTextWatcher);
        edtEmail.addTextChangedListener(profileTextWatcher);
        edtPassword.addTextChangedListener(profileTextWatcher);
        edtConfirmPassword.addTextChangedListener(profileTextWatcher);

        imgProfileAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
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
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getApiService().getUserProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    initialName = user.getDisplayName();
                    initialEmail = user.getEmail();
                    currentAvatarUrl = user.getAvatarUrl();

                    // THÊM: Kích hoạt xử lý cấu hình đa phân quyền Header & Menu trượt dựa trên Role thật
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
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, "Không thể tải hồ sơ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * SỬA ĐỔI TOÀN DIỆN: Hàm phân quyền cấu hình Header và nạp Menu trượt động tại runtime
     */
    private void setupDynamicHeaderAndMenu(User user) {
        FrameLayout drawerMenuContainer = findViewById(R.id.drawerMenuContainer);
        layoutHeader = findViewById(R.id.layoutHeaderProfile);
        if (drawerMenuContainer == null || layoutHeader == null) return;

        drawerMenuContainer.removeAllViews(); // Dọn dẹp container trước khi nạp
        headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);

        // Kiểm tra điều kiện Role của hệ thống (Hãy đối chiếu với Entity User của bạn: e.g. "ADMIN" hoặc "Admin")
        boolean isAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole());

        // 1. Kích hoạt tính năng cốt lõi của thanh Header (Avatar nhỏ, Bell thông báo, Global Search)
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);

        if (isAdmin) {
            // NẠP MENU ADMIN: Đẩy giao diện menu quản trị vào Container trượt
            getLayoutInflater().inflate(R.layout.layout_admin_side_menu, drawerMenuContainer, true);
            MenuUtils.setupAdminSideMenu(this, drawerLayout, headerMenu);

            // Cấu hình phong cách Header Admin: Chữ đỏ cam, ẩn ô kính lúp và chuông báo
            headerLogo.setText("COMIC APP");
            headerLogo.setTextColor(Color.parseColor("#E74C3C"));
            layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
            layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);

            // Thiết lập click vào tên App nhảy về Dashboard Admin
            headerLogo.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.yuhbui.comicapp.ui.admin.AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        } else {
            // NẠP MENU USER: Đẩy giao diện các tính năng của người đọc thông thường vào Container
            getLayoutInflater().inflate(R.layout.layout_side_menu, drawerMenuContainer, true);
            MenuUtils.setupSideMenu(this, drawerLayout, headerMenu);

            // Cấu hình phong cách Header User: Hiện đầy đủ Tìm kiếm chung, Chuông báo đỏ
            headerLogo.setText("COMIC APP");
            headerLogo.setTextColor(Color.parseColor("#333333"));
            layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.VISIBLE);
            layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.VISIBLE);

            // Thiết lập click vào tên App nhảy về Trang chủ truyện của người dùng
            headerLogo.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
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

        if (!password.isEmpty() && !password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
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
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            Toast.makeText(this, "Không thể xử lý tệp ảnh này!", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedImageUri)), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        ApiClient.getApiService().uploadAvatar(userId, body).enqueue(new Callback<java.util.Map<String, String>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, String>> call, Response<java.util.Map<String, String>> response) {
                if (file.exists()) {
                    file.delete();
                }

                if (response.isSuccessful() && response.body() != null) {
                    String newAvatarUrl = response.body().get("avatarUrl");
                    currentAvatarUrl = newAvatarUrl;

                    // Cập nhật ngay lập tức ảnh đại diện thu nhỏ trên thanh Header sau khi upload thành công
                    if (layoutHeader != null && layoutHeader.findViewById(R.id.headerAvatar) != null) {
                        HeaderUtils.loadHeaderAvatar(ProfileActivity.this, layoutHeader.findViewById(R.id.headerAvatar));
                    }

                    sendProfileTextData(name, email, password, confirmPassword, newAvatarUrl);
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Lỗi khi upload ảnh lên server!", Toast.LENGTH_SHORT).show();
                }
            }

            // Hàm tương thích phiên bản retrofit cũ / mới nếu đổi tên phương thức
            @Override public void onFailure(Call<java.util.Map<String, String>> call, Throwable t) {
                if (file.exists()) file.delete();
                progressBar.setVisibility(View.GONE);
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
                progressBar.setVisibility(View.GONE);
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
                progressBar.setVisibility(View.GONE);
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