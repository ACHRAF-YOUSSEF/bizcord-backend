package com.bizcord.backend.mapper;

import com.bizcord.backend.dto.ConversationResponse;
import com.bizcord.backend.entity.Conversation;
import com.bizcord.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ConversationMapper {
    ConversationMapper INSTANCE = Mappers.getMapper(ConversationMapper.class);

    ConversationResponse toResponse(Conversation conversation);

    @Mapping(source = "username2", target = "username")
    ConversationResponse.UserItem toUserItem(User user);
}
