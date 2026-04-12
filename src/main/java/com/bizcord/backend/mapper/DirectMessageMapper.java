package com.bizcord.backend.mapper;

import com.bizcord.backend.dto.DirectMessageResponse;
import com.bizcord.backend.entity.DirectMessage;
import com.bizcord.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DirectMessageMapper {
    DirectMessageMapper INSTANCE = Mappers.getMapper(DirectMessageMapper.class);

    @Mapping(source = "conversation.id", target = "conversationId")
    @Mapping(target = "reactions", ignore = true)
    @Mapping(target = "parentMessage", ignore = true)
    @Mapping(target = "pinned", expression = "java(directMessage.getPinnedAt() != null)")
    @Mapping(target = "pinnedByUsername", expression = "java(directMessage.getPinnedBy() != null ? directMessage.getPinnedBy().getUsername2() : null)")
    DirectMessageResponse toResponse(DirectMessage directMessage);

    @Mapping(source = "username2", target = "username")
    DirectMessageResponse.UserItem toUserItem(User user);
}
