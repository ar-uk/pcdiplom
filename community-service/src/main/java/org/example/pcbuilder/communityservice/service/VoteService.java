package org.example.pcbuilder.communityservice.service;

import lombok.RequiredArgsConstructor;
import org.example.pcbuilder.communityservice.dto.CreateVoteRequest;
import org.example.pcbuilder.communityservice.dto.VoteResponse;
import org.example.pcbuilder.communityservice.model.Comment;
import org.example.pcbuilder.communityservice.model.Post;
import org.example.pcbuilder.communityservice.model.Vote;
import org.example.pcbuilder.communityservice.model.VoteTargetType;
import org.example.pcbuilder.communityservice.repository.CommentRepository;
import org.example.pcbuilder.communityservice.repository.PostRepository;
import org.example.pcbuilder.communityservice.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public VoteResponse castVote(CreateVoteRequest request) {
        if (request.value() != 1 && request.value() != -1) {
            throw new IllegalArgumentException("Vote value must be 1 or -1");
        }

        String normalizedUserId = normalizeUserId(request.userId());
        Vote vote = voteRepository.findByUserIdAndTargetTypeAndTargetId(normalizedUserId, request.targetType(), request.targetId())
                .orElseGet(Vote::new);

        vote.setUserId(normalizedUserId);
        vote.setTargetType(request.targetType());
        vote.setTargetId(request.targetId());
        vote.setValue(request.value());
        voteRepository.save(vote);

        Integer score = refreshTargetScore(request.targetType(), request.targetId());

        return new VoteResponse(request.targetType(), request.targetId(), score, request.value());
    }

    @Transactional
    public VoteResponse removeVote(String userId, VoteTargetType targetType, Long targetId) {
        String normalizedUserId = normalizeUserId(userId);
        voteRepository.deleteByUserIdAndTargetTypeAndTargetId(normalizedUserId, targetType, targetId);
        Integer score = refreshTargetScore(targetType, targetId);
        return new VoteResponse(targetType, targetId, score, null);
    }

    private Integer refreshTargetScore(VoteTargetType targetType, Long targetId) {
        Integer score = voteRepository.sumForTarget(targetType, targetId);
        if (targetType == VoteTargetType.POST) {
            Post post = postRepository.findByIdAndDeletedAtIsNull(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("Post not found"));
            post.setScore(score);
            postRepository.save(post);
            return score;
        }

        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        comment.setScore(score);
        commentRepository.save(comment);
        return score;
    }

    private String normalizeUserId(String userId) {
        return userId.trim().toLowerCase();
    }
}
