package com.yuhbui.comicapp.ui;

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
import com.yuhbui.comicapp.data.model.RegisterRequest;
import com.yuhbui.comicapp.data.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtEmail, edtDisplayName, edtPassword, edtConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ánh xạ View
        edtEmail = findViewById(R.id.edtRegisterEmail);
        edtDisplayName = findViewById(R.id.edtRegisterDisplayName);
        edtPassword = findViewById(R.id.edtRegisterPassword);
        edtConfirmPassword = findViewById(R.id.edtRegisterConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBarRegister);

        // Xử lý sự kiện đăng ký
        btnRegister.setOnClickListener(v -> performRegister());

        // Quay lại màn hình đăng nhập
        if (tvLogin != null) {
            tvLogin.setOnClickListener(v -> finish()); // Đóng màn hình này để về Login
        }
    }

    private void performRegister() {
        String email = edtEmail.getText().toString().trim();
        String displayName = edtDisplayName.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        // 1. Kiểm tra rỗng
        if (email.isEmpty() || displayName.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Kiểm tra định dạng Email hợp lệ
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Địa chỉ Email không đúng định dạng chuẩn!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Kiểm tra mật khẩu khớp nhau ở phía App để tiết kiệm thời gian gọi mạng
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiện loading
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Gọi API
        RegisterRequest request = new RegisterRequest(email, displayName, password, confirmPassword);

        ApiClient.getApiService().register(request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                // Ẩn loading
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Hãy đăng nhập.", Toast.LENGTH_LONG).show();
                    finish(); // Trở về màn hình đăng nhập
                } else {
                    // Lấy thông báo lỗi từ Backend (ví dụ: "Email đã tồn tại")
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Đăng ký thất bại!";
                        Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Toast.makeText(RegisterActivity.this, "Lỗi khi xử lý phản hồi", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Ẩn loading
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}