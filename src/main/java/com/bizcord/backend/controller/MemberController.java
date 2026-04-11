package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.MemberRoleUpdateRequest;
import com.bizcord.backend.dto.ServerResponse;
import com.bizcord.backend.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @RateLimit(limit = 15, keyType = RateLimitKeyType.UID)
    @PatchMapping("/{memberId}")
    public ResponseEntity<ServerResponse> updateMemberRole(@PathVariable String memberId,
                                                           @RequestParam String serverId,
                                                           @Valid @RequestBody MemberRoleUpdateRequest request,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(memberService.updateMemberRole(memberId, serverId, request, userDetails.getUsername()));
    }

    @RateLimit(limit = 15, keyType = RateLimitKeyType.UID)
    @DeleteMapping("/{memberId}")
    public ResponseEntity<ServerResponse> kickMember(@PathVariable String memberId,
                                                     @RequestParam String serverId,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(memberService.kickMember(memberId, serverId, userDetails.getUsername()));
    }

    @RateLimit(limit = 15, keyType = RateLimitKeyType.UID)
    @PostMapping("/{memberId}/ban")
    public ResponseEntity<ServerResponse> banMember(@PathVariable String memberId,
                                                    @RequestParam String serverId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(memberService.banMember(memberId, serverId, userDetails.getUsername()));
    }

    @RateLimit(limit = 15, keyType = RateLimitKeyType.UID)
    @DeleteMapping("/bans/{userId}")
    public ResponseEntity<Void> unbanUser(@PathVariable String userId,
                                          @RequestParam String serverId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        memberService.unbanUser(userId, serverId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping("/bans")
    public ResponseEntity<List<ServerResponse.BannedUserItem>> getBannedUsers(@RequestParam String serverId,
                                                                              @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(memberService.getBannedUsers(serverId, userDetails.getUsername()));
    }
}
