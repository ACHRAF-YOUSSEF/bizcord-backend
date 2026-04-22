package com.bizcord.backend.mapper;

import com.bizcord.backend.dto.DirectMessageResponse;
import com.bizcord.backend.dto.DmSearchResponse;
import com.bizcord.backend.entity.DirectMessage;
import com.bizcord.backend.entity.Reaction;
import com.bizcord.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(config = MapStructConfig.class)
public interface DirectMessageMapper {
    @Mapping(source = "conversation.id", target = "conversationId")
    @Mapping(target = "reactions", ignore = true)
    @Mapping(target = "parentMessage", ignore = true)
    @Mapping(target = "pinned", expression = "java(directMessage.getPinnedAt() != null)")
    @Mapping(target = "pinnedByUsername", expression = "java(directMessage.getPinnedBy() != null ? directMessage.getPinnedBy().getUsername2() : null)")
    DirectMessageResponse toResponse(DirectMessage directMessage);

    @Mapping(source = "conversation.id", target = "conversationId")
    DmSearchResponse toSearchResponse(DirectMessage directMessage);

    @Mapping(source = "username2", target = "username")
    DirectMessageResponse.UserItem toResponseUserItem(User user);

    @Mapping(source = "username2", target = "username")
    DmSearchResponse.UserItem toSearchUserItem(User user);

    default DirectMessageResponse toResponse(DirectMessage directMessage, List<Reaction> reactions, String currentUserId) {
        DirectMessageResponse response = toResponse(directMessage);
        response.setReactions(toReactionGroups(reactions, currentUserId));
        response.setParentMessage(toParentMessagePreview(directMessage.getParentMessage()));
        return response;
    }

    default List<DirectMessageResponse.ReactionGroup> toReactionGroups(List<Reaction> reactions, String currentUserId) {
        Map<String, List<Reaction>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji));

        return grouped.entrySet().stream()
                .map(entry -> DirectMessageResponse.ReactionGroup.builder()
                        .emoji(entry.getKey())
                        .count(entry.getValue().size())
                        .userIds(entry.getValue().stream().map(reaction -> reaction.getUser().getId()).toList())
                        .me(entry.getValue().stream().anyMatch(reaction -> reaction.getUser().getId().equals(currentUserId)))
                        .build())
                .toList();
    }

    default DirectMessageResponse.ParentMessagePreview toParentMessagePreview(DirectMessage parentMessage) {
        if (parentMessage == null) {
            return null;
        }

        String senderName = parentMessage.getUser() != null
                ? parentMessage.getUser().getUsername2()
                : "Unknown";

        return DirectMessageResponse.ParentMessagePreview.builder()
                .id(parentMessage.getId())
                .content(parentMessage.isDeleted() ? null : parentMessage.getContent())
                .senderName(senderName)
                .build();
    }
}
