package com.yuhbui.ComicAppBackend.controller;

import com.yuhbui.ComicAppBackend.entity.Comment;
import com.yuhbui.ComicAppBackend.entity.CommentInteraction;
import com.yuhbui.ComicAppBackend.entity.CommentReport;
import com.yuhbui.ComicAppBackend.dto.CommentResponseDTO;
import com.yuhbui.ComicAppBackend.repository.CommentRepository;
import com.yuhbui.ComicAppBackend.repository.CommentInteractionRepository;
import com.yuhbui.ComicAppBackend.repository.CommentReportRepository;
import com.yuhbui.ComicAppBackend.repository.UserRepository;
import com.yuhbui.ComicAppBackend.repository.ChapterRepository;
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
    private UserRepository userRepository;

    @Autowired
    private ChapterRepository chapterRepository;

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

        userRepository.findById(comment.getUserId()).ifPresent(user -> {
            dto.setUserDisplayName(user.getDisplayName());
            dto.setUserAvatarUrl(user.getAvatarUrl());
        });

        if (comment.getChapterId() != null) {
            chapterRepository.findById(comment.getChapterId()).ifPresent(chapter -> {
                dto.setChapterName("Chương " + chapter.getChapterNumber());
            });
        } else {
            dto.setChapterName("");
        }

        return dto;
    }

    @GetMapping("/comic/{comicId}")
    public List<CommentResponseDTO> getCommentsByComic(@PathVariable Integer comicId) {
        List<Comment> rawList = commentRepository.findByComicIdAndParentCommentIdIsNullOrderByCreatedAtDesc(comicId);
        return rawList.stream()
                .filter(c -> c.getIsDeleted() == null || !c.getIsDeleted())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{parentCommentId}/replies")
    public List<CommentResponseDTO> getReplies(@PathVariable Integer parentCommentId) {
        List<Comment> rawList = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentCommentId);
        return rawList.stream()
                .filter(c -> c.getIsDeleted() == null || !c.getIsDeleted())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/chapter/{chapterId}")
    public List<CommentResponseDTO> getCommentsByChapter(@PathVariable Integer chapterId) {
        List<Comment> rawList = commentRepository.findByChapterIdAndParentCommentIdIsNullOrderByCreatedAtDesc(chapterId);
        return rawList.stream()
                .filter(c -> c.getIsDeleted() == null || !c.getIsDeleted())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 4. API GỬI BÌNH LUẬN MỚI (Đmap: ĐÃ TÍCH HỢP TÍNH NĂNG KHÓA CHAT CHO TÀI KHOẢN BỊ BAN)
    @PostMapping("/post")
    public ResponseEntity<?> postComment(@RequestBody Comment comment) {
        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Nội dung bình luận không được để trống!");
        }

        // BỔ SUNG: Kiểm tra xem tài khoản này có đang bị BAN (Khóa) hay không
        Optional<com.yuhbui.ComicAppBackend.entity.User> userOpt = userRepository.findById(comment.getUserId());
        if (userOpt.isPresent() && "Banned".equalsIgnoreCase(userOpt.get().getStatus())) {
            // Trả về mã lỗi 403 Forbidden chặn không cho lưu xuống DB
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Tài khoản của bạn đã bị khóa chức năng bình luận do vi phạm quy chế!");
        }

        comment.setCommentId(null);
        comment.setCreatedAt(LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);

        if (savedComment.getParentCommentId() != null) {
            commentRepository.findById(savedComment.getParentCommentId()).ifPresent(parentComment -> {
                parentComment.setReplyCount(parentComment.getReplyCount() + 1);
                commentRepository.save(parentComment);
            });
        }

        return ResponseEntity.ok(savedComment);
    }

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