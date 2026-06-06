package com.studyplanner.backend.mapper;

import com.studyplanner.backend.dto.response.StudyPlanHistoryResponse;
import com.studyplanner.backend.dto.response.StudySessionResponse;
import com.studyplanner.backend.entity.StudyPlan;
import com.studyplanner.backend.entity.StudyPlanHistory;
import com.studyplanner.backend.entity.StudySession;
import com.studyplanner.backend.entity.StudySessionHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StudyPlanHistoryMapper {

    // ── Snapshot: StudyPlan → StudyPlanHistory ───────────────────────────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "plan", source = "plan")
    @Mapping(target = "versionNumber", source = "versionNumber")
    @Mapping(target = "title", source = "plan.title")
    @Mapping(target = "goal", source = "plan.goal")
    @Mapping(target = "difficulty", expression = "java(plan.getDifficulty().name())")
    @Mapping(target = "startDate", source = "plan.startDate")
    @Mapping(target = "endDate", source = "plan.endDate")
    @Mapping(target = "dailyHours", source = "plan.dailyHours")
    @Mapping(target = "status", expression = "java(plan.getStatus().name())")
    @Mapping(target = "totalSessions", source = "plan.totalSessions")
    @Mapping(target = "completedSessions", source = "plan.completedSessions")
    @Mapping(target = "changeReason", source = "plan.changeReason")
    @Mapping(target = "snapshotAt", ignore = true)   // defaults to LocalDateTime.now()
    @Mapping(target = "sessions", ignore = true)      // populated separately
    StudyPlanHistory toHistoryEntity(StudyPlan plan, int versionNumber);

    // ── Snapshot: StudySession → StudySessionHistory ────────────────────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "planHistory", source = "planHistory")
    @Mapping(target = "originalSessionId", source = "session.id")
    @Mapping(target = "scheduledDate", source = "session.scheduledDate")
    @Mapping(target = "topic", source = "session.topic")
    @Mapping(target = "durationMinutes", source = "session.durationMinutes")
    @Mapping(target = "completed", source = "session.completed")
    @Mapping(target = "actualDurationMinutes", source = "session.actualDurationMinutes")
    @Mapping(target = "focusScore", source = "session.focusScore")
    @Mapping(target = "notes", source = "session.notes")
    @Mapping(target = "completedAt", source = "session.completedAt")
    StudySessionHistory toSessionHistoryEntity(StudySession session, StudyPlanHistory planHistory);

    // ── Response: StudyPlanHistory → StudyPlanHistoryResponse ───────────

    @Mapping(target = "planId", source = "plan.id")
    @Mapping(target = "progressPercent",
            expression = "java(history.getTotalSessions() != null && history.getTotalSessions() > 0 ? "
                    + "(history.getCompletedSessions() * 100) / history.getTotalSessions() : 0)")
    @Mapping(target = "sessions", source = "sessions")
    StudyPlanHistoryResponse toHistoryResponse(StudyPlanHistory history);

    List<StudyPlanHistoryResponse> toHistoryResponseList(List<StudyPlanHistory> histories);

    // ── Response: StudySessionHistory → StudySessionResponse ────────────
    //    Reuses the existing StudySessionResponse DTO for consistency.

    @Mapping(target = "id", source = "originalSessionId")
    @Mapping(target = "planId", source = "planHistory.plan.id")
    StudySessionResponse toSessionResponse(StudySessionHistory sessionHistory);

    List<StudySessionResponse> toSessionResponseList(List<StudySessionHistory> sessionHistories);
}
