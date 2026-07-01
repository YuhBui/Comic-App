package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.yuhbui.comicapp.R;

public class VerifyOtpActivity extends AppCompatActivity {

    private EditText edtOtpCode;
    private Button btnVerifyOtp;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        // Nhận lại thông tin email từ màn hình trước
        userEmail = getIntent().getStringExtra("EMAIL_KEY");

        edtOtpCode = findViewById(R.id.edtOtpCode);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);

        btnVerifyOtp.setOnClickListener(v -> {
            String otp = edtOtpCode.getText().toString().trim();

            if (otp.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mã OTP để tiếp tục", Toast.LENGTH_SHORT).show();
                return;
            }

            // Chuyển sang giao diện Reset Password và đính kèm cố định cả Email và OTP
            Intent intent = new Intent(VerifyOtpActivity.this, ResetPasswordActivity.class);
            intent.putExtra("EMAIL_KEY", userEmail);
            intent.putExtra("OTP_KEY", otp);
            startActivity(intent);
        });
    }
}