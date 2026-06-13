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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.adapters.RankingAdapter;
import com.yuhbui.comicapp.utils.HeaderUtils;
import com.yuhbui.comicapp.utils.MenuUtils;
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    private RadioGroup rgDashboardFilter;
    private LineChart lineChartAccess;
    private RecyclerView rvAdminTopComic;
    private RankingAdapter rankingAdapter;

    // Các thành phần điều hướng thời gian mới thêm
    private ImageView btnPrevPeriod, btnNextPeriod;
    private TextView tvCurrentPeriod;

    // Các biến lưu trạng thái bộ lọc và thời gian neo hiện tại
    private String currentType = "day";
    private Calendar currentCalendar = Calendar.getInstance();

    private View layoutHeaderAdmin;
    private ImageView headerMenu, headerAvatar;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        drawerLayout = findViewById(R.id.drawerLayout);
        rgDashboardFilter = findViewById(R.id.rgDashboardFilter);
        lineChartAccess = findViewById(R.id.lineChartAccess);
        rvAdminTopComic = findViewById(R.id.rvAdminTopComic);

        // Ánh xạ các nút tiến lùi mới
        btnPrevPeriod = findViewById(R.id.btnPrevPeriod);
        btnNextPeriod = findViewById(R.id.btnNextPeriod);
        tvCurrentPeriod = findViewById(R.id.tvCurrentPeriod);

        setupAdminHeaderView();

        rvAdminTopComic.setLayoutManager(new LinearLayoutManager(this));
        rankingAdapter = new RankingAdapter(true);
        rvAdminTopComic.setAdapter(rankingAdapter);

        // 1. Lắng nghe sự kiện đổi bộ lọc RadioGroup
        rgDashboardFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDashWeek) {
                currentType = "week";
            } else if (checkedId == R.id.rbDashMonth) {
                currentType = "month";
            } else {
                currentType = "day";
            }
            // Đặt lại thời gian về mốc hôm nay khi chuyển bộ lọc
            currentCalendar = Calendar.getInstance();
            updatePeriodDisplayAndLoad();
        });

        // 2. Click mũi tên trái (<) để lùi thời gian
        btnPrevPeriod.setOnClickListener(v -> {
            if ("day".equals(currentType)) {
                currentCalendar.add(Calendar.DAY_OF_MONTH, -1);
            } else if ("week".equals(currentType)) {
                currentCalendar.add(Calendar.WEEK_OF_YEAR, -1);
            } else {
                currentCalendar.add(Calendar.MONTH, -1);
            }
            updatePeriodDisplayAndLoad();
        });

        // 3. Click mũi tên phải (>) để tiến thời gian
        btnNextPeriod.setOnClickListener(v -> {
            if ("day".equals(currentType)) {
                currentCalendar.add(Calendar.DAY_OF_MONTH, 1);
            } else if ("week".equals(currentType)) {
                currentCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            } else {
                currentCalendar.add(Calendar.MONTH, 1);
            }
            updatePeriodDisplayAndLoad();
        });

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

        // Tải dữ liệu ban đầu lần đầu vào màn hình
        updatePeriodDisplayAndLoad();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (layoutHeaderAdmin != null && layoutHeaderAdmin.findViewById(R.id.headerAvatar) != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeaderAdmin.findViewById(R.id.headerAvatar));
        }
    }

    /**
     * Hàm tự động cập nhật định dạng chữ hiển thị trên thanh điều hướng và gọi API dữ liệu
     */
    private void updatePeriodDisplayAndLoad() {
        SimpleDateFormat displayFormat;

        if ("day".equals(currentType)) {
            displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvCurrentPeriod.setText(displayFormat.format(currentCalendar.getTime()));
        } else if ("week".equals(currentType)) {
            Calendar cloneCal = (Calendar) currentCalendar.clone();
            int dayOfWeek = cloneCal.get(Calendar.DAY_OF_WEEK);

            // Đưa lịch về Thứ 2 của tuần hiện tại
            int daysToMonday = (dayOfWeek == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - dayOfWeek);
            cloneCal.add(Calendar.DAY_OF_MONTH, daysToMonday);

            SimpleDateFormat weekFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String startStr = weekFormat.format(cloneCal.getTime());

            cloneCal.add(Calendar.DAY_OF_MONTH, 6);
            String endStr = weekFormat.format(cloneCal.getTime());

            tvCurrentPeriod.setText(startStr + " - " + endStr);
        } else {
            displayFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
            tvCurrentPeriod.setText(displayFormat.format(currentCalendar.getTime()));
        }

        // Tạo chuỗi ngày dạng yyyy-MM-dd gửi lên API Server
        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String targetDateStr = apiFormat.format(currentCalendar.getTime());

        loadDashboardData(currentType, targetDateStr);
    }

    private void loadDashboardData(String type, String targetDate) {
        fetchAccessChartData(type, targetDate);
        fetchTop10ComicsData(type);
    }

    private void setupAdminHeaderView() {
        layoutHeaderAdmin = findViewById(R.id.layoutHeaderAdmin);
        headerMenu = layoutHeaderAdmin.findViewById(R.id.headerMenu);
        headerLogo = layoutHeaderAdmin.findViewById(R.id.headerLogo);
        headerAvatar = layoutHeaderAdmin.findViewById(R.id.headerAvatar);

        HeaderUtils.initHeader(this, layoutHeaderAdmin, drawerLayout);
        MenuUtils.setupAdminSideMenu(this, drawerLayout, headerMenu);

        if (layoutHeaderAdmin.findViewById(R.id.headerSearch) != null) {
            layoutHeaderAdmin.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        }
        if (layoutHeaderAdmin.findViewById(R.id.headerNotification) != null) {
            layoutHeaderAdmin.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        }

        if (headerLogo != null) {
            headerLogo.setText("COMIC APP");
            headerLogo.setTextColor(Color.parseColor("#E74C3C"));
        }

        if (headerAvatar != null) {
            headerAvatar.setOnClickListener(v -> showAvatarPopupMenu(v));
        }
    }

    private void fetchAccessChartData(String type, String targetDate) {
        ApiClient.getApiService().getAdminAccessStats(type, targetDate).enqueue(new Callback<List<Map<String, Object>>>() {
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
                        lineChartAccess.setNoDataText("Không có dữ liệu truy cập!");
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
                Toast.makeText(AdminDashboardActivity.this, "Lỗi kết nối biểu đồ!", Toast.LENGTH_SHORT).show();
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
}