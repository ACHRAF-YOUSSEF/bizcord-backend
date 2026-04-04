package com.bizcord.backend.service;

import com.bizcord.backend.dto.ChannelCreateRequest;
import com.bizcord.backend.dto.ChannelUpdateRequest;
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
public class ChannelService {
    private static final String GENERAL_CHANNEL = "general";
    private static final String USER_NOT_FOUND = "User not found";
    private static final String NOT_A_MEMBER = "You are not a member of this server";
    private static final String SERVER_NOT_FOUND = "Server not found";

    private final ChannelRepository channelRepository;
    private final MemberRepository memberRepository;
    private final ServerRepository serverRepository;
    private final UserRepository userRepository;

    @Transactional
    public ServerResponse updateChannel(String channelId, String serverId, ChannelUpdateRequest request, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_FOUND));

        Member currentMember = memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_A_MEMBER));

        if (currentMember.getRole() == MemberRole.GUEST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to edit channels");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));

        if (!channel.getServer().getId().equals(serverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel does not belong to this server");
        }

        if (GENERAL_CHANNEL.equalsIgnoreCase(channel.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The 'general' channel cannot be edited");
        }

        if (request.getName() != null) {
            if (GENERAL_CHANNEL.equalsIgnoreCase(request.getName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot rename a channel to 'general'");
            }
            channel.setName(request.getName());
        }
        if (request.getType() != null) {
            channel.setType(request.getType());
        }

        channelRepository.save(channel);

        Server server = serverRepository.findByIdWithOwner(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, SERVER_NOT_FOUND));

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
                .channels(channels.stream().map(ch -> ServerResponse.ChannelItem.builder()
                        .id(ch.getId())
                        .name(ch.getName())
                        .type(ch.getType())
                        .userId(ch.getUser().getId())
                        .serverId(ch.getServer().getId())
                        .build()).toList())
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
    }

    @Transactional
    public ServerResponse deleteChannel(String channelId, String serverId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_FOUND));

        Member currentMember = memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_A_MEMBER));

        if (currentMember.getRole() == MemberRole.GUEST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete channels");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));

        if (!channel.getServer().getId().equals(serverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel does not belong to this server");
        }

        if (GENERAL_CHANNEL.equalsIgnoreCase(channel.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The 'general' channel cannot be deleted");
        }

        channelRepository.delete(channel);

        Server server = serverRepository.findByIdWithOwner(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, SERVER_NOT_FOUND));

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
                .channels(channels.stream().map(ch -> ServerResponse.ChannelItem.builder()
                        .id(ch.getId())
                        .name(ch.getName())
                        .type(ch.getType())
                        .userId(ch.getUser().getId())
                        .serverId(ch.getServer().getId())
                        .build()).toList())
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
    }

    @Transactional
    public ServerResponse createChannel(String serverId, ChannelCreateRequest request, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_FOUND));

        Member currentMember = memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_A_MEMBER));

        if (currentMember.getRole() == MemberRole.GUEST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to create channels");
        }

        Server server = serverRepository.findByIdWithOwner(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, SERVER_NOT_FOUND));

        Channel channel = Channel.builder()
                .name(request.getName())
                .type(request.getType())
                .user(currentUser)
                .server(server)
                .build();

        server.getChannels().add(channel);
        serverRepository.saveAndFlush(server);

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
                .channels(channels.stream().map(ch -> ServerResponse.ChannelItem.builder()
                        .id(ch.getId())
                        .name(ch.getName())
                        .type(ch.getType())
                        .userId(ch.getUser().getId())
                        .serverId(ch.getServer().getId())
                        .build()).toList())
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
    }
}

