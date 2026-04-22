package com.bizcord.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryUpdateRequest {
    @Size(min = 1, max = 100)
    @Pattern(regexp = "^[^<>\"'`]*$", message = "Category name contains unsupported characters")
    private String name;
}
