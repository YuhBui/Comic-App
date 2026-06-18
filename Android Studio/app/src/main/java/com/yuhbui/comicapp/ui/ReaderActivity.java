package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.local.AppDatabase;
import com.yuhbui.comicapp.data.model.Chapter;
import com.yuhbui.comicapp.data.model.ChapterImage;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.data.model.DownloadedChapter;
import com.yuhbui.comicapp.data.model.DownloadedComic;
import com.yuhbui.comicapp.data.model.DownloadedImage;
import com.yuhbui.comicapp.ui.adapters.CommentAdapter;
import com.yuhbui.comicapp.ui.adapters.ImageAdapter;
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

public class ReaderActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    private RecyclerView recyclerViewImages;
    private ImageAdapter imageAdapter;

    // Khai báo các biến cho phần bình luận
    private RecyclerView recyclerViewComments;
    private CommentAdapter commentAdapter;
    private EditText edtCommentInput;
    private Button btnSendComment;

    // --- CÁC BIẾN ĐIỀU HƯỚNG CHUYỂN CHƯƠNG MỚI TÍCH HỢP ---
    private Button btnPrevChapter, btnNextChapter;
    private Spinner spinnerChapters;
    private List<Chapter> allChaptersInComic = new ArrayList<>();
    private int currentChapterIndex = -1;
    private boolean isSpinnerFirstInit = true;

    private int currentChapterId = -1;
    private int currentComicId = -1;
    private Integer targetParentCommentId = null;

    // --- BIẾN PHỤC VỤ CHẾ ĐỘ OFFLINE ---
    private boolean isOfflineMode = false;
    private LinearLayout layoutCommentContainerReader;
    private Button btnDeleteChapterReader;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // --- KHAI BÁO CÁC THÀNH PHẦN CỦA HEADER DÙNG CHUNG ---
    private View layoutHeader;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        drawerLayout = findViewById(R.id.drawerLayout);
        layoutHeader = findViewById(R.id.layoutHeaderReader);
        headerLogo = layoutHeader.findViewById(R.id.headerLogo);

        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);
        MenuUtils.setupSideMenu(this, drawerLayout, layoutHeader.findViewById(R.id.headerMenu));

        headerLogo.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
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

        // 1. Ánh xạ phần đọc ảnh truyện
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewImages.setHasFixedSize(false);
        recyclerViewImages.setItemViewCacheSize(30);
        recyclerViewImages.setNestedScrollingEnabled(false);

        imageAdapter = new ImageAdapter();
        recyclerViewImages.setAdapter(imageAdapter);

        // 2. Ánh xạ phần bình luận và nút offline bổ sung
        recyclerViewComments = findViewById(R.id.recyclerViewCommentsReader);
        edtCommentInput = findViewById(R.id.edtCommentInputReader);
        btnSendComment = findViewById(R.id.btnSendCommentReader);
        layoutCommentContainerReader = findViewById(R.id.layoutCommentContainerReader);
        btnDeleteChapterReader = findViewById(R.id.btnDeleteChapterReader);

        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter();
        recyclerViewComments.setAdapter(commentAdapter);

        btnPrevChapter = findViewById(R.id.btnPrevChapter);
        btnNextChapter = findViewById(R.id.btnNextChapter);
        spinnerChapters = findViewById(R.id.spinnerChapters);

        // Lấy dữ liệu Intent truyền từ màn hình chi tiết sang
        currentChapterId = getIntent().getIntExtra("CHAPTER_ID", -1);
        currentComicId = getIntent().getIntExtra("COMIC_ID", -1);
        isOfflineMode = getIntent().getBooleanExtra("IS_OFFLINE_MODE", false);

        // THIẾT LẬP TRẠNG THÁI GIAO DIỆN PHÂN TÁCH OFFLINE
        if (isOfflineMode) {
            layoutCommentContainerReader.setVisibility(View.GONE); // Ẩn hoàn toàn khối comment theo ý bạn
            btnDeleteChapterReader.setVisibility(View.VISIBLE);    // Hiện nút xóa chương nhanh

            btnDeleteChapterReader.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(ReaderActivity.this)
                        .setTitle("Xóa chương truyện")
                        .setMessage("Bạn có muốn xóa chương đang đọc này khỏi máy không?")
                        .setPositiveButton("Xóa", (dialog, which) -> deleteCurrentChapterOffline())
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }

        // Tải nội dung ảnh chương truyện
        if (currentChapterId != -1) {
            loadChapterContent(currentChapterId);
        }

        // Tải danh sách chương gán vào thanh chọn dropdown (Spinner)
        if (currentComicId != -1) {
            if (isOfflineMode) {
                loadOfflineChaptersNavigation();
            } else {
                loadAllChaptersNavigation(currentComicId);
            }
        }

        btnPrevChapter.setOnClickListener(v -> {
            if (currentChapterIndex < allChaptersInComic.size() - 1) {
                navigateToChapter(currentChapterIndex + 1);
            }
        });

        btnNextChapter.setOnClickListener(v -> {
            if (currentChapterIndex > 0) {
                navigateToChapter(currentChapterIndex - 1);
            }
        });

        commentAdapter.setOnCommentClickListener(new CommentAdapter.OnCommentClickListener() {
            @Override
            public void onReplyClick(Comment parentComment) {
                targetParentCommentId = parentComment.getCommentId();
                if (parentComment.getParentCommentId() == null && parentComment.getUserId() != 0) {
                    String tagText = "@Thành viên #" + parentComment.getUserId() + " ";
                    edtCommentInput.setText(tagText);
                    edtCommentInput.setSelection(tagText.length());
                    edtCommentInput.setHint("Đang trả lời...");
                } else {
                    edtCommentInput.setHint("Trả lời bình luận của #" + parentComment.getUserId() + ":");
                }
                edtCommentInput.requestFocus();
            }
        });

        btnSendComment.setOnClickListener(v -> sendCommentToServer());

        // CẤU HÌNH: Xử lý nút Tải xuống hình vuông cố định ở góc (Floating Button)
        android.widget.ImageButton btnFloatingDownloadReader = findViewById(R.id.btnFloatingDownloadReader);
        if (isOfflineMode) {
            if (btnFloatingDownloadReader != null) {
                btnFloatingDownloadReader.setVisibility(View.GONE); // Đang offline ẩn nút tải đi
            }
        } else {
            if (btnFloatingDownloadReader != null) {
                btnFloatingDownloadReader.setVisibility(View.VISIBLE);
                btnFloatingDownloadReader.setOnClickListener(v -> downloadCurrentChapterViaReader());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (layoutHeader != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
            HeaderUtils.loadUnreadNotificationCount(this, layoutHeader.findViewById(R.id.tvNotificationBadge));
        }
    }

    private void loadChapterContent(int chapterId) {
        if (isOfflineMode) {
            loadOfflineImages(chapterId);
        } else {
            loadImages(chapterId);
            loadChapterComments(chapterId);
            int userId = SharedPrefsManager.getUserId(this);
            if (userId != -1 && currentComicId != -1) {
                saveHistoryToServer(userId, currentComicId, chapterId);
            }

            // CHÈN THÊM ĐOẠN NÀY: Kiểm tra xem chương này đã tải chưa để ẩn/hiện nút tải nổi
            View btnFloatingDownload = findViewById(R.id.btnFloatingDownloadReader);
            if (btnFloatingDownload != null) {
                databaseExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(this);
                    // Truy vấn kiểm tra bản ghi chương trong Room DB
                    DownloadedChapter localChapter = db.offlineDao().getChapterById(chapterId);
                    runOnUiThread(() -> {
                        if (localChapter != null) {
                            btnFloatingDownload.setVisibility(View.GONE);  // Đã tải -> Ẩn nút
                        } else {
                            btnFloatingDownload.setVisibility(View.VISIBLE); // Chưa tải -> Hiện nút
                        }
                    });
                });
            }
        }
    }

    // ========== XỬ LÝ ĐỌC VÀ ĐIỀU HƯỚNG OFFLINE (ROOM DB) ==========

    private void loadOfflineImages(int chapterId) {
        if (imageAdapter != null) {
            imageAdapter.setImages(new ArrayList<>());
        }
        databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<DownloadedImage> localImages = db.offlineDao().getImagesByChapter(chapterId);
            List<ChapterImage> mappedList = new ArrayList<>();

            for (DownloadedImage localImg : localImages) {
                ChapterImage img = new ChapterImage();
                img.setImageUrl(localImg.getLocalFilePath()); // Gán đường dẫn local vào url để adapter xử lý
                mappedList.add(img);
            }

            runOnUiThread(() -> imageAdapter.setImages(mappedList));
        });
    }

    private void loadOfflineChaptersNavigation() {
        databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<DownloadedChapter> localChapters = db.offlineDao().getChaptersByComic(currentComicId);
            List<Chapter> mappedChapters = new ArrayList<>();

            for (DownloadedChapter localCh : localChapters) {
                Chapter ch = new Chapter();
                ch.setChapterId(localCh.getChapterId());
                ch.setComicId(localCh.getComicId());
                ch.setChapterNumber(localCh.getChapterNumber());
                ch.setTitle(localCh.getTitle());
                mappedChapters.add(ch);
            }

            allChaptersInComic = mappedChapters;
            setupSpinnerNavigation();
        });
    }

    private void setupSpinnerNavigation() {
        runOnUiThread(() -> {
            List<String> spinnerItems = new ArrayList<>();
            for (int i = 0; i < allChaptersInComic.size(); i++) {
                Chapter ch = allChaptersInComic.get(i);
                spinnerItems.add("Chương " + ch.getChapterNumber() + (ch.getTitle() != null ? ": " + ch.getTitle() : ""));
                if (ch.getChapterId() == currentChapterId) {
                    currentChapterIndex = i;
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(ReaderActivity.this,
                    android.R.layout.simple_spinner_item, spinnerItems);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerChapters.setAdapter(adapter);

            if (currentChapterIndex != -1) {
                isSpinnerFirstInit = true;
                spinnerChapters.setSelection(currentChapterIndex);
                updateButtonNavigationUI();
            }

            spinnerChapters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isSpinnerFirstInit) {
                        isSpinnerFirstInit = false;
                        return;
                    }
                    navigateToChapter(position);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void deleteCurrentChapterOffline() {
        databaseExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);

                // 1. Xóa file vật lý trong máy
                File chapterDirectory = new File(getFilesDir(), "truyen_downloads/comic_" + currentComicId + "/chapter_" + currentChapterId);
                if (chapterDirectory.exists() && chapterDirectory.isDirectory()) {
                    File[] images = chapterDirectory.listFiles();
                    if (images != null) {
                        for (File f : images) f.delete();
                    }
                    chapterDirectory.delete();
                }

                // 2. Xóa bản ghi trong Room DB
                db.offlineDao().deleteChapterById(currentChapterId);
                db.offlineDao().deleteImagesByChapter(currentChapterId);

                // 3. Đếm xem truyện còn chương nào không
                int countLeft = db.offlineDao().getChapterCountByComic(currentComicId);
                if (countLeft == 0) {
                    DownloadedComic targetComic = db.offlineDao().getComicById(currentComicId);
                    if (targetComic != null) {
                        if (targetComic.getLocalCoverPath() != null && !targetComic.getLocalCoverPath().isEmpty()) {
                            File coverFile = new File(targetComic.getLocalCoverPath());
                            if (coverFile.exists()) coverFile.delete();
                        }
                        db.offlineDao().deleteComic(targetComic);
                    }
                    File comicDir = new File(getFilesDir(), "truyen_downloads/comic_" + currentComicId);
                    if (comicDir.exists() && comicDir.isDirectory()) comicDir.delete();

                    runOnUiThread(() -> {
                        Toast.makeText(ReaderActivity.this, "Bộ truyện không còn chương nào nên đã được gỡ!", Toast.LENGTH_LONG).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ReaderActivity.this, "Đã xóa chương thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // THÊM MỚI: Luồng xử lý tải chương truyện trực tiếp từ màn hình đọc
    private void downloadCurrentChapterViaReader() {
        runOnUiThread(() -> Toast.makeText(this, "Đang tải xuống chương hiện tại...", Toast.LENGTH_SHORT).show());
        databaseExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                View btnFloatingDownload = findViewById(R.id.btnFloatingDownloadReader);
                if (btnFloatingDownload != null) {
                    btnFloatingDownload.setVisibility(View.GONE);
                }

                // 1. Đồng bộ thông tin bộ truyện cốt lõi nếu chưa tồn tại ở local
                if (db.offlineDao().getComicById(currentComicId) == null) {
                    Response<com.yuhbui.comicapp.data.model.ComicDetailResponse> response =
                            ApiClient.getApiService().getComicDetail(currentComicId, null).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        com.yuhbui.comicapp.data.model.Comic onlineComic = response.body().getComic();
                        String coverPath = com.yuhbui.comicapp.utils.DownloadUtils.downloadFile(this, onlineComic.getCoverImageUrl(), "comic_" + currentComicId, "cover.jpg");
                        DownloadedComic newComic = new DownloadedComic();
                        newComic.setComicId(currentComicId);
                        newComic.setTitle(onlineComic.getTitle());
                        newComic.setLocalCoverPath(coverPath != null ? coverPath : "");
                        newComic.setAuthor(onlineComic.getAuthor());
                        newComic.setDescription(onlineComic.getDescription());
                        newComic.setGenres(response.body().getGenres());
                        db.offlineDao().insertComic(newComic);
                    }
                }

                // 2. Định dạng tìm kiếm đối tượng chương hiện tại để lấy thông tin text
                Chapter currentCh = null;
                if (currentChapterIndex >= 0 && currentChapterIndex < allChaptersInComic.size()) {
                    currentCh = allChaptersInComic.get(currentChapterIndex);
                } else {
                    for (Chapter ch : allChaptersInComic) {
                        if (ch.getChapterId() == currentChapterId) {
                            currentCh = ch;
                            break;
                        }
                    }
                }
                if (currentCh == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Chưa tải xong danh sách chương từ máy chủ!", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 3. Tải danh sách ảnh trang truyện về bộ nhớ máy
                Response<List<ChapterImage>> imgResponse = ApiClient.getApiService().getImagesByChapterId(currentChapterId).execute();
                if (imgResponse.isSuccessful() && imgResponse.body() != null) {
                    List<DownloadedImage> localImagesList = new ArrayList<>();
                    List<ChapterImage> serverImages = imgResponse.body();

                    for (int i = 0; i < serverImages.size(); i++) {
                        String localImgPath = com.yuhbui.comicapp.utils.DownloadUtils.downloadFile(this, serverImages.get(i).getImageUrl(), "comic_" + currentComicId + "/chapter_" + currentChapterId, "page_" + i + ".jpg");
                        if (localImgPath != null) {
                            DownloadedImage dImg = new DownloadedImage();
                            dImg.setChapterId(currentChapterId);
                            dImg.setLocalFilePath(localImgPath);
                            dImg.setPosition(i);
                            localImagesList.add(dImg);
                        }
                    }

                    DownloadedChapter dCh = new DownloadedChapter();
                    dCh.setChapterId(currentChapterId);
                    dCh.setComicId(currentComicId);
                    dCh.setChapterNumber(currentCh.getChapterNumber());
                    dCh.setTitle(currentCh.getTitle());

                    db.offlineDao().insertChapter(dCh);
                    db.offlineDao().insertImages(localImagesList);
                    runOnUiThread(() -> Toast.makeText(this, "Đã tải chương offline thành công!", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ========== LUỒNG XỬ LÝ MẠNG ONLINE CŨ GIỮ NGUYÊN ==========

    private void loadAllChaptersNavigation(int comicId) {
        ApiClient.getApiService().getChaptersByComicId(comicId).enqueue(new Callback<List<Chapter>>() {
            @Override
            public void onResponse(Call<List<Chapter>> call, Response<List<Chapter>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allChaptersInComic = response.body();
                    setupSpinnerNavigation();
                }
            }
            @Override
            public void onFailure(Call<List<Chapter>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải danh sách chương điều hướng: " + t.getMessage());
            }
        });
    }

    private void navigateToChapter(int targetIndex) {
        if (targetIndex >= 0 && targetIndex < allChaptersInComic.size()) {
            Chapter targetChapter = allChaptersInComic.get(targetIndex);
            currentChapterId = targetChapter.getChapterId();
            currentChapterIndex = targetIndex;

            loadChapterContent(currentChapterId);

            isSpinnerFirstInit = true;
            spinnerChapters.setSelection(currentChapterIndex);
            updateButtonNavigationUI();
        }
    }

    private void updateButtonNavigationUI() {
        if (currentChapterIndex >= allChaptersInComic.size() - 1) {
            btnPrevChapter.setEnabled(false);
            btnPrevChapter.setAlpha(0.4f);
        } else {
            btnPrevChapter.setEnabled(true);
            btnPrevChapter.setAlpha(1.0f);
        }

        if (currentChapterIndex <= 0) {
            btnNextChapter.setEnabled(false);
            btnNextChapter.setAlpha(0.4f);
        } else {
            btnNextChapter.setEnabled(true);
            btnNextChapter.setAlpha(1.0f);
        }
    }

    private void loadImages(int chapterId) {
        if (imageAdapter != null) {
            imageAdapter.setImages(new ArrayList<>());
        }
        ApiClient.getApiService().getImagesByChapterId(chapterId).enqueue(new Callback<List<ChapterImage>>() {
            @Override
            public void onResponse(Call<List<ChapterImage>> call, Response<List<ChapterImage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    imageAdapter.setImages(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<ChapterImage>> call, Throwable t) {}
        });
    }

    private void loadChapterComments(int chapterId) {
        ApiClient.getApiService().getCommentsByChapter(chapterId).enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentAdapter.setComments(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {}
        });
    }

    private void sendCommentToServer() {
        String content = edtCommentInput.getText().toString().trim();
        int userId = SharedPrefsManager.getUserId(this);

        if (content.isEmpty()) return;
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để bình luận!", Toast.LENGTH_SHORT).show();
            return;
        }

        Comment newComment = new Comment();
        newComment.setUserId(userId);
        newComment.setChapterId(currentChapterId);
        newComment.setComicId(currentComicId);
        newComment.setContent(content);
        newComment.setParentCommentId(targetParentCommentId);

        ApiClient.getApiService().postComment(newComment).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful()) {
                    edtCommentInput.setText("");
                    edtCommentInput.setHint("Chia sẻ cảm xúc về chương này...");
                    if (targetParentCommentId != null) {
                        commentAdapter.resetRepliesCache(targetParentCommentId);
                    }
                    targetParentCommentId = null;
                    loadChapterComments(currentChapterId);
                } else if (response.code() == 403) {
                    Toast.makeText(ReaderActivity.this, "Tài khoản của bạn đang bị khóa chức năng bình luận!", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<Comment> call, Throwable t) {}
        });
    }

    private void saveHistoryToServer(int userId, int comicId, int chapterId) {
        com.yuhbui.comicapp.data.model.ReadingHistory history = new com.yuhbui.comicapp.data.model.ReadingHistory();
        history.setUserId(userId);
        history.setComicId(comicId);
        history.setLastChapterId(chapterId);
        ApiClient.getApiService().saveReadingHistory(history).enqueue(new Callback<com.yuhbui.comicapp.data.model.ReadingHistory>() {
            @Override public void onResponse(Call<com.yuhbui.comicapp.data.model.ReadingHistory> call, Response<com.yuhbui.comicapp.data.model.ReadingHistory> response) {}
            @Override public void onFailure(Call<com.yuhbui.comicapp.data.model.ReadingHistory> call, Throwable t) {}
        });
    }
}