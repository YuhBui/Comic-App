package com.yuhbui.comicapp.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Category;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.adapters.CategoryFilterAdapter;
import com.yuhbui.comicapp.ui.adapters.ComicAdapter;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvRecommended, rvNewUpdates, rvRank, rvCategoriesFilter;
    private ComicAdapter recommendedAdapter, newUpdatesAdapter, rankAdapter;
    private CategoryFilterAdapter catFilterAdapter;

    private Button btnPrevPage, btnNextPage;
    private TextView tvPageIndicator;
    private RadioGroup rgRankFilter;

    private int currentPage = 0; // Quản lý số trang hiện tại của "Truyện mới cập nhật"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. ÁNH XẠ TOÀN BỘ CÁC VIEW TRÊN LƯỚI GIAO DIỆN HOME
        rvRecommended = findViewById(R.id.rvRecommendedComic);
        rvCategoriesFilter = findViewById(R.id.rvCategoriesFilter);
        rvNewUpdates = findViewById(R.id.recyclerViewComics);
        rvRank = findViewById(R.id.rvTopRank);

        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);
        rgRankFilter = findViewById(R.id.rgRankFilter);

        // 2. CẤU HÌNH HƯỚNG CUỘN CHO RECYCLERVIEW
        // Truyện đề cử và Bộ lọc danh mục -> Trượt theo chiều NGANG (Horizontal)
        rvRecommended.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategoriesFilter.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Bảng xếp hạng và Truyện mới cập nhật -> Cuộn theo chiều DỌC (Vertical)
        rvNewUpdates.setLayoutManager(new LinearLayoutManager(this));
        rvRank.setLayoutManager(new LinearLayoutManager(this));

        // 3. KHỞI TẠO VÀ GẮN ADAPTER CHO TỪNG PHÂN VÙNG
        recommendedAdapter = new ComicAdapter();
        newUpdatesAdapter = new ComicAdapter();
        rankAdapter = new ComicAdapter();

        rvRecommended.setAdapter(recommendedAdapter);
        rvNewUpdates.setAdapter(newUpdatesAdapter);
        rvRank.setAdapter(rankAdapter);

        // Khởi tạo adapter Bộ lọc thể loại kèm thiết lập sự kiện lắng nghe Click
        catFilterAdapter = new CategoryFilterAdapter(new CategoryFilterAdapter.OnCatClickListener() {
            @Override
            public void onCatClick(Category category) {
                if (category == null) {
                    // Nếu người dùng bỏ chọn danh mục (Unclick) -> Quay về hiển thị list phân trang mặc định
                    currentPage = 0;
                    loadNewUpdatesComics(currentPage);
                } else {
                    // Nếu kích hoạt chọn danh mục -> Lọc truyện thật kết nối từ DB qua CategoryID
                    loadComicsByCategory(category.getCategoryId());
                }
            }
        });
        rvCategoriesFilter.setAdapter(catFilterAdapter);

        // 4. TIẾN HÀNH GỌI CÁC KÊNH API KẾT NỐI DATABASE THỰC TẾ
        loadCategoriesFilterData();        // Tải danh sách nhãn thể loại cho bộ lọc
        loadRecommendedComics();           // Tải danh sách slider truyện hot đề cử
        loadNewUpdatesComics(currentPage); // Tải danh sách truyện mới cập nhật (Trang 1 - index 0)
        loadRankingData("day");            // Tải dữ liệu Bảng xếp hạng (Mặc định Tab Ngày)

        // 5. CÀI ĐẶT SỰ KIỆN ĐIỀU HƯỚNG PHÂN TRANG CHUYỂN BÀI (< VÀ >)
        btnNextPage.setOnClickListener(v -> {
            currentPage++;
            loadNewUpdatesComics(currentPage);
        });

        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                loadNewUpdatesComics(currentPage);
            }
        });

        // 6. CÀI ĐẶT SỰ KIỆN CHUYỂN TAB BẢNG XẾP HẠNG (NGÀY / TUẦN / THÁNG)
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

    // --- CỤM HÀM THỰC THI GỌI MẠNG RETROFIT KẾT NỐI DATABASE ---

    private void loadCategoriesFilterData() {
        ApiClient.getApiService().getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    catFilterAdapter.setCategories(response.body());
                }
            }
            @Override public void onFailure(Call<List<Category>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải danh mục: " + t.getMessage());
            }
        });
    }

    private void loadRecommendedComics() {
        ApiClient.getApiService().getRecommendedComics().enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recommendedAdapter.setComics(response.body());
                }
            }
            @Override public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải truyện đề cử: " + t.getMessage());
            }
        });
    }

    private void loadNewUpdatesComics(int page) {
        ApiClient.getApiService().getHomeUpdates(page).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comic> comics = response.body();
                    newUpdatesAdapter.setComics(comics);

                    // Cập nhật số hiệu trang lên TextView giao diện
                    tvPageIndicator.setText("Trang " + (page + 1));

                    // Khóa/Mở các nút chuyển trang tùy theo điều kiện biên dữ liệu
                    btnPrevPage.setEnabled(page > 0);
                    btnNextPage.setEnabled(comics.size() == 10); // Nếu trang hiện tại có đủ 10 truyện thì mới cho sang tiếp trang sau
                }
            }
            @Override public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải truyện mới phân trang: " + t.getMessage());
            }
        });
    }

    private void loadComicsByCategory(int catId) {
        ApiClient.getApiService().getComicsByCategory(catId).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    newUpdatesAdapter.setComics(response.body());

                    // Khi đang ở chế độ xem kết quả lọc danh mục, ta tạm ẩn phân trang số đi để tránh xung đột
                    tvPageIndicator.setText("Kết quả lọc");
                    btnPrevPage.setEnabled(false);
                    btnNextPage.setEnabled(false);
                }
            }
            @Override public void onFailure(Call<List<Comic>> call, Throwable t) {}
        });
    }

    private void loadRankingData(String type) {
        ApiClient.getApiService().getTopRanking(type).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Đổ dữ liệu BXH Top 10 lấy thật từ DB ra ngoài màn hình
                    rankAdapter.setComics(response.body());
                }
            }
            @Override public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi BXH: " + t.getMessage());
            }
        });
    }
}