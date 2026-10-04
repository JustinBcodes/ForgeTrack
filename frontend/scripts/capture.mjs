import { chromium } from "playwright";
import { mkdir } from "node:fs/promises";

const browser = await chromium.launch({
  headless: true,
  executablePath:
    process.env.CHROME_PATH ||
    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
});
const page = await browser.newPage({
  viewport: { width: 1440, height: 920 },
  deviceScaleFactor: 1,
});
const member = (id, displayName) => ({
  id,
  displayName,
  email: `${id}@forgetrack.dev`,
  role: "MEMBER",
});
const members = [
  member("justin", "Justin Bean"),
  member("maya", "Maya Chen"),
  member("jordan", "Jordan Lee"),
];
const project = {
  id: "forge",
  organizationId: "bean",
  name: "ForgeTrack Platform",
  key: "FORGE",
  description: "A focused engineering project management workspace.",
  githubRepository: "JustinBcodes/ForgeTrack",
};
const now = new Date().toISOString();
const issues = [
  [
    "FORGE-1",
    "Add issue filtering to the REST API",
    "FEATURE",
    "IN_PROGRESS",
    "HIGH",
    members[0],
  ],
  [
    "FORGE-2",
    "Fix stale dashboard counts",
    "BUG",
    "IN_REVIEW",
    "URGENT",
    members[1],
  ],
  [
    "FORGE-3",
    "Model organization memberships",
    "TASK",
    "DONE",
    "MEDIUM",
    members[2],
  ],
  [
    "FORGE-4",
    "Link GitHub pull requests",
    "FEATURE",
    "TODO",
    "MEDIUM",
    members[1],
  ],
  [
    "FORGE-5",
    "Write controller integration tests",
    "TASK",
    "TODO",
    "HIGH",
    members[2],
  ],
].map(([identifier, title, type, status, priority, assignee]) => ({
  id: identifier,
  identifier,
  title,
  type,
  status,
  priority,
  assignee,
  updatedAt: now,
}));
await page.route("**/api/**", async (route) => {
  const path = new URL(route.request().url()).pathname;
  const body =
    path === "/api/bootstrap"
      ? {
          organization: { id: "bean", name: "Bean Built", slug: "bean-built" },
          projects: [project],
          members,
        }
      : path.endsWith("/dashboard")
        ? {
            total: 5,
            open: 4,
            inProgress: 1,
            inReview: 1,
            completed: 1,
            byStatus: {
              BACKLOG: 0,
              TODO: 2,
              IN_PROGRESS: 1,
              IN_REVIEW: 1,
              DONE: 1,
            },
            recentlyUpdated: issues,
          }
        : path.startsWith("/api/issues/")
          ? {
              ...issues[0],
              projectId: "forge",
              description:
                "Filter by status, priority, type, and free text. Keep combinations predictable and fast.",
              reporter: members[0],
              createdAt: now,
              comments: [
                {
                  id: "c1",
                  author: members[1],
                  body: "The combined filters are ready for review. Please verify pagination with empty results.",
                  createdAt: now,
                },
              ],
              activities: [
                {
                  id: "a1",
                  actor: members[0],
                  action: "UPDATED",
                  details: "moved this issue to In Progress",
                  createdAt: now,
                },
              ],
              pullRequests: [],
            }
          : { items: issues, page: 0, size: 20, totalItems: 5, totalPages: 1 };
  await route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
});
await mkdir("../screenshots", { recursive: true });
await page.goto(process.env.APP_URL || "http://localhost:5173", {
  waitUntil: "networkidle",
});
await page.getByRole("heading", { name: "ForgeTrack Platform" }).waitFor();
await page.screenshot({ path: "../screenshots/overview.png", fullPage: true });
await page
  .getByRole("button", { name: /Add issue filtering to the REST API/ })
  .click();
await page.getByRole("dialog", { name: "FORGE-1 details" }).waitFor();
await page.screenshot({
  path: "../screenshots/issue-details.png",
  fullPage: false,
});
await page.getByRole("button", { name: "Close issue details" }).click();
await page.getByRole("button", { name: "New issue" }).click();
await page.screenshot({
  path: "../screenshots/new-issue.png",
  fullPage: false,
});
await browser.close();
