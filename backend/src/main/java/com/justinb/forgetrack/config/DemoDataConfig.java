package com.justinb.forgetrack.config;

import com.justinb.forgetrack.domain.IssuePriority;
import com.justinb.forgetrack.domain.IssueStatus;
import com.justinb.forgetrack.domain.IssueType;
import com.justinb.forgetrack.domain.MemberRole;
import com.justinb.forgetrack.repository.OrganizationRepository;
import com.justinb.forgetrack.repository.UserAccountRepository;
import com.justinb.forgetrack.service.IssueService;
import com.justinb.forgetrack.service.WorkspaceService;
import com.justinb.forgetrack.web.ApiModels;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DemoDataConfig {

    @Bean
    @ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
    ApplicationRunner seedDemoData(OrganizationRepository organizations, UserAccountRepository users,
                                   WorkspaceService workspaces, IssueService issues) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) {
                if (organizations.count() > 0) return;

                ApiModels.OrganizationView organization = workspaces.createOrganization(
                        new ApiModels.CreateOrganizationRequest("Bean Built", "bean-built",
                                "Justin Bean", "justin@forgetrack.dev"));
                ApiModels.UserView maya = workspaces.inviteMember(organization.id(),
                        new ApiModels.InviteMemberRequest("Maya Chen", "maya@forgetrack.dev", MemberRole.MEMBER));
                ApiModels.UserView jordan = workspaces.inviteMember(organization.id(),
                        new ApiModels.InviteMemberRequest("Jordan Lee", "jordan@forgetrack.dev", MemberRole.MEMBER));
                ApiModels.ProjectView project = workspaces.createProject(new ApiModels.CreateProjectRequest(
                        organization.id(), "ForgeTrack Platform", "FORGE",
                        "A focused engineering project management workspace.", "JustinBcodes/ForgeTrack"));
                var owner = users.findByEmailIgnoreCase("justin@forgetrack.dev").orElseThrow();

                List<SeedIssue> seeds = List.of(
                        new SeedIssue("Add issue filtering to the REST API", "Filter by status, priority, type, and free text.",
                                IssueType.FEATURE, IssuePriority.HIGH, IssueStatus.IN_PROGRESS, owner.getId()),
                        new SeedIssue("Fix stale dashboard counts", "Refresh aggregate counts after mutations.",
                                IssueType.BUG, IssuePriority.URGENT, IssueStatus.IN_REVIEW, maya.id()),
                        new SeedIssue("Model organization memberships", "Represent roles with a constrained join table.",
                                IssueType.TASK, IssuePriority.MEDIUM, IssueStatus.DONE, jordan.id()),
                        new SeedIssue("Link GitHub pull requests", "Fetch public pull request metadata through the GitHub REST API.",
                                IssueType.FEATURE, IssuePriority.MEDIUM, IssueStatus.TODO, maya.id()),
                        new SeedIssue("Write controller integration tests", "Cover validation, creation, filtering, and dashboard endpoints.",
                                IssueType.TASK, IssuePriority.HIGH, IssueStatus.TODO, jordan.id())
                );

                for (SeedIssue seed : seeds) {
                    ApiModels.IssueDetail issue = issues.create(new ApiModels.CreateIssueRequest(project.id(), seed.title,
                            seed.description, seed.type, seed.priority, owner.getId(), seed.assigneeId));
                    if (seed.status != IssueStatus.TODO) {
                        issues.update(issue.id(), new ApiModels.UpdateIssueRequest(seed.title, seed.description,
                                seed.type, seed.status, seed.priority, owner.getId(), seed.assigneeId));
                    }
                }
            }
        };
    }

    private record SeedIssue(String title, String description, IssueType type, IssuePriority priority,
                             IssueStatus status, java.util.UUID assigneeId) {
    }
}
