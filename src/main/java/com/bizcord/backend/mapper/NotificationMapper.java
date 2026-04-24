package com.bizcord.backend.mapper;

import com.bizcord.backend.dto.NotificationResponse;
import com.bizcord.backend.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}
