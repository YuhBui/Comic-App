package com.yuhbui.comicapp.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.RegisterRequest;
import com.yuhbui.comicapp.data.model.User;
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.io.File;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userId = SharedPrefsManager.getUserId(this);

        initViews();
        initImagePicker();
        fetchUserProfile();

        btnCancel.setOnClickListener(v -> resetFields());
        btnSave.setOnClickListener(v -> saveChanges());
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

                    if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(currentAvatarUrl)
                                // Sử dụng Signature với thời gian thực để ép Glide tải ảnh mới
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

    private void resetFields() {
        edtName.setText(initialName);
        edtEmail.setText(initialEmail);
        edtPassword.setText("");
        edtConfirmPassword.setText("");
        isAvatarChanged = false;
        selectedImageUri = null;

        // Nếu nhấn hủy hoặc vừa lưu xong, nạp lại ảnh avatar từ DB
        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentAvatarUrl)
                    // Sử dụng Signature với thời gian thực để ép Glide tải ảnh mới
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
        File file = new File(getRealPathFromURI(selectedImageUri));
        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedImageUri)), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        // Đảm bảo Enqueue truyền vào đúng loại Callback<Map<String, String>>
        ApiClient.getApiService().uploadAvatar(userId, body).enqueue(new Callback<java.util.Map<String, String>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, String>> call, Response<java.util.Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Lấy chính xác URL từ cấu trúc Map bằng từ khóa "avatarUrl"
                    String newAvatarUrl = response.body().get("avatarUrl");
                    currentAvatarUrl = newAvatarUrl;

                    // Bước tiếp theo: Lưu thông tin chữ xuống database
                    sendProfileTextData(name, email, password, confirmPassword, newAvatarUrl);
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Lỗi khi upload ảnh lên server!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<java.util.Map<String, String>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                // In ra log chi tiết để theo dõi dễ dàng
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối upload file: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor == null) return contentUri.getPath();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }
}