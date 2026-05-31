package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // LOGIC KIỂM TRA TỰ ĐỘNG ĐĂNG NHẬP (ĐÃ SỬA ĐỔI PHÂN QUYỀN)
        if (SharedPrefsManager.isLoggedIn(this)) {
            Intent intent;
            String currentRole = SharedPrefsManager.getUserRole(this);

            if ("Admin".equals(currentRole)) {
                // Nếu phiên đăng nhập cũ lưu quyền Admin -> Vào thẳng vùng quản trị
                intent = new Intent(this, com.yuhbui.comicapp.ui.admin.AdminDashboardActivity.class);
            } else {
                // Ngược lại nếu là User thường -> Vào trang chủ đọc truyện
                intent = new Intent(this, MainActivity.class);
            }

            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> performLogin());
        if (tvRegister != null) {
            tvRegister.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }
    }

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

                    // GỌI HÀM LƯU TỔNG HỢP: Lưu ID, Name và tự động lưu kèm cả biến Role mới
                    SharedPrefsManager.saveUser(LoginActivity.this, loggedInUser);

                    Toast.makeText(LoginActivity.this, "Chào mừng " + loggedInUser.getDisplayName(), Toast.LENGTH_SHORT).show();

                    // LOGIC KIỂM TRA ROLE ĐỂ PHÂN PHỐI GIAO DIỆN ĐÍCH
                    Intent intent;
                    if ("Admin".equals(loggedInUser.getRole())) {
                        // Trạng thái tài khoản trả về có chữ "Admin" viết hoa chữ đầu -> Mở Dashboard của Admin
                        intent = new Intent(LoginActivity.this, com.yuhbui.comicapp.ui.admin.AdminDashboardActivity.class);
                    } else {
                        // Trạng thái thông thường (User) -> Vào màn hình chính đọc truyện
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    }

                    startActivity(intent);
                    finish(); // Đóng hẳn LoginActivity
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