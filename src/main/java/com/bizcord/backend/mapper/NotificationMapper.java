package com.bizcord.backend.mapper;

import com.bizcord.backend.dto.NotificationResponse;
import com.bizcord.backend.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface NotificationMapper {
    NotificationMapper INSTANCE = Mappers.getMapper(NotificationMapper.class);

    NotificationResponse toResponse(Notification notification);
}
