package com.justinb.forgetrack.web;

import com.justinb.forgetrack.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class WorkspaceController {

    private final WorkspaceService workspaces;

    public WorkspaceController(WorkspaceService workspaces) {
        this.workspaces = workspaces;
    }

    @GetMapping("/bootstrap")
    ApiModels.BootstrapView bootstrap() {
        return workspaces.bootstrap();
    }

    @PostMapping("/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    ApiModels.OrganizationView createOrganization(@Valid @RequestBody ApiModels.CreateOrganizationRequest request) {
        return workspaces.createOrganization(request);
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    ApiModels.ProjectView createProject(@Valid @RequestBody ApiModels.CreateProjectRequest request) {
        return workspaces.createProject(request);
    }

    @PostMapping("/organizations/{organizationId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    ApiModels.UserView inviteMember(@PathVariable UUID organizationId,
                                    @Valid @RequestBody ApiModels.InviteMemberRequest request) {
        return workspaces.inviteMember(organizationId, request);
    }
}

