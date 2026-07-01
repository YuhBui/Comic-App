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
import com.yuhbui.comicapp.data.model.ResetPasswordRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextView tvDisplayEmail;
    private EditText edtForgotNewPassword, edtForgotConfirmNewPassword;
    private Button btnConfirmReset;
    private ProgressBar resetProgressBar;

    private String safeEmail;
    private String safeOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Nhận dữ liệu cố định, đóng gói an toàn từ chuỗi hoạt động trước
        safeEmail = getIntent().getStringExtra("EMAIL_KEY");
        safeOtp = getIntent().getStringExtra("OTP_KEY");

        tvDisplayEmail = findViewById(R.id.tvDisplayEmail);
        edtForgotNewPassword = findViewById(R.id.edtForgotNewPassword);
        edtForgotConfirmNewPassword = findViewById(R.id.edtForgotConfirmNewPassword);
        btnConfirmReset = findViewById(R.id.btnConfirmReset);
        resetProgressBar = findViewById(R.id.resetProgressBar);

        // Hiển thị email cho người dùng thấy nhưng không thể chỉnh sửa
        if (safeEmail != null) {
            tvDisplayEmail.setText(safeEmail);
        }

        btnConfirmReset.setOnClickListener(v -> submitNewPasswordToBackend());
    }

    private void submitNewPasswordToBackend() {
        String newPassword = edtForgotNewPassword.getText().toString().trim();
        String confirmNewPassword = edtForgotConfirmNewPassword.getText().toString().trim();

        if (newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ mật khẩu mới thiết lập", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không trùng khớp! Vui lòng nhập lại.", Toast.LENGTH_LONG).show();
            return;
        }

        if (resetProgressBar != null) resetProgressBar.setVisibility(View.VISIBLE);
        btnConfirmReset.setEnabled(false);

        // Đóng gói Object gửi lên Server bằng Email và OTP bất biến
        ResetPasswordRequest request = new ResetPasswordRequest(safeEmail, safeOtp, newPassword);

        ApiClient.getApiService().resetPassword(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (resetProgressBar != null) resetProgressBar.setVisibility(View.GONE);
                btnConfirmReset.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this, "Đặt lại mật khẩu thành công! Mời bạn đăng nhập.", Toast.LENGTH_LONG).show();

                    // Giải phóng và xóa toàn bộ ngăn xếp phục hồi mật khẩu để quay lại màn hình đăng nhập sạch sẽ
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finishAffinity();
                } else {
                    Toast.makeText(ResetPasswordActivity.this, "Mã OTP nhập vào sai hoặc đã hết hạn!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (resetProgressBar != null) resetProgressBar.setVisibility(View.GONE);
                btnConfirmReset.setEnabled(true);
                Toast.makeText(ResetPasswordActivity.this, "Lỗi kết nối mạng, vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
            }
        });
    }
}