package com.bizcord.backend.service;

import com.bizcord.backend.dto.PasswordChangeRequest;
import com.bizcord.backend.dto.PresenceEvent;
import com.bizcord.backend.dto.UserProfileResponse;
import com.bizcord.backend.dto.UserProfileUpdateRequest;
import com.bizcord.backend.entity.Member;
import com.bizcord.backend.entity.Server;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.entity.UserStatus;
import com.bizcord.backend.mapper.UserProfileMapper;
import com.bizcord.backend.repository.MemberRepository;
import com.bizcord.backend.repository.ServerRepository;
import com.bizcord.backend.repository.UserRepository;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.domain.PageRequest;
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
    private final UserProfileMapper userProfileMapper;

    public UserProfileResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        return userProfileMapper.toResponse(user);
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
                .map(userProfileMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> searchUsers(String query, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        if (query == null || query.isBlank()) {
            return List.of();
        }

        return userRepository.searchUsers(query.trim(), currentUser.getId(), PageRequest.of(0, 20))
                .stream()
                .map(userProfileMapper::toResponse)
                .toList();
    }

    @Transactional
    public void updateStatus(String email, UserStatus status) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        user.setPreferredStatus(status);
        user.setStatus(status == UserStatus.INVISIBLE ? UserStatus.OFFLINE : status);
        userRepository.save(user);

        broadcastPresence(user.getId(), status);
    }

    @Transactional
    public void setStatusOnConnect(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            UserStatus preferred = user.getPreferredStatus();
            if (preferred == UserStatus.INVISIBLE) {
                broadcastPresence(user.getId(), UserStatus.INVISIBLE);
            } else {
                user.setStatus(preferred);
                userRepository.save(user);
                broadcastPresence(user.getId(), preferred);
            }
        });
    }

    @Transactional
    public void setStatusOnDisconnect(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getStatus() != UserStatus.OFFLINE) {
                user.setStatus(UserStatus.OFFLINE);
                userRepository.save(user);
            }
            broadcastPresence(user.getId(), UserStatus.OFFLINE);
        });
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UserProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.username() != null) {
            user.setUsername(request.username());
        }
        userRepository.save(user);
        return userProfileMapper.toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateAvatar(String email, String imageUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        user.setImageUrl(imageUrl);
        userRepository.save(user);
        return userProfileMapper.toResponse(user);
    }

    @Transactional
    public void changePassword(String email, PasswordChangeRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        user.setDeleted(true);
        user.setEnabled(false);
        user.setUsername("Deleted Account");
        user.setFullName("Deleted Account");
        user.setEmail("deleted+" + user.getId() + "@bizcord.deleted");
        user.setPassword("[DELETED]");
        user.setImageUrl(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public void broadcastServerPresence(String serverId) {
        List<Member> members = memberRepository.findAllByServerIdWithUserAndServer(serverId);
        for (Member member : members) {
            User user = member.getUser();
            PresenceEvent event = new PresenceEvent("PRESENCE_UPDATE", user.getId(), user.getStatus());
            messagingTemplate.convertAndSend("/topic/servers/" + serverId + "/presence", event);
        }
    }

    private void broadcastPresence(String userId, UserStatus status) {
        // INVISIBLE users appear as OFFLINE to others
        UserStatus broadcastStatus = status == UserStatus.INVISIBLE ? UserStatus.OFFLINE : status;
        List<Member> members = memberRepository.findAllByUserIdWithUserAndServer(userId);
        PresenceEvent event = new PresenceEvent("PRESENCE_UPDATE", userId, broadcastStatus);
        for (Member member : members) {
            messagingTemplate.convertAndSend(
                    "/topic/servers/" + member.getServer().getId() + "/presence", event);
        }
        messagingTemplate.convertAndSend("/topic/users/" + userId + "/presence", event);
    }
}
