package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.LoginRequest;
import com.yuhbui.comicapp.data.model.User;
import com.yuhbui.comicapp.utils.SharedPrefsManager;

// Import đầy đủ các thư viện Google Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;
    private CheckBox cbRememberMe;

    // Thuộc tính phục vụ đăng nhập Google
    private SignInButton btnGoogleSignIn;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. LOGIC KIỂM TRA TỰ ĐỘNG ĐĂNG NHẬP (Theo CheckBox ghi nhớ)
        if (SharedPrefsManager.isLoggedIn(this)) {
            if (SharedPrefsManager.isRemembered(this)) {
                Intent intent;
                String currentRole = SharedPrefsManager.getUserRole(this);

                if ("Admin".equals(currentRole)) {
                    intent = new Intent(this, com.yuhbui.comicapp.ui.admin.AdminDashboardActivity.class);
                } else {
                    intent = new Intent(this, MainActivity.class);
                }

                startActivity(intent);
                finish();
                return;
            } else {
                // Nếu phiên trước không tích ghi nhớ -> Xóa session cũ bắt nhập lại
                SharedPrefsManager.logout(this);
            }
        }

        setContentView(R.layout.activity_login);

        // 2. ÁNH XẠ TOÀN BỘ CÁC VIEW TRÊN GIAO DIỆN DARK THEME
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        // 3. CẤU HÌNH ĐĂNG NHẬP GOOGLE SIGN-IN
        // CHÚ Ý: Đổi chuỗi bên dưới thành mã WEB_CLIENT_ID thật của bạn lấy trên Google Cloud
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("904461562945-dddi1tckgjg94f2m0n0d9n5t4140j8tg.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 4. CÀI ĐẶT SỰ KIỆN CLICK CHO TẤT CẢ CÁC NÚT BẤM
        btnLogin.setOnClickListener(v -> performLogin());

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        if (tvRegister != null) {
            tvRegister.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            });
        }
    }

    // Hàm kích hoạt mở form chọn tài khoản Google (Đã tối ưu luôn xóa cache để hiện bảng chọn)
    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    // Nhận dữ liệu Token Google gửi về thiết bị
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String idToken = account.getIdToken();
                    // Đóng gói gửi lên Spring Boot Backend
                    sendTokenToBackend(idToken);
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Lỗi kết nối Google (Code: " + e.getStatusCode() + ")", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Gửi ID Token lên Spring Boot xác thực tài khoản/vai trò
    private void sendTokenToBackend(String idToken) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnGoogleSignIn.setEnabled(false);

        Map<String, String> body = new HashMap<>();
        body.put("idToken", idToken);

        ApiClient.getApiService().loginWithGoogle(body).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnGoogleSignIn.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    User loggedInUser = response.body();

                    // Đối với đăng nhập mạng xã hội Google, mặc định là luôn duy trì đăng nhập (true)
                    SharedPrefsManager.saveUser(LoginActivity.this, loggedInUser, true);

                    Toast.makeText(LoginActivity.this, "Chào mừng " + loggedInUser.getDisplayName(), Toast.LENGTH_SHORT).show();

                    Intent intent;
                    if ("Admin".equals(loggedInUser.getRole())) {
                        intent = new Intent(LoginActivity.this, com.yuhbui.comicapp.ui.admin.AdminDashboardActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    }

                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Backend từ chối xác thực tài khoản Google!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnGoogleSignIn.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Lỗi kết nối mạng đến Server Backend", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Logic Đăng nhập bằng Email & Mật khẩu truyền thống
    private void performLogin() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        LoginRequest request = new LoginRequest(email, password);
        request.setEmail(email);
        request.setPassword(password);

        ApiClient.getApiService().login(request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    User loggedInUser = response.body();

                    // Đọc trạng thái check của ô ghi nhớ tài khoản từ Form giao diện tối mới
                    boolean isCheckedRemember = cbRememberMe != null && cbRememberMe.isChecked();

                    // Lưu thông tin kèm điều kiện CheckBox
                    SharedPrefsManager.saveUser(LoginActivity.this, loggedInUser, isCheckedRemember);

                    Toast.makeText(LoginActivity.this, "Chào mừng " + loggedInUser.getDisplayName(), Toast.LENGTH_SHORT).show();

                    Intent intent;
                    if ("Admin".equals(loggedInUser.getRole())) {
                        intent = new Intent(LoginActivity.this, com.yuhbui.comicapp.ui.admin.AdminDashboardActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    }

                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}