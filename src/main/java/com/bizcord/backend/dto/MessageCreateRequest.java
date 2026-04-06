package com.bizcord.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class MessageCreateRequest {
    private String content;
    private List<String> attachments;
}

