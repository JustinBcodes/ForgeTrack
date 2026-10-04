import { useEffect, useState, type FormEvent } from "react";
import {
  Activity,
  ArrowUpRight,
  Check,
  CircleAlert,
  GitPullRequest,
  MessageCircle,
  Send,
  X,
} from "lucide-react";
import { api } from "./api";
import type { IssueDetail, IssuePriority, IssueStatus, User } from "./types";

const statuses: IssueStatus[] = [
  "BACKLOG",
  "TODO",
  "IN_PROGRESS",
  "IN_REVIEW",
  "DONE",
];
const priorities: IssuePriority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];
const label = (value: string) =>
  value
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());

export default function IssueDetailPanel({
  issueId,
  repository,
  currentUserId,
  members,
  onClose,
  onChanged,
}: {
  issueId: string;
  repository: string;
  currentUserId: string;
  members: User[];
  onClose: () => void;
  onChanged: () => Promise<void>;
}) {
  const [issue, setIssue] = useState<IssueDetail | null>(null);
  const [comment, setComment] = useState("");
  const [prNumber, setPrNumber] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  useEffect(() => {
    let active = true;
    api
      .issue(issueId)
      .then((data) => {
        if (active) setIssue(data);
      })
      .catch((reason: Error) => {
        if (active) setError(reason.message);
      });
    return () => {
      active = false;
    };
  }, [issueId]);

  async function update(
    changes: Partial<Pick<IssueDetail, "status" | "priority" | "assignee">>,
  ) {
    if (!issue) return;
    setSaving(true);
    setError("");
    try {
      const data = await api.updateIssue(issue.id, {
        title: issue.title,
        description: issue.description,
        type: issue.type,
        status: changes.status ?? issue.status,
        priority: changes.priority ?? issue.priority,
        actorId: currentUserId,
        assigneeId:
          changes.assignee === undefined
            ? (issue.assignee?.id ?? null)
            : (changes.assignee?.id ?? null),
      });
      setIssue(data);
      await onChanged();
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "Unable to update issue.",
      );
    } finally {
      setSaving(false);
    }
  }
  async function postComment(event: FormEvent) {
    event.preventDefault();
    if (!issue || !comment.trim()) return;
    setSaving(true);
    setError("");
    try {
      await api.addComment(issue.id, currentUserId, comment.trim());
      setComment("");
      setIssue(await api.issue(issue.id));
      await onChanged();
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "Unable to add comment.",
      );
    } finally {
      setSaving(false);
    }
  }
  async function linkPR(event: FormEvent) {
    event.preventDefault();
    if (!issue || !prNumber) return;
    setSaving(true);
    setError("");
    try {
      await api.linkPullRequest(
        issue.id,
        repository,
        Number(prNumber),
        currentUserId,
      );
      setPrNumber("");
      setIssue(await api.issue(issue.id));
      await onChanged();
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : "Unable to link pull request.",
      );
    } finally {
      setSaving(false);
    }
  }
  return (
    <div
      className="detail-backdrop"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <section
        className="detail-panel"
        role="dialog"
        aria-modal="true"
        aria-label={issue ? `${issue.identifier} details` : "Issue details"}
      >
        <header className="detail-top">
          <span>
            ISSUE DETAILS <i /> {issue?.identifier || "Loading"}
          </span>
          <button onClick={onClose} aria-label="Close issue details">
            <X size={19} />
          </button>
        </header>
        {error && (
          <div className="detail-error" role="alert">
            <CircleAlert size={15} />
            {error}
          </div>
        )}
        {!issue ? (
          <div className="detail-loading">Loading issue details…</div>
        ) : (
          <div className="detail-scroll">
            <div className="detail-title">
              <span className="detail-type">{label(issue.type)}</span>
              <h2>{issue.title}</h2>
              <p>{issue.description || "No description provided."}</p>
            </div>
            <div className="detail-properties">
              <label>
                Status
                <select
                  aria-label="Issue status"
                  value={issue.status}
                  disabled={saving}
                  onChange={(event) =>
                    void update({ status: event.target.value as IssueStatus })
                  }
                >
                  {statuses.map((status) => (
                    <option value={status} key={status}>
                      {label(status)}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Priority
                <select
                  aria-label="Issue priority"
                  value={issue.priority}
                  disabled={saving}
                  onChange={(event) =>
                    void update({
                      priority: event.target.value as IssuePriority,
                    })
                  }
                >
                  {priorities.map((priority) => (
                    <option value={priority} key={priority}>
                      {label(priority)}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Assignee
                <select
                  aria-label="Issue assignee"
                  value={issue.assignee?.id ?? ""}
                  disabled={saving}
                  onChange={(event) =>
                    void update({
                      assignee:
                        members.find(
                          (member) => member.id === event.target.value,
                        ) ?? null,
                    })
                  }
                >
                  <option value="">Unassigned</option>
                  {members.map((member) => (
                    <option value={member.id} key={member.id}>
                      {member.displayName}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div className="detail-section">
              <h3>
                <GitPullRequest size={17} /> Pull requests{" "}
                <span>{issue.pullRequests.length}</span>
              </h3>
              {issue.pullRequests.map((pr) => (
                <a
                  className="linked-pr"
                  href={pr.url}
                  target="_blank"
                  rel="noreferrer"
                  key={pr.id}
                >
                  <GitPullRequest size={16} />
                  <span>
                    {pr.repository} #{pr.number}
                    <small>
                      {pr.title} · {pr.state}
                    </small>
                  </span>
                  <ArrowUpRight size={15} />
                </a>
              ))}
              <form className="pr-form" onSubmit={linkPR}>
                <input
                  type="number"
                  min="1"
                  placeholder="Pull request number"
                  aria-label="Pull request number"
                  value={prNumber}
                  onChange={(event) => setPrNumber(event.target.value)}
                />
                <button type="submit" disabled={!prNumber || saving}>
                  Link PR
                </button>
              </form>
            </div>
            <div className="detail-section">
              <h3>
                <MessageCircle size={17} /> Discussion{" "}
                <span>{issue.comments.length}</span>
              </h3>
              {issue.comments.length ? (
                issue.comments.map((entry) => (
                  <div className="comment" key={entry.id}>
                    <span className="comment-avatar">
                      {entry.author.displayName
                        .split(" ")
                        .map((n) => n[0])
                        .join("")}
                    </span>
                    <div>
                      <strong>{entry.author.displayName}</strong>
                      <small>
                        {new Date(entry.createdAt).toLocaleDateString()}
                      </small>
                      <p>{entry.body}</p>
                    </div>
                  </div>
                ))
              ) : (
                <p className="detail-empty">
                  No comments yet. Start the conversation.
                </p>
              )}
              <form className="comment-form" onSubmit={postComment}>
                <textarea
                  aria-label="Add a comment"
                  placeholder="Add an update or question…"
                  value={comment}
                  onChange={(event) => setComment(event.target.value)}
                />
                <button type="submit" disabled={!comment.trim() || saving}>
                  <Send size={14} /> Post comment
                </button>
              </form>
            </div>
            <div className="detail-section">
              <h3>
                <Activity size={17} /> Activity{" "}
                <span>{issue.activities.length}</span>
              </h3>
              {issue.activities.slice(0, 5).map((entry) => (
                <div className="activity-row" key={entry.id}>
                  <Check size={14} />
                  <span>
                    <strong>{entry.actor.displayName}</strong>{" "}
                    {entry.details || entry.action}
                  </span>
                  <time>{new Date(entry.createdAt).toLocaleDateString()}</time>
                </div>
              ))}
              {!issue.activities.length && (
                <p className="detail-empty">No recorded activity yet.</p>
              )}
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
