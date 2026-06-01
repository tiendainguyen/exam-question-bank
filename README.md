# exam-question-bank

AI-powered exam question-bank: extract exams to JSON, classify question types,
generate practice exams, AI grading, and a mastery mindmap.

**Stack:** Spring Boot 3.5 + Java 21 · PostgreSQL 16 + pgvector · React 19 + Vite + Tailwind.
See [DESIGN.md](DESIGN.md) for the full architecture and the MVP build order (B0–B7).

## Quick start

### 1. Database (Docker)
```bash
docker compose up -d        # Postgres 16 + pgvector on :5432, Adminer on :8081
```

### 2. Backend (needs JDK 21)
```bash
./mvnw spring-boot:run      # http://localhost:8080
./mvnw verify               # tests (Testcontainers — needs Docker running)
```

### 3. Frontend (needs Node 20+)
```bash
cd frontend
npm install
npm run dev                 # http://localhost:5173 (proxies /api → :8080)
```

## Auth API (B0)

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/api/auth/signup` | public | Create account → returns JWT |
| POST | `/api/auth/login`  | public | Log in → returns JWT |
| GET  | `/api/me`          | Bearer | Current user identity (protected) |

Config via env: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_JWT_SECRET` (≥32 bytes).
