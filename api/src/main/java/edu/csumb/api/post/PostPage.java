package edu.csumb.api.post;

import java.util.List;

// One page of posts, the PostPage schema in openapi.yaml. Pages start at 0.
public record PostPage(int page, int size, long totalElements, int totalPages, List<PostResponse> content) {
}