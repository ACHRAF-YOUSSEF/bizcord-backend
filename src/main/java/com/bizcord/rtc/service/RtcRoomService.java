package com.bizcord.rtc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RtcRoomService {

    private final ConcurrentHashMap<String, Set<String>> rooms = new ConcurrentHashMap<>();

    private final CallRecordService callRecordService;

    public void joinRoom(String roomId, String userId) {
        rooms.computeIfAbsent(roomId, id -> ConcurrentHashMap.newKeySet()).add(userId);
        log.debug("User {} joined room {} — participants: {}", userId, roomId, rooms.get(roomId));
    }

    public void leaveRoom(String roomId, String userId) {
        Set<String> participants = rooms.get(roomId);
        if (participants == null) {
            return;
        }

        participants.remove(userId);
        log.debug("User {} left room {} — remaining: {}", userId, roomId, participants);

        if (participants.isEmpty()) {
            rooms.remove(roomId);
            log.info("Room {} is now empty — finalizing call record", roomId);
            callRecordService.finalizeCallRecord(roomId);
        }
    }

    public Set<String> getParticipants(String roomId) {
        Set<String> participants = rooms.get(roomId);
        return participants == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(participants);
    }

    public boolean isInRoom(String roomId, String userId) {
        Set<String> participants = rooms.get(roomId);
        return participants != null && participants.contains(userId);
    }

    public int getRoomCount() {
        return rooms.size();
    }

    public int getTotalParticipants() {
        return rooms.values().stream().mapToInt(Set::size).sum();
    }
}

