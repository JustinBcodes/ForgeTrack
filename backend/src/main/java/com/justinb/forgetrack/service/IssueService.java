package com.justinb.forgetrack.service;

import com.justinb.forgetrack.domain.Issue;
import com.justinb.forgetrack.domain.IssueActivity;
import com.justinb.forgetrack.domain.IssueComment;
import com.justinb.forgetrack.domain.IssuePriority;
import com.justinb.forgetrack.domain.IssueStatus;
import com.justinb.forgetrack.domain.IssueType;
import com.justinb.forgetrack.domain.Project;
import com.justinb.forgetrack.domain.PullRequestLink;
import com.justinb.forgetrack.domain.UserAccount;
import com.justinb.forgetrack.repository.IssueActivityRepository;
import com.justinb.forgetrack.repository.IssueCommentRepository;
import com.justinb.forgetrack.repository.IssueRepository;
import com.justinb.forgetrack.repository.MembershipRepository;
import com.justinb.forgetrack.repository.ProjectRepository;
import com.justinb.forgetrack.repository.PullRequestLinkRepository;
import com.justinb.forgetrack.repository.UserAccountRepository;
import com.justinb.forgetrack.web.ApiModels;
import com.justinb.forgetrack.web.ConflictException;
import com.justinb.forgetrack.web.NotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class IssueService {

    private final IssueRepository issues;
    private final ProjectRepository projects;
    private final UserAccountRepository users;
    private final MembershipRepository memberships;
    private final IssueCommentRepository comments;
    private final IssueActivityRepository activities;
    private final PullRequestLinkRepository pullRequests;
    private final GitHubService gitHub;

    public IssueService(IssueRepository issues, ProjectRepository projects, UserAccountRepository users,
                        MembershipRepository memberships, IssueCommentRepository comments,
                        IssueActivityRepository activities, PullRequestLinkRepository pullRequests,
                        GitHubService gitHub) {
        this.issues = issues;
        this.projects = projects;
        this.users = users;
        this.memberships = memberships;
        this.comments = comments;
        this.activities = activities;
        this.pullRequests = pullRequests;
        this.gitHub = gitHub;
    }

    @Transactional(readOnly = true)
    public ApiModels.PagedResponse<ApiModels.IssueSummary> search(UUID projectId, IssueStatus status,
                                                                  IssuePriority priority, IssueType type,
                                                                  String text, int page, int size) {
        projects.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found."));
        Specification<Issue> filters = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("project").get("id"), projectId));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (priority != null) predicates.add(builder.equal(root.get("priority"), priority));
            if (type != null) predicates.add(builder.equal(root.get("type"), type));
            if (text != null && !text.isBlank()) {
                String pattern = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Page<Issue> result = issues.findAll(filters,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return new ApiModels.PagedResponse<>(result.getContent().stream().map(this::summary).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public ApiModels.IssueDetail create(ApiModels.CreateIssueRequest request) {
        Project project = projects.findForUpdateById(request.projectId())
                .orElseThrow(() -> new NotFoundException("Project not found."));
        UserAccount reporter = member(project, request.reporterId(), "Reporter");
        UserAccount assignee = request.assigneeId() == null ? null : member(project, request.assigneeId(), "Assignee");
        Issue issue = issues.save(new Issue(project, project.reserveIssueNumber(), request.title().trim(),
                clean(request.description()), request.type(), request.priority(), reporter, assignee));
        activities.save(new IssueActivity(issue, reporter, "CREATED", "Created this issue."));
        return detail(issue);
    }

    @Transactional(readOnly = true)
    public ApiModels.IssueDetail get(UUID issueId) {
        return detail(findIssue(issueId));
    }

    @Transactional
    public ApiModels.IssueDetail update(UUID issueId, ApiModels.UpdateIssueRequest request) {
        Issue issue = findIssue(issueId);
        UserAccount actor = member(issue.getProject(), request.actorId(), "Actor");
        UserAccount assignee = request.assigneeId() == null ? null : member(issue.getProject(), request.assigneeId(), "Assignee");
        IssueStatus previousStatus = issue.getStatus();
        issue.update(request.title().trim(), clean(request.description()), request.type(), request.status(),
                request.priority(), assignee);
        String details = previousStatus == request.status()
                ? "Updated issue fields."
                : "Moved from " + previousStatus + " to " + request.status() + ".";
        activities.save(new IssueActivity(issue, actor, "UPDATED", details));
        return detail(issue);
    }

    @Transactional
    public ApiModels.CommentView addComment(UUID issueId, ApiModels.AddCommentRequest request) {
        Issue issue = findIssue(issueId);
        UserAccount author = member(issue.getProject(), request.authorId(), "Author");
        IssueComment comment = comments.save(new IssueComment(issue, author, request.body().trim()));
        activities.save(new IssueActivity(issue, author, "COMMENTED", "Added a comment."));
        return commentView(comment);
    }

    @Transactional
    public ApiModels.PullRequestView linkPullRequest(UUID issueId, ApiModels.LinkPullRequestRequest request) {
        Issue issue = findIssue(issueId);
        UserAccount actor = member(issue.getProject(), request.actorId(), "Actor");
        boolean duplicate = pullRequests.findByIssueIdOrderByCreatedAtDesc(issueId).stream()
                .anyMatch(link -> link.getRepository().equalsIgnoreCase(request.repository())
                        && link.getNumber() == request.pullRequestNumber());
        if (duplicate) throw new ConflictException("That pull request is already linked to this issue.");
        GitHubService.PullRequestData data = gitHub.getPullRequest(request.repository(), request.pullRequestNumber());
        PullRequestLink link = pullRequests.save(new PullRequestLink(issue, request.repository(),
                request.pullRequestNumber(), data.title(), data.htmlUrl(), data.displayState()));
        activities.save(new IssueActivity(issue, actor, "LINKED_PULL_REQUEST",
                "Linked " + request.repository() + "#" + request.pullRequestNumber() + "."));
        return pullRequestView(link);
    }

    @Transactional(readOnly = true)
    public ApiModels.DashboardView dashboard(UUID projectId) {
        projects.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found."));
        Map<IssueStatus, Long> byStatus = new EnumMap<>(IssueStatus.class);
        for (IssueStatus status : IssueStatus.values()) {
            byStatus.put(status, issues.countByProjectIdAndStatus(projectId, status));
        }
        long total = issues.countByProjectId(projectId);
        long completed = byStatus.get(IssueStatus.DONE);
        return new ApiModels.DashboardView(total, total - completed,
                byStatus.get(IssueStatus.IN_PROGRESS), byStatus.get(IssueStatus.IN_REVIEW), completed,
                byStatus, issues.findTop5ByProjectIdOrderByUpdatedAtDesc(projectId).stream().map(this::summary).toList());
    }

    private Issue findIssue(UUID issueId) {
        return issues.findById(issueId).orElseThrow(() -> new NotFoundException("Issue not found."));
    }

    private UserAccount member(Project project, UUID userId, String label) {
        UserAccount user = users.findById(userId).orElseThrow(() -> new NotFoundException(label + " not found."));
        if (!memberships.existsByOrganizationIdAndUserId(project.getOrganization().getId(), userId)) {
            throw new ConflictException(label + " must be a member of the project's organization.");
        }
        return user;
    }

    private ApiModels.IssueSummary summary(Issue issue) {
        return new ApiModels.IssueSummary(issue.getId(), identifier(issue), issue.getTitle(), issue.getType(),
                issue.getStatus(), issue.getPriority(), WorkspaceService.userView(issue.getAssignee(), null),
                issue.getUpdatedAt());
    }

    private ApiModels.IssueDetail detail(Issue issue) {
        return new ApiModels.IssueDetail(issue.getId(), identifier(issue), issue.getProject().getId(),
                issue.getTitle(), issue.getDescription(), issue.getType(), issue.getStatus(), issue.getPriority(),
                WorkspaceService.userView(issue.getReporter(), null), WorkspaceService.userView(issue.getAssignee(), null),
                issue.getCreatedAt(), issue.getUpdatedAt(),
                comments.findByIssueIdOrderByCreatedAtAsc(issue.getId()).stream().map(this::commentView).toList(),
                activities.findByIssueIdOrderByCreatedAtDesc(issue.getId()).stream().map(activity ->
                        new ApiModels.ActivityView(activity.getId(), WorkspaceService.userView(activity.getActor(), null),
                                activity.getAction(), activity.getDetails(), activity.getCreatedAt())).toList(),
                pullRequests.findByIssueIdOrderByCreatedAtDesc(issue.getId()).stream().map(this::pullRequestView).toList());
    }

    private ApiModels.CommentView commentView(IssueComment comment) {
        return new ApiModels.CommentView(comment.getId(), WorkspaceService.userView(comment.getAuthor(), null),
                comment.getBody(), comment.getCreatedAt());
    }

    private ApiModels.PullRequestView pullRequestView(PullRequestLink link) {
        return new ApiModels.PullRequestView(link.getId(), link.getRepository(), link.getNumber(), link.getTitle(),
                link.getUrl(), link.getState());
    }

    private static String identifier(Issue issue) {
        return issue.getProject().getKey() + "-" + issue.getNumber();
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
