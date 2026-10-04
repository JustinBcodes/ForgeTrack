import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react'
import { api } from './api'
import IssueDetailPanel from './IssueDetailPanel'
import type {
  Bootstrap,
  CreateIssueInput,
  Dashboard,
  IssuePage,
  IssuePriority,
  IssueStatus,
  IssueType,
  Project,
} from './types'

const EMPTY_FILTERS = { q: '', status: '' as IssueStatus | '', priority: '' as IssuePriority | '', type: '' as IssueType | '' }

const label = (value: string) => value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())

function App() {
  const [bootstrap, setBootstrap] = useState<Bootstrap | null>(null)
  const [projectId, setProjectId] = useState('')
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [issues, setIssues] = useState<IssuePage | null>(null)
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [selectedIssueId, setSelectedIssueId] = useState<string | null>(null)

  const project = useMemo(
    () => bootstrap?.projects.find((candidate) => candidate.id === projectId) ?? bootstrap?.projects[0],
    [bootstrap, projectId],
  )

  useEffect(() => {
    api.bootstrap()
      .then((data) => {
        setBootstrap(data)
        setProjectId(data.projects[0]?.id ?? '')
      })
      .catch((reason: Error) => setError(reason.message))
      .finally(() => setLoading(false))
  }, [])

  const loadProjectData = useCallback(() => {
    if (!projectId) return Promise.resolve(null)
    return Promise.all([api.dashboard(projectId), api.issues(projectId, filters)])
  }, [filters, projectId])

  const refresh = useCallback(async () => {
    setError('')
    try {
      const result = await loadProjectData()
      if (!result) return
      const [dashboardData, issueData] = result
      setDashboard(dashboardData)
      setIssues(issueData)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to load project data.')
    }
  }, [loadProjectData])

  useEffect(() => {
    let active = true
    loadProjectData()
      .then((result) => {
        if (!active || !result) return
        setDashboard(result[0])
        setIssues(result[1])
      })
      .catch((reason: Error) => {
        if (active) setError(reason.message)
      })
    return () => { active = false }
  }, [loadProjectData])

  if (loading) return <LoadingScreen />

  if (!bootstrap || !project) {
    return (
      <main className="empty-screen">
        <Brand />
        <h1>ForgeTrack needs a workspace</h1>
        <p>{error || 'Create an organization and project through the REST API to get started.'}</p>
      </main>
    )
  }

  return (
    <div className="app-shell">
      <Sidebar bootstrap={bootstrap} project={project} onProjectChange={setProjectId} />
      <main className="main-content">
        <header className="topbar">
          <div>
            <p className="eyebrow">{bootstrap.organization.name} / {project.key}</p>
            <h1>Project overview</h1>
          </div>
          <div className="topbar-actions">
            <button className="icon-button" aria-label="Notifications"><BellIcon /></button>
            <div className="avatar" title={bootstrap.members[0]?.displayName}>{initials(bootstrap.members[0]?.displayName ?? 'FT')}</div>
          </div>
        </header>

        {error && <div className="error-banner" role="alert">{error}</div>}

        <section className="welcome-row">
          <div>
            <h2>{project.name}</h2>
            <p>{project.description}</p>
          </div>
          <button className="primary-button" onClick={() => setCreateOpen(true)}><PlusIcon /> New issue</button>
        </section>

        <Stats dashboard={dashboard} />
        <ProjectPulse dashboard={dashboard} />

        <section className="issues-card" id="issues">
          <div className="issues-heading">
            <div>
              <h2>Issues</h2>
              <p>{issues?.totalItems ?? 0} work items in this view</p>
            </div>
            <button className="ghost-button" onClick={() => void refresh()}>Refresh</button>
          </div>
          <Filters filters={filters} onChange={setFilters} />
          <IssueTable issues={issues} onSelect={setSelectedIssueId} />
        </section>
      </main>

      {createOpen && (
        <CreateIssueModal
          project={project}
          bootstrap={bootstrap}
          onClose={() => setCreateOpen(false)}
          onCreated={async () => {
            setCreateOpen(false)
            await refresh()
          }}
        />
      )}
      {selectedIssueId && <IssueDetailPanel issueId={selectedIssueId} repository={project.githubRepository} currentUserId={bootstrap.members[0].id} members={bootstrap.members} onClose={() => setSelectedIssueId(null)} onChanged={refresh} />}
    </div>
  )
}

function Sidebar({ bootstrap, project, onProjectChange }: { bootstrap: Bootstrap; project: Project; onProjectChange: (id: string) => void }) {
  return (
    <aside className="sidebar">
      <Brand />
      <div className="workspace-switcher">
        <div className="workspace-mark">BB</div>
        <div><span>Workspace</span><strong>{bootstrap.organization.name}</strong></div>
        <ChevronIcon />
      </div>
      <nav aria-label="Primary navigation">
        <a className="nav-item active" href="#overview"><GridIcon /> Overview</a>
        <a className="nav-item" href="#issues"><IssueIcon /> Issues <span className="nav-count">{bootstrap.projects.length}</span></a>
        <a className="nav-item" href="#activity"><PulseIcon /> Activity</a>
        <p className="nav-label">Projects</p>
        {bootstrap.projects.map((item) => (
          <button className={`project-link ${item.id === project.id ? 'selected' : ''}`} key={item.id} onClick={() => onProjectChange(item.id)}>
            <span className="project-dot" /> {item.name}
          </button>
        ))}
      </nav>
      <div className="sidebar-bottom">
        <a className="nav-item" href={project.githubRepository ? `https://github.com/${project.githubRepository}` : '#github'} target="_blank" rel="noreferrer"><GitHubIcon /> GitHub repository</a>
        <a className="nav-item" href="#settings"><GearIcon /> Settings</a>
      </div>
    </aside>
  )
}

function Stats({ dashboard }: { dashboard: Dashboard | null }) {
  const stats = [
    { label: 'Open issues', value: dashboard?.open ?? '—', tone: 'neutral' },
    { label: 'In progress', value: dashboard?.inProgress ?? '—', tone: 'blue' },
    { label: 'In review', value: dashboard?.inReview ?? '—', tone: 'amber' },
    { label: 'Completed', value: dashboard?.completed ?? '—', tone: 'green' },
  ]
  return (
    <section className="stats-grid" aria-label="Project statistics">
      {stats.map((stat) => (
        <article className="stat-card" key={stat.label}>
          <div className={`stat-icon ${stat.tone}`}><PulseIcon /></div>
          <div><span>{stat.label}</span><strong>{stat.value}</strong></div>
        </article>
      ))}
    </section>
  )
}

function ProjectPulse({ dashboard }: { dashboard: Dashboard | null }) {
  const states: { status: IssueStatus; color: string }[] = [
    { status: 'BACKLOG', color: '#b3bdb6' }, { status: 'TODO', color: '#8eb69b' },
    { status: 'IN_PROGRESS', color: '#75aee4' }, { status: 'IN_REVIEW', color: '#e8bb6a' },
    { status: 'DONE', color: '#3d9d69' },
  ]
  const total = dashboard?.total ?? 0
  const progress = total ? Math.round((dashboard?.completed ?? 0) / total * 100) : 0
  return (
    <section className="pulse-grid" id="activity" aria-label="Project health and recent activity">
      <article className="pulse-card">
        <div className="pulse-heading"><div><span className="section-kicker">PROJECT HEALTH</span><h2>Work in motion</h2></div><span className="pulse-badge"><PulseIcon /> Live overview</span></div>
        <p>A snapshot of progress across every stage of the workflow.</p>
        <div className="progress-head"><strong>{progress}%</strong><span>of issues completed</span></div>
        <div className="progress-track" role="progressbar" aria-valuenow={progress} aria-valuemin={0} aria-valuemax={100} aria-label="Issues completed"><span style={{ width: `${progress}%` }} /></div>
        <div className="state-breakdown">{states.map(({ status, color }) => <div key={status}><i style={{ background: color }} /><span>{label(status)}</span><strong>{dashboard?.byStatus[status] ?? 0}</strong></div>)}</div>
      </article>
      <article className="pulse-card recent-card">
        <div className="pulse-heading"><div><span className="section-kicker">LATEST UPDATES</span><h2>Recently active</h2></div><span className="pulse-badge">{dashboard?.recentlyUpdated.length ?? 0} updates</span></div>
        <p>Issues that your team touched most recently.</p>
        <div className="recent-list">{dashboard?.recentlyUpdated.slice(0, 4).map(issue => <div className="recent-row" key={issue.id}><span className="recent-type"><TypeIcon type={issue.type}/></span><div><strong>{issue.title}</strong><small>{issue.identifier} · {label(issue.status)}</small></div><time>{relativeDate(issue.updatedAt)}</time></div>)}{!dashboard?.recentlyUpdated.length && <span className="recent-empty">No recent activity yet.</span>}</div>
      </article>
    </section>
  )
}

function Filters({ filters, onChange }: { filters: typeof EMPTY_FILTERS; onChange: (filters: typeof EMPTY_FILTERS) => void }) {
  return (
    <div className="filters">
      <label className="search-box">
        <SearchIcon />
        <span className="sr-only">Search issues</span>
        <input value={filters.q} onChange={(event) => onChange({ ...filters, q: event.target.value })} placeholder="Search issues…" />
      </label>
      <select aria-label="Filter by status" value={filters.status} onChange={(event) => onChange({ ...filters, status: event.target.value as IssueStatus | '' })}>
        <option value="">All statuses</option>
        {(['BACKLOG', 'TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'] as IssueStatus[]).map((status) => <option value={status} key={status}>{label(status)}</option>)}
      </select>
      <select aria-label="Filter by priority" value={filters.priority} onChange={(event) => onChange({ ...filters, priority: event.target.value as IssuePriority | '' })}>
        <option value="">All priorities</option>
        {(['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as IssuePriority[]).map((priority) => <option value={priority} key={priority}>{label(priority)}</option>)}
      </select>
      <select aria-label="Filter by type" value={filters.type} onChange={(event) => onChange({ ...filters, type: event.target.value as IssueType | '' })}>
        <option value="">All types</option>
        {(['BUG', 'TASK', 'FEATURE'] as IssueType[]).map((type) => <option value={type} key={type}>{label(type)}</option>)}
      </select>
    </div>
  )
}

function IssueTable({ issues, onSelect }: { issues: IssuePage | null; onSelect: (id: string) => void }) {
  if (!issues) return <div className="table-state">Loading issues…</div>
  if (!issues.items.length) return <div className="table-state"><IssueIcon /><strong>No issues match these filters</strong><span>Try clearing one or more filters.</span></div>
  return (
    <div className="table-wrap">
      <table>
        <thead><tr><th>Issue</th><th>Status</th><th>Priority</th><th>Assignee</th><th>Updated</th></tr></thead>
        <tbody>
          {issues.items.map((issue) => (
            <tr key={issue.id}>
              <td><button className="issue-open" onClick={() => onSelect(issue.id)}><div className="issue-title"><TypeIcon type={issue.type} /><div><strong>{issue.title}</strong><span>{issue.identifier} · {label(issue.type)}</span></div></div></button></td>
              <td><span className={`status status-${issue.status.toLowerCase()}`}><i />{label(issue.status)}</span></td>
              <td><span className={`priority priority-${issue.priority.toLowerCase()}`}><PriorityIcon />{label(issue.priority)}</span></td>
              <td>{issue.assignee ? <div className="assignee"><span className="avatar small">{initials(issue.assignee.displayName)}</span>{issue.assignee.displayName}</div> : <span className="muted">Unassigned</span>}</td>
              <td className="muted">{relativeDate(issue.updatedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function CreateIssueModal({ project, bootstrap, onClose, onCreated }: { project: Project; bootstrap: Bootstrap; onClose: () => void; onCreated: () => Promise<void> }) {
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState<CreateIssueInput>({
    projectId: project.id,
    title: '',
    description: '',
    type: 'TASK',
    priority: 'MEDIUM',
    reporterId: bootstrap.members[0].id,
    assigneeId: bootstrap.members[0].id,
  })

  async function submit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await api.createIssue(form)
      await onCreated()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to create issue.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="modal" role="dialog" aria-modal="true" aria-labelledby="create-title">
        <header><div><p className="eyebrow">{project.key}</p><h2 id="create-title">Create a new issue</h2></div><button className="icon-button" onClick={onClose} aria-label="Close"><CloseIcon /></button></header>
        <form onSubmit={submit}>
          {error && <div className="error-banner" role="alert">{error}</div>}
          <label>Title<input autoFocus required maxLength={180} value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="What needs to be done?" /></label>
          <label>Description<textarea rows={5} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} placeholder="Add context, requirements, or acceptance criteria…" /></label>
          <div className="form-grid">
            <label>Type<select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as IssueType })}><option value="TASK">Task</option><option value="BUG">Bug</option><option value="FEATURE">Feature</option></select></label>
            <label>Priority<select value={form.priority} onChange={(event) => setForm({ ...form, priority: event.target.value as IssuePriority })}><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option><option value="URGENT">Urgent</option></select></label>
            <label>Assignee<select value={form.assigneeId ?? ''} onChange={(event) => setForm({ ...form, assigneeId: event.target.value || null })}><option value="">Unassigned</option>{bootstrap.members.map((member) => <option value={member.id} key={member.id}>{member.displayName}</option>)}</select></label>
          </div>
          <footer><button type="button" className="ghost-button" onClick={onClose}>Cancel</button><button type="submit" className="primary-button" disabled={saving || !form.title.trim()}>{saving ? 'Creating…' : 'Create issue'}</button></footer>
        </form>
      </section>
    </div>
  )
}

function Brand() { return <div className="brand"><span className="brand-mark"><ForgeIcon /></span><span>ForgeTrack</span></div> }
function LoadingScreen() { return <main className="loading-screen"><Brand /><div className="loader" /><p>Loading your workspace…</p></main> }
function initials(name: string) { return name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() }
function relativeDate(value: string) { const days = Math.floor((Date.now() - new Date(value).getTime()) / 86_400_000); return days < 1 ? 'Today' : days === 1 ? 'Yesterday' : `${days}d ago` }

type IconProps = { className?: string }
const Icon = ({ children, className }: IconProps & { children: React.ReactNode }) => <svg className={className} width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{children}</svg>
const ForgeIcon = () => <Icon><path d="M8 3h8l2 4-6 14L6 7l2-4Z"/><path d="M6 7h12M9 12h6"/></Icon>
const GridIcon = () => <Icon><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></Icon>
const IssueIcon = () => <Icon><circle cx="12" cy="12" r="9"/><path d="M12 8v4M12 16h.01"/></Icon>
const PulseIcon = () => <Icon><path d="M3 12h4l2-5 4 10 2-5h6"/></Icon>
const GearIcon = () => <Icon><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06A1.7 1.7 0 0 0 15 19.4a1.7 1.7 0 0 0-1 .6 1.7 1.7 0 0 0-.4 1v.1h-4v-.1A1.7 1.7 0 0 0 8.6 19.4a1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-.6-1 1.7 1.7 0 0 0-1-.4h-.1v-4H3A1.7 1.7 0 0 0 4.6 8.6a1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.83-2.83.06.06A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-.6 1.7 1.7 0 0 0 .4-1v-.1h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.88-.34l.06-.06 2.83 2.83-.06.06A1.7 1.7 0 0 0 19.4 9c.14.36.36.7.6 1 .28.3.64.4 1 .4h.1v4H21a1.7 1.7 0 0 0-1.6.6Z"/></Icon>
const GitHubIcon = () => <Icon><path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3.3-.4 6.8-1.6 6.8-7.4A5.8 5.8 0 0 0 19.3 3 5.4 5.4 0 0 0 19.1 0S17.9-.4 15 1.6a13.4 13.4 0 0 0-7 0C5.1-.4 3.9 0 3.9 0a5.4 5.4 0 0 0-.2 3A5.8 5.8 0 0 0 2.2 7.1c0 5.8 3.5 7 6.8 7.4A4.8 4.8 0 0 0 8 18v4M8 19c-3 .9-3-1.5-4-2"/></Icon>
const BellIcon = () => <Icon><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/></Icon>
const ChevronIcon = () => <Icon><path d="m9 18 6-6-6-6"/></Icon>
const PlusIcon = () => <Icon><path d="M12 5v14M5 12h14"/></Icon>
const SearchIcon = () => <Icon><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></Icon>
const PriorityIcon = () => <Icon><path d="M4 18V9M10 18V5M16 18v-7M22 18V3"/></Icon>
const CloseIcon = () => <Icon><path d="m6 6 12 12M18 6 6 18"/></Icon>
function TypeIcon({ type }: { type: IssueType }) { return <span className={`type-icon type-${type.toLowerCase()}`}>{type === 'BUG' ? '●' : type === 'FEATURE' ? '◆' : '✓'}</span> }

export default App
