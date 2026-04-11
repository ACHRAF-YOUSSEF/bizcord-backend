package com.bizcord.backend.service;

import com.bizcord.backend.dto.MemberRoleUpdateRequest;
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
public class MemberService {
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final ChannelCategoryRepository categoryRepository;

    @Transactional
    public ServerResponse updateMemberRole(String memberId, String serverId, MemberRoleUpdateRequest request, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        Member currentMember = memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.NOT_A_MEMBER));

        Member targetMember = memberRepository.findByIdWithUserAndServer(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.MEMBER_NOT_FOUND));

        if (!targetMember.getServer().getId().equals(serverId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.MEMBER_NOT_IN_SERVER);
        }

        if (targetMember.getId().equals(currentMember.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.MEMBER_SELF_ROLE_CHANGE);
        }

        MemberRole currentRole = currentMember.getRole();
        MemberRole targetRole = targetMember.getRole();

        if (currentRole == MemberRole.GUEST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.MEMBER_ROLE_NO_PERMISSION);
        }

        if (currentRole == MemberRole.MODERATOR && (targetRole == MemberRole.ADMIN || targetRole == MemberRole.MODERATOR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.MEMBER_MODERATOR_CANNOT_CHANGE_ADMIN);
        }

        if (request.getRole() == MemberRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.MEMBER_CANNOT_ASSIGN_ADMIN);
        }

        targetMember.setRole(request.getRole());
        memberRepository.saveAndFlush(targetMember);

        return buildServerResponse(serverId);
    }

    @Transactional
    public ServerResponse kickMember(String memberId, String serverId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        Member currentMember = memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.NOT_A_MEMBER));

        Member targetMember = memberRepository.findByIdWithUserAndServer(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.MEMBER_NOT_FOUND));

        if (!targetMember.getServer().getId().equals(serverId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.MEMBER_NOT_IN_SERVER);
        }

        if (targetMember.getId().equals(currentMember.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.MEMBER_SELF_KICK);
        }

        MemberRole currentRole = currentMember.getRole();
        MemberRole targetRole = targetMember.getRole();

        if (currentRole == MemberRole.GUEST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.MEMBER_KICK_NO_PERMISSION);
        }

        if (currentRole == MemberRole.MODERATOR && targetRole == MemberRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.MEMBER_MODERATOR_CANNOT_KICK_ADMIN);
        }

        memberRepository.delete(targetMember);
        memberRepository.flush();

        return buildServerResponse(serverId);
    }

    private ServerResponse buildServerResponse(String serverId) {
        Server server = serverRepository.findByIdWithOwner(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.SERVER_NOT_FOUND));

        List<Member> members = memberRepository.findAllByServerIdWithUserAndServer(serverId);
        List<Channel> channels = channelRepository.findAllByServerIdWithUserAndServer(serverId);
        List<ChannelCategory> categories = categoryRepository.findAllByServerIdWithServer(serverId);

        return ChannelCategoryService.toResponse(server, members, channels, categories);
    }
}
