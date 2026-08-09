import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

const bootstrap = {
  organization: { id: 'org-1', name: 'Bean Built', slug: 'bean-built' },
  projects: [{
    id: 'project-1', organizationId: 'org-1', name: 'ForgeTrack Platform', key: 'FORGE',
    description: 'A focused engineering project management workspace.', githubRepository: 'JustinBcodes/ForgeTrack',
  }],
  members: [{ id: 'user-1', displayName: 'Justin Bean', email: 'justin@example.com', role: 'OWNER' }],
}

const dashboard = {
  total: 5, open: 4, inProgress: 1, inReview: 1, completed: 1,
  byStatus: { BACKLOG: 0, TODO: 2, IN_PROGRESS: 1, IN_REVIEW: 1, DONE: 1 },
  recentlyUpdated: [],
}

const issuePage = {
  items: [{
    id: 'issue-1', identifier: 'FORGE-1', title: 'Add issue filtering to the REST API',
    type: 'FEATURE', status: 'IN_PROGRESS', priority: 'HIGH',
    assignee: bootstrap.members[0], updatedAt: new Date().toISOString(),
  }],
  page: 0, size: 20, totalItems: 1, totalPages: 1,
}

function jsonResponse(value: unknown) {
  return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(value) } as Response)
}

describe('ForgeTrack dashboard', () => {
  afterEach(() => vi.restoreAllMocks())

  it('loads the project dashboard and opens the issue form', async () => {
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => {
      const url = String(input)
      if (url.endsWith('/api/bootstrap')) return jsonResponse(bootstrap)
      if (url.includes('/dashboard')) return jsonResponse(dashboard)
      if (url.includes('/issues')) return jsonResponse(issuePage)
      throw new Error(`Unexpected request: ${url}`)
    }))

    render(<App />)

    expect(await screen.findByText('Add issue filtering to the REST API')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'ForgeTrack Platform' })).toBeInTheDocument()
    expect(screen.getByText('4')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'New issue' }))
    expect(screen.getByRole('dialog', { name: 'Create a new issue' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText('What needs to be done?')).toHaveFocus()
  })
})
