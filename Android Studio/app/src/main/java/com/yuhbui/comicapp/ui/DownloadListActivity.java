package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
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
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);

        // 1. Đồng bộ cấu trúc thanh Header tiêu chuẩn
        drawerLayout = findViewById(R.id.drawerLayout);
        View layoutHeader = findViewById(R.id.layoutHeaderDownload);
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);
        MenuUtils.setupSideMenu(this, drawerLayout, layoutHeader.findViewById(R.id.headerMenu));

        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        headerLogo.setOnClickListener(v -> finish());

        // 2. Ánh xạ danh sách giao diện
        recyclerView = findViewById(R.id.recyclerViewDownloads);
        tvEmptyDownload = findViewById(R.id.tvEmptyDownload);

        recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        comicAdapter = new ComicAdapter();
        recyclerView.setAdapter(comicAdapter);
        comicAdapter.setDownloadMode(true);

        // 3. Đăng ký nhận callback để ép ứng dụng mở màn hình chi tiết ở chế độ OFFLINE
        comicAdapter.setOnItemClickListener(comic -> {
            Intent intent = new Intent(DownloadListActivity.this, ComicDetailActivity.class);

            intent.putExtra("COMIC_ID", comic.getComicId());

            intent.putExtra("COMIC_TITLE", comic.getTitle());
            intent.putExtra("IS_OFFLINE_MODE", true);
            startActivity(intent);
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
        loadOfflineComics(); // Luôn refresh danh sách mỗi khi quay lại màn hình (tránh trường hợp vừa xóa truyện)
        if (findViewById(R.id.layoutHeaderDownload) != null) {
            HeaderUtils.loadHeaderAvatar(this, findViewById(R.id.layoutHeaderDownload).findViewById(R.id.headerAvatar));
            HeaderUtils.loadUnreadNotificationCount(this, findViewById(R.id.layoutHeaderDownload).findViewById(R.id.tvNotificationBadge));
        }
    }

    private void loadOfflineComics() {
        databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<DownloadedComic> localComics = db.offlineDao().getAllDownloadedComics();
            List<Comic> mappedComics = new ArrayList<>();

            // Chuyển đổi dữ liệu từ thực thể Room sang thực thể Comic để tái sử dụng ComicAdapter
            for (DownloadedComic local : localComics) {
                Comic comic = new Comic();
                comic.setComicId(local.getComicId());
                comic.setTitle(local.getTitle());
                comic.setCoverImageUrl(local.getLocalCoverPath()); // Gán path local vào Glide
                comic.setAuthor(local.getAuthor());
                comic.setDescription(local.getDescription());
                mappedComics.add(comic);
            }

            runOnUiThread(() -> {
                if (mappedComics.isEmpty()) {
                    tvEmptyDownload.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmptyDownload.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    comicAdapter.setComics(mappedComics);
                }
            });
        });
    }
}