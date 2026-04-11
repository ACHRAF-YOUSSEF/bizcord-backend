package com.bizcord.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@EqualsAndHashCode(exclude = {"server", "channels"})
@ToString(exclude = {"server", "channels"})
@Entity(name = "bizcord_channel_categories")
@EntityListeners(AuditingEntityListener.class)
public class ChannelCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    private String name;

    private int position;

    @Builder.Default
    private boolean defaultCategory = false;

    @ManyToOne(fetch = FetchType.LAZY)
    private Server server;

    @Builder.Default
    @OneToMany(mappedBy = "category", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("position ASC")
    private List<Channel> channels = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
