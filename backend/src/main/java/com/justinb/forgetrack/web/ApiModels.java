package com.justinb.forgetrack.web;

import com.justinb.forgetrack.domain.IssuePriority;
import com.justinb.forgetrack.domain.IssueStatus;
import com.justinb.forgetrack.domain.IssueType;
import com.justinb.forgetrack.domain.MemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApiModels {

    private ApiModels() {
    }

    public record UserView(UUID id, String displayName, String email, MemberRole role) {
    }

    public record OrganizationView(UUID id, String name, String slug) {
    }

    public record ProjectView(UUID id, UUID organizationId, String name, String key, String description,
                              String githubRepository) {
    }

    public record BootstrapView(OrganizationView organization, List<ProjectView> projects, List<UserView> members) {
    }

    public record CreateOrganizationRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = "[a-z0-9-]{3,80}") String slug,
            @NotBlank @Size(max = 120) String ownerName,
            @NotBlank @Email @Size(max = 200) String ownerEmail) {
    }

    public record CreateProjectRequest(
            @NotNull UUID organizationId,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9]{1,9}") String key,
            @Size(max = 500) String description,
            @Pattern(regexp = "^$|^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$") String githubRepository) {
    }

    public record InviteMemberRequest(
            @NotBlank @Size(max = 120) String displayName,
            @NotBlank @Email @Size(max = 200) String email,
            @NotNull MemberRole role) {
    }

    public record CreateIssueRequest(
            @NotNull UUID projectId,
            @NotBlank @Size(max = 180) String title,
            @Size(max = 10_000) String description,
            @NotNull IssueType type,
            @NotNull IssuePriority priority,
            @NotNull UUID reporterId,
            UUID assigneeId) {
    }

    public record UpdateIssueRequest(
            @NotBlank @Size(max = 180) String title,
            @Size(max = 10_000) String description,
            @NotNull IssueType type,
            @NotNull IssueStatus status,
            @NotNull IssuePriority priority,
            @NotNull UUID actorId,
            UUID assigneeId) {
    }

    public record AddCommentRequest(
            @NotNull UUID authorId,
            @NotBlank @Size(max = 5_000) String body) {
    }

    public record LinkPullRequestRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$") String repository,
            @Min(1) @Max(999_999_999) int pullRequestNumber,
            @NotNull UUID actorId) {
    }

    public record IssueSummary(UUID id, String identifier, String title, IssueType type, IssueStatus status,
                               IssuePriority priority, UserView assignee, Instant updatedAt) {
    }

    public record CommentView(UUID id, UserView author, String body, Instant createdAt) {
    }

    public record ActivityView(UUID id, UserView actor, String action, String details, Instant createdAt) {
    }

    public record PullRequestView(UUID id, String repository, int number, String title, String url, String state) {
    }

    public record IssueDetail(UUID id, String identifier, UUID projectId, String title, String description,
                              IssueType type, IssueStatus status, IssuePriority priority, UserView reporter,
                              UserView assignee, Instant createdAt, Instant updatedAt, List<CommentView> comments,
                              List<ActivityView> activities, List<PullRequestView> pullRequests) {
    }

    public record PagedResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }

    public record DashboardView(long total, long open, long inProgress, long inReview, long completed,
                                Map<IssueStatus, Long> byStatus, List<IssueSummary> recentlyUpdated) {
    }

    public record ApiError(Instant timestamp, int status, String error, String message,
                           String path, Map<String, String> validationErrors) {
    }
}

