/**
 * Admin-only write endpoints (e.g. PUT /admin/matches/{id}, POST /admin/teams/{id}/refresh),
 * plus a public GET /health used to wake the backend. Public read endpoints are not implemented
 * here — the frontend reads Firestore directly.
 */
package no.kampklar.evm.controller;
