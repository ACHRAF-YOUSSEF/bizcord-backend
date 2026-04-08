package com.bizcord.rtc.rest;

import com.bizcord.rtc.dto.CallHistoryResponse;
import com.bizcord.rtc.service.CallRecordService;
import com.bizcord.rtc.service.MediasoupSidecarService;
import com.bizcord.rtc.service.RtcRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CallHistoryController {
    private final CallRecordService callRecordService;
    private final RtcRoomService rtcRoomService;
    private final MediasoupSidecarService mediasoupSidecarService;

    @GetMapping("/calls/history")
    public ResponseEntity<Page<CallHistoryResponse>> getCallHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<CallHistoryResponse> history =
                callRecordService.getCallHistory(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/rtc/status")
    public ResponseEntity<Map<String, Object>> getRtcStatus() {
        return ResponseEntity.ok(Map.of(
                "roomCount",           rtcRoomService.getRoomCount(),
                "totalParticipants",   rtcRoomService.getTotalParticipants(),
                "mediasoupAvailable",  mediasoupSidecarService.isMediasoupAvailable()
        ));
    }
}

