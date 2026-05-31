package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Category;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.adapters.CategoryFilterAdapter;
import com.yuhbui.comicapp.ui.adapters.ComicAdapter;
import com.yuhbui.comicapp.ui.adapters.RankingAdapter;
import com.yuhbui.comicapp.ui.adapters.RecommendedBannerAdapter;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    // ========== PHẦN 1: TRUYỆN ĐỀ CỬ - Banner Slider ==========
    private ViewPager2 vpRecommended;
    private RecommendedBannerAdapter bannerAdapter;
    private ImageButton btnSliderPrev, btnSliderNext;
    private LinearLayout layoutDotIndicator;

    // ========== PHẦN 2: TRUYỆN MỚI CẬP NHẬT - Grid 2 cột ==========
    private RecyclerView recyclerViewComics;
    private ComicAdapter newUpdatesAdapter;
    private ImageView btnFilterIcon;
    private TextView tvActiveFilter;

    // Phân trang
    private Button btnPrevPage, btnNextPage;
    private LinearLayout layoutPageNumbers;
    private int currentPage = 0;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 10;

    // Bộ lọc thể loại
    private CategoryFilterAdapter catFilterAdapter;
    private Integer activeFilterCategoryId = null;
    private String activeFilterCategoryName = null;

    // ========== PHẦN 3: BẢNG XẾP HẠNG TOP 10 ==========
    private RecyclerView rvTopRank;
    private RankingAdapter rankingAdapter;
    private RadioGroup rgRankFilter;

    // ========== HEADER VÀ TÌM KIẾM GỢI Ý ==========
    private View layoutHeader;
    private ImageView headerMenu, headerSearch, headerNotification, headerAvatar;
    private EditText edtHeaderSearch;
    private TextView headerLogo;
    private android.widget.ListPopupWindow suggestionPopup;
    private android.widget.ArrayAdapter<String> suggestionAdapter;
    private java.util.List<String> suggestionList = new java.util.ArrayList<>();

    // ========== BIẾN PHỤC VỤ TÌM KIẾM CHUYÊN BIỆT HÀNG DỌC ==========
    private androidx.core.widget.NestedScrollView scrollMainContainer;
    private LinearLayout layoutSearchContainer;
    private RecyclerView recyclerViewSearchResults;
    private ComicAdapter searchResultAdapter; // Adapter riêng biệt chạy item_comic_full hàng dọc

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupHeader();
        setupRecommendedSlider();
        setupNewUpdatesSection();
        setupRankingSection();
        setupSearchSuggestions();
        loadAllData();

        // Kiểm tra xem có nhận lệnh mở ô tìm kiếm từ màn hình khác (như ComicDetailActivity) hay không
        handleSearchIntent(getIntent());

        // FIX SỬA LỖI: Đưa khối đăng ký nút Back cứng vào bên trong hàm onCreate() theo đúng tiêu chuẩn Java Android
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Kiểm tra xem vùng chứa kết quả tìm kiếm hàng dọc có đang hiển thị không
                if (layoutSearchContainer != null && layoutSearchContainer.getVisibility() == View.VISIBLE) {
                    // Nếu đang hiện kết quả tìm kiếm, bấm nút Back sẽ đóng giao diện tìm kiếm để quay lại trang chủ
                    closeSearch();
                } else {
                    // Nếu đang ở trang chủ mặc định, cho phép thoát ứng dụng bình thường
                    setEnabled(false); // Tạm thời vô hiệu hóa Callback này
                    getOnBackPressedDispatcher().onBackPressed(); // Thực hiện hành vi Back hệ thống
                    setEnabled(true);  // Kích hoạt lại Callback cho lần sau
                }
            }
        });
    }

    /**
     * Nhận Intent mới khi MainActivity gọi lại ở chế độ SINGLE_TOP
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSearchIntent(intent);
    }

    /**
     * Xử lý tự động kích hoạt thanh tìm kiếm từ dữ liệu Intent
     */
    private void handleSearchIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("OPEN_SEARCH", false)) {
            if (edtHeaderSearch.getVisibility() == View.GONE) {
                headerSearch.performClick(); // Kích hoạt sự kiện click để tự mở ô nhập liệu
            }
        }
    }

    private void loadHeaderAvatar() {
        int userId = SharedPrefsManager.getUserId(this);
        if (userId == -1) {
            headerAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
            return;
        }

        ApiClient.getApiService().getUserProfile(userId).enqueue(new retrofit2.Callback<com.yuhbui.comicapp.data.model.User>() {
            @Override
            public void onResponse(retrofit2.Call<com.yuhbui.comicapp.data.model.User> call, retrofit2.Response<com.yuhbui.comicapp.data.model.User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String avatarUrl = response.body().getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(getApplicationContext())
                                .load(avatarUrl)
                                .signature(new com.bumptech.glide.signature.ObjectKey(String.valueOf(System.currentTimeMillis())))
                                .circleCrop()
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .into(headerAvatar);
                    } else {
                        headerAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.yuhbui.comicapp.data.model.User> call, Throwable t) {
                headerAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHeaderAvatar();
    }

    // ========== KHỞI TẠO VIEW VÀ PHÂN TÁCH CONTAINER ==========

    private void initViews() {
        // Header
        layoutHeader       = findViewById(R.id.layoutHeader);
        headerMenu         = layoutHeader.findViewById(R.id.headerMenu);
        headerLogo         = layoutHeader.findViewById(R.id.headerLogo);
        headerSearch       = layoutHeader.findViewById(R.id.headerSearch);
        edtHeaderSearch    = layoutHeader.findViewById(R.id.edtHeaderSearch);
        headerNotification = layoutHeader.findViewById(R.id.headerNotification);
        headerAvatar       = layoutHeader.findViewById(R.id.headerAvatar);

        // Phần 1: Slider
        vpRecommended    = findViewById(R.id.vpRecommended);
        btnSliderPrev    = findViewById(R.id.btnSliderPrev);
        btnSliderNext    = findViewById(R.id.btnSliderNext);
        layoutDotIndicator = findViewById(R.id.layoutDotIndicator);

        // Phần 2: Truyện mới
        recyclerViewComics = findViewById(R.id.recyclerViewComics);
        btnFilterIcon      = findViewById(R.id.btnFilterIcon);
        tvActiveFilter     = findViewById(R.id.tvActiveFilter);
        btnPrevPage        = findViewById(R.id.btnPrevPage);
        btnNextPage        = findViewById(R.id.btnNextPage);
        layoutPageNumbers  = findViewById(R.id.layoutPageNumbers);

        // Phần 3: BXH
        rvTopRank    = findViewById(R.id.rvTopRank);
        rgRankFilter = findViewById(R.id.rgRankFilter);

        // Bổ sung ánh xạ đầy đủ cho phần Tìm kiếm chuyên biệt Container hàng dọc
        scrollMainContainer       = findViewById(R.id.scrollMainContainer);
        layoutSearchContainer     = findViewById(R.id.layoutSearchContainer);
        recyclerViewSearchResults = findViewById(R.id.recyclerViewSearchResults);

        // Khởi tạo cấu trúc hiển thị danh sách dọc cho kết quả tìm kiếm (isListView = true)
        if (recyclerViewSearchResults != null) {
            recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(this));
            searchResultAdapter = new ComicAdapter(true); // true cấu hình dùng item_comic_full dọc
            recyclerViewSearchResults.setAdapter(searchResultAdapter);
        }
    }

    // ========== HEADER VÀ SỰ KIỆN TÌM KIẾM ==========

    private void setupHeader() {
        headerMenu.setOnClickListener(v -> showHeaderPopupMenu(v));
        headerLogo.setOnClickListener(v -> Toast.makeText(this, "Trang chủ", Toast.LENGTH_SHORT).show());
        headerNotification.setOnClickListener(v -> Toast.makeText(this, "Thông báo", Toast.LENGTH_SHORT).show());
        headerAvatar.setOnClickListener(v -> showAvatarMenu(v));

        // LOGIC ĐIỀU KHIỂN NÚT KÍNH LÚP / NÚT GỬI TÌM KIẾM
        headerSearch.setOnClickListener(v -> {
            if (edtHeaderSearch.getVisibility() == View.GONE) {
                // Mở thanh tìm kiếm ra nếu đang ẩn
                headerLogo.setVisibility(View.GONE);
                edtHeaderSearch.setVisibility(View.VISIBLE);
                edtHeaderSearch.requestFocus();

                // Hiện bàn phím điện thoại
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(edtHeaderSearch, InputMethodManager.SHOW_IMPLICIT);

                // Đổi icon kính lúp sang nút đóng (X)
                headerSearch.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                // Nếu thanh tìm kiếm đã mở, lấy text xem người dùng nhập gì chưa
                String keyword = edtHeaderSearch.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    if (suggestionPopup.isShowing()) suggestionPopup.dismiss();
                    performSearch(keyword);
                } else {
                    closeSearch();
                }
            }
        });

        // Hỗ trợ thêm việc bấm nút "Tìm kiếm/Kính lúp" ngay trên bàn phím ảo điện thoại
        edtHeaderSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String keyword = edtHeaderSearch.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    if (suggestionPopup.isShowing()) suggestionPopup.dismiss();
                    performSearch(keyword);
                }
                return true;
            }
            return false;
        });
    }

    // ========== PHƯƠNG THỨC XỬ LÝ TÌM KIẾM VÀ KHÔI PHỤC ==========

    private void performSearch(String keyword) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(edtHeaderSearch.getWindowToken(), 0);

        // Ẩn toàn bộ vùng chứa trang chủ, hiển thị khối giao diện hàng dọc độc lập
        if (scrollMainContainer != null) scrollMainContainer.setVisibility(View.GONE);
        if (layoutSearchContainer != null) layoutSearchContainer.setVisibility(View.VISIBLE);

        // Gọi API lấy kết quả tìm kiếm đổ vào searchResultAdapter hàng dọc
        ApiClient.getApiService().searchComics(keyword).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (searchResultAdapter != null) {
                        searchResultAdapter.setComics(response.body());
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi tải kết quả tìm kiếm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void closeSearch() {
        if (suggestionPopup.isShowing()) suggestionPopup.dismiss();
        edtHeaderSearch.setText("");
        edtHeaderSearch.setVisibility(View.GONE);
        headerLogo.setVisibility(View.VISIBLE);
        headerSearch.setImageResource(android.R.drawable.ic_menu_search);

        // Hiện lại trang chủ và ẩn vùng tìm kiếm hàng dọc đi
        scrollMainContainer.setVisibility(View.VISIBLE);
        layoutSearchContainer.setVisibility(View.GONE);

        // Xóa danh sách kết quả tìm kiếm cũ để giải phóng bộ nhớ
        if (searchResultAdapter != null) {
            searchResultAdapter.setComics(new java.util.ArrayList<>());
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(edtHeaderSearch.getWindowToken(), 0);
    }

    private void setupSearchSuggestions() {
        suggestionPopup = new android.widget.ListPopupWindow(this);
        suggestionPopup.setAnchorView(edtHeaderSearch);

        suggestionAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, suggestionList);
        suggestionPopup.setAdapter(suggestionAdapter);

        suggestionPopup.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTitle = suggestionAdapter.getItem(position);
            edtHeaderSearch.setText(selectedTitle);
            edtHeaderSearch.setSelection(selectedTitle.length());
            suggestionPopup.dismiss();
            performSearch(selectedTitle);
        });

        edtHeaderSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String query = s.toString().trim();
                if (query.length() >= 1) {
                    ApiClient.getApiService().searchComics(query).enqueue(new Callback<List<Comic>>() {
                        @Override
                        public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                suggestionList.clear();
                                for (Comic comic : response.body()) {
                                    suggestionList.add(comic.getTitle());
                                }
                                if (!suggestionList.isEmpty() && edtHeaderSearch.getVisibility() == View.VISIBLE) {
                                    suggestionAdapter.notifyDataSetChanged();

                                    // Ép ListPopupWindow lấy độ rộng cụ thể bằng thanh nhập liệu để hiển thị nổi bật
                                    suggestionPopup.setWidth(edtHeaderSearch.getWidth() > 0 ? edtHeaderSearch.getWidth() : 800);
                                    suggestionPopup.setInputMethodMode(android.widget.ListPopupWindow.INPUT_METHOD_NEEDED);
                                    suggestionPopup.show();
                                } else {
                                    suggestionPopup.dismiss();
                                }
                            }
                        }
                        @Override public void onFailure(Call<List<Comic>> call, Throwable t) {}
                    });
                } else {
                    suggestionPopup.dismiss();
                }
            }
        });
    }

    // ========== PHẦN 1: SLIDER TRUYỆN ĐỀ CỬ ==========

    private void setupRecommendedSlider() {
        bannerAdapter = new RecommendedBannerAdapter();
        vpRecommended.setAdapter(bannerAdapter);

        btnSliderPrev.setOnClickListener(v -> {
            int cur = vpRecommended.getCurrentItem();
            if (cur > 0) vpRecommended.setCurrentItem(cur - 1, true);
        });
        btnSliderNext.setOnClickListener(v -> {
            int cur = vpRecommended.getCurrentItem();
            int max = bannerAdapter.getItemCount() - 1;
            if (cur < max) vpRecommended.setCurrentItem(cur + 1, true);
        });

        vpRecommended.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDotIndicator(position);
            }
        });
    }

    private void buildDotIndicator(int count) {
        layoutDotIndicator.removeAllViews();
        int dp6 = dpToPx(6);
        int dp3 = dpToPx(3);

        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp6, dp6);
            params.setMargins(dp3, 0, dp3, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.dot_inactive);
            layoutDotIndicator.addView(dot);
        }
        updateDotIndicator(0);
    }

    private void updateDotIndicator(int activePosition) {
        for (int i = 0; i < layoutDotIndicator.getChildCount(); i++) {
            View dot = layoutDotIndicator.getChildAt(i);
            if (i == activePosition) {
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
                p.setMargins(dpToPx(3), 0, dpToPx(3), 0);
                dot.setLayoutParams(p);
                dot.setBackgroundResource(R.drawable.dot_active);
            } else {
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dpToPx(6), dpToPx(6));
                p.setMargins(dpToPx(3), 0, dpToPx(3), 0);
                dot.setLayoutParams(p);
                dot.setBackgroundResource(R.drawable.dot_inactive);
            }
        }
    }

    // ========== PHẦN 2: TRUYỆN MỚI CẬP NHẬT ==========

    private void setupNewUpdatesSection() {
        newUpdatesAdapter = new ComicAdapter();
        recyclerViewComics.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewComics.setAdapter(newUpdatesAdapter);
        recyclerViewComics.setNestedScrollingEnabled(false);

        btnFilterIcon.setOnClickListener(v -> showCategoryFilterDialog());

        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                if (activeFilterCategoryId != null) {
                    loadComicsByCategory(activeFilterCategoryId);
                } else {
                    loadNewUpdatesComics(currentPage);
                }
            }
        });
        btnNextPage.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                if (activeFilterCategoryId != null) {
                    loadComicsByCategory(activeFilterCategoryId);
                } else {
                    loadNewUpdatesComics(currentPage);
                }
            }
        });
    }

    private void showCategoryFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_filter, null);
        dialog.setContentView(dialogView);

        RecyclerView rvPopup = dialogView.findViewById(R.id.rvCategoryPopup);
        TextView tvClear = dialogView.findViewById(R.id.tvClearFilter);

        catFilterAdapter = new CategoryFilterAdapter(category -> {
            if (category == null) {
                activeFilterCategoryId = null;
                activeFilterCategoryName = null;
                tvActiveFilter.setVisibility(View.GONE);
                tvActiveFilter.setText("");
                currentPage = 0;
                loadNewUpdatesComics(currentPage);
            } else {
                activeFilterCategoryId = category.getCategoryId();
                activeFilterCategoryName = category.getName();
                tvActiveFilter.setText("Đang lọc: " + category.getName());
                tvActiveFilter.setVisibility(View.VISIBLE);
                currentPage = 0;
                loadComicsByCategory(activeFilterCategoryId);
            }
            dialog.dismiss();
        });

        rvPopup.setLayoutManager(new GridLayoutManager(this, 3));
        rvPopup.setAdapter(catFilterAdapter);

        ApiClient.getApiService().getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    catFilterAdapter.setCategories(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải danh mục: " + t.getMessage());
            }
        });

        tvClear.setOnClickListener(v -> {
            activeFilterCategoryId = null;
            activeFilterCategoryName = null;
            tvActiveFilter.setVisibility(View.GONE);
            tvActiveFilter.setText("");
            currentPage = 0;
            loadNewUpdatesComics(currentPage);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updatePageNumbers(int currentPage, int totalPages) {
        layoutPageNumbers.removeAllViews();

        int maxVisible = 5;
        int startPage = Math.max(0, currentPage - maxVisible / 2);
        int endPage = Math.min(totalPages - 1, startPage + maxVisible - 1);

        if (endPage - startPage < maxVisible - 1) {
            startPage = Math.max(0, endPage - maxVisible + 1);
        }

        int dpSize = dpToPx(34);
        int marginDp = dpToPx(2);

        for (int i = startPage; i <= endPage; i++) {
            final int pageIndex = i;
            TextView tvPage = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpSize, dpSize);
            params.setMargins(marginDp, 0, marginDp, 0);
            tvPage.setLayoutParams(params);
            tvPage.setText(String.valueOf(i + 1));
            tvPage.setGravity(Gravity.CENTER);
            tvPage.setTextSize(13);

            if (i == currentPage) {
                tvPage.setBackgroundResource(R.drawable.bg_page_btn);
                tvPage.setBackgroundColor(Color.parseColor("#FF9800"));
                tvPage.setTextColor(Color.WHITE);
                tvPage.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tvPage.setBackgroundResource(R.drawable.bg_page_btn);
                tvPage.setBackgroundColor(Color.parseColor("#EEEEEE"));
                tvPage.setTextColor(Color.parseColor("#333333"));
                tvPage.setOnClickListener(v -> {
                    MainActivity.this.currentPage = pageIndex;
                    if (activeFilterCategoryId != null) {
                        loadComicsByCategory(activeFilterCategoryId);
                    } else {
                        loadNewUpdatesComics(pageIndex);
                    }
                });
            }
            layoutPageNumbers.addView(tvPage);
        }

        btnPrevPage.setEnabled(currentPage > 0);
        btnNextPage.setEnabled(currentPage < totalPages - 1);
        btnPrevPage.setAlpha(currentPage > 0 ? 1.0f : 0.4f);
        btnNextPage.setAlpha(currentPage < totalPages - 1 ? 1.0f : 0.4f);
    }

    // ========== PHẦN 3: BẢNG XẾP HẠNG ==========

    private void setupRankingSection() {
        rankingAdapter = new RankingAdapter();
        rvTopRank.setLayoutManager(new LinearLayoutManager(this));
        rvTopRank.setAdapter(rankingAdapter);
        rvTopRank.setNestedScrollingEnabled(false);

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

    // ========== LOAD DỮ LIỆU TỪ API ==========

    private void loadAllData() {
        loadRecommendedComics();
        loadNewUpdatesComics(currentPage);
        loadRankingData("day");
    }

    private void loadRecommendedComics() {
        ApiClient.getApiService().getRecommendedComics().enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bannerAdapter.setComics(response.body());
                    buildDotIndicator(response.body().size());
                }
            }
            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
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

                    if (comics.size() == PAGE_SIZE) {
                        totalPages = Math.max(totalPages, page + 2);
                    } else {
                        totalPages = page + 1;
                    }
                    updatePageNumbers(page, totalPages);
                }
            }
            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải truyện mới: " + t.getMessage());
            }
        });
    }

    private void loadComicsByCategory(int catId) {
        ApiClient.getApiService().getComicsByCategory(catId).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comic> comics = response.body();
                    newUpdatesAdapter.setComics(comics);
                    totalPages = 1;
                    updatePageNumbers(0, 1);
                }
            }
            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi lọc thể loại: " + t.getMessage());
            }
        });
    }

    private void loadRankingData(String type) {
        ApiClient.getApiService().getTopRanking(type).enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    rankingAdapter.setComics(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi BXH: " + t.getMessage());
            }
        });
    }

    private void showHeaderPopupMenu(View anchorView) {
        androidx.appcompat.widget.PopupMenu popupMenu =
                new androidx.appcompat.widget.PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_header_options, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (id == R.id.menu_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (id == R.id.menu_follow) {
                startActivity(new Intent(this, FollowActivity.class));
                return true;
            } else if (id == R.id.menu_downloads) {
                Toast.makeText(this, "Truyện tải xuống", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                return true;
            } else if (id == R.id.menu_logout) {
                performLogout();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showAvatarMenu(View anchorView) {
        androidx.appcompat.widget.PopupMenu popupMenu =
                new androidx.appcompat.widget.PopupMenu(this, anchorView);

        popupMenu.getMenu().add(0, 1, 0, "Hồ sơ cá nhân");
        popupMenu.getMenu().add(0, 2, 1, "Đăng xuất");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                return true;
            } else if (item.getItemId() == 2) {
                performLogout();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void performLogout() {
        Toast.makeText(this, "Đang đăng xuất...", Toast.LENGTH_SHORT).show();
        SharedPrefsManager.logout(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}