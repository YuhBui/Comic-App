package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
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

    // Khai báo các biến cho phần bình luận theo ID mới
    private RecyclerView recyclerViewComments;
    private CommentAdapter commentAdapter;
    private EditText edtCommentInput;
    private Button btnSendComment;

    // --- CÁC BIẾN ĐIỀU HƯỚNG CHUYỂN CHƯƠNG MỚI THEO KHUNG ĐÔI ICON (FOOTER) ---
    private LinearLayout layoutReaderFooter;
    private ImageButton btnPrevChapter, btnNextChapter;
    private TextView tvChapterSelector;
    private List<Chapter> allChaptersInComic = new ArrayList<>();
    private int currentChapterIndex = -1;

    // --- CÁC BIẾN ĐIỀU HƯỚNG CỐ ĐỊNH (INLINE GIỮA TRUYỆN VÀ COMMENT) ---
    private LinearLayout layoutReaderInlineNav;
    private ImageButton btnInlinePrevChapter, btnInlineNextChapter;
    private TextView tvInlineChapterSelector;
    private RecyclerView rvInlineChaptersDropdown;
    private NestedScrollView scrollReaderContainer;

    // --- ĐÃ BỔ SUNG: CÁC BIẾN TOÀN CỤC ĐIỀU KHIỂN POPUP DANH SÁCH CHƯƠNG FOOTER NỔI NGƯỢC ---
    private View cvFooterChaptersPopup;
    private RecyclerView rvFooterChaptersPopup;

    private int currentChapterId = -1;
    private int currentComicId = -1;
    private Integer targetParentCommentId = null;

    // --- BIẾN PHỤC VỤ CHẾ ĐỘ OFFLINE ---
    private boolean isOfflineMode = false;
    private LinearLayout layoutChapterCommentsMainBox;
    private View cardFabDownload;
    private ImageView btnFabDownloadChapter;
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

        headerLogo.setText(android.text.Html.fromHtml("<font color='#D97707'>h</font><font color='#FFFFFF'>ay</font><font color='#D97707'>c</font><font color='#FFFFFF'>omic</font>", android.text.Html.FROM_HTML_MODE_COMPACT));

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
                } else if (cvFooterChaptersPopup != null && cvFooterChaptersPopup.getVisibility() == View.VISIBLE) {
                    cvFooterChaptersPopup.setVisibility(View.GONE);
                } else if (rvInlineChaptersDropdown != null && rvInlineChaptersDropdown.getVisibility() == View.VISIBLE) {
                    rvInlineChaptersDropdown.setVisibility(View.GONE);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        // 1. Ánh xạ phần đọc ảnh truyện theo ID mới: recyclerViewReaderPages
        recyclerViewImages = findViewById(R.id.recyclerViewReaderPages);
        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewImages.setHasFixedSize(false);
        recyclerViewImages.setItemViewCacheSize(30);
        recyclerViewImages.setNestedScrollingEnabled(false);

        imageAdapter = new ImageAdapter();
        recyclerViewImages.setAdapter(imageAdapter);

        // 2. Ánh xạ phần bình luận và các nút chức năng theo ID thiết kế mới
        recyclerViewComments = findViewById(R.id.recyclerViewChapterComments);
        edtCommentInput = findViewById(R.id.edtChapterCommentInput);
        btnSendComment = findViewById(R.id.btnChapterSendComment);
        layoutChapterCommentsMainBox = findViewById(R.id.layoutChapterCommentsMainBox);

        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter();
        recyclerViewComments.setAdapter(commentAdapter);

        // Ánh xạ cụm điều hướng Footer nổi dưới đáy
        layoutReaderFooter = findViewById(R.id.layoutReaderFooter);
        btnPrevChapter = findViewById(R.id.btnPrevChapter);
        btnNextChapter = findViewById(R.id.btnNextChapter);
        tvChapterSelector = findViewById(R.id.tvChapterSelector);

        // Ánh xạ cụm điều hướng Cố định (Inline Nav) mới tinh
        scrollReaderContainer = findViewById(R.id.scrollReaderContainer);
        layoutReaderInlineNav = findViewById(R.id.layoutReaderInlineNav);
        btnInlinePrevChapter = findViewById(R.id.btnInlinePrevChapter);
        btnInlineNextChapter = findViewById(R.id.btnInlineNextChapter);
        tvInlineChapterSelector = findViewById(R.id.tvInlineChapterSelector);
        rvInlineChaptersDropdown = findViewById(R.id.rvInlineChaptersDropdown);

        rvInlineChaptersDropdown.setLayoutManager(new LinearLayoutManager(this));

        // Ánh xạ cụm nút FAB ô vuông di động nổi
        cardFabDownload = findViewById(R.id.cardFabDownload);
        btnFabDownloadChapter = findViewById(R.id.btnFabDownloadChapter);

        // --- ĐÃ BỔ SUNG: ÁNH XẠ KHUNG CHỨA DANH SÁCH CHƯƠNG NỔI LÊN TRÊN CỦA FOOTER ---
        cvFooterChaptersPopup = findViewById(R.id.cvFooterChaptersPopup);
        rvFooterChaptersPopup = findViewById(R.id.rvFooterChaptersPopup);
        if (rvFooterChaptersPopup != null) {
            rvFooterChaptersPopup.setLayoutManager(new LinearLayoutManager(this));
        }

        // Lấy dữ liệu Intent truyền từ màn hình chi tiết sang
        currentChapterId = getIntent().getIntExtra("CHAPTER_ID", -1);
        currentComicId = getIntent().getIntExtra("COMIC_ID", -1);
        isOfflineMode = getIntent().getBooleanExtra("IS_OFFLINE_MODE", false);

        // THIẾT LẬP TRẠNG THÁI GIAO DIỆN PHÂN TÁCH OFFLINE
        if (isOfflineMode) {
            if (layoutChapterCommentsMainBox != null) {
                layoutChapterCommentsMainBox.setVisibility(View.GONE); // Ẩn khối comment offline
            }
            if (layoutReaderInlineNav != null) {
                layoutReaderInlineNav.setVisibility(View.GONE); // Ẩn luôn cụm inline khi offline cho tối giản
            }

            // Biến đổi nút FAB thành tính năng Xóa chương truyện khi đang offline
            if (cardFabDownload != null) {
                cardFabDownload.setVisibility(View.VISIBLE);
            }
            if (btnFabDownloadChapter != null) {
                btnFabDownloadChapter.setImageResource(android.R.drawable.ic_menu_delete);
                btnFabDownloadChapter.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E74C3C")));
                btnFabDownloadChapter.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(ReaderActivity.this)
                        .setTitle("Xóa chương truyện")
                        .setMessage("Bạn có muốn xóa chương đang đọc này khỏi máy không?")
                        .setPositiveButton("Xóa", (dialog, which) -> deleteCurrentChapterOffline())
                        .setNegativeButton("Hủy", null)
                        .show());
            }
        } else {
            // Chế độ Online: Gắn sự kiện click tải chương cho nút FAB ô vuông
            if (cardFabDownload != null) {
                cardFabDownload.setVisibility(View.VISIBLE);
            }
            if (btnFabDownloadChapter != null) {
                btnFabDownloadChapter.setOnClickListener(v -> downloadCurrentChapterViaReader());
            }

            // LOGIC LẮNG NGHE CUỘN MÀN HÌNH ĐỂ ẨN/HIỆN FOOTER THÔNG MINH
            if (scrollReaderContainer != null && layoutReaderInlineNav != null) {
                scrollReaderContainer.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    int[] location = new int[2];
                    layoutReaderInlineNav.getLocationOnScreen(location);
                    int inlineNavY = location[1];
                    int screenHeight = v.getContext().getResources().getDisplayMetrics().heightPixels;

                    // Nếu thanh Inline Nav đã lọt vào màn hình -> Ẩn thanh Footer dưới đáy và gập luôn popup nổi lên trên
                    if (inlineNavY < screenHeight - 50) {
                        layoutReaderFooter.setVisibility(View.GONE);
                        if (cvFooterChaptersPopup != null) {
                            cvFooterChaptersPopup.setVisibility(View.GONE);
                        }
                    } else {
                        layoutReaderFooter.setVisibility(View.VISIBLE);
                    }
                });
            }
        }

        // Tải nội dung ảnh chương truyện
        if (currentChapterId != -1) {
            loadChapterContent(currentChapterId);
        }

        // Tải danh sách chương gán vào thanh chọn điều hướng
        if (currentComicId != -1) {
            if (isOfflineMode) {
                loadOfflineChaptersNavigation();
            } else {
                loadAllChaptersNavigation(currentComicId);
            }
        }

        // Thiết lập sự kiện click cho các cụm nút bấm chuyển chương (Đồng bộ lẫn nhau)
        View.OnClickListener prevClick = v -> {
            if (currentChapterIndex < allChaptersInComic.size() - 1) {
                navigateToChapter(currentChapterIndex + 1);
            }
        };
        btnPrevChapter.setOnClickListener(prevClick);
        btnInlinePrevChapter.setOnClickListener(prevClick);

        View.OnClickListener nextClick = v -> {
            if (currentChapterIndex > 0) {
                navigateToChapter(currentChapterIndex - 1);
            }
        };
        btnNextChapter.setOnClickListener(nextClick);
        btnInlineNextChapter.setOnClickListener(nextClick);

        commentAdapter.setOnCommentClickListener(new CommentAdapter.OnCommentClickListener() {
            @Override
            public void onReplyClick(Comment parentComment) {
                targetParentCommentId = parentComment.getCommentId();
                if (parentComment.getUserDisplayName() != null && !parentComment.getUserDisplayName().isEmpty()) {
                    String tagText = "@" + parentComment.getUserDisplayName() + " ";
                    edtCommentInput.setText(tagText);
                    edtCommentInput.setSelection(tagText.length());
                    edtCommentInput.setHint("Đang trả lời...");
                } else {
                    edtCommentInput.setHint("Trả lời bình luận...");
                }
                edtCommentInput.requestFocus();
            }
        });

        btnSendComment.setOnClickListener(v -> sendCommentToServer());
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

            if (cardFabDownload != null) {
                databaseExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(this);
                    DownloadedChapter localChapter = db.offlineDao().getChapterById(chapterId);
                    runOnUiThread(() -> {
                        if (localChapter != null) {
                            cardFabDownload.setVisibility(View.GONE);
                        } else {
                            cardFabDownload.setVisibility(View.VISIBLE);
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
                img.setImageUrl(localImg.getLocalFilePath());
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
            setupChapterSelectorNavigation();
        });
    }

    private void setupChapterSelectorNavigation() {
        runOnUiThread(() -> {
            if (allChaptersInComic == null || allChaptersInComic.isEmpty()) return;

            for (int i = 0; i < allChaptersInComic.size(); i++) {
                if (allChaptersInComic.get(i).getChapterId() == currentChapterId) {
                    currentChapterIndex = i;
                    break;
                }
            }

            if (currentChapterIndex != -1) {
                Chapter currentCh = allChaptersInComic.get(currentChapterIndex);
                String chText = "Chương " + currentCh.getChapterNumber();
                tvChapterSelector.setText(chText);
                tvInlineChapterSelector.setText(chText);
                updateButtonNavigationUI();
            }

            // =========================================================================
            // 🛠️ ĐÃ NÂNG CẤP: SỬ DỤNG POPUPWINDOW ĐỂ BIẾN THÀNH Ô BỌC NỔI DROPDOWN CHUẨN FIGMA
            // =========================================================================
            View.OnClickListener showDropdownMenuAction = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Khởi tạo giao diện ô bọc menu từ file XML mới tạo
                    View popupView = LayoutInflater.from(ReaderActivity.this).inflate(R.layout.layout_chapter_dropdown, null);
                    RecyclerView rvDropdownChapters = popupView.findViewById(R.id.rvDropdownChapters);
                    rvDropdownChapters.setLayoutManager(new LinearLayoutManager(ReaderActivity.this));

                    // Đo kích thước thực tế của CardView bọc danh sách
                    popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    int popupWidth = v.getWidth(); // Cho chiều rộng khớp hoàn toàn với độ dài nút bấm
                    int popupHeight = (int) (280 * getResources().getDisplayMetrics().density); // Khóa cứng chiều cao tối đa 280dp chống tràn màn hình

                    // Thiết lập cửa sổ nổi đè lên trên layer giao diện chính
                    final PopupWindow popupWindow = new PopupWindow(popupView, popupWidth, popupHeight, true);

                    // Cơ chế chạm vùng trống bên ngoài tự đóng (Bỏ hoàn toàn nút Hủy rườm rà)
                    popupWindow.setOutsideTouchable(true);
                    popupWindow.setFocusable(true);
                    popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

                    // Thiết lập dữ liệu và bắt sự kiện click chuyển chương
                    InlineChapterDropdownAdapter dropdownAdapter = new InlineChapterDropdownAdapter(allChaptersInComic, currentChapterIndex, index -> {
                        navigateToChapter(index);
                        popupWindow.dismiss(); // Tự gập menu lại ngay khi chọn chương thành công
                    });
                    rvDropdownChapters.setAdapter(dropdownAdapter);

                    // TOÁN TỬ ĐỊNH VỊ HƯỚNG HIỂN THỊ THÔNG MINH CHO TỪNG DẠNG NÚT
                    if (v.getId() == R.id.tvChapterSelector) {
                        // 1. ĐỐI VỚI FOOTER: Ép tọa độ Y nhảy ngược lên PHÍA TRÊN thanh Footer điều hướng
                        popupWindow.showAsDropDown(v, 0, -(popupHeight + v.getHeight() + 8)); // Lệch lên cách 8px cho thoáng
                    } else if (v.getId() == R.id.tvInlineChapterSelector) {
                        // 2. ĐỐI VỚI CỐ ĐỊNH (INLINE): Thả trôi tự nhiên ngay PHÍA DƯỚI nút bấm (Vẽ đè lên, KHÔNG đẩy dịch bình luận)
                        popupWindow.showAsDropDown(v, 0, 4);
                    }
                }
            };

            // Gán đồng bộ hành động mở Dropdown Card cho cả 2 view điều hướng
            tvChapterSelector.setOnClickListener(showDropdownMenuAction);
            tvInlineChapterSelector.setOnClickListener(showDropdownMenuAction);
        });
    }

    private void deleteCurrentChapterOffline() {
        databaseExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);

                File chapterDirectory = new File(getFilesDir(), "truyen_downloads/comic_" + currentComicId + "/chapter_" + currentChapterId);
                if (chapterDirectory.exists() && chapterDirectory.isDirectory()) {
                    File[] images = chapterDirectory.listFiles();
                    if (images != null) {
                        for (File f : images) f.delete();
                    }
                    chapterDirectory.delete();
                }

                db.offlineDao().deleteChapterById(currentChapterId);
                db.offlineDao().deleteImagesByChapter(currentChapterId);

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

    private void downloadCurrentChapterViaReader() {
        runOnUiThread(() -> Toast.makeText(this, "Đang tải xuống chương hiện tại...", Toast.LENGTH_SHORT).show());
        databaseExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                if (cardFabDownload != null) {
                    cardFabDownload.setVisibility(View.GONE);
                }

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
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Chưa tải xong danh sách chương!", Toast.LENGTH_SHORT).show();
                        if (cardFabDownload != null) cardFabDownload.setVisibility(View.VISIBLE);
                    });
                    return;
                }

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
                } else {
                    runOnUiThread(() -> {
                        if (cardFabDownload != null) cardFabDownload.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (cardFabDownload != null) cardFabDownload.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    // ========== LUỒNG XỬ LÝ MẠNG ONLINE ==========

    private void loadAllChaptersNavigation(int comicId) {
        ApiClient.getApiService().getChaptersByComicId(comicId).enqueue(new Callback<List<Chapter>>() {
            @Override
            public void onResponse(Call<List<Chapter>> call, Response<List<Chapter>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allChaptersInComic = response.body();
                    setupChapterSelectorNavigation();
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

            Chapter currentCh = allChaptersInComic.get(currentChapterIndex);
            String chText = "Chương " + currentCh.getChapterNumber();
            tvChapterSelector.setText(chText);
            tvInlineChapterSelector.setText(chText);
            updateButtonNavigationUI();

            // Tự động gập ẩn danh sách chương ở cả 2 phân hệ sau khi chuyển chương thành công
            if (rvInlineChaptersDropdown != null) {
                rvInlineChaptersDropdown.setVisibility(View.GONE);
            }
            if (cvFooterChaptersPopup != null) {
                cvFooterChaptersPopup.setVisibility(View.GONE);
            }
        }
    }

    private void updateButtonNavigationUI() {
        boolean hasPrev = currentChapterIndex < allChaptersInComic.size() - 1;
        btnPrevChapter.setEnabled(hasPrev);
        btnPrevChapter.setAlpha(hasPrev ? 1.0f : 0.4f);
        if (btnInlinePrevChapter != null) {
            btnInlinePrevChapter.setEnabled(hasPrev);
            btnInlinePrevChapter.setAlpha(hasPrev ? 1.0f : 0.4f);
        }

        boolean hasNext = currentChapterIndex > 0;
        btnNextChapter.setEnabled(hasNext);
        btnNextChapter.setAlpha(hasNext ? 1.0f : 0.4f);
        if (btnInlineNextChapter != null) {
            btnInlineNextChapter.setEnabled(hasNext);
            btnInlineNextChapter.setAlpha(hasNext ? 1.0f : 0.4f);
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
                    edtCommentInput.setHint("Viết bình luận...");

                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null && getCurrentFocus() != null) {
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }

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

    // =========================================================================
    // ⚙️ LỚP ADAPTER NỘI BỘ: Quản lý danh sách thả xuống mượt mà hợp tone Manga Noir
    // =========================================================================
    private static class InlineChapterDropdownAdapter extends RecyclerView.Adapter<InlineChapterDropdownAdapter.ViewHolder> {
        private final List<Chapter> chapters;
        private final int selectedIndex;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(int index);
        }

        InlineChapterDropdownAdapter(List<Chapter> chapters, int selectedIndex, OnItemClickListener listener) {
            this.chapters = chapters;
            this.selectedIndex = selectedIndex;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Chapter ch = chapters.get(position);
            String titleText = "Chương " + ch.getChapterNumber() + (ch.getTitle() != null && !ch.getTitle().isEmpty() ? ": " + ch.getTitle() : "");
            holder.textView.setText(titleText);

            holder.textView.setTextSize(14);
            holder.itemView.setPadding(32, 24, 32, 24);

            if (position == selectedIndex) {
                holder.textView.setTextColor(Color.parseColor("#FFB77D"));
                holder.textView.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                holder.textView.setTextColor(Color.parseColor("#DBC2B0"));
            }

            holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
        }

        @Override
        public int getItemCount() {
            return chapters != null ? chapters.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}