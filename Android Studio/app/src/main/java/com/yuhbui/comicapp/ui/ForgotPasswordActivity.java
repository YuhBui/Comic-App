package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText edtForgotEmail;
    private Button btnSendOtp;
    private ProgressBar forgotProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        edtForgotEmail = findViewById(R.id.edtForgotEmail);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        forgotProgressBar = findViewById(R.id.forgotProgressBar);

        btnSendOtp.setOnClickListener(v -> requestOtpFromBackend());
    }

    private void requestOtpFromBackend() {
        String email = edtForgotEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Email tài khoản cần khôi phục", Toast.LENGTH_SHORT).show();
            return;
        }

        if (forgotProgressBar != null) forgotProgressBar.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);

        ApiClient.getApiService().forgotPassword(email).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (forgotProgressBar != null) forgotProgressBar.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Mã OTP đã gửi! Vui lòng kiểm tra hộp thư Gmail.", Toast.LENGTH_LONG).show();

                    // Chuyển sang giao diện nhập OTP và truyền Email sang
                    Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
                    intent.putExtra("EMAIL_KEY", email);
                    startActivity(intent);
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Email này không tồn tại trên hệ thống!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (forgotProgressBar != null) forgotProgressBar.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối mạng đến Server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}