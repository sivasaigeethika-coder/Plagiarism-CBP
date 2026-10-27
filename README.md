# AI-Plagiarism-Checker

## CopyCatch® — AI Plagiarism Checker

CopyCatch is a React/Vite frontend connected to the supplied Spring Boot and MongoDB backend. The UI keeps the existing monochrome, full-screen video login style and extends its typography, rounded controls, and spacing into the workspace.

## Backend audit

- **Framework / language:** Spring Boot 4.1.1, Java 21, Spring Web MVC, Spring Security, Spring Data MongoDB, JWT (`jjwt` 0.12.6), Maven.
- **Backend structure:** `controller/` REST routes; `dto/` request/response objects; `model/` Mongo documents; `repository/` Mongo repositories; `security/` JWT filter/configuration; `service/` business logic; `similarity/` token/Jaccard/cosine/structural scoring; `exception/` handlers.
- **Data:** Mongo collections include users, assignments, submissions, similarity results, reports, and audit logs. User passwords are BCrypt hashes. Submission code is stored as source text.
- **Checking method:** JSON source-code submission, not multipart upload; teachers can synchronously compare all pairs of submissions from different students within an assignment. Results are saved per submission pair. File selection in this frontend reads supported text/source files in the browser and sends their text in JSON (1 MB client limit).
- **JWT:** Login returns a one-hour token and profile fields. Frontend keeps the token in `localStorage`; this follows the API's bearer-token model but is accessible to scripts running in the page. For higher security, move to an HttpOnly cookie session.

### API endpoints from the supplied backend

`Auth` means `Authorization: Bearer <JWT>` unless marked public. Objects below are JSON.

| Method and path | Request | Response | Access |
| --- | --- | --- | --- |
| `POST /api/auth/register` | `{name,email,password,role}` | `{id,name,email,role}` | Public |
| `POST /api/auth/login` | `{email,password}` | `{token,name,email,role}` | Public |
| `GET /api/test` | — | Plain text health message | Public |
| `GET /api/protected` | — | Plain text | Auth |
| `GET /api/teacher/dashboard` | — | Plain text | Teacher |
| `GET /api/student/dashboard` | — | Plain text | Student |
| `POST /api/assignments` | `{title,description,language,deadline,similarityThreshold}` | Assignment document | Teacher |
| `GET /api/assignments` | — | Assignment array | Auth |
| `GET /api/assignments/my` | — | Current teacher's assignment array | Teacher |
| `POST /api/submissions` | `{assignmentId,fileName,sourceCode,language}` | Saved submission | Student |
| `GET /api/submissions` | — | All submissions | Teacher |
| `GET /api/submissions/assignment/{assignmentId}` | — | Submission array | Teacher |
| `GET /api/submissions/student/{studentId}` | — | Submission array | Student |
| `POST /api/similarity/assignment/{assignmentId}/scan` | — | Saved pairwise `SimilarityResult` array for different students in that assignment | Owning teacher |
| `POST /api/similarity/compare?submission1Id=…&submission2Id=…` | Query parameters | Saved `SimilarityResult` with component scores and risk; submissions must be from different students in the same assignment | Owning teacher |
| `GET /api/similarity/assignment/{assignmentId}` | — | Similarity result array | Owning teacher |
| `POST /api/reports/{assignmentId}` | — | Saved aggregate `Report` | Teacher |
| `GET /api/reports/{assignmentId}` | — | Aggregate `Report` | Teacher |
| `GET /api/dashboard/stats` | — | `DashboardStats` totals and average | Teacher |
| `GET /api/audit` | — | Audit log array | Teacher |
| `GET /api/audit/user/{userId}` | — | Audit log array | Teacher |
| `GET /api/audit/assignment/{assignmentId}` | — | Audit log array | Teacher |

The controller does not implement `/api/assignments/available`, although the security config mentions it; the sample `backend/test.http` also calls an unimplemented `POST /api/test/user`. There is no delete submission, history-by-current-user convenience route, single-document plagiarism scan, PDF export, matched passage/source data, or results endpoint that returns source text. The UI therefore limits reports to pairwise metric breakdowns and does not claim to show highlighted passages. Students submit work; the assignment owner can compare all saved submission pairs across different students.

### Frontend design spec

- **Stack:** React 19, TypeScript, Vite 8, Tailwind CSS 4, Lucide icons, Motion dependency already present in the supplied project (not required by the new screens).
- **Login source of truth:** full-screen, muted loop video; CopyCatch wordmark and document/search icon at top left; left aligned greeting; two vertically stacked white pill buttons; Helvetica Now Display with Helvetica/Arial fallbacks; black text; transparent white veil over neutral footage; subtle border/shadow; hover inverts buttons; compact mobile type sizes.
- **Workspace extension:** same Helvetica family, monochrome black/white/gray palette, rounded pills/cards, subtle gray borders and shadows, responsive spacing, keyboard focus rings. No new runtime dependencies.

## Run locally

Requires Java 21, Maven (or the included wrapper), Node 22/npm, and MongoDB. From this directory:

```sh
cp .env.example .env
# Replace JWT_SECRET in .env with a random value of at least 32 bytes.
docker compose up --build
```

Open `http://localhost:3000`. The API is at `http://localhost:8080`. To run services without Docker, start MongoDB, export `JWT_SECRET` and `MONGODB_URI`, then run `./backend/mvnw -f backend/pom.xml spring-boot:run`; in another terminal run `cd frontend && cp .env.example .env && npm ci && npm run dev`.

## Build and lint

```sh
cd frontend && npm ci && npm run lint && npm run build
cd ../backend && ./mvnw -B -DskipTests package
```

## Environment variables

See [frontend/.env.example](frontend/.env.example), [backend/.env.example](backend/.env.example), and root [.env.example](.env.example). `VITE_API_URL` is embedded at frontend build time. `JWT_SECRET` must be a unique secret of at least 32 bytes. `MONGODB_URI` selects MongoDB; `CORS_ALLOWED_ORIGINS` is a comma-separated allowlist of exact origins.

## Deploy

### Free-tier example: Vercel + Render + MongoDB Atlas

1. Create a MongoDB Atlas Free cluster and database user. Set its network access so the backend can connect, then copy its connection string into the Render service's `MONGODB_URI` setting.
2. Create a Render Web Service from this repository with root directory `backend` and Dockerfile path `Dockerfile`. Set `JWT_SECRET` to a unique random value of at least 32 bytes and `CORS_ALLOWED_ORIGINS` to the exact Vercel site origin (for example `https://your-project.vercel.app`). Render free web services sleep after 15 minutes idle and can take about a minute to wake; Render describes free services as suitable for hobby projects and previews, not production ([Render Free](https://render.com/docs/free)).
3. Import the repository into Vercel with the project root set to `frontend`, build command `npm run build`, output directory `dist`, and environment variable `VITE_API_URL` set to the Render backend URL. Vercel's Hobby plan is free for personal projects ([Vercel plans](https://vercel.com/docs/plans)).
4. In Atlas, allow the outbound IP ranges shown under your Render service's Connect → Outbound settings. Render may use any address in those ranges ([Render outbound IPs](https://render.com/docs/outbound-ip-addresses)); avoid `0.0.0.0/0` for real data. Atlas Free currently caps storage at 0.5 GB and has other operational limits ([Atlas Free limits](https://www.mongodb.com/docs/atlas/reference/free-shared-limitations/)).

After connecting the repository to Render and setting the backend variables above, trigger the backend deploy in the Render dashboard. Once the service is live, deploy the frontend with the Vercel CLI:

```sh
cd frontend
npx vercel login
npx vercel link
npx vercel env add VITE_API_URL production
# Enter the Render backend URL when prompted.
npx vercel --prod
```

The root Docker Compose setup runs everything locally. Free-tier hosting is a demo setup: plan limits, wake-up delay, storage caps, and data policies can change. For production student data, use paid services with backups, monitored capacity, and private networking where available.

## Known backend constraints and changes

Backend changes: Mongo/JWT/CORS configuration is now environment-driven; registration returns a password-free response DTO; student/teacher assignment listing is permitted for both roles while assignment creation remains teacher-only; `GET /api/submissions` is restricted to teachers; and students may only fetch their own submission history. No similarity algorithm was changed. The audit log APIs remain teacher-only. **Before public launch, gate teacher registration with an invitation or approval flow:** the supplied API lets anyone register with the `TEACHER` role. Also enforce assignment ownership inside teacher submission, similarity, and report endpoints; those backend routes currently authorize by role without checking that the teacher owns the assignment.
