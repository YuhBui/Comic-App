package com.yuhbui.comicapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.ResetPasswordRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText edtForgotEmail, edtOtpCode, edtForgotNewPassword;
    private Button btnSendOtp, btnConfirmReset;
    private LinearLayout layoutResetPasswordStep;
    private ProgressBar forgotProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        edtForgotEmail = findViewById(R.id.edtForgotEmail);
        edtOtpCode = findViewById(R.id.edtOtpCode);
        edtForgotNewPassword = findViewById(R.id.edtForgotNewPassword);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnConfirmReset = findViewById(R.id.btnConfirmReset);
        layoutResetPasswordStep = findViewById(R.id.layoutResetPasswordStep);
        forgotProgressBar = findViewById(R.id.forgotProgressBar);

        // Giai đoạn 1: Gửi yêu cầu mã OTP
        btnSendOtp.setOnClickListener(v -> requestOtpFromBackend());

        // Giai đoạn 2: Gửi OTP kèm mật khẩu mới để cập nhật dữ liệu
        btnConfirmReset.setOnClickListener(v -> submitNewPasswordToBackend());
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
                    Toast.makeText(ForgotPasswordActivity.this, "Mã OTP đã được gửi! Vui lòng kiểm tra màn hình log console của Server.", Toast.LENGTH_LONG).show();
                    // Hiển thị phần nhập mã OTP và đổi mật khẩu mới lên
                    layoutResetPasswordStep.setVisibility(View.VISIBLE);
                    edtForgotEmail.setEnabled(false); // Khóa ô nhập email để cố định thông tin xác thực
                    btnSendOtp.setVisibility(View.GONE); // Ẩn nút gửi mã ban đầu
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Email này không tồn tại trong hệ thống!", Toast.LENGTH_SHORT).show();
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

    private void submitNewPasswordToBackend() {
        String email = edtForgotEmail.getText().toString().trim();
        String otp = edtOtpCode.getText().toString().trim();
        String newPassword = edtForgotNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ mã OTP và Mật khẩu mới", Toast.LENGTH_SHORT).show();
            return;
        }

        if (forgotProgressBar != null) forgotProgressBar.setVisibility(View.VISIBLE);
        btnConfirmReset.setEnabled(false);

        ResetPasswordRequest request = new ResetPasswordRequest(email, otp, newPassword);

        ApiClient.getApiService().resetPassword(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (forgotProgressBar != null) forgotProgressBar.setVisibility(View.GONE);
                btnConfirmReset.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Đặt lại mật khẩu thành công! Hãy đăng nhập bằng mật khẩu mới.", Toast.LENGTH_LONG).show();
                    finish(); // Đóng màn hình khôi phục, quay trở lại màn hình đăng nhập
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Mã OTP không chính xác hoặc đã hết hạn!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (forgotProgressBar != null) forgotProgressBar.setVisibility(View.GONE);
                btnConfirmReset.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối mạng, vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
            }
        });
    }
}