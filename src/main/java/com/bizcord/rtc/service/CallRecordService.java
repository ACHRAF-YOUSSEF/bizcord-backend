package com.bizcord.rtc.service;

import com.bizcord.rtc.dto.CallHistoryResponse;
import com.bizcord.rtc.model.CallRecord;
import com.bizcord.rtc.model.CallStatus;
import com.bizcord.rtc.model.CallType;
import com.bizcord.rtc.repository.CallRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallRecordService {

    private final CallRecordRepository callRecordRepository;

    @Transactional
    public CallRecord createCallRecord(String roomId, String initiatorId,
                                       String targetId, CallType callType) {
        Optional<CallRecord> existing =
                callRecordRepository.findByRoomIdAndStatus(roomId, CallStatus.ONGOING);
        if (existing.isPresent()) {
            log.warn("CallRecord already ONGOING for room {} – returning existing", roomId);
            return existing.get();
        }

        CallRecord callRecord = CallRecord.builder()
                .roomId(roomId)
                .initiatorId(initiatorId)
                .targetId(targetId)
                .callType(callType)
                .status(CallStatus.ONGOING)
                .build();

        CallRecord saved = callRecordRepository.save(callRecord);
        log.info("Created CallRecord {} for room {} ({} → {})",
                saved.getId(), roomId, initiatorId, targetId);
        return saved;
    }

    @Transactional
    public void finalizeCallRecord(String roomId) {
        callRecordRepository.findByRoomIdAndStatus(roomId, CallStatus.ONGOING)
                .ifPresentOrElse(cr -> {
                    LocalDateTime now = LocalDateTime.now();
                    cr.setEndedAt(now);
                    cr.setStatus(CallStatus.COMPLETED);
                    if (cr.getStartedAt() != null) {
                        cr.setDurationSeconds(Duration.between(cr.getStartedAt(), now).toSeconds());
                    }
                    callRecordRepository.save(cr);
                    log.info("Finalized CallRecord {} for room {} — duration {}s",
                            cr.getId(), roomId, cr.getDurationSeconds());
                }, () -> log.debug("No ONGOING CallRecord for room {} — nothing to finalize", roomId));
    }

    @Transactional(readOnly = true)
    public Page<CallHistoryResponse> getCallHistory(String userId, Pageable pageable) {
        return callRecordRepository
                .findByInitiatorIdOrTargetIdOrderByStartedAtDesc(userId, userId, pageable)
                .map(cr -> new CallHistoryResponse(
                        cr.getId(),
                        cr.getRoomId(),
                        cr.getCallType(),
                        cr.getInitiatorId().equals(userId) ? cr.getTargetId() : cr.getInitiatorId(),
                        cr.getStartedAt(),
                        cr.getDurationSeconds(),
                        cr.getStatus()
                ));
    }
}
