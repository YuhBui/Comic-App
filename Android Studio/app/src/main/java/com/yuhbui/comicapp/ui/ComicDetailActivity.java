package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.local.AppDatabase;
import com.yuhbui.comicapp.data.model.Chapter;
import com.yuhbui.comicapp.data.model.ChapterImage;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.data.model.ComicDetailResponse;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.data.model.DownloadedChapter;
import com.yuhbui.comicapp.data.model.DownloadedComic;
import com.yuhbui.comicapp.data.model.DownloadedImage;
import com.yuhbui.comicapp.data.model.User;
import com.yuhbui.comicapp.ui.adapters.ChapterAdapter;
import com.yuhbui.comicapp.ui.adapters.CommentAdapter;
import com.yuhbui.comicapp.utils.DownloadUtils;
import com.yuhbui.comicapp.utils.HeaderUtils;
import com.yuhbui.comicapp.utils.MenuUtils;
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicDetailActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    private int currentComicId;
    private String currentComicTitle;
    private Integer targetParentCommentId = null;

    // --- BIẾN ĐIỀU KHIỂN CHI TIẾT TRUYỆN ---
    private ImageView imgComicCover;
    private TextView tvAuthor, tvGenre, tvRelease, tvStatus;
    private TextView tvViews, tvFavorites, tvRatingAverage, tvDescription;
    private RatingBar ratingBarUser;
    private Button btnStartReading;
    private TextView btnToggleFavorite;

    private boolean isCurrentlyFavorite = false;
    private boolean isUserBanned = false;

    // --- THÊM BIẾN QUẢN LÝ OFFLINE ---
    private boolean isOfflineMode = false;
    private Comic onlineComicData; // Giữ lại thông tin để phục vụ lúc download dữ liệu text
    private String onlineGenres;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor(); // Luồng nền xử lý Room DB & Tải file

    // Thành phần xử lý danh sách chương truyện (MVVM)
    private ComicDetailViewModel viewModel;
    private ChapterAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvTitle;
    private List<Chapter> globalChapterList = new ArrayList<>();

    // Thành phần xử lý Bình luận
    private RecyclerView recyclerViewComments;
    private CommentAdapter commentAdapter;
    private EditText edtCommentInput;
    private Button btnSendComment;

    // --- KHAI BÁO CÁC THÀNH PHẦN CỦA HEADER DÙNG CHUNG ---
    private View layoutHeader;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_detail);

        // 1. Ánh xạ toàn bộ các View từ XML chi tiết truyện
        tvTitle = findViewById(R.id.tvComicTitle);
        imgComicCover = findViewById(R.id.imgComicCover);
        tvAuthor = findViewById(R.id.tvComicAuthor);
        tvGenre = findViewById(R.id.tvComicGenre);
        tvRelease = findViewById(R.id.tvComicRelease);
        tvStatus = findViewById(R.id.tvComicStatus);
        tvViews = findViewById(R.id.tvComicViews);
        tvFavorites = findViewById(R.id.tvComicFavorites);
        tvRatingAverage = findViewById(R.id.tvComicRatingAverage);
        tvDescription = findViewById(R.id.tvComicDescription);
        ratingBarUser = findViewById(R.id.ratingBarUser);
        btnStartReading = findViewById(R.id.btnStartReading);
        btnToggleFavorite = findViewById(R.id.btnToggleFavorite);

        recyclerView = findViewById(R.id.recyclerViewChapters);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        edtCommentInput = findViewById(R.id.edtCommentInput);
        btnSendComment = findViewById(R.id.btnSendComment);

        // --- 2. ÁNH XẠ VÀ CẤU HÌNH ĐỒNG BỘ LAYOUT HEADER & MENU TRƯỢT MỚI ---
        drawerLayout = findViewById(R.id.drawerLayout);
        layoutHeader = findViewById(R.id.layoutHeader);
        headerLogo = layoutHeader.findViewById(R.id.headerLogo);

        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);
        MenuUtils.setupSideMenu(this, drawerLayout, layoutHeader.findViewById(R.id.headerMenu));

        headerLogo.setOnClickListener(v -> finish());

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

        // 3. Cài đặt cấu trúc hiển thị danh sách Chương truyện
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChapterAdapter();
        recyclerView.setAdapter(adapter);

        // 4. Cài đặt cấu trúc hiển thị danh sách Bình luận
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter();
        recyclerViewComments.setAdapter(commentAdapter);

        commentAdapter.setOnCommentClickListener(new CommentAdapter.OnCommentClickListener() {
            @Override
            public void onReplyClick(Comment parentComment) {
                if (isUserBanned) {
                    Toast.makeText(ComicDetailActivity.this, "Bạn hiện đang bị cấm chat!", Toast.LENGTH_SHORT).show();
                    return;
                }
                targetParentCommentId = parentComment.getCommentId();
                if (parentComment.getUserDisplayName() != null) {
                    String tagText = "@" + parentComment.getUserDisplayName() + " ";
                    edtCommentInput.setText(tagText);
                    edtCommentInput.setSelection(tagText.length());
                    edtCommentInput.setHint("Đang trả lời...");
                } else {
                    edtCommentInput.setText("");
                    edtCommentInput.setHint("Viết phản hồi...");
                }
                edtCommentInput.requestFocus();
            }
        });

        // 5. "Hứng" dữ liệu thông tin truyện sơ bộ từ Intent gửi sang
        if (getIntent() != null) {
            currentComicId = getIntent().getIntExtra("COMIC_ID", -1);
            currentComicTitle = getIntent().getStringExtra("COMIC_TITLE");
            isOfflineMode = getIntent().getBooleanExtra("IS_OFFLINE_MODE", false); // THÊM: Nhận diện chế độ Offline
        }
        tvTitle.setText(currentComicTitle);
        adapter.setOfflineMode(isOfflineMode); // Đồng bộ chế độ hiển thị nút cho Adapter

        int currentUserId = SharedPrefsManager.getUserId(this);

        // --- XỬ LÝ PHÂN TÁCH GIAO DIỆN OFFLINE / ONLINE ---
        if (isOfflineMode) {
            // Thực hiện ẩn trạng thái, lượt xem, yêu thích, đánh giá, bình luận theo đúng yêu cầu
            tvStatus.setVisibility(View.GONE);
            tvViews.setVisibility(View.GONE);
            btnToggleFavorite.setVisibility(View.GONE);
            tvFavorites.setVisibility(View.GONE);
            tvRatingAverage.setVisibility(View.GONE);
            ratingBarUser.setVisibility(View.GONE);

            recyclerViewComments.setVisibility(View.GONE);
            edtCommentInput.setVisibility(View.GONE);
            btnSendComment.setVisibility(View.GONE);

            // Tải dữ liệu lưu local từ Room Database
            loadOfflineComicDetailsAndChapters();
        } else {
            // Chế độ ONLINE hoạt động bình thường như cũ
            if (currentUserId != -1) {
                checkCurrentUserBanStatus(currentUserId);
            }

            viewModel = new ViewModelProvider(this).get(ComicDetailViewModel.class);
            viewModel.getChapters().observe(this, new Observer<List<Chapter>>() {
                @Override
                public void onChanged(List<Chapter> chapters) {
                    if (chapters != null) {
                        globalChapterList = chapters;
                        adapter.setChapters(chapters);
                        adapter.notifyDataSetChanged();
                    }
                }
            });

            if (currentComicId != -1) {
                loadComicFullDetails(currentComicId, currentUserId != -1 ? currentUserId : null);
                viewModel.loadChapters(currentComicId);
                loadComments(currentComicId);
                loadDownloadedChapterIds(); // Kiểm tra những chương đã tải để hiển thị dấu tích tích xanh
            }

            btnToggleFavorite.setOnClickListener(v -> {
                if (currentUserId == -1) {
                    Toast.makeText(this, "Vui lòng đăng nhập để thêm vào danh sách yêu thích!", Toast.LENGTH_SHORT).show();
                    return;
                }
                ApiClient.getApiService().toggleFavorite(currentComicId, currentUserId).enqueue(new Callback<Boolean>() {
                    @Override
                    public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            isCurrentlyFavorite = response.body();
                            updateFavoriteButtonUI(isCurrentlyFavorite);
                            loadComicFullDetails(currentComicId, currentUserId);
                        }
                    }
                    @Override
                    public void onFailure(Call<Boolean> call, Throwable t) {}
                });
            });

            ratingBarUser.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                @Override
                public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                    if (fromUser) {
                        if (currentUserId == -1) {
                            Toast.makeText(ComicDetailActivity.this, "Vui lòng đăng nhập để đánh giá!", Toast.LENGTH_SHORT).show();
                            ratingBarUser.setRating(0);
                            return;
                        }
                        sendRatingToServer(currentComicId, currentUserId, (int) rating);
                    }
                }
            });

            btnSendComment.setOnClickListener(v -> {
                if (isUserBanned) {
                    Toast.makeText(this, "Bạn hiện đang bị cấm chat!", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendCommentToServer();
            });
        }

        // Bắt sự kiện click nút BẮT ĐẦU ĐỌC TRUYỆN (Áp dụng cho cả Online và Offline)
        btnStartReading.setOnClickListener(v -> {
            if (globalChapterList != null && !globalChapterList.isEmpty()) {
                // Đọc chương đầu tiên (phần tử cuối của mảng xếp DESC)
                Chapter firstChapter = globalChapterList.get(globalChapterList.size() - 1);

                Intent intent = new Intent(ComicDetailActivity.this, ReaderActivity.class);
                intent.putExtra("CHAPTER_ID", firstChapter.getChapterId());
                intent.putExtra("COMIC_ID", currentComicId);
                intent.putExtra("IS_OFFLINE_MODE", isOfflineMode); // Gửi kèm trạng thái mạng sang Reader
                startActivity(intent);
            } else {
                Toast.makeText(this, "Truyện hiện chưa cập nhật chương nội dung nào!", Toast.LENGTH_SHORT).show();
            }
        });

        // Đăng ký nhận Callback xử lý sự kiện bấm Nút vuông (Tải xuống / Xóa) từ Adapter
        adapter.setOnChapterActionListener(new ChapterAdapter.OnChapterActionListener() {
            @Override
            public void onDownloadClick(Chapter chapter) {
                downloadChapterTask(chapter);
            }

            @Override
            public void onDeleteClick(Chapter chapter) {
                new androidx.appcompat.app.AlertDialog.Builder(ComicDetailActivity.this)
                        .setTitle("Xóa chương truyện")
                        .setMessage("Bạn có chắc chắn muốn xóa Chương " + chapter.getChapterNumber() + " khỏi bộ nhớ máy?")
                        .setPositiveButton("Xóa", (dialog, which) -> deleteChapterOfflineTask(chapter))
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (layoutHeader != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
            HeaderUtils.loadUnreadNotificationCount(this, layoutHeader.findViewById(R.id.tvNotificationBadge));
        }
    }

    // ========== LOGIC TRUY VẤN VÀ TRIỂN KHAI OFFLINE (ROOM DB) ==========

    private void loadOfflineComicDetailsAndChapters() {
        databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            DownloadedComic localComic = db.offlineDao().getComicById(currentComicId);
            List<DownloadedChapter> localChapters = db.offlineDao().getChaptersByComic(currentComicId);

            runOnUiThread(() -> {
                if (localComic != null) {
                    tvTitle.setText(localComic.getTitle());
                    tvAuthor.setText("Tác giả: " + (localComic.getAuthor() != null ? localComic.getAuthor() : "Đang cập nhật"));
                    tvDescription.setText(localComic.getDescription());
                    tvGenre.setText("Thể loại: " + (localComic.getGenres() != null ? localComic.getGenres() : "Đang cập nhật"));

                    if (localComic.getLocalCoverPath() != null && !localComic.getLocalCoverPath().isEmpty()) {
                        Glide.with(ComicDetailActivity.this)
                                .load(new File(localComic.getLocalCoverPath()))
                                .placeholder(R.drawable.ic_launcher_background)
                                .into(imgComicCover);
                    }
                }

                // Ánh xạ danh sách chương local về đối tượng Chapter của Adapter để hiển thị lên RecyclerView
                List<Chapter> mappedChapters = new ArrayList<>();
                for (DownloadedChapter localCh : localChapters) {
                    Chapter ch = new Chapter();
                    ch.setChapterId(localCh.getChapterId());
                    ch.setComicId(localCh.getComicId());
                    ch.setChapterNumber(localCh.getChapterNumber());
                    ch.setTitle(localCh.getTitle());
                    mappedChapters.add(ch);
                }

                globalChapterList = mappedChapters; // Cập nhật biến global để nút bắt đầu đọc click được
                adapter.setChapters(mappedChapters);
            });
        });
    }

    private void loadDownloadedChapterIds() {
        databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<DownloadedChapter> localChapters = db.offlineDao().getChaptersByComic(currentComicId);
            List<Integer> downloadedIds = new ArrayList<>();
            for (DownloadedChapter ch : localChapters) {
                downloadedIds.add(ch.getChapterId());
            }
            runOnUiThread(() -> adapter.setDownloadedChapterIds(downloadedIds));
        });
    }

    private void downloadChapterTask(Chapter chapter) {
        if (onlineComicData == null) {
            Toast.makeText(this, "Đang tải dữ liệu truyện, vui lòng bấm lại sau ít giây!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Đang tải xuống Chương " + chapter.getChapterNumber() + "...", Toast.LENGTH_SHORT).show();

        databaseExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(ComicDetailActivity.this);

                // 1. Kiểm tra xem thông tin cốt lõi của truyện đã được lưu local chưa
                DownloadedComic localComic = db.offlineDao().getComicById(currentComicId);
                String localCoverPath = "";
                if (localComic == null) {
                    // Tiến hành download file ảnh bìa về máy
                    if (onlineComicData.getCoverImageUrl() != null && !onlineComicData.getCoverImageUrl().isEmpty()) {
                        localCoverPath = DownloadUtils.downloadFile(
                                ComicDetailActivity.this,
                                onlineComicData.getCoverImageUrl(),
                                "comic_" + currentComicId,
                                "cover.jpg"
                        );
                    }

                    DownloadedComic newComic = new DownloadedComic();
                    newComic.setComicId(currentComicId);
                    newComic.setTitle(onlineComicData.getTitle());
                    newComic.setLocalCoverPath(localCoverPath != null ? localCoverPath : "");
                    newComic.setAuthor(onlineComicData.getAuthor());
                    newComic.setDescription(onlineComicData.getDescription());
                    newComic.setGenres(onlineGenres);
                    db.offlineDao().insertComic(newComic);
                }

                // 2. Chạy API lấy danh sách ảnh của chương bằng cơ chế đồng bộ (.execute()) bên trong Thread nền
                Response<List<ChapterImage>> imageResponse = ApiClient.getApiService()
                        .getImagesByChapterId(chapter.getChapterId()).execute();

                if (imageResponse.isSuccessful() && imageResponse.body() != null) {
                    List<ChapterImage> serverImages = imageResponse.body();
                    List<DownloadedImage> localImagesList = new ArrayList<>();

                    // Vòng lặp tải tuần tự toàn bộ trang ảnh truyện về Internal Storage
                    for (int i = 0; i < serverImages.size(); i++) {
                        ChapterImage serverImg = serverImages.get(i);
                        String localImgPath = DownloadUtils.downloadFile(
                                ComicDetailActivity.this,
                                serverImg.getImageUrl(),
                                "comic_" + currentComicId + "/chapter_" + chapter.getChapterId(),
                                "page_" + i + ".jpg"
                        );

                        if (localImgPath != null) {
                            DownloadedImage localImgRecord = new DownloadedImage();
                            localImgRecord.setChapterId(chapter.getChapterId());
                            localImgRecord.setLocalFilePath(localImgPath);
                            localImgRecord.setPosition(i);
                            localImagesList.add(localImgRecord);
                        }
                    }

                    // 3. Ghi thông tin chương và đường dẫn ảnh local vào Room DB
                    DownloadedChapter localChapter = new DownloadedChapter();
                    localChapter.setChapterId(chapter.getChapterId());
                    localChapter.setComicId(currentComicId);
                    localChapter.setChapterNumber(chapter.getChapterNumber());
                    localChapter.setTitle(chapter.getTitle());

                    db.offlineDao().insertChapter(localChapter);
                    db.offlineDao().insertImages(localImagesList);

                    runOnUiThread(() -> {
                        Toast.makeText(ComicDetailActivity.this, "Tải thành công Chương " + chapter.getChapterNumber(), Toast.LENGTH_SHORT).show();
                        loadDownloadedChapterIds(); // Làm tươi giao diện hiển thị dấu tích
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ComicDetailActivity.this, "Không thể tải danh sách ảnh từ Server!", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ComicDetailActivity.this, "Gặp lỗi kết nối khi tải offline!", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void deleteChapterOfflineTask(Chapter chapter) {
        databaseExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(ComicDetailActivity.this);

                // 1. Tìm và xóa sạch file ảnh trang truyện vật lý trong thư mục máy
                File chapterDirectory = new File(getFilesDir(), "truyen_downloads/comic_" + currentComicId + "/chapter_" + chapter.getChapterId());
                if (chapterDirectory.exists() && chapterDirectory.isDirectory()) {
                    File[] images = chapterDirectory.listFiles();
                    if (images != null) {
                        for (File f : images) {
                            f.delete();
                        }
                    }
                    chapterDirectory.delete();
                }

                // 2. Xóa liên kết bản ghi dữ liệu trong Room DB
                db.offlineDao().deleteChapterById(chapter.getChapterId());
                db.offlineDao().deleteImagesByChapter(chapter.getChapterId());

                // 3. Kiểm tra xem bộ truyện này còn chương nào khác không
                int totalChaptersLeft = db.offlineDao().getChapterCountByComic(currentComicId);
                if (totalChaptersLeft == 0) {
                    // Nếu không còn chương nào, tiến hành xóa sạch truyện khỏi bộ lưu trữ offline
                    DownloadedComic targetComic = db.offlineDao().getComicById(currentComicId);
                    if (targetComic != null) {
                        if (targetComic.getLocalCoverPath() != null && !targetComic.getLocalCoverPath().isEmpty()) {
                            File coverImgFile = new File(targetComic.getLocalCoverPath());
                            if (coverImgFile.exists()) coverImgFile.delete();
                        }
                        db.offlineDao().deleteComic(targetComic);
                    }

                    // Xóa nốt thư mục gốc của bộ truyện nếu trống
                    File comicDirectory = new File(getFilesDir(), "truyen_downloads/comic_" + currentComicId);
                    if (comicDirectory.exists() && comicDirectory.isDirectory()) {
                        comicDirectory.delete();
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(ComicDetailActivity.this, "Truyện không còn chương nào nên đã được gỡ khỏi mục Tải xuống!", Toast.LENGTH_LONG).show();
                        finish(); // Out màn hình quay ra lại trang danh sách truyện đã tải
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ComicDetailActivity.this, "Đã xóa chương thành công!", Toast.LENGTH_SHORT).show();
                        loadOfflineComicDetailsAndChapters(); // Làm tươi danh sách chương local còn lại
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ComicDetailActivity.this, "Lỗi trong quá trình xóa dữ liệu máy!", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ========== LUỒNG XỬ LÝ MẠNG ONLINE CŨ GIỮ NGUYÊN ==========

    private void checkCurrentUserBanStatus(int userId) {
        ApiClient.getApiService().getUserProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    if ("Banned".equalsIgnoreCase(user.getStatus())) {
                        isUserBanned = true;
                        edtCommentInput.setEnabled(false);
                        btnSendComment.setEnabled(false);
                        edtCommentInput.setHint("Bạn hiện đang bị cấm chat");
                        edtCommentInput.setOnClickListener(v ->
                                Toast.makeText(ComicDetailActivity.this, "Tài khoản của bạn hiện đang bị cấm chat!", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    private void updateFavoriteButtonUI(boolean isFav) {
        if (isFav) {
            btnToggleFavorite.setText("❤️ Đã yêu thích");
            btnToggleFavorite.setAlpha(0.6f);
        } else {
            btnToggleFavorite.setText("🤍 Yêu thích");
            btnToggleFavorite.setAlpha(1.0f);
        }
    }

    private void loadComicFullDetails(int comicId, Integer userId) {
        ApiClient.getApiService().getComicDetail(comicId, userId).enqueue(new Callback<ComicDetailResponse>() {
            @Override
            public void onResponse(Call<ComicDetailResponse> call, Response<ComicDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ComicDetailResponse data = response.body();
                    Comic comic = data.getComic();

                    // Gán vào biến online phục vụ khi người dùng nhấn download
                    onlineComicData = comic;
                    onlineGenres = data.getGenres();

                    tvTitle.setText(comic.getTitle());
                    tvAuthor.setText("Tác giả: " + (comic.getAuthor() != null ? comic.getAuthor() : "Đang cập nhật"));
                    tvViews.setText("👁️ " + comic.getViewCount());
                    tvDescription.setText(comic.getDescription());
                    tvGenre.setText("Thể loại: " + data.getGenres());

                    tvFavorites.setText("❤️ " + String.valueOf(data.getFavoriteCount()));
                    tvRatingAverage.setText("⭐ " + comic.getRating() + "/5");

                    String statusStr = (comic.getStatus() != null ? comic.getStatus() : "Đang tiến hành");
                    String latestChapStr = (data.getLatestChapterNumber() != null ? data.getLatestChapterNumber() : "Chưa có");
                    tvStatus.setText("Tình trạng: " + statusStr);

                    if (comic.getCreatedAt() != null && !comic.getCreatedAt().isEmpty()) {
                        tvRelease.setText("Phát hành: " + formatToDateOnly(comic.getCreatedAt()));
                    } else {
                        tvRelease.setText("Phát hành: Đang cập nhật");
                    }

                    isCurrentlyFavorite = data.isFavorite();
                    updateFavoriteButtonUI(isCurrentlyFavorite);

                    Glide.with(ComicDetailActivity.this)
                            .load(comic.getCoverImageUrl())
                            .placeholder(R.drawable.ic_launcher_background)
                            .into(imgComicCover);
                }
            }
            @Override
            public void onFailure(Call<ComicDetailResponse> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải chi tiết truyện: " + t.getMessage());
            }
        });
    }

    private String formatToDateOnly(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.trim().isEmpty()) {
            return "Đang cập nhật";
        }
        try {
            // Tách lấy phần ngày yyyy-MM-dd trước ký tự 'T' hoặc khoảng trắng của timestamp
            String datePart = rawDateTime.contains("T") ? rawDateTime.split("T")[0] : rawDateTime.split(" ")[0];
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                // Đảo thứ tự hiển thị từ yyyy-MM-dd sang dd/MM/yyyy tương tự bên Admin
                return parts[2] + "/" + parts[1] + "/" + parts[0];
            }
            return datePart;
        } catch (Exception e) {
            return rawDateTime;
        }
    }

    private void sendRatingToServer(int comicId, int userId, int score) {
        ApiClient.getApiService().rateComic(comicId, userId, score).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ComicDetailActivity.this, "Cảm ơn bạn đã đánh giá " + score + " sao!", Toast.LENGTH_SHORT).show();
                    loadComicFullDetails(comicId, userId);
                }
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(ComicDetailActivity.this, "Đã ghi nhận đánh giá!", Toast.LENGTH_SHORT).show();
                loadComicFullDetails(comicId, userId);
            }
        });
    }

    private void loadComments(int comicId) {
        ApiClient.getApiService().getCommentsByComic(comicId).enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentAdapter.setComments(response.body());
                    commentAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {
                Toast.makeText(ComicDetailActivity.this, "Không thể tải bình luận", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendCommentToServer() {
        String content = edtCommentInput.getText().toString().trim();
        int userId = SharedPrefsManager.getUserId(this);

        if (content.isEmpty()) {
            Toast.makeText(this, "Nội dung bình luận không được để trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để bình luận!", Toast.LENGTH_SHORT).show();
            return;
        }

        Comment newComment = new Comment();
        newComment.setUserId(userId);
        newComment.setComicId(currentComicId);
        newComment.setContent(content);
        newComment.setParentCommentId(targetParentCommentId);

        ApiClient.getApiService().postComment(newComment).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ComicDetailActivity.this, "Đăng bình luận thành công!", Toast.LENGTH_SHORT).show();
                    edtCommentInput.setText("");
                    edtCommentInput.setHint("Viết bình luận của bạn...");

                    if (targetParentCommentId != null) {
                        commentAdapter.resetRepliesCache(targetParentCommentId);
                    }
                    targetParentCommentId = null;
                    loadComments(currentComicId);
                }
            }
            @Override
            public void onFailure(Call<Comment> call, Throwable t) {
                Toast.makeText(ComicDetailActivity.this, "Lỗi kết nối mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}