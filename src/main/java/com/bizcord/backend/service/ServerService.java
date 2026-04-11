package com.bizcord.backend.service;

import com.bizcord.backend.dto.ServerCreateRequest;
import com.bizcord.backend.dto.ServerResponse;
import com.bizcord.backend.dto.ServerUpdateRequest;
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
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServerService {
    private static final String CANONICAL_IMAGE_PREFIX = "/api/uploads/images/";
    private static final String LEGACY_IMAGE_PREFIX = "/uploads/images/";

    private final ServerRepository serverRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final ChannelRepository channelRepository;
    private final ChannelCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ServerResponse> getServersThatTheCurrentUserIsMemberOf(String email) {
        User currentUser = getCurrentUser(email);
        List<Server> servers = serverRepository.findAllByMemberUserIdWithOwner(currentUser.getId());
        if (servers.isEmpty()) {
            return List.of();
        }

        List<String> serverIds = servers.stream().map(Server::getId).toList();
        var membersByServerId = memberRepository.findAllByServerIdInWithUserAndServer(serverIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(member -> member.getServer().getId()));
        var channelsByServerId = channelRepository.findAllByServerIdInWithUserAndServer(serverIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(channel -> channel.getServer().getId()));

        // Load all categories for all servers in one batch
        var allCategories = new java.util.ArrayList<ChannelCategory>();
        for (String sid : serverIds) {
            allCategories.addAll(categoryRepository.findAllByServerIdWithServer(sid));
        }
        var categoriesByServerId = allCategories.stream()
                .collect(java.util.stream.Collectors.groupingBy(cat -> cat.getServer().getId()));

        return servers.stream()
                .map(server -> ChannelCategoryService.toResponse(
                        server,
                        membersByServerId.getOrDefault(server.getId(), List.of()),
                        channelsByServerId.getOrDefault(server.getId(), List.of()),
                        categoriesByServerId.getOrDefault(server.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ServerResponse getServerById(String serverId, String email) {
        User currentUser = getCurrentUser(email);
        Server server = getServerWithOwner(serverId);
        boolean isMember = memberRepository.existsByServerIdAndUserId(serverId, currentUser.getId());

        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.SERVER_NOT_FOUND);
        }

        return buildFullResponse(serverId, server);
    }

    @Transactional
    public ServerResponse createServer(ServerCreateRequest request, String email) {
        User currentUser = getCurrentUser(email);

        Server server = Server
                .builder()
                .name(request.getName())
                .imageUrl(normalizeServerImageUrl(request.getImageUrl()))
                .inviteCode(generateInviteCode())
                .user(currentUser)
                .build();

        Member ownerMember = Member
                .builder()
                .name(currentUser.getUsername2())
                .role(MemberRole.ADMIN)
                .user(currentUser)
                .server(server)
                .build();

        ChannelCategory textCategory = ChannelCategory.builder()
                .name("Text Channels")
                .position(0)
                .server(server)
                .defaultCategory(true)
                .build();

        ChannelCategory voiceCategory = ChannelCategory.builder()
                .name("Voice Channels")
                .position(1)
                .server(server)
                .build();

        Channel generalChannel = Channel
                .builder()
                .name("general")
                .type(ChannelType.TEXT)
                .position(0)
                .user(currentUser)
                .server(server)
                .category(textCategory)
                .build();

        server.getMembers().add(ownerMember);
        server.getChannels().add(generalChannel);
        server.getCategories().add(textCategory);
        server.getCategories().add(voiceCategory);

        Server saved = serverRepository.save(server);
        return ChannelCategoryService.toResponse(saved, List.of(ownerMember), List.of(generalChannel), List.of(textCategory, voiceCategory));
    }

    @Transactional
    public ServerResponse updateServer(String serverId, ServerUpdateRequest request, String email) {
        User currentUser = getCurrentUser(email);
        Server server = getServerWithOwner(serverId);

        if (!server.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.SERVER_UPDATE_FORBIDDEN);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            server.setName(request.getName());
        }

        if (request.getImageUrl() != null) {
            server.setImageUrl(normalizeServerImageUrl(request.getImageUrl()));
        }

        return buildFullResponse(serverId, server);
    }

    @Transactional(readOnly = true)
    public ServerResponse getServerByInviteCode(String inviteCode, String email) {
        getCurrentUser(email);
        Server server = serverRepository.findByInviteCodeWithOwner(inviteCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.SERVER_INVALID_INVITE_CODE));

        return buildFullResponse(server.getId(), server);
    }

    @Transactional
    public ServerResponse joinServer(String inviteCode, String email) {
        User currentUser = getCurrentUser(email);
        Server server = serverRepository.findByInviteCodeWithOwner(inviteCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.SERVER_INVALID_INVITE_CODE));

        boolean alreadyMember = memberRepository.existsByServerIdAndUserId(server.getId(), currentUser.getId());

        if (alreadyMember) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ErrorMessages.SERVER_ALREADY_MEMBER);
        }

        Member newMember = Member
                .builder()
                .name(currentUser.getUsername2())
                .role(MemberRole.GUEST)
                .user(currentUser)
                .server(server)
                .build();

        server.getMembers().add(newMember);
        serverRepository.saveAndFlush(server);

        return buildFullResponse(server.getId(), server);
    }

    @Transactional
    public ServerResponse newInviteCode(String serverId, String email) {
        User currentUser = getCurrentUser(email);
        Server server = getServerWithOwner(serverId);

        if (!server.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.SERVER_INVITE_REGEN_FORBIDDEN);
        }

        server.setInviteCode(generateInviteCode());

        return buildFullResponse(serverId, server);
    }

    @Transactional
    public ServerResponse leaveServer(String serverId, String email) {
        User currentUser = getCurrentUser(email);

        Server server = getServerWithOwner(serverId);

        if (server.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.SERVER_OWNER_LEAVE_FORBIDDEN);
        }

        Member member = memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.NOT_A_MEMBER));

        memberRepository.delete(member);
        memberRepository.flush();

        return buildFullResponse(serverId, server);
    }

    @Transactional
    public void deleteServer(String serverId, String email) {
        User currentUser = getCurrentUser(email);
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.SERVER_NOT_FOUND));

        if (!server.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.SERVER_DELETE_FORBIDDEN);
        }

        serverRepository.delete(server);
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));
    }

    private Server getServerWithOwner(String serverId) {
        return serverRepository.findByIdWithOwner(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.SERVER_NOT_FOUND));
    }

    private String generateInviteCode() {
        String code;

        do {
            code = UUID.randomUUID().toString();
        } while (serverRepository.existsByInviteCode(code));

        return code;
    }

    private String normalizeServerImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }

        String value = imageUrl.trim();
        String fileName = extractImageFileName(value);

        if (!StringUtils.hasText(fileName)
                || fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.SERVER_INVALID_IMAGE_URL);
        }

        return CANONICAL_IMAGE_PREFIX + fileName;
    }

    private String extractImageFileName(String value) {
        if (value.startsWith(CANONICAL_IMAGE_PREFIX) || value.startsWith(LEGACY_IMAGE_PREFIX)) {
            return extractImageFileNameFromPath(value);
        }

        if (value.startsWith("http://") || value.startsWith("https://")) {
            return extractImageFileNameFromAbsoluteUrl(value);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.SERVER_INVALID_IMAGE_URL);
    }

    private String extractImageFileNameFromAbsoluteUrl(String value) {
        try {
            String path = URI.create(value).getPath();
            if (!StringUtils.hasText(path)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.SERVER_INVALID_IMAGE_URL);
            }

            return extractImageFileNameFromPath(path);
        } catch (IllegalArgumentException _) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.SERVER_INVALID_IMAGE_URL);
        }
    }

    private String extractImageFileNameFromPath(String path) {
        if (path.startsWith(CANONICAL_IMAGE_PREFIX)) {
            return path.substring(CANONICAL_IMAGE_PREFIX.length());
        }

        if (path.startsWith(LEGACY_IMAGE_PREFIX)) {
            return path.substring(LEGACY_IMAGE_PREFIX.length());
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.SERVER_INVALID_IMAGE_URL);
    }

    private ServerResponse buildFullResponse(String serverId, Server server) {
        List<Member> members = memberRepository.findAllByServerIdWithUserAndServer(serverId);
        List<Channel> channels = channelRepository.findAllByServerIdWithUserAndServer(serverId);
        List<ChannelCategory> categories = categoryRepository.findAllByServerIdWithServer(serverId);
        return ChannelCategoryService.toResponse(server, members, channels, categories);
    }
}
