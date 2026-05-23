package com.yuhbui.comicapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.HomeViewModel;
import com.yuhbui.comicapp.ui.adapters.ComicAdapter;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private HomeViewModel homeViewModel;

    // Khai báo các RecyclerView mới tương ứng với giao diện XML nâng cấp
    private RecyclerView recyclerViewComics; // Mục Truyện mới (ID cũ của bạn)
    private RecyclerView rvRecommendedComic;  // Mục Đề cử trượt ngang
    private RecyclerView rvTopRank;          // Mục Bảng xếp hạng

    // Khai báo các nút điều khiển phân trang số
    private Button btnPrevPage, btnNextPage;
    private TextView tvPageIndicator;
    private RadioGroup rgRankFilter;

    private ComicAdapter comicAdapter;           // Tải lại adapter lưới cũ cho truyện mới
    private ComicAdapter recommendedAdapter;     // Adapter cho truyện đề cử
    private ComicAdapter rankingAdapter;         // Adapter cho bảng xếp hạng

    private int currentPage = 0; // Biến lưu trữ số trang hiện tại (Trang 1 tương ứng với số 0)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. ÁNH XẠ TOÀN BỘ CÁC VIEW MỚI TỪ XML
        recyclerViewComics = findViewById(R.id.recyclerViewComics);
        rvRecommendedComic = findViewById(R.id.rvRecommendedComic);
        rvTopRank = findViewById(R.id.rvTopRank);

        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);
        rgRankFilter = findViewById(R.id.rgRankFilter);

        // 2. CẤU HÌNH CÁC RECYCLERVIEW VỚI LAYOUT MANAGER PHÙ HỢP
        // Giữ nguyên hiển thị dạng Lưới (Grid) 2 cột xịn sò của bạn cho danh sách truyện mới
        recyclerViewComics.setLayoutManager(new GridLayoutManager(this, 2));
        comicAdapter = new ComicAdapter();
        recyclerViewComics.setAdapter(comicAdapter);

        // Cấu hình chiều NGANG (Horizontal) cho danh sách Đề cử trượt lướt
        rvRecommendedComic.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recommendedAdapter = new ComicAdapter();
        rvRecommendedComic.setAdapter(recommendedAdapter);

        // Cấu hình chiều DỌC bình thường cho Bảng xếp hạng top 10
        rvTopRank.setLayoutManager(new LinearLayoutManager(this));
        rankingAdapter = new ComicAdapter();
        rvTopRank.setAdapter(rankingAdapter);

        // 3. KẾT NỐI VỚI VIEWMODEL ĐỂ LẤY DANH SÁCH TRUYỆN ĐỀ CỬ (Dùng lại hàm load cũ làm đề cử)
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.getComics().observe(this, new Observer<List<Comic>>() {
            @Override
            public void onChanged(List<Comic> comics) {
                if (comics != null) {
                    // Đổ dữ liệu tổng hợp cũ vào phần Đề cử trượt ngang phía trên cùng
                    recommendedAdapter.setComics(comics);
                    Log.d("YUH_TEST", "Đã vẽ xong danh sách truyện đề cử!");
                }
            }
        });
        homeViewModel.loadComics(); // Kích hoạt lệnh tải truyện đề cử

        // 4. KHỞI CHẠY TẢI DỮ LIỆU TRUYỆN MỚI CẬP NHẬT THEO PHÂN TRANG (Trang 1)
        loadNewUpdatesPage(currentPage);

        // Mặc định tải BXH theo "Ngày" khi vừa vào app
        loadRankingData("day");

        // 5. CÀI ĐẶT SỰ KIỆN CLICK CHO THANH ĐIỀU HƯỚNG PHÂN TRANG < 1 2 3 >
        // Bấm nút lùi trang (<)
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                loadNewUpdatesPage(currentPage);
            }
        });

        // Bấm nút tiến trang (>)
        btnNextPage.setOnClickListener(v -> {
            currentPage++;
            loadNewUpdatesPage(currentPage);
        });

        // 6. CÀI ĐẶT LẮNG NGHE ĐỔI TAB BẢNG XẾP HẠNG (Ngày / Tuần / Tháng)
        rgRankFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDay) {
                loadRankingData("day");
            } else if (checkedId == R.id.rbWeek) {
                loadRankingData("week");
            } else if (checkedId == R.id.rbMonth) {
                loadRankingData("month");
            }
        });
    }

    // Hàm gọi mạng xử lý phân trang 10 truyện độc lập
    private void loadNewUpdatesPage(int page) {
        ApiClient.getApiService().getHomeUpdates(page).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comic> updateList = response.body();

                    if (updateList.isEmpty() && page > 0) {
                        // Nếu bấm trang tiếp theo mà không còn truyện nào thì lùi lại và báo cho user biết
                        Toast.makeText(MainActivity.this, "Đã hết danh sách truyện!", Toast.LENGTH_SHORT).show();
                        currentPage--; // Trả số trang về vị trí cũ
                        return;
                    }

                    // Cập nhật dữ liệu lưới truyện mới cập nhật
                    comicAdapter.setComics(updateList);

                    // Vẽ lại số hiệu trang lên giao diện (page + 1 để thân thiện với người dùng: Trang 1, Trang 2)
                    tvPageIndicator.setText("Trang " + (page + 1));

                    // Khóa không cho ấn nút lùi trang nếu đang ở trang đầu tiên
                    btnPrevPage.setEnabled(page > 0);
                    btnPrevPage.setAlpha(page > 0 ? 1.0f : 0.4f);
                }
            }

            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải phân trang truyện mới: " + t.getMessage());
            }
        });
    }

    // Hàm gọi mạng xử lý đổi số liệu Bảng xếp hạng Top 10
    private void loadRankingData(String type) {
        ApiClient.getApiService().getTopRanking(type).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Đổ 10 truyện xếp hạng cao nhất vào danh sách BXH giữa trang chủ
                    rankingAdapter.setComics(response.body());
                    Log.d("YUH_TEST", "Cập nhật thành công BXH theo: " + type);
                }
            }

            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải bảng xếp hạng: " + t.getMessage());
            }
        });
    }
}