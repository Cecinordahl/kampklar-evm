# Kampklar EVM

Unofficial fan project tracking men's national team competition progress, starting with the
UEFA Nations League 2026/27. Live at https://kampklar-evm.vercel.app.

## Structure

```
/backend   Java 21 + Spring Boot. Admin-only write endpoints (match entry, standings
           recomputation, "Oppdater lagdata" AI refresh). No public read API - the
           frontend reads Firestore directly.
/frontend  React + TypeScript (Vite). Public pages read Firestore via the client SDK;
           the admin view calls the backend after authenticating with Firebase Auth.
```

## Setup

### Backend
```
cd backend
cp .env.example .env   # fill in FIREBASE_SERVICE_ACCOUNT_JSON, ADMIN_UID, ANTHROPIC_API_KEY
set -a; source .env; set +a   # Spring does not read .env itself
./mvnw spring-boot:run
```
`ANTHROPIC_API_KEY` is the one paid dependency in this stack (powers the "Oppdater lagdata"
button only); everything else runs on free tiers.

### Frontend
```
cd frontend
cp .env.example .env   # fill in the Firebase web config (VITE_FIREBASE_*)
npm install
npm run dev
```

### Firestore rules and seed data (once per Firebase project)
```
firebase use --add                               # pick the Firebase project
firebase deploy --only firestore:rules           # public read-only, no client writes

cd backend
set -a; source .env; set +a   # if not already loaded in this shell
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
```
The seed loads competitions, groups, teams and all fixtures from
`backend/src/main/resources/seed/unl-2026-27.json`, then exits. It is safe to re-run: it never
overwrites existing matches, results, standings or team data. Results are entered through the
admin endpoint, not the seed file.

The same run also loads squads, coaches and tournament history for Norge, Spania and Frankrike
from `seed/kampklar-seed-teams.json`. Re-running never creates duplicate players and never
touches caps/goals of existing players (the file is a pre-Nations League baseline; match entry
owns them after that), but it does reset club, notes, coach, history and "sist oppdatert" to
the file's values.

### Admin account (once per Firebase project)
1. Firebase console > Authentication > Sign-in method: enable **Email/Password**.
2. Authentication > Users > Add user: create the single admin account.
3. Copy that user's UID into the backend's `ADMIN_UID`.

The admin view lives at `/admin` (not linked from the public pages). The backend verifies the
admin's token and UID on every request; the page itself only decides what to show.

### Local development with the Firebase emulators
No real project or credentials needed:
```
firebase emulators:start --only firestore,auth --project demo-kampklar
```
- Backend: set `FIRESTORE_EMULATOR_HOST=127.0.0.1:8085` and `FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099`
  (the service account JSON can then be any well-formed placeholder with `project_id` `demo-kampklar`).
- Frontend: set `VITE_USE_FIREBASE_EMULATORS=true` and `VITE_FIREBASE_PROJECT_ID=demo-kampklar` in `frontend/.env.local`.

## Deployment
- Backend: Render (free web service tier)
- Frontend: Vercel (free tier)
- Database/Storage: Firebase (Spark plan)
