package com.yuhbui.ComicAppBackend.controller;

import com.yuhbui.ComicAppBackend.entity.Comment;
import com.yuhbui.ComicAppBackend.entity.CommentInteraction;
import com.yuhbui.ComicAppBackend.entity.CommentReport;
import com.yuhbui.ComicAppBackend.dto.CommentResponseDTO; // Import DTO mới tạo
import com.yuhbui.ComicAppBackend.repository.CommentRepository;
import com.yuhbui.ComicAppBackend.repository.CommentInteractionRepository;
import com.yuhbui.ComicAppBackend.repository.CommentReportRepository;
import com.yuhbui.ComicAppBackend.repository.UserRepository;       // Bổ sung repo user
import com.yuhbui.ComicAppBackend.repository.ChapterRepository;    // Bổ sung repo chapter
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentInteractionRepository interactionRepository;

    @Autowired
    private CommentReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository; // Inject thêm để lấy Tên thật và Avatar

    @Autowired
    private ChapterRepository chapterRepository; // Inject thêm để lấy Tên Chapter

    // --- HÀM PHỤ TRỢ: Chuyển đổi từ Comment Entity sang CommentResponseDTO đầy đủ thông tin ---
    private CommentResponseDTO convertToDTO(Comment comment) {
        CommentResponseDTO dto = new CommentResponseDTO();
        dto.setCommentId(comment.getCommentId());
        dto.setUserId(comment.getUserId());
        dto.setComicId(comment.getComicId());
        dto.setChapterId(comment.getChapterId());
        dto.setParentCommentId(comment.getParentCommentId());
        dto.setContent(comment.getContent());
        dto.setLikeCount(comment.getLikeCount());
        dto.setDislikeCount(comment.getDislikeCount());
        dto.setIsDeleted(comment.getIsDeleted());
        dto.setCreatedAt(comment.getCreatedAt());

        // 1. Lấy thông tin Tên hiển thị và Avatar từ bảng Users
        userRepository.findById(comment.getUserId()).ifPresent(user -> {
            dto.setUserDisplayName(user.getDisplayName());
            dto.setUserAvatarUrl(user.getAvatarUrl());
        });

        // 2. Lấy tên Chapter tag từ bảng Chapters nếu có gắn ChapterID
        if (comment.getChapterId() != null) {
            chapterRepository.findById(comment.getChapterId()).ifPresent(chapter -> {
                dto.setChapterName("Chương " + chapter.getChapterNumber());
            });
        } else {
            dto.setChapterName(""); // Để trống nếu bình luận ở ngoài màn hình chi tiết truyện
        }

        return dto;
    }

    // 1. API lấy bình luận gốc của một truyện (Đã chuyển sang trả về DTO + Lọc sạch IsDeleted)
    @GetMapping("/comic/{comicId}")
    public List<CommentResponseDTO> getCommentsByComic(@PathVariable Integer comicId) {
        List<Comment> rawList = commentRepository.findByComicIdAndParentCommentIdIsNullOrderByCreatedAtDesc(comicId);

        return rawList.stream()
                .filter(c -> c.getIsDeleted() == null || !c.getIsDeleted())
                .map(this::convertToDTO) // Biến đổi sang DTO
                .collect(Collectors.toList());
    }

    // 2. API lấy các phản hồi (replies) của một bình luận (Đã chuyển sang trả về DTO + Lọc sạch IsDeleted)
    @GetMapping("/{parentCommentId}/replies")
    public List<CommentResponseDTO> getReplies(@PathVariable Integer parentCommentId) {
        List<Comment> rawList = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentCommentId);

        return rawList.stream()
                .filter(c -> c.getIsDeleted() == null || !c.getIsDeleted())
                .map(this::convertToDTO) // Biến đổi sang DTO
                .collect(Collectors.toList());
    }

    // 3. API lấy bình luận gốc của một CHAPTER cụ thể (Đã chuyển sang trả về DTO + Lọc sạch IsDeleted)
    @GetMapping("/chapter/{chapterId}")
    public List<CommentResponseDTO> getCommentsByChapter(@PathVariable Integer chapterId) {
        List<Comment> rawList = commentRepository.findByChapterIdAndParentCommentIdIsNullOrderByCreatedAtDesc(chapterId);

        return rawList.stream()
                .filter(c -> c.getIsDeleted() == null || !c.getIsDeleted())
                .map(this::convertToDTO) // Biến đổi sang DTO
                .collect(Collectors.toList());
    }

    // 4. API gửi bình luận mới (Hoặc phản hồi nếu truyền parentCommentId)
    @PostMapping("/post")
    public ResponseEntity<?> postComment(@RequestBody Comment comment) {
        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Nội dung bình luận không được để trống!");
        }

        comment.setCommentId(null);
        comment.setCreatedAt(LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);

        // Nếu đây là một bình luận phản hồi, tự động tăng số lượng đếm của bình luận cha
        if (savedComment.getParentCommentId() != null) {
            commentRepository.findById(savedComment.getParentCommentId()).ifPresent(parentComment -> {
                parentComment.setReplyCount(parentComment.getReplyCount() + 1);
                commentRepository.save(parentComment);
            });
        }

        return ResponseEntity.ok(savedComment);
    }

    // 5. API tương tác Like/Dislike bình luận
    @PostMapping("/{commentId}/interact")
    public ResponseEntity<?> interactWithComment(
            @PathVariable Integer commentId,
            @RequestParam Integer userId,
            @RequestParam Integer type) {

        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (!commentOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Bình luận không tồn tại!");
        }
        Comment comment = commentOpt.get();

        Optional<CommentInteraction> existingInteraction =
                interactionRepository.findByUserIdAndCommentId(userId, commentId);

        if (existingInteraction.isPresent()) {
            CommentInteraction interaction = existingInteraction.get();

            if (interaction.getInteractionType().equals(type)) {
                interactionRepository.delete(interaction);
                if (type == 1) comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
                else comment.setDislikeCount(Math.max(0, comment.getDislikeCount() - 1));
            } else {
                interaction.setInteractionType(type);
                interactionRepository.save(interaction);

                if (type == 1) {
                    comment.setLikeCount(comment.getLikeCount() + 1);
                    comment.setDislikeCount(Math.max(0, comment.getDislikeCount() - 1));
                } else {
                    comment.setDislikeCount(comment.getDislikeCount() + 1);
                    comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
                }
            }
        } else {
            CommentInteraction newInteraction = new CommentInteraction();
            newInteraction.setUserId(userId);
            newInteraction.setCommentId(commentId);
            newInteraction.setInteractionType(type);
            interactionRepository.save(newInteraction);

            if (type == 1) comment.setLikeCount(comment.getLikeCount() + 1);
            else comment.setDislikeCount(comment.getDislikeCount() + 1);
        }

        commentRepository.save(comment);
        return ResponseEntity.ok(comment);
    }

    // 6. API Gửi báo cáo bình luận xấu lên hệ thống
    @PostMapping("/{commentId}/report")
    public ResponseEntity<?> reportComment(
            @PathVariable Integer commentId,
            @RequestParam Integer userId,
            @RequestParam String reason) {

        if (reportRepository.existsByUserIdAndCommentId(userId, commentId)) {
            return ResponseEntity.badRequest().body("Bạn đã gửi báo cáo cho bình luận này rồi!");
        }

        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (!commentOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Bình luận không tồn tại!");
        }
        Comment comment = commentOpt.get();

        CommentReport report = new CommentReport();
        report.setUserId(userId);
        report.setCommentId(commentId);
        report.setReason(reason);
        reportRepository.save(report);

        comment.setReportCount(comment.getReportCount() + 1);
        commentRepository.save(comment);

        return ResponseEntity.ok("Báo cáo đã được ghi nhận thành công!");
    }

    // 7. API Xóa bình luận (Chuyển trạng thái IsDeleted thành true và trả về đối tượng)
    @PutMapping("/{commentId}/delete")
    public ResponseEntity<?> deleteComment(
            @PathVariable Integer commentId,
            @RequestParam Integer userId) {

        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (!commentOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Bình luận không tồn tại!");
        }

        Comment comment = commentOpt.get();

        if (!comment.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền xóa!");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);

        return ResponseEntity.ok(comment);
    }
}