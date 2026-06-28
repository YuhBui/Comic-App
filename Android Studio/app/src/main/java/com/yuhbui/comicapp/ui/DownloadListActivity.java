package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.local.AppDatabase;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.data.model.DownloadedComic;
import com.yuhbui.comicapp.ui.adapters.ComicAdapter;
import com.yuhbui.comicapp.utils.HeaderUtils;
import com.yuhbui.comicapp.utils.MenuUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadListActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private ComicAdapter comicAdapter;
    private TextView tvEmptyDownload;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 10;
    private int totalPages = 1;
    private List<Comic> allMappedComics = new ArrayList<>();

    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);

        drawerLayout = findViewById(R.id.drawerLayout);
        View layoutHeader = findViewById(R.id.layoutHeaderDownload);
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);
        MenuUtils.setupSideMenu(this, drawerLayout, layoutHeader.findViewById(R.id.headerMenu));

        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);

        headerLogo.setText(android.text.Html.fromHtml("<font color='#D97707'>h</font><font color='#FFFFFF'>ay</font><font color='#D97707'>c</font><font color='#FFFFFF'>omic</font>", android.text.Html.FROM_HTML_MODE_COMPACT));

        headerLogo.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerViewDownloads);
        tvEmptyDownload = findViewById(R.id.tvEmptyDownload);

        recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        comicAdapter = new ComicAdapter();
        recyclerView.setAdapter(comicAdapter);
        comicAdapter.setDownloadMode(true); // Ép kích hoạt ẩn hoàn toàn các thanh thông số

        comicAdapter.setOnItemClickListener(comic -> {
            Intent intent = new Intent(DownloadListActivity.this, ComicDetailActivity.class);
            intent.putExtra("COMIC_ID", comic.getComicId());
            intent.putExtra("COMIC_TITLE", comic.getTitle());
            intent.putExtra("IS_OFFLINE_MODE", true);
            startActivity(intent);
        });

        // Thiết lập sự kiện click cho nút Trang trước / Trang sau
        findViewById(R.id.btnPrevPageDownload).setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                displayPagedData();
            }
        });

        findViewById(R.id.btnNextPageDownload).setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                displayPagedData();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOfflineComics();
        if (findViewById(R.id.layoutHeaderDownload) != null) {
            HeaderUtils.loadHeaderAvatar(this, findViewById(R.id.layoutHeaderDownload).findViewById(R.id.headerAvatar));
            HeaderUtils.loadUnreadNotificationCount(this, findViewById(R.id.layoutHeaderDownload).findViewById(R.id.tvNotificationBadge));
        }
    }

    private void loadOfflineComics() {
        databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<DownloadedComic> localComics = db.offlineDao().getAllDownloadedComics();
            allMappedComics.clear();

            for (DownloadedComic local : localComics) {
                Comic comic = new Comic();
                comic.setComicId(local.getComicId());
                comic.setTitle(local.getTitle());
                comic.setCoverImageUrl(local.getLocalCoverPath());
                comic.setAuthor(local.getAuthor());
                comic.setDescription(local.getDescription());
                allMappedComics.add(comic);
            }

            runOnUiThread(() -> {
                if (allMappedComics.isEmpty()) {
                    tvEmptyDownload.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    findViewById(R.id.btnPrevPageDownload).setVisibility(View.GONE);
                    findViewById(R.id.btnNextPageDownload).setVisibility(View.GONE);
                } else {
                    tvEmptyDownload.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    findViewById(R.id.btnPrevPageDownload).setVisibility(View.VISIBLE);
                    findViewById(R.id.btnNextPageDownload).setVisibility(View.VISIBLE);

                    // Tính toán tổng số trang dựa trên độ dài danh sách
                    totalPages = (int) Math.ceil((double) allMappedComics.size() / PAGE_SIZE);
                    if (currentPage >= totalPages) currentPage = 0; // Tránh tràn chỉ số

                    displayPagedData();
                }
            });
        });
    }

    // Hàm cắt danh sách mảng và vẽ số thứ tự nút trang
    private void displayPagedData() {
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allMappedComics.size());

        List<Comic> pagedList = allMappedComics.subList(start, end);
        comicAdapter.setComics(pagedList);

        updatePageNumberIndicators();
    }

    private void updatePageNumberIndicators() {
        LinearLayout layoutPageNumbers = findViewById(R.id.layoutPageNumbersDownload);
        layoutPageNumbers.removeAllViews();

        int density = (int) getResources().getDisplayMetrics().density;
        int size = 40 * density;
        int margin = 4 * density;

        for (int i = 0; i < totalPages; i++) {
            final int pageIndex = i;
            Button btnPage = new Button(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            btnPage.setLayoutParams(params);
            btnPage.setPadding(0, 0, 0, 0);
            btnPage.setText(String.valueOf(i + 1));
            btnPage.setTextSize(14);
            btnPage.setAllCaps(false);

            // Style nút trang chủ active / inactive trùng khớp Figma
            if (i == currentPage) {
                btnPage.setBackgroundResource(R.drawable.bg_nav_btn);
                btnPage.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFB77D")));
                btnPage.setTextColor(Color.parseColor("#4D2600"));
                btnPage.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                btnPage.setBackgroundResource(R.drawable.bg_nav_btn);
                btnPage.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E1E1E")));
                btnPage.setTextColor(Color.parseColor("#DBC2B0"));
            }

            btnPage.setOnClickListener(v -> {
                currentPage = pageIndex;
                displayPagedData();
            });

            layoutPageNumbers.addView(btnPage);
        }

        Button btnPrev = findViewById(R.id.btnPrevPageDownload);
        Button btnNext = findViewById(R.id.btnNextPageDownload);
        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled(currentPage < totalPages - 1);
        btnPrev.setBackgroundResource(R.drawable.bg_nav_btn);
        btnPrev.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E1E1E")));
        btnPrev.setTextColor(Color.parseColor(currentPage > 0 ? "#DBC2B0" : "#555555"));
        btnNext.setBackgroundResource(R.drawable.bg_nav_btn);
        btnNext.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E1E1E")));
        btnNext.setTextColor(Color.parseColor(currentPage < totalPages - 1 ? "#DBC2B0" : "#555555"));
    }
}