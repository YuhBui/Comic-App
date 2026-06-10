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
import androidx.core.view.GravityCompat;              // THÊM: Điều hướng DrawerLayout trượt trái
import androidx.drawerlayout.widget.DrawerLayout;    // THÊM: Thành phần DrawerLayout root
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
import com.yuhbui.comicapp.utils.HeaderUtils;          // THÊM: Đồng bộ thanh Header tập trung
import com.yuhbui.comicapp.utils.MenuUtils;            // THÊM: Điều hướng Menu trượt Admin dùng chung
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout; // THÊM: Khai báo thành phần quản lý Menu trượt đè

    private RadioGroup rgDashboardFilter;
    private LineChart lineChartAccess;
    private RecyclerView rvAdminTopComic;
    private RankingAdapter rankingAdapter;

    // Các thành phần thuộc thanh Header dùng chung
    private View layoutHeaderAdmin;
    private ImageView headerMenu, headerAvatar;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // 1. Ánh xạ DrawerLayout đè và các thành phần giao diện chính
        drawerLayout = findViewById(R.id.drawerLayout);
        rgDashboardFilter = findViewById(R.id.rgDashboardFilter);
        lineChartAccess = findViewById(R.id.lineChartAccess);
        rvAdminTopComic = findViewById(R.id.rvAdminTopComic);

        // 2. Thiết lập cấu hình tùy biến thanh Header Admin & Menu trượt tập trung
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
            loadDashboardData(type);
        });

        // 5. CẤU HÌNH: Khóa nút quay lại (Back cứng) - Ưu tiên đóng Menu trượt nếu đang mở
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        // Tải dữ liệu thống kê mặc định lần đầu tiên (Theo Ngày)
        loadDashboardData("day");
    }

    // Làm mới avatar Admin mỗi khi quay lại từ trang Profile
    @Override
    protected void onResume() {
        super.onResume();
        if (layoutHeaderAdmin != null && layoutHeaderAdmin.findViewById(R.id.headerAvatar) != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeaderAdmin.findViewById(R.id.headerAvatar));
        }
    }

    private void loadDashboardData(String type) {
        fetchAccessChartData(type);
        fetchTop10ComicsData(type);
    }

    /**
     * Hàm cấu hình thanh Header chuyên dụng cho Admin tích hợp Menu trượt mới
     */
    private void setupAdminHeaderView() {
        layoutHeaderAdmin = findViewById(R.id.layoutHeaderAdmin);
        headerMenu = layoutHeaderAdmin.findViewById(R.id.headerMenu);
        headerLogo = layoutHeaderAdmin.findViewById(R.id.headerLogo);
        headerAvatar = layoutHeaderAdmin.findViewById(R.id.headerAvatar);

        // 1. Khởi tạo cấu hình Header và Menu trượt Admin
        HeaderUtils.initHeader(this, layoutHeaderAdmin, drawerLayout);
        MenuUtils.setupAdminSideMenu(this, drawerLayout, headerMenu);

        // 2. THÊM ĐOẠN NÀY: Ẩn triệt để hai nút Tìm kiếm và Thông báo đối với Admin
        if (layoutHeaderAdmin.findViewById(R.id.headerSearch) != null) {
            layoutHeaderAdmin.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        }
        if (layoutHeaderAdmin.findViewById(R.id.headerNotification) != null) {
            layoutHeaderAdmin.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        }

        // Tùy biến phong cách chữ tiêu đề Admin như cũ
        if (headerLogo != null) {
            headerLogo.setText("COMIC APP");
            headerLogo.setTextColor(Color.parseColor("#E74C3C"));
        }

        if (headerAvatar != null) {
            headerAvatar.setOnClickListener(v -> showAvatarPopupMenu(v));
        }
    }

    private void fetchAccessChartData(String type) {
        ApiClient.getApiService().getAdminAccessStats(type).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> dataList = response.body();

                    ArrayList<Entry> entries = new ArrayList<>();
                    ArrayList<String> xLabels = new ArrayList<>();

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

                    LineDataSet dataSet = new LineDataSet(entries, "Lượt truy cập đọc truyện");
                    dataSet.setColor(Color.parseColor("#E74C3C"));
                    dataSet.setCircleColor(Color.parseColor("#2C3E50"));
                    dataSet.setLineWidth(2.5f);
                    dataSet.setCircleRadius(4f);
                    dataSet.setDrawValues(true);
                    dataSet.setValueTextSize(10f);
                    dataSet.setValueTextColor(Color.parseColor("#333333"));

                    LineData lineData = new LineData(dataSet);
                    lineChartAccess.setData(lineData);

                    XAxis xAxis = lineChartAccess.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setGranularity(1f);
                    xAxis.setLabelRotationAngle(-30);

                    lineChartAccess.getDescription().setEnabled(false);
                    lineChartAccess.animateX(800);
                    lineChartAccess.invalidate();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Lỗi kết nối máy chủ biểu đồ!", Toast.LENGTH_SHORT).show();
            }
        });
    }

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

    private void showAvatarPopupMenu(View anchorView) {
        androidx.appcompat.widget.PopupMenu popupMenu = new androidx.appcompat.widget.PopupMenu(this, anchorView);
        popupMenu.getMenu().add(0, 1, 1, "👤 Hồ sơ cá nhân");
        popupMenu.getMenu().add(0, 2, 2, "🚪 Đăng xuất hệ thống");

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                Intent intent = new Intent(AdminDashboardActivity.this, com.yuhbui.comicapp.ui.ProfileActivity.class);
                startActivity(intent);
                return true;
            } else if (id == 2) {
                SharedPrefsManager.logout(this);
                Intent intent = new Intent(this, com.yuhbui.comicapp.ui.LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    // Đã xóa bỏ hàm showAdminPopupMenu() cũ do logic bấm mục menu trượt đã được quản lý tập trung bên trong lớp MenuUtils.
}