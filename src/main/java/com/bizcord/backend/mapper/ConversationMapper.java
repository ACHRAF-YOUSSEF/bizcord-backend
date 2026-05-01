package com.bizcord.backend.mapper;

import com.bizcord.backend.dto.ConversationResponse;
import com.bizcord.backend.entity.Conversation;
import com.bizcord.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface ConversationMapper {
    @Mapping(source = "requester.id", target = "requesterId")
    ConversationResponse toResponse(Conversation conversation);

    @Mapping(source = "username2", target = "username")
    ConversationResponse.UserItem toUserItem(User user);
}
