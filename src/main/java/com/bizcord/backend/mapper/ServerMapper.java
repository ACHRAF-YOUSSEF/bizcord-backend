package com.bizcord.backend.mapper;

import com.bizcord.backend.dto.ServerResponse;
import com.bizcord.backend.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ServerMapper {
    ServerMapper INSTANCE = Mappers.getMapper(ServerMapper.class);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "channels", ignore = true)
    @Mapping(target = "categories", ignore = true)
    ServerResponse toResponse(Server server);

    default ServerResponse toResponse(Server server, List<Member> members, List<Channel> channels, List<ChannelCategory> categories) {
        ServerResponse response = toResponse(server);
        response.setMembers(members.stream().map(this::toMemberItem).toList());
        response.setChannels(channels.stream().map(this::toChannelItem).toList());
        response.setCategories(categories.stream().map(this::toCategoryItem).toList());
        return response;
    }

    @Mapping(source = "server.id", target = "serverId")
    @Mapping(source = "user", target = "user", qualifiedByName = "toServerUserItem")
    ServerResponse.MemberItem toMemberItem(Member member);

    @Named("toServerUserItem")
    @Mapping(source = "username2", target = "username")
    @Mapping(source = "status", target = "status")
    ServerResponse.UserItem toUserItem(User user);

    @Mapping(source = "server.id", target = "serverId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "category.id", target = "categoryId")
    ServerResponse.ChannelItem toChannelItem(Channel channel);

    @Mapping(source = "server.id", target = "serverId")
    ServerResponse.CategoryItem toCategoryItem(ChannelCategory category);

    @Mapping(source = "user", target = "user", qualifiedByName = "toServerUserItem")
    ServerResponse.BannedUserItem toBannedUserItem(BannedUser bannedUser);
}
