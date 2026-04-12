package com.bizcord.backend.service;

import com.bizcord.backend.dto.PasswordChangeRequest;
import com.bizcord.backend.dto.PresenceEvent;
import com.bizcord.backend.dto.UserProfileResponse;
import com.bizcord.backend.dto.UserProfileUpdateRequest;
import com.bizcord.backend.entity.Member;
import com.bizcord.backend.entity.Server;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.entity.UserStatus;
import com.bizcord.backend.repository.MemberRepository;
import com.bizcord.backend.repository.ServerRepository;
import com.bizcord.backend.repository.UserRepository;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        return toProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getFriends(String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        List<Server> servers = serverRepository.findAllByMemberUserIdWithOwner(currentUser.getId());
        if (servers.isEmpty()) {
            return List.of();
        }

        List<String> serverIds = servers.stream().map(Server::getId).toList();
        List<Member> allMembers = memberRepository.findAllByServerIdInWithUserAndServer(serverIds);

        return allMembers.stream()
                .filter(u -> !u.getUser().getId().equals(currentUser.getId()))
                .collect(Collectors.toMap(m -> m.getUser().getId(), m -> m, (a, _) -> a))
                .values()
                .stream()
                .map(this::toProfileResponse)
                .toList();
    }

    @Transactional
    public void updateStatus(String email, UserStatus status) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        user.setStatus(status);
        userRepository.save(user);

        broadcastPresence(user.getId(), status);
    }

    @Transactional
    public void setStatusOnConnect(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setStatus(UserStatus.ONLINE);
            userRepository.save(user);
            broadcastPresence(user.getId(), UserStatus.ONLINE);
        });
    }

    @Transactional
    public void setStatusOnDisconnect(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setStatus(UserStatus.OFFLINE);
            userRepository.save(user);
            broadcastPresence(user.getId(), UserStatus.OFFLINE);
        });
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UserProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateAvatar(String email, String imageUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        user.setImageUrl(imageUrl);
        userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public void changePassword(String email, PasswordChangeRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        userRepository.delete(user);
    }

    private void broadcastPresence(String userId, UserStatus status) {
        List<Member> members = memberRepository.findAllByUserIdWithUserAndServer(userId);
        PresenceEvent event = PresenceEvent.builder()
                .type("PRESENCE_UPDATE")
                .userId(userId)
                .status(status)
                .build();
        for (Member member : members) {
            messagingTemplate.convertAndSend(
                    "/topic/servers/" + member.getServer().getId() + "/presence", event);
        }
        messagingTemplate.convertAndSend("/topic/users/" + userId + "/presence", event);
    }

    private UserProfileResponse toProfileResponse(Member member) {
        User user = member.getUser();

        return UserProfileResponse
                .builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername2())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse
                .builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername2())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
