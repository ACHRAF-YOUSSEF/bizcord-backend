package com.bizcord.rtc.repository;

import com.bizcord.rtc.model.CallRecord;
import com.bizcord.rtc.model.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CallRecordRepository extends JpaRepository<CallRecord, UUID> {
    Page<CallRecord> findByInitiatorIdOrTargetIdOrderByStartedAtDesc(
            String initiatorId,
            String targetId,
            Pageable pageable);

    Optional<CallRecord> findByRoomIdAndStatus(String roomId, CallStatus status);
}

