package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.data.model.User;
import com.yuhbui.comicapp.ui.adapters.RankingAdapter;
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private RadioGroup rgDashboardFilter;
    private LineChart lineChartAccess;
    private RecyclerView rvAdminTopComic;
    private RankingAdapter rankingAdapter;

    // Các thành phần thuộc thanh Header dùng chung
    private View layoutHeaderAdmin;
    private ImageView headerMenu, headerAvatar, headerSearch, headerNotification;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // 1. Ánh xạ các thành phần giao diện chính
        rgDashboardFilter = findViewById(R.id.rgDashboardFilter);
        lineChartAccess = findViewById(R.id.lineChartAccess);
        rvAdminTopComic = findViewById(R.id.rvAdminTopComic);

        // 2. Thiết lập cấu hình tùy biến thanh Header Admin
        setupAdminHeaderView();

        // 3. Cấu hình RecyclerView hiển thị danh sách dọc Top 10 truyện
        rvAdminTopComic.setLayoutManager(new LinearLayoutManager(this));
        rankingAdapter = new RankingAdapter();
        rvAdminTopComic.setAdapter(rankingAdapter);

        // 4. Lắng nghe sự kiện từ bộ lọc dùng chung Ngày / Tuần / Tháng
        rgDashboardFilter.setOnCheckedChangeListener((group, checkedId) -> {
            String type = "day";
            if (checkedId == R.id.rbDashWeek) {
                type = "week";
            } else if (checkedId == R.id.rbDashMonth) {
                type = "month";
            }
            // Đồng bộ tải lại cả biểu đồ xu hướng và bảng xếp hạng truyện
            loadDashboardData(type);
        });

        // 5. Quản lý nút Quay lại (Back cứng) theo tiêu chuẩn Android 13+ độc lập
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Cho phép thoát không gian Admin về màn hình trước đó hoặc thoát app
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });

        // Tải dữ liệu thống kê mặc định lần đầu tiên (Theo Ngày)
        loadDashboardData("day");
    }

    private void loadDashboardData(String type) {
        fetchAccessChartData(type);
        fetchTop10ComicsData(type);
    }

    /**
     * Hàm cấu hình thanh Header chuyên dụng cho Admin
     * Thực hiện ẩn Tìm kiếm, Thông báo và đổi màu sắc nhận diện quản trị
     */
    private void setupAdminHeaderView() {
        layoutHeaderAdmin = findViewById(R.id.layoutHeaderAdmin);
        headerMenu = layoutHeaderAdmin.findViewById(R.id.headerMenu);
        headerLogo = layoutHeaderAdmin.findViewById(R.id.headerLogo);
        headerAvatar = layoutHeaderAdmin.findViewById(R.id.headerAvatar);
        headerSearch = layoutHeaderAdmin.findViewById(R.id.headerSearch);
        headerNotification = layoutHeaderAdmin.findViewById(R.id.headerNotification);

        // YÊU CẦU: Ẩn triệt để hai nút không thuộc phận sự của Admin
        headerSearch.setVisibility(View.GONE);
        headerNotification.setVisibility(View.GONE);

        // Tùy biến phong cách không gian làm việc Admin
        headerLogo.setText("HỆ THỐNG QUẢN TRỊ");
        headerLogo.setTextColor(Color.parseColor("#E74C3C")); // Màu đỏ cam nổi bật

        // Đăng ký sự kiện nút menu tùy chọn và nạp ảnh đại diện Admin
        headerMenu.setOnClickListener(v -> showAdminPopupMenu(v));
        loadAdminHeaderAvatar();
    }

    /**
     * Luồng 1: Gọi API lấy dữ liệu thật từ MariaDB để vẽ biểu đồ hàng xu hướng (LineChart)
     */
    private void fetchAccessChartData(String type) {
        ApiClient.getApiService().getAdminAccessStats(type).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> dataList = response.body();

                    ArrayList<Entry> entries = new ArrayList<>();
                    ArrayList<String> xLabels = new ArrayList<>();

                    // Chuyển đổi Json trả về thành các mốc tọa độ (X, Y)
                    for (int i = 0; i < dataList.size(); i++) {
                        Map<String, Object> point = dataList.get(i);
                        String label = (String) point.get("label");

                        Number valueNum = (Number) point.get("value");
                        float value = valueNum != null ? valueNum.floatValue() : 0f;

                        entries.add(new Entry(i, value));
                        xLabels.add(label);
                    }

                    if (entries.isEmpty()) {
                        lineChartAccess.clear();
                        lineChartAccess.setNoDataText("Không có dữ liệu truy cập trong khoảng thời gian này!");
                        lineChartAccess.invalidate();
                        return;
                    }

                    // Thiết lập định dạng đường kẻ đồ thị hàng
                    LineDataSet dataSet = new LineDataSet(entries, "Lượt truy cập đọc truyện");
                    dataSet.setColor(Color.parseColor("#E74C3C")); // Đường line màu đỏ tương phản
                    dataSet.setCircleColor(Color.parseColor("#2C3E50"));
                    dataSet.setLineWidth(2.5f);
                    dataSet.setCircleRadius(4f);
                    dataSet.setDrawValues(true); // Hiện chỉ số lượt ngay trên đầu mốc chấm
                    dataSet.setValueTextSize(10f);
                    dataSet.setValueTextColor(Color.parseColor("#333333"));

                    LineData lineData = new LineData(dataSet);
                    lineChartAccess.setData(lineData);

                    // Cấu hình nhãn hiển thị thời gian trục hoành X (Giờ hoặc Ngày/Tháng)
                    XAxis xAxis = lineChartAccess.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setGranularity(1f);
                    xAxis.setLabelRotationAngle(-30); // Xoay chữ nghiêng tránh đè nhãn lên nhau

                    lineChartAccess.getDescription().setEnabled(false);
                    lineChartAccess.animateX(800); // Tạo hiệu ứng vẽ hàng chạy từ trái sang phải
                    lineChartAccess.invalidate();   // Ép làm mới giao diện biểu đồ
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Lỗi kết nối máy chủ biểu đồ!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Luồng 2: Gọi API lấy danh sách Top 10 bộ truyện tương ứng theo chu kỳ đã chọn
     */
    private void fetchTop10ComicsData(String type) {
        ApiClient.getApiService().getTopRanking(type).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    rankingAdapter.setComics(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Lỗi kết nối tải bảng xếp hạng truyện!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Tải và đồng bộ ảnh đại diện của tài khoản Admin lên Header
     */
    private void loadAdminHeaderAvatar() {
        int userId = SharedPrefsManager.getUserId(this);
        if (userId == -1) return;

        ApiClient.getApiService().getUserProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String avatarUrl = response.body().getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(getApplicationContext())
                                .load(avatarUrl)
                                .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                                .circleCrop()
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .into(headerAvatar);
                    }
                }
            }
            @Override public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    /**
     * Hiển thị danh mục quản lý chuyên sâu khi Admin bấm nút Menu góc trái
     */
    private void showAdminPopupMenu(View anchorView) {
        androidx.appcompat.widget.PopupMenu popupMenu = new androidx.appcompat.widget.PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_admin_header_options, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_admin_dashboard) {
                return true; // Đang ở chính màn hình này
            } else if (id == R.id.menu_admin_manage_comics) {
                // KÍCH HOẠT DÒNG NÀY ĐỂ MỞ TRANG QUẢN LÝ TRUYỆN TRANH CHUYÊN SÂU
                startActivity(new Intent(AdminDashboardActivity.this, AdminManageComicsActivity.class));
                return true;
            } else if (id == R.id.menu_admin_manage_users) {
                // ĐÃ SỬA: Thay thế Toast cũ bằng lệnh Intent mở trang Quản lý người dùng chuyên sâu
                startActivity(new Intent(AdminDashboardActivity.this, AdminManageUsersActivity.class));
                return true;
            } else if (id == R.id.menu_admin_manage_notifications) {
                Toast.makeText(this, "Chức năng quản lý thông báo đang phát triển", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_admin_logout) {
                // Đăng xuất xóa bộ nhớ tạm và đẩy về Login
                SharedPrefsManager.logout(this);
                android.content.Intent intent = new android.content.Intent(this, com.yuhbui.comicapp.ui.LoginActivity.class);
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
}