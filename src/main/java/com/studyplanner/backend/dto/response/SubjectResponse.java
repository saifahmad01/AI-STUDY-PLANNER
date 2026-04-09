package com.studyplanner.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectResponse {

    private UUID id;
    private String name;
    private String category;
    private String colorHex;
    private Boolean isArchived;
    private LocalDateTime createdAt;
}
