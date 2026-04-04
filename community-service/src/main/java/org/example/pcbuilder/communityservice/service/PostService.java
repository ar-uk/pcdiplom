package org.example.pcbuilder.communityservice.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.pcbuilder.communityservice.dto.CreatePostRequest;
import org.example.pcbuilder.communityservice.dto.PostResponse;
import org.example.pcbuilder.communityservice.dto.TagResponse;
import org.example.pcbuilder.communityservice.dto.UpdatePostRequest;
import org.example.pcbuilder.communityservice.model.Post;
import org.example.pcbuilder.communityservice.model.PostImage;
import org.example.pcbuilder.communityservice.model.PostTag;
import org.example.pcbuilder.communityservice.model.PostTagId;
import org.example.pcbuilder.communityservice.model.Tag;
import org.example.pcbuilder.communityservice.repository.PostRepository;
import org.example.pcbuilder.communityservice.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;

    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        Post post = new Post();
        post.setAuthorUserId(normalizeUserId(request.authorUserId()));
        post.setTitle(request.title().trim());
        post.setBody(request.body().trim());
        post.setBuildId(request.buildId());
        post.setBuildSnapshotJson(request.buildSnapshotJson());

        if (request.imageUrls() != null) {
            for (int i = 0; i < request.imageUrls().size(); i++) {
                String imageUrl = request.imageUrls().get(i);
                if (imageUrl == null || imageUrl.isBlank()) {
                    continue;
                }
                PostImage image = new PostImage();
                image.setPost(post);
                image.setImageUrl(imageUrl.trim());
                image.setSortOrder(i);
                post.getImages().add(image);
            }
        }

        if (request.tags() != null) {
            for (String rawTag : request.tags()) {
                if (rawTag == null || rawTag.isBlank()) {
                    continue;
                }
                String slug = slugify(rawTag);
                Tag tag = tagRepository.findBySlug(slug)
                        .orElseGet(() -> {
                            Tag created = new Tag();
                            created.setSlug(slug);
                            created.setDisplayName(rawTag.trim());
                            return tagRepository.save(created);
                        });

                PostTag postTag = new PostTag();
                postTag.setPost(post);
                postTag.setTag(tag);
                postTag.setId(new PostTagId(null, tag.getId()));
                post.getPostTags().add(postTag);
            }
        }

        Post saved = postRepository.save(post);
        for (PostTag postTag : saved.getPostTags()) {
            postTag.setId(new PostTagId(saved.getId(), postTag.getTag().getId()));
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
        public List<PostResponse> listPosts(String sort, String tag) {
        List<Post> posts = new ArrayList<>(postRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc());

        if (tag != null && !tag.isBlank()) {
            String requestedSlug = slugify(tag);
            posts = posts.stream()
                .filter(post -> post.getPostTags().stream()
                    .anyMatch(postTag -> requestedSlug.equals(postTag.getTag().getSlug())))
                .toList();
        }

        if ("hot".equalsIgnoreCase(sort)) {
            posts = posts.stream()
                .sorted(
                    Comparator.<Post, Integer>comparing(post -> post.getScore() == null ? 0 : post.getScore())
                        .reversed()
                        .thenComparing(post -> post.getCommentCount() == null ? 0 : post.getCommentCount(), Comparator.reverseOrder())
                        .thenComparing(Post::getCreatedAt, Comparator.reverseOrder())
                )
                .toList();
        }

        return posts.stream()
                .map(this::toResponse)
                .toList();
    }

        @Transactional(readOnly = true)
        public List<TagResponse> listTags() {
        return tagRepository.findAll().stream()
            .sorted(Comparator.comparing(Tag::getDisplayName, String.CASE_INSENSITIVE_ORDER))
            .map(tag -> new TagResponse(tag.getId(), tag.getSlug(), tag.getDisplayName()))
            .toList();
        }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        return toResponse(post);
    }

    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostRequest request) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        String editor = normalizeUserId(request.editorUserId());
        if (!post.getAuthorUserId().equals(editor)) {
            throw new IllegalArgumentException("Only the author can edit this post");
        }

        if (request.title() != null && !request.title().isBlank()) {
            post.setTitle(request.title().trim());
        }
        if (request.body() != null && !request.body().isBlank()) {
            post.setBody(request.body().trim());
        }

        if (request.imageUrls() != null) {
            post.getImages().clear();
            for (int i = 0; i < request.imageUrls().size(); i++) {
                String imageUrl = request.imageUrls().get(i);
                if (imageUrl == null || imageUrl.isBlank()) {
                    continue;
                }
                PostImage image = new PostImage();
                image.setPost(post);
                image.setImageUrl(imageUrl.trim());
                image.setSortOrder(i);
                post.getImages().add(image);
            }
        }

        if (request.tags() != null) {
            post.getPostTags().clear();
            for (String rawTag : request.tags()) {
                if (rawTag == null || rawTag.isBlank()) {
                    continue;
                }
                String slug = slugify(rawTag);
                Tag tag = tagRepository.findBySlug(slug)
                        .orElseGet(() -> {
                            Tag created = new Tag();
                            created.setSlug(slug);
                            created.setDisplayName(rawTag.trim());
                            return tagRepository.save(created);
                        });

                PostTag postTag = new PostTag();
                postTag.setPost(post);
                postTag.setTag(tag);
                postTag.setId(new PostTagId(post.getId(), tag.getId()));
                post.getPostTags().add(postTag);
            }
        }

        return toResponse(postRepository.save(post));
    }

    @Transactional
    public void deletePost(Long postId, String requesterUserId) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        String requester = normalizeUserId(requesterUserId);
        if (!post.getAuthorUserId().equals(requester)) {
            throw new IllegalArgumentException("Only the author can delete this post");
        }

        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    private PostResponse toResponse(Post post) {
        List<String> tags = new ArrayList<>();
        for (PostTag postTag : post.getPostTags()) {
            tags.add(postTag.getTag().getDisplayName());
        }

        List<String> imageUrls = new ArrayList<>();
        for (PostImage image : post.getImages()) {
            imageUrls.add(image.getImageUrl());
        }

        return new PostResponse(
                post.getId(),
                post.getAuthorUserId(),
                post.getTitle(),
                post.getBody(),
                post.getBuildId(),
            post.getBuildSnapshotJson(),
                post.getScore(),
                post.getCommentCount(),
                tags,
                imageUrls,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private String normalizeUserId(String userId) {
        return userId == null ? "" : userId.trim().toLowerCase();
    }

    private String slugify(String value) {
        String cleaned = value.trim().toLowerCase();
        cleaned = cleaned.replaceAll("[^a-z0-9\\s-]", "");
        cleaned = cleaned.replaceAll("\\s+", "-");
        cleaned = cleaned.replaceAll("-+", "-");
        return cleaned;
    }
}
