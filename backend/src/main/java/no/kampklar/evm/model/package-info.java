/**
 * Domain model mirroring the Firestore schema. All collections are top-level (competitions,
 * divisions, groups, teams, players, matches) and reference each other by id, so the frontend
 * can query e.g. all matches in a group with a single where-clause. Coach and
 * TournamentHistoryEntry are embedded in the team document; Standing is embedded in the group.
 */
package no.kampklar.evm.model;
