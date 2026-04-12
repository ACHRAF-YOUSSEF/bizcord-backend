package com.bizcord.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "conversation"})
@ToString(exclude = {"user", "conversation"})
@Entity(name = "bizcord_direct_messages")
@EntityListeners(AuditingEntityListener.class)
public class DirectMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Size(max = 2000)
    private String content;

    @Builder.Default
    private List<String> attachments = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    private DirectMessage parentMessage;

    @Builder.Default
    private boolean deleted = false;

    private LocalDateTime pinnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private User pinnedBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
