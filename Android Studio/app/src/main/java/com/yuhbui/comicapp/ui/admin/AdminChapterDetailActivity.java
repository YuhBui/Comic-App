package com.yuhbui.comicapp.ui.admin;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.ui.adapters.AdminChapterImageAdapter;
import com.yuhbui.comicapp.ui.adapters.AdminCommentAdapter;
import com.yuhbui.comicapp.utils.HeaderUtils;
import com.yuhbui.comicapp.utils.MenuUtils;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminChapterDetailActivity extends AppCompatActivity implements AdminCommentAdapter.OnCommentAdminActionListener {

    private DrawerLayout drawerLayout;

    private int chapterId;
    private int comicId;
    private RecyclerView rvPages;
    private AdminChapterImageAdapter adapter;
    private Button btnUploadPage;
    private NestedScrollView nestedScrollView;
    private RecyclerView rvComments;
    private AdminCommentAdapter commentAdapter;
    private EditText edtCommentInput;
    private ImageButton btnSendComment;

    // --- CÁC BIẾN MỚI NÂNG CẤP ĐỒNG BỘ THEO CHAPTER ADD ---
    private EditText edtChapterNumber, edtChapterTitle;
    private Button btnSaveChanges, btnCancelChanges;
    private List<Map<String, Object>> originalPageList = new ArrayList<>(); // Bản sao gốc dự phòng hoàn tác
    private List<Integer> pagesToDeleteLocal = new ArrayList<>(); // Lưu ID các trang đánh dấu xóa chờ bấm Lưu

    // --- CÁC BIẾN ĐIỀU HƯỚNG CHUYỂN CHƯƠNG ---
    private LinearLayout layoutReaderFooter;
    private LinearLayout layoutReaderInlineNav;
    private ImageButton btnPrevChapter, btnNextChapter;
    private TextView tvChapterSelector;
    private ImageButton btnInlinePrevChapter, btnInlineNextChapter;
    private TextView tvInlineChapterSelector;
    private List<Map<String, Object>> allChaptersInComic = new ArrayList<>();
    private int currentChapterIndex = -1;

    // Mảng lưu danh sách dữ liệu trang truyện phục vụ kéo thả reorder vị trí cục bộ
    private List<Map<String, Object>> pageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chapter_detail);

        chapterId = getIntent().getIntExtra("CHAPTER_ID", -1);
        comicId = getIntent().getIntExtra("COMIC_ID", -1);

        drawerLayout = findViewById(R.id.drawerLayout);
        setupAdminHeaderView();

        rvPages = findViewById(R.id.rvAdminChapterPages);
        btnUploadPage = findViewById(R.id.btnAdminUploadPage);
        nestedScrollView = findViewById(R.id.nestedScrollViewChapterDetail);

        // Ánh xạ thành phần mới nâng cấp
        edtChapterNumber = findViewById(R.id.edtChapterDetailNumber);
        edtChapterTitle = findViewById(R.id.edtChapterDetailTitle);
        btnSaveChanges = findViewById(R.id.btnSaveChapterChanges);
        btnCancelChanges = findViewById(R.id.btnCancelChapterChanges);

        rvComments = findViewById(R.id.rvAdminChapterComments);
        edtCommentInput = findViewById(R.id.edtAdminChapterCommentInput);
        btnSendComment = findViewById(R.id.btnAdminChapterSendComment);

        layoutReaderFooter = findViewById(R.id.layoutReaderFooter);
        layoutReaderInlineNav = findViewById(R.id.layoutReaderInlineNav);
        btnPrevChapter = findViewById(R.id.btnPrevChapter);
        btnNextChapter = findViewById(R.id.btnNextChapter);
        tvChapterSelector = findViewById(R.id.tvChapterSelector);

        btnInlinePrevChapter = findViewById(R.id.btnInlinePrevChapter);
        btnInlineNextChapter = findViewById(R.id.btnInlineNextChapter);
        tvInlineChapterSelector = findViewById(R.id.tvInlineChapterSelector);

        rvPages.setLayoutManager(new LinearLayoutManager(this));

        // ĐÃ SỬA: Khi bấm xóa trang truyện, chỉ gỡ khỏi danh sách hiển thị và lưu ID vào hàng chờ xóa
        adapter = new AdminChapterImageAdapter((imageId, position) -> {
            if (position >= 0 && position < pageList.size()) {
                Map<String, Object> removedPage = pageList.remove(position);
                if (removedPage.get("imageId") != null) {
                    int imgId = ((Double) removedPage.get("imageId")).intValue();
                    if (imgId > 0) {
                        pagesToDeleteLocal.add(imgId); // Đưa vào hàng chờ xóa thật trên server
                    }
                }
                refreshLocalPagesUI();
            }
        });
        rvPages.setAdapter(adapter);

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new AdminCommentAdapter(this);
        rvComments.setAdapter(commentAdapter);

        // Cấu hình Kéo thả đổi vị trí cục bộ không gửi Request ngay lập tức
        ItemTouchHelper.SimpleCallback touchHelperCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder src, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = src.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                Collections.swap(pageList, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
                    View itemView = viewHolder.itemView;
                    int[] itemLocation = new int[2];
                    itemView.getLocationOnScreen(itemLocation);
                    int itemY = itemLocation[1];

                    int[] scrollLocation = new int[2];
                    nestedScrollView.getLocationOnScreen(scrollLocation);
                    int scrollViewTop = scrollLocation[1];
                    int scrollViewBottom = scrollViewTop + nestedScrollView.getHeight();

                    int threshold = 250;
                    int scrollSpeed = 25;

                    if (itemY < scrollViewTop + threshold) {
                        nestedScrollView.smoothScrollBy(0, -scrollSpeed);
                    } else if (itemY + itemView.getHeight() > scrollViewBottom - threshold) {
                        nestedScrollView.smoothScrollBy(0, scrollSpeed);
                    }
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // ĐÃ SỬA: Xóa bỏ hàm gọi API reorder ngay lập tức, chỉ tính toán lại số trang hiển thị
                refreshLocalPagesUI();
            }
        };
        new ItemTouchHelper(touchHelperCallback).attachToRecyclerView(rvPages);

        btnUploadPage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Chọn các trang truyện muốn thêm"), 202);
        });

        btnSendComment.setOnClickListener(v -> sendChapterCommentToServer());

        // Xử lý sự kiện 2 nút mới: LƯU và HỦY BỎ
        btnCancelChanges.setOnClickListener(v -> performCancelChangesAction());
        btnSaveChanges.setOnClickListener(v -> performSaveChangesPipelineToServer());

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

        ImageView btnBackChapterDetail = findViewById(R.id.btnBackChapterDetail);
        if (btnBackChapterDetail != null) {
            btnBackChapterDetail.setOnClickListener(v -> finish());
        }

        if (comicId != -1) {
            loadChaptersNavigation();
        }

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

        if (nestedScrollView != null && layoutReaderInlineNav != null) {
            nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                int[] location = new int[2];
                layoutReaderInlineNav.getLocationOnScreen(location);
                int inlineNavY = location[1];
                int screenHeight = v.getContext().getResources().getDisplayMetrics().heightPixels;

                if (inlineNavY < screenHeight - 50) {
                    layoutReaderFooter.setVisibility(View.GONE);
                } else {
                    layoutReaderFooter.setVisibility(View.VISIBLE);
                }
            });
        }

        // THÊM ĐOẠN NÀY VÀO TRONG HÀM onCreate()
        android.text.TextWatcher textWatcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveButtonState();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        edtChapterNumber.addTextChangedListener(textWatcher);
        edtChapterTitle.addTextChangedListener(textWatcher);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChapterPages();
        loadChapterComments();
        View layoutHeader = findViewById(R.id.layoutHeaderChapterDetail);
        if (layoutHeader != null && layoutHeader.findViewById(R.id.headerAvatar) != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
        }
    }

    private void setupAdminHeaderView() {
        View layoutHeader = findViewById(R.id.layoutHeaderChapterDetail);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);

        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);
        MenuUtils.setupAdminSideMenu(this, drawerLayout, layoutHeader.findViewById(R.id.headerMenu));

        if (layoutHeader.findViewById(R.id.headerSearch) != null) {
            layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        }
        if (layoutHeader.findViewById(R.id.headerNotification) != null) {
            layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        }

        if (headerLogo != null) {
            headerLogo.setText("COMIC APP");
            headerLogo.setTextColor(Color.parseColor("#E74C3C"));
            headerLogo.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }
    }

    private void loadChaptersNavigation() {
        ApiClient.getApiService().adminGetChapters(comicId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allChaptersInComic = response.body();
                    setupChapterSelectorNavigation();
                }
            }
            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    private void setupChapterSelectorNavigation() {
        if (allChaptersInComic == null || allChaptersInComic.isEmpty()) return;

        for (int i = 0; i < allChaptersInComic.size(); i++) {
            Map<String, Object> ch = allChaptersInComic.get(i);
            Number chIdNum = (Number) ch.get("chapterId");
            int chId = chIdNum != null ? chIdNum.intValue() : -1;
            if (chId == chapterId) {
                currentChapterIndex = i;
                break;
            }
        }

        if (currentChapterIndex != -1) {
            Map<String, Object> currentCh = allChaptersInComic.get(currentChapterIndex);
            String chText = "Chương " + currentCh.get("chapterNumber");
            tvChapterSelector.setText(chText);
            tvInlineChapterSelector.setText(chText);

            // NẠP DỮ LIỆU BAN ĐẦU VÀO 2 Ô NHẬP LIỆU CHỈNH SỬA
            edtChapterNumber.setText(String.valueOf(currentCh.get("chapterNumber")));
            edtChapterTitle.setText(currentCh.get("title") != null ? (String) currentCh.get("title") : "");

            updateButtonNavigationUI();
        }

        View.OnClickListener showDropdownMenuAction = v -> {
            View popupView = LayoutInflater.from(AdminChapterDetailActivity.this).inflate(R.layout.layout_chapter_dropdown, null);
            RecyclerView rvDropdownChapters = popupView.findViewById(R.id.rvDropdownChapters);
            rvDropdownChapters.setLayoutManager(new LinearLayoutManager(AdminChapterDetailActivity.this));

            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int popupWidth = v.getWidth();
            int popupHeight = (int) (280 * getResources().getDisplayMetrics().density);

            final PopupWindow popupWindow = new PopupWindow(popupView, popupWidth, popupHeight, true);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setFocusable(true);
            popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

            InlineChapterDropdownAdapter dropdownAdapter = new InlineChapterDropdownAdapter(allChaptersInComic, currentChapterIndex, index -> {
                navigateToChapter(index);
                popupWindow.dismiss();
            });
            rvDropdownChapters.setAdapter(dropdownAdapter);

            if (v.getId() == R.id.tvChapterSelector) {
                popupWindow.showAsDropDown(v, 0, -(popupHeight + v.getHeight() + 8));
            } else if (v.getId() == R.id.tvInlineChapterSelector) {
                popupWindow.showAsDropDown(v, 0, 4);
            }
        };

        tvChapterSelector.setOnClickListener(showDropdownMenuAction);
        tvInlineChapterSelector.setOnClickListener(showDropdownMenuAction);
    }

    private void navigateToChapter(int targetIndex) {
        if (targetIndex >= 0 && targetIndex < allChaptersInComic.size()) {
            Map<String, Object> targetChapter = allChaptersInComic.get(targetIndex);
            Number chIdNum = (Number) targetChapter.get("chapterId");
            chapterId = chIdNum != null ? chIdNum.intValue() : -1;
            currentChapterIndex = targetIndex;

            pagesToDeleteLocal.clear(); // Xóa sạch bộ nhớ tạm chương cũ
            loadChapterPages();
            loadChapterComments();

            Map<String, Object> currentCh = allChaptersInComic.get(currentChapterIndex);
            String chText = "Chương " + currentCh.get("chapterNumber");
            tvChapterSelector.setText(chText);
            tvInlineChapterSelector.setText(chText);

            edtChapterNumber.setText(String.valueOf(currentCh.get("chapterNumber")));
            edtChapterTitle.setText(currentCh.get("title") != null ? (String) currentCh.get("title") : "");

            updateButtonNavigationUI();
        }
    }

    private void updateButtonNavigationUI() {
        boolean hasPrev = currentChapterIndex < allChaptersInComic.size() - 1;
        btnPrevChapter.setEnabled(hasPrev);
        btnPrevChapter.setAlpha(hasPrev ? 1.0f : 0.4f);
        btnInlinePrevChapter.setEnabled(hasPrev);
        btnInlinePrevChapter.setAlpha(hasPrev ? 1.0f : 0.4f);

        boolean hasNext = currentChapterIndex > 0;
        btnNextChapter.setEnabled(hasNext);
        btnNextChapter.setAlpha(hasNext ? 1.0f : 0.4f);
        btnInlineNextChapter.setEnabled(hasNext);
        btnInlineNextChapter.setAlpha(hasNext ? 1.0f : 0.4f);
    }

    private void loadChapterPages() {
        pageList.clear();
        originalPageList.clear();
        pagesToDeleteLocal.clear();
        adapter.setData(pageList);

        ApiClient.getApiService().adminGetChapterPages(chapterId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    pageList = response.body();

                    // Tạo một bản lưu gốc phục vụ hoàn tác
                    for (Map<String, Object> p : pageList) {
                        originalPageList.add(new HashMap<>(p));
                    }
                    refreshLocalPagesUI();
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    // Hàm bổ trợ sắp lại số trang hiển thị thời gian thực
    private void refreshLocalPagesUI() {
        for (int i = 0; i < pageList.size(); i++) {
            pageList.get(i).put("pageNumber", (double) (i + 1));
        }
        adapter.setData(pageList);
    }

    private void loadChapterComments() {
        commentAdapter.setData(new ArrayList<>());
        ApiClient.getApiService().adminGetChapterComments(chapterId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentAdapter.setData(response.body());
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    private void sendChapterCommentToServer() {
        String content = edtCommentInput.getText().toString().trim();
        if (content.isEmpty()) return;

        Comment adminComment = new Comment();
        adminComment.setChapterId(chapterId);
        adminComment.setUserId(SharedPrefsManager.getUserId(this));
        adminComment.setContent(content);

        ApiClient.getApiService().postComment(adminComment).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful()) {
                    edtCommentInput.setText("");

                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null && getCurrentFocus() != null) {
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }

                    Toast.makeText(AdminChapterDetailActivity.this, "Đã đăng bình luận chương thành công!", Toast.LENGTH_SHORT).show();
                    loadChapterComments();
                } else if (response.code() == 403) {
                    Toast.makeText(AdminChapterDetailActivity.this, "Tài khoản đang bị khóa chức năng bình luận!", Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<Comment> call, Throwable t) {}
        });
    }

    // === HÀM HỦY BỎ THAY ĐỔI CỤC BỘ ===
    private void performCancelChangesAction() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy bỏ thay đổi")
                .setMessage("Bạn có chắc chắn muốn hủy bỏ mọi sửa đổi chưa lưu trên chương này không?")
                .setPositiveButton("Hủy bỏ hết", (dialog, which) -> {
                    // Khôi phục thông tin chương cũ
                    if (currentChapterIndex != -1 && currentChapterIndex < allChaptersInComic.size()) {
                        Map<String, Object> currentCh = allChaptersInComic.get(currentChapterIndex);
                        edtChapterNumber.setText(String.valueOf(currentCh.get("chapterNumber")));
                        edtChapterTitle.setText(currentCh.get("title") != null ? (String) currentCh.get("title") : "");
                    }

                    // Khôi phục danh sách ảnh cũ
                    pagesToDeleteLocal.clear();
                    pageList.clear();
                    for (Map<String, Object> op : originalPageList) {
                        pageList.add(new HashMap<>(op));
                    }
                    refreshLocalPagesUI();
                    Toast.makeText(this, "Đã khôi phục dữ liệu gốc!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Tiếp tục sửa", null)
                .show();
    }

    // === CHUỖI TIẾN TRÌNH LƯU TẤT CẢ THAY ĐỔI LÊN SERVER (TRANSACTION PIPELINE) ===
    private void performSaveChangesPipelineToServer() {
        String numStr = edtChapterNumber.getText().toString().trim();
        String titleStr = edtChapterTitle.getText().toString().trim();

        if (numStr.isEmpty()) {
            Toast.makeText(this, "Số thứ tự chương không được bỏ trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("⏳ ĐANG LƯU THAY ĐỔI...");

        double newNum = Double.parseDouble(numStr);

        // BƯỚC 1: Cập nhật số chương và tiêu đề chương
        ApiClient.getApiService().adminUpdateChapter(chapterId, newNum, titleStr).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // Thành công bước 1 -> Chuyển sang BƯỚC 2: Xóa ảnh đã đánh dấu
                executePageDeletionsPipeline(0);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                resetSaveButtonUI();
                Toast.makeText(AdminChapterDetailActivity.this, "Lỗi cập nhật thông tin chương!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executePageDeletionsPipeline(int index) {
        if (index >= pagesToDeleteLocal.size()) {
            // Xóa xong -> Chuyển sang BƯỚC 3: Tải các ảnh mới thêm tạm lên server
            executePageAdditionsPipeline(0);
            return;
        }

        int imgId = pagesToDeleteLocal.get(index);
        ApiClient.getApiService().adminDeleteChapterPage(imgId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                executePageDeletionsPipeline(index + 1);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                executePageDeletionsPipeline(index + 1); // Tiếp tục xử lý kể cả lỗi
            }
        });
    }

    private void executePageAdditionsPipeline(int index) {
        // Lọc tìm ra các ảnh mới thêm cục bộ (có ID tạm = -1.0)
        List<Uri> newLocalUris = new ArrayList<>();
        for (Map<String, Object> p : pageList) {
            double idVal = p.get("imageId") != null ? (Double) p.get("imageId") : -1.0;
            if (idVal == -1.0 && p.get("localUri") != null) {
                newLocalUris.add((Uri) p.get("localUri"));
            }
        }

        if (index >= newLocalUris.size()) {
            // Thêm xong -> Chuyển sang BƯỚC 4: Đồng bộ hóa toàn bộ vị trí kéo thả trật tự ảnh
            executeFinalReorderSyncPipeline();
            return;
        }

        Uri uri = newLocalUris.get(index);
        File file = getFileFromUri(uri);
        if (file == null) {
            executePageAdditionsPipeline(index + 1);
            return;
        }

        RequestBody rf = RequestBody.create(MediaType.parse(getContentResolver().getType(uri)), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), rf);

        ApiClient.getApiService().adminUploadChapterPage(chapterId, part).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                executePageAdditionsPipeline(index + 1);
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                executePageAdditionsPipeline(index + 1);
            }
        });
    }

    private void executeFinalReorderSyncPipeline() {
        // Lấy danh sách ảnh mới nhất từ Server để lấy ID thật của các ảnh vừa được thêm vào
        ApiClient.getApiService().adminGetChapterPages(chapterId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> finalServerPages = response.body();

                    // Tìm ra các ID mới được sinh ra của các ảnh mới thêm
                    List<Integer> newServerIds = new ArrayList<>();
                    for (Map<String, Object> sp : finalServerPages) {
                        int sId = ((Double) sp.get("imageId")).intValue();
                        boolean isOld = false;
                        for (Map<String, Object> op : originalPageList) {
                            if (((Double) op.get("imageId")).intValue() == sId) {
                                isOld = true;
                                break;
                            }
                        }
                        if (!isOld) {
                            newServerIds.add(sId);
                        }
                    }

                    // Khớp nối lại với trật tự kéo thả hiển thị hiện tại trên màn hình
                    List<Integer> finalOrderedIds = new ArrayList<>();
                    int newImgIdx = 0;

                    for (Map<String, Object> localPage : pageList) {
                        double idVal = localPage.get("imageId") != null ? (Double) localPage.get("imageId") : -1.0;
                        int id = (int) idVal;
                        if (id > 0) {
                            finalOrderedIds.add(id);
                        } else {
                            if (newImgIdx < newServerIds.size()) {
                                finalOrderedIds.add(newServerIds.get(newImgIdx));
                                newImgIdx++;
                            }
                        }
                    }

                    // Gửi chuỗi danh sách ID trật tự kéo thả cuối cùng lên Server để sắp xếp chốt hạ
                    if (!finalOrderedIds.isEmpty()) {
                        ApiClient.getApiService().adminReorderPages(finalOrderedIds).enqueue(new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                finishSuccessPipeline();
                            }
                            @Override
                            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                finishSuccessPipeline();
                            }
                        });
                    } else {
                        finishSuccessPipeline();
                    }
                } else {
                    finishSuccessPipeline();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                finishSuccessPipeline();
            }
        });
    }

    private void finishSuccessPipeline() {
        resetSaveButtonUI();
        Toast.makeText(this, "Đã cập nhật toàn bộ thay đổi chương truyện thành công!", Toast.LENGTH_SHORT).show();
        pagesToDeleteLocal.clear();
        loadChapterPages(); // Đồng bộ lại bộ nhớ đệm chuẩn từ server
        loadChaptersNavigation(); // Cập nhật số chương trên thanh Dropdown chọn nhanh
    }

    private void resetSaveButtonUI() {
        btnSaveChanges.setEnabled(true);
        btnSaveChanges.setText("💾 LƯU THAY ĐỔI");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 202 && resultCode == RESULT_OK && data != null) {
            List<Uri> selectedUris = new ArrayList<>();

            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    selectedUris.add(clipData.getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedUris.add(data.getData());
            }

            // ĐÃ SỬA: Không gửi API upload ngay lập tức, chỉ add tạm cấu trúc dữ liệu cục bộ vào mảng hiển thị dọc
            for (Uri uri : selectedUris) {
                Map<String, Object> tempPageMap = new HashMap<>();
                tempPageMap.put("imageId", -1.0); // Flag đánh dấu ảnh mới thêm cục bộ
                tempPageMap.put("imageUrl", uri.toString());
                tempPageMap.put("localUri", uri); // Lưu trữ Uri thật để nạp dữ liệu Multipart khi bấm Lưu
                pageList.add(tempPageMap);
            }
            refreshLocalPagesUI();
        }
    }

    private File getFileFromUri(Uri uri) {
        try {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(uri));
            File tempFile = new File(getCacheDir(), "page_ch_" + System.currentTimeMillis() + "." + (ext != null ? ext : "jpg"));
            try (InputStream is = getContentResolver().openInputStream(uri); OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buf = new byte[4096]; int len;
                while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
                os.flush();
            }
            return tempFile;
        } catch (Exception e) { return null; }
    }

    @Override
    public void onReply(Map<String, Object> comment) {
        String username = (String) comment.get("username");
        if (username != null) {
            edtCommentInput.setText("@" + username + " ");
            edtCommentInput.requestFocus();
            edtCommentInput.setSelection(edtCommentInput.getText().length());
        }
    }

    @Override
    public void onShowReports(int commentId) {
        ApiClient.getApiService().adminGetCommentReports(commentId).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> reports = response.body();
                    if (reports.isEmpty()) {
                        Toast.makeText(AdminChapterDetailActivity.this, "Bình luận chương này chưa bị báo cáo!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CharSequence[] items = reports.toArray(new CharSequence[0]);
                    new AlertDialog.Builder(AdminChapterDetailActivity.this)
                            .setTitle("Nội dung người dùng báo cáo (" + reports.size() + ")")
                            .setItems(items, null)
                            .setPositiveButton("Đóng", null).show();
                }
            }
            @Override public void onFailure(Call<List<String>> call, Throwable t) {}
        });
    }

    @Override
    public void onDelete(int commentId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bình luận chương")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn bình luận này không?")
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> {
                    ApiClient.getApiService().adminDeleteComment(commentId).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminChapterDetailActivity.this, "Đã xóa bình luận khỏi hệ thống!", Toast.LENGTH_SHORT).show();
                                loadChapterComments();
                            }
                        }
                        @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Hủy", null).show();
    }

    @Override
    public void onInteract(int commentId, int type, int position) {
        int currentUserId = SharedPrefsManager.getUserId(this);
        if (currentUserId == -1) return;
        ApiClient.getApiService().interactWithComment(commentId, currentUserId, type).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful()) { loadChapterComments(); }
            }
            @Override public void onFailure(Call<Comment> call, Throwable t) {}
        });
    }

    private static class InlineChapterDropdownAdapter extends RecyclerView.Adapter<InlineChapterDropdownAdapter.ViewHolder> {
        private final List<Map<String, Object>> chapters;
        private final int selectedIndex;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(int index);
        }

        InlineChapterDropdownAdapter(List<Map<String, Object>> chapters, int selectedIndex, OnItemClickListener listener) {
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
            Map<String, Object> ch = chapters.get(position);
            Object chNumObj = ch.get("chapterNumber");
            String title = (String) ch.get("title");
            String titleText = "Chương " + chNumObj + (title != null && !title.isEmpty() ? ": " + title : "");
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

    // THÊM VÀO CUỐI CLASS ADMINCHAPTERDETAILACTIVITY
    private boolean hasChanges() {
        if (currentChapterIndex == -1 || allChaptersInComic.isEmpty()) return false;

        // 1. Lấy thông tin gốc từ bộ nhớ đệm danh sách chương
        Map<String, Object> currentCh = allChaptersInComic.get(currentChapterIndex);
        String origNum = String.valueOf(currentCh.get("chapterNumber")).trim();
        String origTitle = currentCh.get("title") != null ? ((String) currentCh.get("title")).trim() : "";

        // 2. Lấy thông tin hiện tại trên các ô EditText
        String currentNum = edtChapterNumber.getText().toString().trim();
        String currentTitle = edtChapterTitle.getText().toString().trim();

        // Kiểm tra nếu thông tin chữ có thay đổi
        if (!currentNum.equals(origNum)) return true;
        if (!currentTitle.equals(origTitle)) return true;

        // 3. Kiểm tra số lượng trang truyện ảnh
        if (pageList.size() != originalPageList.size()) return true;

        // 4. Kiểm tra sâu trật tự sắp xếp hoặc sự xuất hiện của ảnh mới thêm tạm (-1.0)
        for (int i = 0; i < pageList.size(); i++) {
            Map<String, Object> currPage = pageList.get(i);
            Map<String, Object> origPage = originalPageList.get(i);

            double currId = currPage.get("imageId") != null ? (Double) currPage.get("imageId") : -1.0;
            double origId = origPage.get("imageId") != null ? (Double) origPage.get("imageId") : -1.0;

            if (currId != origId) return true;
        }

        return false;
    }

    private void updateSaveButtonState() {
        boolean changed = hasChanges();
        btnSaveChanges.setEnabled(changed);
        // Thay đổi độ mờ của nút để hiển thị trực quan trạng thái đóng/mở khóa
        btnSaveChanges.setAlpha(changed ? 1.0f : 0.5f);
    }
}