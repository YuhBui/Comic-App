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

    private EditText edtForgotEmail, edtOtpCode, edtForgotNewPassword, edtForgotConfirmNewPassword;
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
        edtForgotConfirmNewPassword = findViewById(R.id.edtForgotConfirmNewPassword);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnConfirmReset = findViewById(R.id.btnConfirmReset);
        layoutResetPasswordStep = findViewById(R.id.layoutResetPasswordStep);
        forgotProgressBar = findViewById(R.id.forgotProgressBar);

        // Giai đoạn 1: Gửi yêu cầu mã OTP về Gmail
        btnSendOtp.setOnClickListener(v -> requestOtpFromBackend());

        // Giai đoạn 2: Xác nhận OTP và đổi mật khẩu mới
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
                    Toast.makeText(ForgotPasswordActivity.this, "Mã OTP đã gửi! Vui lòng kiểm tra hộp thư Gmail.", Toast.LENGTH_LONG).show();
                    layoutResetPasswordStep.setVisibility(View.VISIBLE);
                    edtForgotEmail.setEnabled(false);
                    btnSendOtp.setVisibility(View.GONE);
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

    private void submitNewPasswordToBackend() {
        String email = edtForgotEmail.getText().toString().trim();
        String otp = edtOtpCode.getText().toString().trim();
        String newPassword = edtForgotNewPassword.getText().toString().trim();
        String confirmNewPassword = edtForgotConfirmNewPassword.getText().toString().trim(); // Lấy chữ người dùng gõ ở ô thứ 2

        // 1. Kiểm tra không được bỏ trống thông tin
        if (otp.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ mã OTP và mật khẩu mới thiết lập", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. BƯỚC QUAN TRỌNG: Kiểm tra trùng khớp mật khẩu nhập 2 lần ở phía App để tối ưu hệ thống
        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không trùng khớp! Vui lòng nhập lại.", Toast.LENGTH_LONG).show();
            return; // Chặn đứng luồng kết nối không cho gọi API gửi lên server
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
                    Toast.makeText(ForgotPasswordActivity.this, "Đặt lại mật khẩu thành công! Mời bạn đăng nhập.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Mã OTP nhập vào sai hoặc đã hết hạn!", Toast.LENGTH_SHORT).show();
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