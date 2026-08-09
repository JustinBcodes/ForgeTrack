create table user_accounts (
    id uuid primary key,
    display_name varchar(120) not null,
    email varchar(200) not null unique,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null
);

create table organizations (
    id uuid primary key,
    name varchar(120) not null,
    slug varchar(80) not null unique,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null
);

create table memberships (
    id uuid primary key,
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references user_accounts(id) on delete cascade,
    role varchar(30) not null check (role in ('OWNER', 'ADMIN', 'MEMBER')),
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    constraint uk_membership_organization_user unique (organization_id, user_id)
);

create table projects (
    id uuid primary key,
    organization_id uuid not null references organizations(id) on delete cascade,
    name varchar(120) not null,
    project_key varchar(10) not null,
    description varchar(500),
    github_repository varchar(200),
    next_issue_number integer not null default 1,
    version bigint not null default 0,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    constraint uk_project_organization_key unique (organization_id, project_key)
);

create table issues (
    id uuid primary key,
    project_id uuid not null references projects(id) on delete cascade,
    issue_number integer not null,
    title varchar(180) not null,
    description text,
    type varchar(30) not null check (type in ('BUG', 'TASK', 'FEATURE')),
    status varchar(30) not null check (status in ('BACKLOG', 'TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    priority varchar(30) not null check (priority in ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    reporter_id uuid not null references user_accounts(id),
    assignee_id uuid references user_accounts(id),
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    constraint uk_issue_project_number unique (project_id, issue_number)
);

create table issue_comments (
    id uuid primary key,
    issue_id uuid not null references issues(id) on delete cascade,
    author_id uuid not null references user_accounts(id),
    body text not null,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null
);

create table issue_activities (
    id uuid primary key,
    issue_id uuid not null references issues(id) on delete cascade,
    actor_id uuid not null references user_accounts(id),
    action varchar(80) not null,
    details varchar(500) not null,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null
);

create table pull_request_links (
    id uuid primary key,
    issue_id uuid not null references issues(id) on delete cascade,
    repository varchar(200) not null,
    pull_request_number integer not null,
    title varchar(300) not null,
    url varchar(500) not null,
    state varchar(30) not null,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    constraint uk_pull_request_issue_repo_number unique (issue_id, repository, pull_request_number)
);

create index idx_memberships_organization on memberships(organization_id);
create index idx_projects_organization on projects(organization_id);
create index idx_issues_project_status on issues(project_id, status);
create index idx_issues_project_priority on issues(project_id, priority);
create index idx_issues_assignee on issues(assignee_id);
create index idx_issue_comments_issue on issue_comments(issue_id, created_at);
create index idx_issue_activities_issue on issue_activities(issue_id, created_at desc);
create index idx_pull_request_links_issue on pull_request_links(issue_id);
