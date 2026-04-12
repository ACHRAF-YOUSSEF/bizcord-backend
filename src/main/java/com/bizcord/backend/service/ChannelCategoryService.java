package com.bizcord.backend.service;

import com.bizcord.backend.dto.*;
import com.bizcord.backend.entity.*;
import com.bizcord.backend.mapper.ServerMapper;
import com.bizcord.backend.repository.*;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelCategoryService {

    private final ChannelCategoryRepository categoryRepository;
    private final ChannelRepository channelRepository;
    private final MemberRepository memberRepository;
    private final ServerRepository serverRepository;
    private final UserRepository userRepository;

    @Transactional
    public ServerResponse createCategory(String serverId, CategoryCreateRequest request, String email) {
        User currentUser = getUser(email);
        assertNotGuest(serverId, currentUser.getId());

        Server server = getServer(serverId);

        int nextPosition = (int) categoryRepository.countByServerId(serverId);

        ChannelCategory category = ChannelCategory.builder()
                .name(request.getName())
                .position(nextPosition)
                .server(server)
                .build();

        server.getCategories().add(category);
        serverRepository.saveAndFlush(server);

        return buildFullResponse(serverId);
    }

    @Transactional
    public ServerResponse updateCategory(String serverId, String categoryId, CategoryUpdateRequest request, String email) {
        User currentUser = getUser(email);
        assertNotGuest(serverId, currentUser.getId());

        ChannelCategory category = categoryRepository.findByIdWithServer(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CATEGORY_NOT_FOUND));

        if (!category.getServer().getId().equals(serverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.CATEGORY_WRONG_SERVER);
        }

        if (request.getName() != null) {
            category.setName(request.getName());
        }

        categoryRepository.save(category);
        return buildFullResponse(serverId);
    }

    @Transactional
    public ServerResponse deleteCategory(String serverId, String categoryId, String email) {
        User currentUser = getUser(email);
        assertNotGuest(serverId, currentUser.getId());

        ChannelCategory category = categoryRepository.findByIdWithServer(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CATEGORY_NOT_FOUND));

        if (!category.getServer().getId().equals(serverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.CATEGORY_WRONG_SERVER);
        }

        if (category.isDefaultCategory()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CATEGORY_DEFAULT_DELETE);
        }

        long count = categoryRepository.countByServerId(serverId);
        if (count <= 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CATEGORY_LAST_DELETE);
        }

        // Move channels from deleted category to null (uncategorized)
        List<Channel> orphanedChannels = channelRepository.findAllByServerIdWithUserAndServer(serverId).stream()
                .filter(ch -> ch.getCategory() != null && ch.getCategory().getId().equals(categoryId))
                .toList();

        for (Channel ch : orphanedChannels) {
            ch.setCategory(null);
        }
        channelRepository.saveAll(orphanedChannels);

        categoryRepository.delete(category);
        categoryRepository.flush();

        // Re-sequence remaining category positions
        List<ChannelCategory> remaining = categoryRepository.findAllByServerIdWithServer(serverId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }
        categoryRepository.saveAll(remaining);

        return buildFullResponse(serverId);
    }

    @Transactional
    public ServerResponse reorder(String serverId, ReorderRequest request, String email) {
        User currentUser = getUser(email);
        assertNotGuest(serverId, currentUser.getId());

        List<ChannelCategory> categories = categoryRepository.findAllByServerIdWithServer(serverId);
        Map<String, ChannelCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(ChannelCategory::getId, Function.identity()));

        List<Channel> channels = channelRepository.findAllByServerIdWithUserAndServer(serverId);
        Map<String, Channel> channelMap = channels.stream()
                .collect(Collectors.toMap(Channel::getId, Function.identity()));

        for (int catIdx = 0; catIdx < request.getCategories().size(); catIdx++) {
            ReorderRequest.ReorderItem item = request.getCategories().get(catIdx);
            ChannelCategory cat = categoryMap.get(item.getCategoryId());
            if (cat == null) continue;

            cat.setPosition(catIdx);

            List<String> channelIds = item.getChannelIds();
            for (int chIdx = 0; chIdx < channelIds.size(); chIdx++) {
                Channel ch = channelMap.get(channelIds.get(chIdx));
                if (ch == null) continue;
                ch.setCategory(cat);
                ch.setPosition(chIdx);
            }
        }

        List<String> uncategorizedIds = request.getUncategorizedChannelIds();
        if (uncategorizedIds != null) {
            for (int i = 0; i < uncategorizedIds.size(); i++) {
                Channel ch = channelMap.get(uncategorizedIds.get(i));
                if (ch == null) continue;
                ch.setCategory(null);
                ch.setPosition(i);
            }
        }

        categoryRepository.saveAll(categories);
        channelRepository.saveAll(channels);

        return buildFullResponse(serverId);
    }

    private ServerResponse buildFullResponse(String serverId) {
        Server server = getServer(serverId);
        List<Member> members = memberRepository.findAllByServerIdWithUserAndServer(serverId);
        List<Channel> channels = channelRepository.findAllByServerIdWithUserAndServer(serverId);
        List<ChannelCategory> categories = categoryRepository.findAllByServerIdWithServer(serverId);
        return toResponse(server, members, channels, categories);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));
    }

    private Server getServer(String serverId) {
        return serverRepository.findByIdWithOwner(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.SERVER_NOT_FOUND));
    }

    private void assertNotGuest(String serverId, String userId) {
        Member member = memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.NOT_A_MEMBER));
        if (member.getRole() == MemberRole.GUEST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CATEGORY_CREATE_FORBIDDEN);
        }
    }

    static ServerResponse toResponse(Server server, List<Member> members, List<Channel> channels, List<ChannelCategory> categories) {
        return ServerMapper.INSTANCE.toResponse(server, members, channels, categories);
    }
}
