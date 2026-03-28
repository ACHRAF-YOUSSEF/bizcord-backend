package com.bizcord.backend.controller;

import com.bizcord.backend.dto.ServerCreateRequest;
import com.bizcord.backend.dto.ServerResponse;
import com.bizcord.backend.dto.ServerUpdateRequest;
import com.bizcord.backend.service.ServerService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {
    private final ServerService serverService;

    @GetMapping
    public ResponseEntity<List<ServerResponse>> getServers(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serverService.getServersThatTheCurrentUserIsMemberOf(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerResponse> getServer(@PathVariable String id,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serverService.getServerById(id, userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<ServerResponse> createServer(@Valid @RequestBody ServerCreateRequest request,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(CREATED)
                .body(serverService.createServer(request, userDetails.getUsername()));
    }

    @GetMapping("/invite/{inviteCode}")
    public ResponseEntity<ServerResponse> getServerByInviteCode(@PathVariable String inviteCode,
                                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serverService.getServerByInviteCode(inviteCode, userDetails.getUsername()));
    }

    @PatchMapping("/invite/{inviteCode}")
    public ResponseEntity<ServerResponse> joinServer(@PathVariable String inviteCode,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serverService.joinServer(inviteCode, userDetails.getUsername()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServerResponse> updateServer(@PathVariable String id,
                                                       @RequestBody ServerUpdateRequest request,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serverService.updateServer(id, request, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/invite-code")
    public ResponseEntity<ServerResponse> newInviteCode(@PathVariable String id,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serverService.newInviteCode(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/leave")
    public ResponseEntity<ServerResponse> leaveServer(@PathVariable String id,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serverService.leaveServer(id, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable String id,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        serverService.deleteServer(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
