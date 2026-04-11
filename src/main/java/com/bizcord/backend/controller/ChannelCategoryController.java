package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.CategoryCreateRequest;
import com.bizcord.backend.dto.CategoryUpdateRequest;
import com.bizcord.backend.dto.ReorderRequest;
import com.bizcord.backend.dto.ServerResponse;
import com.bizcord.backend.service.ChannelCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/servers/{serverId}/categories")
@RequiredArgsConstructor
public class ChannelCategoryController {
    private final ChannelCategoryService categoryService;

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @PostMapping
    public ResponseEntity<ServerResponse> createCategory(@PathVariable String serverId,
                                                         @Valid @RequestBody CategoryCreateRequest request,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(CREATED)
                .body(categoryService.createCategory(serverId, request, userDetails.getUsername()));
    }

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @PatchMapping("/{categoryId}")
    public ResponseEntity<ServerResponse> updateCategory(@PathVariable String serverId,
                                                         @PathVariable String categoryId,
                                                         @Valid @RequestBody CategoryUpdateRequest request,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(categoryService.updateCategory(serverId, categoryId, request, userDetails.getUsername()));
    }

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ServerResponse> deleteCategory(@PathVariable String serverId,
                                                         @PathVariable String categoryId,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(categoryService.deleteCategory(serverId, categoryId, userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @PatchMapping("/reorder")
    public ResponseEntity<ServerResponse> reorder(@PathVariable String serverId,
                                                  @Valid @RequestBody ReorderRequest request,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(categoryService.reorder(serverId, request, userDetails.getUsername()));
    }
}
