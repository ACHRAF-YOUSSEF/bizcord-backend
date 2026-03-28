package com.bizcord.backend.controller;

import com.bizcord.backend.dto.MemberRoleUpdateRequest;
import com.bizcord.backend.dto.ServerResponse;
import com.bizcord.backend.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @PatchMapping("/{memberId}")
    public ResponseEntity<ServerResponse> updateMemberRole(@PathVariable String memberId,
                                                           @RequestParam String serverId,
                                                           @Valid @RequestBody MemberRoleUpdateRequest request,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(memberService.updateMemberRole(memberId, serverId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<ServerResponse> kickMember(@PathVariable String memberId,
                                                     @RequestParam String serverId,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(memberService.kickMember(memberId, serverId, userDetails.getUsername()));
    }
}

