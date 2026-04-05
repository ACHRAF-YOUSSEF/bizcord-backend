package com.bizcord.backend.service;

import com.bizcord.backend.dto.ConversationResponse;
import com.bizcord.backend.entity.Conversation;
import com.bizcord.backend.entity.Member;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.repository.ConversationRepository;
import com.bizcord.backend.repository.MemberRepository;
import com.bizcord.backend.repository.UserRepository;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(String conversationId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        Conversation conversation = conversationRepository.findByIdWithMembers(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CONVERSATION_NOT_FOUND));

        assertParticipant(conversation, currentUser);

        return toResponse(conversation);
    }

    @Transactional
    public ConversationResponse getOrCreateConversation(String memberId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        Member targetMember = memberRepository.findByIdWithUserAndServer(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.MEMBER_NOT_FOUND));

        User targetUser = targetMember.getUser();

        Optional<Conversation> existing = conversationRepository.findByUserIds(currentUser.getId(), targetUser.getId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        List<Member> currentUserMembers = memberRepository.findAllByUserIdWithUserAndServer(currentUser.getId());
        List<Member> targetUserMembers = memberRepository.findAllByUserIdWithUserAndServer(targetUser.getId());

        Set<String> targetUserServerIds = targetUserMembers.stream()
                .map(m -> m.getServer().getId())
                .collect(Collectors.toSet());

        Member currentUserMemberInCommonServer = currentUserMembers.stream()
                .filter(m -> targetUserServerIds.contains(m.getServer().getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.CONVERSATION_NO_COMMON_SERVER));

        String commonServerId = currentUserMemberInCommonServer.getServer().getId();

        Member targetUserMemberInCommonServer = targetUserMembers.stream()
                .filter(m -> m.getServer().getId().equals(commonServerId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.CONVERSATION_NO_COMMON_SERVER));

        Conversation conversation = Conversation.builder()
                .member1(currentUserMemberInCommonServer)
                .member2(targetUserMemberInCommonServer)
                .build();

        conversationRepository.save(conversation);

        Conversation saved = conversationRepository.findByIdWithMembers(conversation.getId())
                .orElseThrow();

        return toResponse(saved);
    }

    private ConversationResponse toResponse(Conversation c) {
        return ConversationResponse.builder()
                .id(c.getId())
                .member1(toMemberItem(c.getMember1()))
                .member2(toMemberItem(c.getMember2()))
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private ConversationResponse.MemberItem toMemberItem(Member member) {
        return ConversationResponse.MemberItem.builder()
                .id(member.getId())
                .name(member.getName())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .user(toUserItem(member.getUser()))
                .build();
    }

    private ConversationResponse.UserItem toUserItem(User user) {
        return ConversationResponse.UserItem.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername2())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private void assertParticipant(Conversation conversation, User user) {
        boolean isParticipant = conversation.getMember1().getUser().getId().equals(user.getId())
                || conversation.getMember2().getUser().getId().equals(user.getId());
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CONVERSATION_NOT_A_PARTICIPANT);
        }
    }
}
