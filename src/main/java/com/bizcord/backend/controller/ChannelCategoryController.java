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
import org.springframework.web.util.HtmlUtils;
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
        ServerResponse response = categoryService.createCategory(serverId, request, userDetails.getUsername());
        return ResponseEntity.status(CREATED)
                .body(sanitizeResponse(response));
    }

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @PatchMapping("/{categoryId}")
    public ResponseEntity<ServerResponse> updateCategory(@PathVariable String serverId,
                                                         @PathVariable String categoryId,
                                                         @Valid @RequestBody CategoryUpdateRequest request,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        ServerResponse response = categoryService.updateCategory(serverId, categoryId, request, userDetails.getUsername());
        return ResponseEntity.ok(sanitizeResponse(response));
    }

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ServerResponse> deleteCategory(@PathVariable String serverId,
                                                         @PathVariable String categoryId,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        ServerResponse response = categoryService.deleteCategory(serverId, categoryId, userDetails.getUsername());
        return ResponseEntity.ok(sanitizeResponse(response));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @PatchMapping("/reorder")
    public ResponseEntity<ServerResponse> reorder(@PathVariable String serverId,
                                                  @Valid @RequestBody ReorderRequest request,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        ServerResponse response = categoryService.reorder(serverId, request, userDetails.getUsername());
        return ResponseEntity.ok(sanitizeResponse(response));
    }

    private ServerResponse sanitizeResponse(ServerResponse response) {
        response.setName(escape(response.getName()));
        if (response.getMembers() != null) {
            response.getMembers().forEach(member -> {
                member.setName(escape(member.getName()));
                if (member.getUser() != null) {
                    member.getUser().setUsername(escape(member.getUser().getUsername()));
                    member.getUser().setFullName(escape(member.getUser().getFullName()));
                }
            });
        }
        if (response.getChannels() != null) {
            response.getChannels().forEach(channel -> channel.setName(escape(channel.getName())));
        }
        if (response.getCategories() != null) {
            response.getCategories().forEach(category -> category.setName(escape(category.getName())));
        }
        return response;
    }

    private String escape(String value) {
        return value == null ? null : HtmlUtils.htmlEscape(value);
    }
}
