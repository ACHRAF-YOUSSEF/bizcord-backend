package com.bizcord.backend.mapper;

import com.bizcord.backend.dto.MessageResponse;
import com.bizcord.backend.dto.MessageSearchResponse;
import com.bizcord.backend.entity.Member;
import com.bizcord.backend.entity.Message;
import com.bizcord.backend.entity.Reaction;
import com.bizcord.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(config = MapStructConfig.class)
public interface MessageMapper {
    @Mapping(source = "channel.id", target = "channelId")
    @Mapping(target = "reactions", ignore = true)
    @Mapping(target = "parentMessage", ignore = true)
    @Mapping(target = "pinned", expression = "java(message.getPinnedAt() != null)")
    @Mapping(target = "pinnedByUsername", expression = "java(message.getPinnedBy() != null ? message.getPinnedBy().getUsername2() : null)")
    MessageResponse toResponse(Message message);

    @Mapping(source = "channel.id", target = "channelId")
    @Mapping(source = "channel.name", target = "channelName")
    MessageSearchResponse toSearchResponse(Message message);

    MessageResponse.MemberItem toResponseMemberItem(Member member);

    @Mapping(source = "username2", target = "username")
    MessageResponse.UserItem toResponseUserItem(User user);

    MessageSearchResponse.MemberItem toSearchMemberItem(Member member);

    @Mapping(source = "username2", target = "username")
    MessageSearchResponse.UserItem toSearchUserItem(User user);

    default MessageResponse toResponse(Message message, List<Reaction> reactions, String currentUserId) {
        MessageResponse response = toResponse(message);
        response.setReactions(toReactionGroups(reactions, currentUserId));
        response.setParentMessage(toParentMessagePreview(message.getParentMessage()));
        return response;
    }

    default List<MessageResponse.ReactionGroup> toReactionGroups(List<Reaction> reactions, String currentUserId) {
        Map<String, List<Reaction>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji));

        return grouped.entrySet().stream()
                .map(entry -> MessageResponse.ReactionGroup.builder()
                        .emoji(entry.getKey())
                        .count(entry.getValue().size())
                        .userIds(entry.getValue().stream().map(reaction -> reaction.getUser().getId()).toList())
                        .me(entry.getValue().stream().anyMatch(reaction -> reaction.getUser().getId().equals(currentUserId)))
                        .build())
                .toList();
    }

    default MessageResponse.ParentMessagePreview toParentMessagePreview(Message parentMessage) {
        if (parentMessage == null) {
            return null;
        }

        String senderName = parentMessage.getMember() != null
                ? parentMessage.getMember().getUser().getUsername2()
                : "Unknown";

        return MessageResponse.ParentMessagePreview.builder()
                .id(parentMessage.getId())
                .content(parentMessage.isDeleted() ? null : parentMessage.getContent())
                .senderName(senderName)
                .build();
    }
}
