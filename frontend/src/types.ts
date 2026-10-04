export type IssueStatus = 'BACKLOG' | 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE'
export type IssuePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type IssueType = 'BUG' | 'TASK' | 'FEATURE'

export interface User {
  id: string
  displayName: string
  email: string
  role: 'OWNER' | 'ADMIN' | 'MEMBER' | null
}

export interface Project {
  id: string
  organizationId: string
  name: string
  key: string
  description: string
  githubRepository: string
}

export interface Bootstrap {
  organization: { id: string; name: string; slug: string }
  projects: Project[]
  members: User[]
}

export interface IssueSummary {
  id: string
  identifier: string
  title: string
  type: IssueType
  status: IssueStatus
  priority: IssuePriority
  assignee: User | null
  updatedAt: string
}

export interface IssuePage {
  items: IssueSummary[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface IssueDetail extends IssueSummary {
  projectId: string
  description: string
  reporter: User
  createdAt: string
  comments: { id: string; author: User; body: string; createdAt: string }[]
  activities: { id: string; actor: User; action: string; details: string; createdAt: string }[]
  pullRequests: { id: string; repository: string; number: number; title: string; url: string; state: string }[]
}

export interface Dashboard {
  total: number
  open: number
  inProgress: number
  inReview: number
  completed: number
  byStatus: Record<IssueStatus, number>
  recentlyUpdated: IssueSummary[]
}

export interface CreateIssueInput {
  projectId: string
  title: string
  description: string
  type: IssueType
  priority: IssuePriority
  reporterId: string
  assigneeId: string | null
}

