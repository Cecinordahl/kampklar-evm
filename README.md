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

## Deployment
- Backend: Render (free web service tier)
- Frontend: Vercel (free tier)
- Database/Storage: Firebase (Spark plan)
