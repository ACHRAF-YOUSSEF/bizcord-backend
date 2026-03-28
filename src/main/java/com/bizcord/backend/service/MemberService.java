package com.bizcord.backend.service;

import com.bizcord.backend.dto.MemberRoleUpdateRequest;
import com.bizcord.backend.dto.ServerResponse;
import com.bizcord.backend.entity.Channel;
import com.bizcord.backend.entity.Member;
import com.bizcord.backend.entity.MemberRole;
import com.bizcord.backend.entity.Server;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.repository.ChannelRepository;
import com.bizcord.backend.repository.MemberRepository;
import com.bizcord.backend.repository.ServerRepository;
import com.bizcord.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;

    @Transactional
    public ServerResponse updateMemberRole(String memberId, String serverId, MemberRoleUpdateRequest request, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Member currentMember = memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not a member of this server"));

        Member targetMember = memberRepository.findByIdWithUserAndServer(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (!targetMember.getServer().getId().equals(serverId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in this server");
        }

        if (targetMember.getId().equals(currentMember.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change your own role");
        }

        MemberRole currentRole = currentMember.getRole();
        MemberRole targetRole = targetMember.getRole();

        if (currentRole == MemberRole.GUEST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to change roles");
        }

        if (currentRole == MemberRole.MODERATOR) {
            if (targetRole == MemberRole.ADMIN || targetRole == MemberRole.MODERATOR) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Moderators can only change the role of guests");
            }
        }

        if (request.getRole() == MemberRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot assign the ADMIN role");
        }

        targetMember.setRole(request.getRole());
        memberRepository.saveAndFlush(targetMember);

        return buildServerResponse(serverId);
    }

    @Transactional
    public ServerResponse kickMember(String memberId, String serverId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Member currentMember = memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not a member of this server"));

        Member targetMember = memberRepository.findByIdWithUserAndServer(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (!targetMember.getServer().getId().equals(serverId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in this server");
        }

        if (targetMember.getId().equals(currentMember.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot kick yourself");
        }

        MemberRole currentRole = currentMember.getRole();
        MemberRole targetRole = targetMember.getRole();

        if (currentRole == MemberRole.GUEST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to kick members");
        }

        if (currentRole == MemberRole.MODERATOR && targetRole == MemberRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Moderators cannot kick the admin");
        }

        memberRepository.delete(targetMember);
        memberRepository.flush();

        return buildServerResponse(serverId);
    }

    private ServerResponse buildServerResponse(String serverId) {
        Server server = serverRepository.findByIdWithOwner(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Server not found"));

        List<Member> members = memberRepository.findAllByServerIdWithUserAndServer(serverId);
        List<Channel> channels = channelRepository.findAllByServerIdWithUserAndServer(serverId);

        return ServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .imageUrl(server.getImageUrl())
                .inviteCode(server.getInviteCode())
                .userId(server.getUser().getId())
                .members(members.stream().map(member -> ServerResponse.MemberItem.builder()
                        .id(member.getId())
                        .name(member.getName())
                        .role(member.getRole())
                        .serverId(member.getServer().getId())
                        .user(ServerResponse.UserItem.builder()
                                .id(member.getUser().getId())
                                .username(member.getUser().getUsername2())
                                .email(member.getUser().getEmail())
                                .fullName(member.getUser().getFullName())
                                .imageUrl(member.getUser().getImageUrl())
                                .createdAt(member.getUser().getCreatedAt())
                                .updatedAt(member.getUser().getUpdatedAt())
                                .build())
                        .build()).toList())
                .channels(channels.stream().map(channel -> ServerResponse.ChannelItem.builder()
                        .id(channel.getId())
                        .name(channel.getName())
                        .type(channel.getType())
                        .userId(channel.getUser().getId())
                        .serverId(channel.getServer().getId())
                        .build()).toList())
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
    }
}
