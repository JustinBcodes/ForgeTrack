import type { Bootstrap, CreateIssueInput, Dashboard, IssueDetail, IssuePage, IssuePriority, IssueStatus, IssueType } from './types'

const API_BASE = import.meta.env.VITE_API_URL ?? ''

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers as Record<string, string> | undefined) },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.message ?? `Request failed with status ${response.status}`)
  }
  return response.json() as Promise<T>
}

export const api = {
  bootstrap: () => request<Bootstrap>('/api/bootstrap'),
  dashboard: (projectId: string) => request<Dashboard>(`/api/projects/${projectId}/dashboard`),
  issues: (projectId: string, filters: { q: string; status: IssueStatus | ''; priority: IssuePriority | ''; type: IssueType | '' }) => {
    const params = new URLSearchParams()
    if (filters.q) params.set('q', filters.q)
    if (filters.status) params.set('status', filters.status)
    if (filters.priority) params.set('priority', filters.priority)
    if (filters.type) params.set('type', filters.type)
    return request<IssuePage>(`/api/projects/${projectId}/issues?${params}`)
  },
  createIssue: (input: CreateIssueInput) => request('/api/issues', { method: 'POST', body: JSON.stringify(input) }),
  issue: (id: string) => request<IssueDetail>(`/api/issues/${id}`),
  updateIssue: (id: string, input: { title: string; description: string; type: IssueType; status: IssueStatus; priority: IssuePriority; actorId: string; assigneeId: string | null }) => request<IssueDetail>(`/api/issues/${id}`, { method: 'PATCH', body: JSON.stringify(input) }),
  addComment: (id: string, authorId: string, body: string) => request(`/api/issues/${id}/comments`, { method: 'POST', body: JSON.stringify({ authorId, body }) }),
  linkPullRequest: (id: string, repository: string, pullRequestNumber: number, actorId: string) => request(`/api/issues/${id}/pull-requests`, { method: 'POST', body: JSON.stringify({ repository, pullRequestNumber, actorId }) }),
}
