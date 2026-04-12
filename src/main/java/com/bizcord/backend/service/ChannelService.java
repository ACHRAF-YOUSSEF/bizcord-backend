package com.bizcord.backend.service;

import com.bizcord.backend.dto.ChannelCreateRequest;
import com.bizcord.backend.dto.ChannelUpdateRequest;
import com.bizcord.backend.dto.ServerResponse;
import com.bizcord.backend.entity.*;
import com.bizcord.backend.repository.ChannelCategoryRepository;
import com.bizcord.backend.repository.ChannelRepository;
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

@Service
@RequiredArgsConstructor
public class ChannelService {
    private static final String GENERAL_CHANNEL = "general";

    private final ChannelRepository channelRepository;
    private final ChannelCategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final ServerRepository serverRepository;
    private final UserRepository userRepository;

    @Transactional
    public ServerResponse updateChannel(String channelId, String serverId, ChannelUpdateRequest request, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        Member currentMember = memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.NOT_A_MEMBER));

        if (currentMember.getRole() == MemberRole.GUEST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CHANNEL_EDIT_FORBIDDEN);
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CHANNEL_NOT_FOUND));

        if (!channel.getServer().getId().equals(serverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.CHANNEL_WRONG_SERVER);
        }

        if (GENERAL_CHANNEL.equalsIgnoreCase(channel.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CHANNEL_GENERAL_EDIT);
        }

        if (request.getName() != null) {
            if (GENERAL_CHANNEL.equalsIgnoreCase(request.getName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.CHANNEL_GENERAL_RENAME);
            }
            channel.setName(request.getName());
        }
        if (request.getType() != null) {
            channel.setType(request.getType());
        }

        channelRepository.save(channel);

        return buildFullResponse(serverId);
    }

    @Transactional
    public ServerResponse deleteChannel(String channelId, String serverId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        Member currentMember = memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.NOT_A_MEMBER));

        if (currentMember.getRole() == MemberRole.GUEST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CHANNEL_DELETE_FORBIDDEN);
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CHANNEL_NOT_FOUND));

        if (!channel.getServer().getId().equals(serverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.CHANNEL_WRONG_SERVER);
        }

        if (GENERAL_CHANNEL.equalsIgnoreCase(channel.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CHANNEL_GENERAL_DELETE);
        }

        channelRepository.delete(channel);

        return buildFullResponse(serverId);
    }

    @Transactional
    public ServerResponse createChannel(String serverId, ChannelCreateRequest request, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        Member currentMember = memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.NOT_A_MEMBER));

        if (currentMember.getRole() == MemberRole.GUEST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CHANNEL_CREATE_FORBIDDEN);
        }

        Server server = serverRepository.findByIdWithOwner(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.SERVER_NOT_FOUND));

        ChannelCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByIdWithServer(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CATEGORY_NOT_FOUND));
            if (!category.getServer().getId().equals(serverId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.CATEGORY_WRONG_SERVER);
            }
        }

        Channel channel = Channel.builder()
                .name(request.getName())
                .type(request.getType())
                .user(currentUser)
                .server(server)
                .category(category)
                .build();

        server.getChannels().add(channel);
        serverRepository.saveAndFlush(server);

        return buildFullResponse(serverId);
    }

    private ServerResponse buildFullResponse(String serverId) {
        Server server = serverRepository.findByIdWithOwner(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.SERVER_NOT_FOUND));
        List<Member> members = memberRepository.findAllByServerIdWithUserAndServer(serverId);
        List<Channel> channels = channelRepository.findAllByServerIdWithUserAndServer(serverId);
        List<ChannelCategory> categories = categoryRepository.findAllByServerIdWithServer(serverId);
        return ChannelCategoryService.toResponse(server, members, channels, categories);
    }
}

