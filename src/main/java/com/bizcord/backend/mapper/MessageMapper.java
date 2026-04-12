package com.bizcord.backend.mapper;

import com.bizcord.backend.dto.MessageResponse;
import com.bizcord.backend.entity.Member;
import com.bizcord.backend.entity.Message;
import com.bizcord.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MessageMapper {
    MessageMapper INSTANCE = Mappers.getMapper(MessageMapper.class);

    @Mapping(source = "channel.id", target = "channelId")
    @Mapping(target = "reactions", ignore = true)
    MessageResponse toResponse(Message message);

    MessageResponse.MemberItem toMemberItem(Member member);

    @Mapping(source = "username2", target = "username")
    @Mapping(source = "status", target = "status")
    MessageResponse.UserItem toUserItem(User user);
}
