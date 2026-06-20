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

    // ĐÃ SỬA: Tiếp nhận 2 tham số để đối chiếu trạng thái Fill màu nút bấm cho từng User cụ thể
    private CommentResponseDTO convertToDTO(Comment comment, Integer userId) {
        CommentResponseDTO dto = new CommentResponseDTO();
        dto.setCommentId(comment.getCommentId());
        dto.setUserId(comment.getUserId());
        dto.setComicId(comment.getComicId());
        dto.setChapterId(comment.getChapterId());
        dto.setParentCommentId(comment.getParentCommentId());
        dto.setContent(comment.getContent());
        dto.setLikeCount(comment.getLikeCount());
        dto.setDislikeCount(comment.getDislikeCount());
        dto.setReplyCount(comment.getReplyCount());
        dto.setReportCount(comment.getReportCount());
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

        // Kiểm tra xem User hiện tại đã từng Like hay Dislike bình luận này chưa để gửi cờ về kích hoạt Fill màu icon Android
        if (userId != null) {
            interactionRepository.findByUserIdAndCommentId(userId, comment.getCommentId()).ifPresent(interaction -> {
                if (interaction.getInteractionType() == 1) {
                    dto.setLiked(true);
                } else if (interaction.getInteractionType() == -1) {
                    dto.setDisliked(true);
                }
            });
        }

        return dto;
    }

    // 1. API lấy danh sách bình luận gốc của bộ truyện (Đã sửa cú pháp map Lambda)
    @GetMapping("/comic/{comicId}")
    public ResponseEntity<List<CommentResponseDTO>> getCommentsByComic(
            @PathVariable Integer comicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer userId) { // Nhận thêm userId từ Android gửi lên

        List<Comment> rawList = commentRepository.findByComicIdAndParentCommentIdIsNullOrderByCreatedAtDesc(comicId);
        List<CommentResponseDTO> dtoList = rawList.stream()
                .filter(c -> c.getIsDeleted() == null || !c.getIsDeleted())
                .map(c -> convertToDTO(c, userId)) // ĐÃ SỬA THÀNH LAMBDA ĐỂ HẾT LỖI GẠCH ĐỎ
                .collect(Collectors.toList());

        int start = page * 10;
        if (start >= dtoList.size()) {
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }
        int end = Math.min(start + 10, dtoList.size());

        return ResponseEntity.ok(dtoList.subList(start, end));
    }

    // 2. API lấy danh sách bình luận con / Phản hồi (Đã sửa cú pháp map Lambda)
    @GetMapping("/{parentCommentId}/replies")
    public ResponseEntity<List<CommentResponseDTO>> getReplies(
            @PathVariable Integer parentCommentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer userId) {

        List<Comment> rawList = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentCommentId);
        List<CommentResponseDTO> dtoList = rawList.stream()
                .filter(c -> c.getIsDeleted() == null || !c.getIsDeleted())
                .map(c -> convertToDTO(c, userId)) // ĐÃ SỬA THÀNH LAMBDA ĐỂ HẾT LỖI GẠCH ĐỎ
                .collect(Collectors.toList());

        int start = page * 10;
        if (start >= dtoList.size()) {
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }
        int end = Math.min(start + 10, dtoList.size());

        return ResponseEntity.ok(dtoList.subList(start, end));
    }

    // 3. API lấy danh sách bình luận gốc của chương truyện (Đã sửa cú pháp map Lambda)
    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<List<CommentResponseDTO>> getCommentsByChapter(
            @PathVariable Integer chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer userId) {

        List<Comment> rawList = commentRepository.findByChapterIdAndParentCommentIdIsNullOrderByCreatedAtDesc(chapterId);
        List<CommentResponseDTO> dtoList = rawList.stream()
                .filter(c -> c.getIsDeleted() == null || !c.getIsDeleted())
                .map(c -> convertToDTO(c, userId)) // ĐÃ SỬA THÀNH LAMBDA ĐỂ HẾT LỖI GẠCH ĐỎ
                .collect(Collectors.toList());

        int start = page * 10;
        if (start >= dtoList.size()) {
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }
        int end = Math.min(start + 10, dtoList.size());

        return ResponseEntity.ok(dtoList.subList(start, end));
    }

    // 4. API GỬI BÌNH LUẬN MỚI
    @PostMapping("/post")
    public ResponseEntity<?> postComment(@RequestBody Comment comment) {
        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Nội dung bình luận không được để trống!");
        }

        Optional<com.yuhbui.ComicAppBackend.entity.User> userOpt = userRepository.findById(comment.getUserId());
        if (userOpt.isPresent() && "Banned".equalsIgnoreCase(userOpt.get().getStatus())) {
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

        return ResponseEntity.ok(convertToDTO(savedComment, comment.getUserId()));
    }

    // 5. API TƯƠNG TÁC LIKE/DISLIKE BÌNH LUẬN (Đã sửa trả về DTO chứa trạng thái fill màu)
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

        // ĐÃ THAY ĐỔI: Trả về DTO kèm thông số đầy đủ thay vì đối tượng Entity thô cũ để Android đổi hình đặc
        return ResponseEntity.ok(convertToDTO(comment, userId));
    }

    // 6. API BÁO CÁO BÌNH LUẬN XẤU
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

    // 7. API XÓA BÌNH LUẬN
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

        return ResponseEntity.ok(convertToDTO(comment, userId));
    }
}