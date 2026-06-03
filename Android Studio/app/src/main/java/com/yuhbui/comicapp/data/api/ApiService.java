package com.yuhbui.comicapp.data.api;

import com.yuhbui.comicapp.data.model.Category;
import com.yuhbui.comicapp.data.model.ChapterImage;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.data.model.Chapter;
import com.yuhbui.comicapp.data.model.ComicDetailResponse;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.data.model.LoginRequest;
import com.yuhbui.comicapp.data.model.ReadingHistory;
import com.yuhbui.comicapp.data.model.RegisterRequest;
import com.yuhbui.comicapp.data.model.User;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path; // Import thêm Path
import retrofit2.http.Query;

public interface ApiService {
    @GET("/api/comics")
    Call<List<Comic>> getAllComics();

    @GET("/api/comics/{comicId}/chapters")
    Call<List<Chapter>> getChaptersByComicId(@Path("comicId") int comicId);

    @GET("/api/chapters/{chapterId}/images")
    Call<List<ChapterImage>> getImagesByChapterId(@Path("chapterId") int chapterId);

    @POST("/api/users/login")
    Call<User> login(@Body LoginRequest request);

    @POST("/api/users/register")
    Call<User> register(@Body RegisterRequest request);

    @POST("/api/history/save")
    Call<ReadingHistory> saveReadingHistory(@Body ReadingHistory history);

    @GET("/api/comments/comic/{comicId}")
    Call<List<Comment>> getCommentsByComic(@Path("comicId") int comicId);

    @POST("/api/comments/post")
    Call<Comment> postComment(@Body Comment comment);

    @POST("/api/comments/{commentId}/interact")
    Call<Comment> interactWithComment(
            @Path("commentId") int commentId,
            @Query("userId") int userId,
            @Query("type") int type
    );

    @GET("/api/comments/{parentCommentId}/replies")
    Call<List<Comment>> getRepliesByParentId(@Path("parentCommentId") int parentCommentId);

    @POST("/api/comments/{commentId}/report")
    Call<String> reportComment(
            @Path("commentId") int commentId,
            @Query("userId") int userId,
            @Query("reason") String reason
    );

    // Lấy bình luận của riêng chapter đó
    @GET("/api/comments/chapter/{chapterId}")
    Call<List<Comment>> getCommentsByChapter(@Path("chapterId") int chapterId);

    // API xóa bình luận chính chủ
    @PUT("/api/comments/{commentId}/delete")
    Call<Comment> deleteComment(
            @Path("commentId") int commentId,
            @Query("userId") int userId
    );

    // API Người dùng gửi đánh giá sao (1-5) cho truyện
    // Server Spring Boot đang trả về chuỗi text, nên ta hứng bằng Call<String>
    @POST("/api/comics/{comicId}/rate")
    Call<String> rateComic(
            @Path("comicId") int comicId,
            @Query("userId") int userId,
            @Query("score") int score
    );

    // Lấy chi tiết truyện kèm thể loại và trạng thái thích của User
    @GET("/api/comics/{comicId}")
    Call<ComicDetailResponse> getComicDetail(
            @Path("comicId") int comicId,
            @Query("userId") Integer userId
    );

    // Bấm nút Yêu thích / Bỏ yêu thích
    @POST("/api/comics/{comicId}/toggle-favorite")
    Call<Boolean> toggleFavorite(
            @Path("comicId") int comicId,
            @Query("userId") int userId
    );

    // API lấy truyện mới cập nhật kèm phân trang số
    @GET("/api/comics/home/updates")
    Call<List<Comic>> getHomeUpdates(@Query("page") int page);

    // API lấy danh sách truyện đề cử hot (top rating)
    @GET("/api/comics/home/recommended")
    Call<List<Comic>> getRecommendedComics();

    // Lấy danh sách toàn bộ thể loại để làm thanh lọc
    @GET("/api/categories")
    Call<List<Category>> getCategories();

    // Lấy danh sách truyện đã được lọc theo mã thể loại
    @GET("/api/comics/filter")
    Call<List<Comic>> getComicsByCategory(@Query("catId") int catId);

    // API lấy bảng xếp hạng top 10
    @GET("/api/comics/home/ranking")
    Call<List<Comic>> getTopRanking(@Query("type") String type);

    @GET("/api/history/user/{userId}")
    Call<List<Comic>> getReadingHistoryByUserId(@Path("userId") int userId);

    // Lấy danh sách truyện yêu thích của người dùng (kèm đầy đủ thông số)
    @GET("/api/comics/favorites/{userId}")
    Call<List<Comic>> getFavoriteComics(@Path("userId") int userId);

    @GET("/api/users/{id}")
    Call<User> getUserProfile(@Path("id") int userId);

    @PUT("/api/users/update/{id}")
    Call<User> updateProfile(@Path("id") int userId, @Body com.yuhbui.comicapp.data.model.RegisterRequest request);
    // Có thể tái sử dụng RegisterRequest vì cấu trúc gửi lên giống hệt nhau (email, displayName, password, confirmPassword)

    @Multipart
    @POST("/api/users/upload-avatar/{id}")
    Call<java.util.Map<String, String>> uploadAvatar(
            @Path("id") int userId,
            @Part MultipartBody.Part file
    );

    @GET("/api/comics/search")
    Call<List<Comic>> searchComics(@Query("keyword") String keyword);

    @GET("/api/admin/dashboard/access-stats")
    Call<List<java.util.Map<String, Object>>> getAdminAccessStats(@Query("type") String type);

    @GET("/api/admin/comics")
    Call<List<Comic>> adminGetAllComics();

    @POST("/api/admin/comics")
    Call<Comic> adminCreateComic(@Body Comic comic);

    @PUT("/api/admin/comics/{id}")
    Call<Comic> adminUpdateComic(@Path("id") Integer id, @Body Comic comic);

    @PUT("/api/admin/comics/{id}/toggle-hidden")
    Call<Boolean> adminToggleHiddenComic(@Path("id") Integer id);

    @POST("/api/categories")
    Call<Category> createCategory(@Body Category category);

    @GET("/api/categories")
    Call<List<Category>> getAllCategories();

    @POST("/api/admin/comics")
    Call<Comic> adminCreateComic(@Body Comic comic, @Query("categoryIds") List<Integer> categoryIds);

    @PUT("/api/admin/comics/{id}")
    Call<Comic> adminUpdateComic(@Path("id") Integer id, @Body Comic comic, @Query("categoryIds") List<Integer> categoryIds);
    @DELETE("/api/admin/comics/{id}")
    Call<okhttp3.ResponseBody> adminDeleteComic(@Path("id") Integer id);

    @Multipart
    @POST("/api/admin/comics/upload-cover")
    Call<Map<String, String>> adminUploadCover(@Part MultipartBody.Part file);

    @GET("/api/admin/comics/{id}/comments")
    Call<List<Map<String, Object>>> adminGetComicComments(@Path("id") Integer id);

    @DELETE("/api/admin/comics/comments/{commentId}")
    Call<Map<String, Object>> adminDeleteComment(@Path("commentId") Integer commentId);

    @GET("/api/admin/chapters/comic/{comicId}")
    Call<List<Map<String, Object>>> adminGetChapters(@Path("comicId") Integer comicId);

    @POST("/api/admin/chapters/comic/{comicId}")
    Call<Map<String, Object>> adminCreateChapter(@Path("comicId") Integer comicId, @Query("chapterNumber") Double chapterNumber, @Query("title") String title);

    @PUT("/api/admin/chapters/{chapterId}")
    Call<Map<String, Object>> adminUpdateChapter(@Path("chapterId") Integer chapterId, @Query("chapterNumber") Double chapterNumber, @Query("title") String title);

    @DELETE("/api/admin/chapters/{chapterId}")
    Call<Map<String, Object>> adminDeleteChapter(@Path("chapterId") Integer chapterId);

    @GET("/api/admin/chapters/{chapterId}/pages")
    Call<List<Map<String, Object>>> adminGetChapterPages(@Path("chapterId") Integer chapterId);

    @Multipart
    @POST("/api/admin/chapters/{chapterId}/upload-page")
    Call<Map<String, String>> adminUploadChapterPage(@Path("chapterId") Integer chapterId, @Part MultipartBody.Part file);

    @DELETE("/api/admin/chapters/pages/{imageId}")
    Call<Map<String, Object>> adminDeleteChapterPage(@Path("imageId") Integer imageId);

    @Multipart
    @POST("/api/admin/chapters/comic/{comicId}/with-images")
    Call<Map<String, Object>> adminCreateChapterWithImages(
            @Path("comicId") Integer comicId,
            @Part("chapterNumber") RequestBody chapterNumber,
            @Part("title") RequestBody title,
            @Part List<MultipartBody.Part> files
    );

    @PUT("/api/admin/chapters/pages/reorder")
    Call<Map<String, Object>> adminReorderPages(@Body List<Integer> imageIds);

    @GET("/api/admin/chapters/comments/{commentId}/reports")
    Call<List<String>> adminGetCommentReports(@Path("commentId") Integer commentId);

    @GET("/api/admin/chapters/{chapterId}/comments")
    Call<List<Map<String, Object>>> adminGetChapterComments(@Path("chapterId") Integer chapterId);
}