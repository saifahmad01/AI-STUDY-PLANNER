package com.studyplanner.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectRequest {

    @NotBlank(message = "Subject name is required")
    @Size(max = 150, message = "Subject name must not exceed 150 characters")
    private String name;

    @Size(max = 80, message = "Category must not exceed 80 characters")
    private String category;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code (e.g. #6B7280)")
    private String colorHex;
}
