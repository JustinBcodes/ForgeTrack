package com.justinb.forgetrack.service;

import com.justinb.forgetrack.domain.MemberRole;
import com.justinb.forgetrack.domain.Membership;
import com.justinb.forgetrack.domain.Organization;
import com.justinb.forgetrack.domain.Project;
import com.justinb.forgetrack.domain.UserAccount;
import com.justinb.forgetrack.repository.MembershipRepository;
import com.justinb.forgetrack.repository.OrganizationRepository;
import com.justinb.forgetrack.repository.ProjectRepository;
import com.justinb.forgetrack.repository.UserAccountRepository;
import com.justinb.forgetrack.web.ApiModels;
import com.justinb.forgetrack.web.ConflictException;
import com.justinb.forgetrack.web.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final OrganizationRepository organizations;
    private final ProjectRepository projects;
    private final UserAccountRepository users;
    private final MembershipRepository memberships;

    public WorkspaceService(OrganizationRepository organizations, ProjectRepository projects,
                            UserAccountRepository users, MembershipRepository memberships) {
        this.organizations = organizations;
        this.projects = projects;
        this.users = users;
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public ApiModels.BootstrapView bootstrap() {
        Organization organization = organizations.findAll(Sort.by("createdAt")).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Create an organization to get started."));
        List<ApiModels.ProjectView> projectViews = projects.findAllByOrderByName().stream()
                .filter(project -> project.getOrganization().getId().equals(organization.getId()))
                .map(WorkspaceService::projectView)
                .toList();
        List<ApiModels.UserView> memberViews = memberships
                .findByOrganizationIdOrderByUserDisplayName(organization.getId()).stream()
                .map(membership -> userView(membership.getUser(), membership.getRole()))
                .toList();
        return new ApiModels.BootstrapView(organizationView(organization), projectViews, memberViews);
    }

    @Transactional
    public ApiModels.OrganizationView createOrganization(ApiModels.CreateOrganizationRequest request) {
        if (organizations.findBySlug(request.slug()).isPresent()) {
            throw new ConflictException("An organization with that slug already exists.");
        }
        Organization organization = organizations.save(new Organization(request.name().trim(), request.slug().trim()));
        UserAccount owner = users.findByEmailIgnoreCase(request.ownerEmail())
                .orElseGet(() -> users.save(new UserAccount(request.ownerName().trim(), request.ownerEmail().trim())));
        memberships.save(new Membership(organization, owner, MemberRole.OWNER));
        return organizationView(organization);
    }

    @Transactional
    public ApiModels.ProjectView createProject(ApiModels.CreateProjectRequest request) {
        Organization organization = organizations.findById(request.organizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found."));
        Project project = projects.save(new Project(organization, request.name().trim(), request.key().trim(),
                clean(request.description()), clean(request.githubRepository())));
        return projectView(project);
    }

    @Transactional
    public ApiModels.UserView inviteMember(UUID organizationId, ApiModels.InviteMemberRequest request) {
        Organization organization = organizations.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found."));
        UserAccount user = users.findByEmailIgnoreCase(request.email())
                .orElseGet(() -> users.save(new UserAccount(request.displayName().trim(), request.email().trim())));
        if (memberships.existsByOrganizationIdAndUserId(organizationId, user.getId())) {
            throw new ConflictException("That user is already a member of this organization.");
        }
        memberships.save(new Membership(organization, user, request.role()));
        return userView(user, request.role());
    }

    static ApiModels.OrganizationView organizationView(Organization organization) {
        return new ApiModels.OrganizationView(organization.getId(), organization.getName(), organization.getSlug());
    }

    static ApiModels.ProjectView projectView(Project project) {
        return new ApiModels.ProjectView(project.getId(), project.getOrganization().getId(), project.getName(),
                project.getKey(), project.getDescription(), project.getGithubRepository());
    }

    static ApiModels.UserView userView(UserAccount user, MemberRole role) {
        return user == null ? null : new ApiModels.UserView(user.getId(), user.getDisplayName(), user.getEmail(), role);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

