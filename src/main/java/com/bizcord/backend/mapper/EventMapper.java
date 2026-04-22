package com.bizcord.backend.mapper;

import com.bizcord.backend.dto.EventResponse;
import com.bizcord.backend.entity.Event;
import com.bizcord.backend.entity.EventAttendee;
import com.bizcord.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface EventMapper {
    @Mapping(source = "server.id", target = "serverId")
    @Mapping(source = "creator", target = "creator")
    @Mapping(target = "attendees", ignore = true)
    EventResponse toResponse(Event event);

    default EventResponse toResponse(Event event, List<EventAttendee> attendees) {
        EventResponse response = toResponse(event);
        response.setAttendees(attendees.stream().map(this::toAttendeeItem).toList());
        return response;
    }

    @Mapping(source = "username2", target = "username")
    EventResponse.CreatorItem toCreatorItem(User user);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.username2", target = "username")
    @Mapping(source = "user.imageUrl", target = "imageUrl")
    EventResponse.AttendeeItem toAttendeeItem(EventAttendee attendee);
}
