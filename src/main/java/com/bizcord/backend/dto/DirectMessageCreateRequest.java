package com.bizcord.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class DirectMessageCreateRequest {
    private String content;
    private List<String> attachments;
}

