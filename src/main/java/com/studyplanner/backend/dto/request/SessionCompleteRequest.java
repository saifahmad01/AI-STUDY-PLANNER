package com.studyplanner.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCompleteRequest {

    @Min(value = 1, message = "Actual duration must be at least 1 minute")
    private Integer actualDurationMinutes;

    @Min(value = 1, message = "Focus score must be between 1 and 10")
    @Max(value = 10, message = "Focus score must be between 1 and 10")
    private Short focusScore;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
