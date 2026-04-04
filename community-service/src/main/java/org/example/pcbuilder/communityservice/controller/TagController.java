package org.example.pcbuilder.communityservice.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.pcbuilder.communityservice.dto.PostResponse;
import org.example.pcbuilder.communityservice.dto.TagResponse;
import org.example.pcbuilder.communityservice.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/community/tags")
@RequiredArgsConstructor
public class TagController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<List<TagResponse>> listTags() {
        return ResponseEntity.ok(postService.listTags());
    }

    @GetMapping("/{slug}/posts")
    public ResponseEntity<List<PostResponse>> listPostsByTag(@PathVariable String slug) {
        return ResponseEntity.ok(postService.listPosts("new", slug));
    }
}
