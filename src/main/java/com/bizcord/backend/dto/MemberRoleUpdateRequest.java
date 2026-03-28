package com.bizcord.backend.dto;

import com.bizcord.backend.entity.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberRoleUpdateRequest {
    @NotNull
    private MemberRole role;
}

