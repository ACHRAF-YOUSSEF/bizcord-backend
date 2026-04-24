package com.bizcord.backend.mapper;

import com.bizcord.backend.dto.UserProfileResponse;
import com.bizcord.backend.entity.Member;
import com.bizcord.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface UserProfileMapper {

    @Mapping(source = "username2", target = "username")
    UserProfileResponse toResponse(User user);

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.username2", target = "username")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.imageUrl", target = "imageUrl")
    @Mapping(source = "user.status", target = "status")
    @Mapping(source = "user.preferredStatus", target = "preferredStatus")
    @Mapping(source = "user.createdAt", target = "createdAt")
    @Mapping(source = "user.updatedAt", target = "updatedAt")
    UserProfileResponse toResponse(Member member);
}
