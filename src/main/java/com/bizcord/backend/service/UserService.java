package com.bizcord.backend.service;

import com.bizcord.backend.dto.UserProfileResponse;
import com.bizcord.backend.entity.Member;
import com.bizcord.backend.entity.Server;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.repository.MemberRepository;
import com.bizcord.backend.repository.ServerRepository;
import com.bizcord.backend.repository.UserRepository;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
                .filter(u -> !u.getId().equals(currentUser.getId()) && !u.getUser().getId().equals(currentUser.getId()))
                .collect(Collectors.toMap(Member::getId, u -> u, (a, b) -> a))
                .values()
                .stream()
                .map(this::toProfileResponse)
                .toList();
    }

    private UserProfileResponse toProfileResponse(Member member) {
        User user = member.getUser();

        return UserProfileResponse
                .builder()
                .id(member.getId())
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
