package com.justinb.forgetrack.web;

import com.justinb.forgetrack.domain.IssuePriority;
import com.justinb.forgetrack.domain.IssueStatus;
import com.justinb.forgetrack.domain.IssueType;
import com.justinb.forgetrack.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class IssueController {

    private final IssueService issues;

    public IssueController(IssueService issues) {
        this.issues = issues;
    }

    @GetMapping("/projects/{projectId}/issues")
    ApiModels.PagedResponse<ApiModels.IssueSummary> search(
            @PathVariable UUID projectId,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) IssuePriority priority,
            @RequestParam(required = false) IssueType type,
            @RequestParam(required = false, name = "q") String text,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return issues.search(projectId, status, priority, type, text, Math.max(page, 0), safeSize);
    }

    @GetMapping("/projects/{projectId}/dashboard")
    ApiModels.DashboardView dashboard(@PathVariable UUID projectId) {
        return issues.dashboard(projectId);
    }

    @PostMapping("/issues")
    @ResponseStatus(HttpStatus.CREATED)
    ApiModels.IssueDetail create(@Valid @RequestBody ApiModels.CreateIssueRequest request) {
        return issues.create(request);
    }

    @GetMapping("/issues/{issueId}")
    ApiModels.IssueDetail get(@PathVariable UUID issueId) {
        return issues.get(issueId);
    }

    @PatchMapping("/issues/{issueId}")
    ApiModels.IssueDetail update(@PathVariable UUID issueId,
                                 @Valid @RequestBody ApiModels.UpdateIssueRequest request) {
        return issues.update(issueId, request);
    }

    @PostMapping("/issues/{issueId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    ApiModels.CommentView addComment(@PathVariable UUID issueId,
                                     @Valid @RequestBody ApiModels.AddCommentRequest request) {
        return issues.addComment(issueId, request);
    }

    @PostMapping("/issues/{issueId}/pull-requests")
    @ResponseStatus(HttpStatus.CREATED)
    ApiModels.PullRequestView linkPullRequest(@PathVariable UUID issueId,
                                              @Valid @RequestBody ApiModels.LinkPullRequestRequest request) {
        return issues.linkPullRequest(issueId, request);
    }
}

