package org.example.pcbuilder.communityservice.service;

import java.util.List;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.pcbuilder.communityservice.dto.CommentResponse;
import org.example.pcbuilder.communityservice.dto.CreateCommentRequest;
import org.example.pcbuilder.communityservice.dto.UpdateCommentRequest;
import org.example.pcbuilder.communityservice.model.Comment;
import org.example.pcbuilder.communityservice.model.Post;
import org.example.pcbuilder.communityservice.repository.CommentRepository;
import org.example.pcbuilder.communityservice.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Transactional
    public CommentResponse createComment(Long postId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .filter(existing -> existing.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        Comment parent = null;
        if (request.parentCommentId() != null) {
            parent = commentRepository.findByIdAndDeletedAtIsNull(request.parentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            if (!parent.getPost().getId().equals(postId)) {
                throw new IllegalArgumentException("Parent comment belongs to another post");
            }
        }

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setParentComment(parent);
        comment.setAuthorUserId(normalizeUserId(request.authorUserId()));
        comment.setBody(request.body().trim());

        Comment saved = commentRepository.save(comment);
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(Long postId) {
        return commentRepository.findAllByPostIdAndDeletedAtIsNullOrderByCreatedAtAsc(postId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse updateComment(Long postId, Long commentId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("Comment belongs to another post");
        }

        String editor = normalizeUserId(request.editorUserId());
        if (!comment.getAuthorUserId().equals(editor)) {
            throw new IllegalArgumentException("Only the author can edit this comment");
        }

        comment.setBody(request.body().trim());
        return toResponse(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, String requesterUserId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("Comment belongs to another post");
        }

        String requester = normalizeUserId(requesterUserId);
        if (!comment.getAuthorUserId().equals(requester)) {
            throw new IllegalArgumentException("Only the author can delete this comment");
        }

        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);

        Post post = comment.getPost();
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParentComment() == null ? null : comment.getParentComment().getId(),
                comment.getAuthorUserId(),
                comment.getBody(),
                comment.getScore(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private String normalizeUserId(String userId) {
        return userId.trim().toLowerCase();
    }
}
