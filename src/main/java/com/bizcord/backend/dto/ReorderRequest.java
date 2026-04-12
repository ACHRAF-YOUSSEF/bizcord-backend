package com.bizcord.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReorderRequest {
    @NotNull
    @Valid
    private List<ReorderItem> categories;

    private List<String> uncategorizedChannelIds;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReorderItem {
        @NotNull
        private String categoryId;
        @NotNull
        private List<String> channelIds;
    }
}
