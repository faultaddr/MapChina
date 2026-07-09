# MapChina Cloud Sync Design

Date: 2026-07-10

## Goal

Move user-owned MapChina data from local-only storage toward server-backed cloud synchronization, then deploy the backend locally so the Android app and scripts can talk to a real Ktor service.

## Current State

The project already has a Ktor server, JWT login, PostgreSQL tables for core map data, a local SQLDelight `sync_queue`, `SyncEngine`, and `MapChinaApiClient`.

The current sync implementation is not usable end to end because the client sends generic queue items:

```text
entityType, entityId, operation, payload
```

but the server `/sync/push` route expects typed lists:

```text
footprints, attractionVisits
```

The first implementation must unify the protocol before adding more entity types.

## Sync Scope

V1 cloud sync covers user-owned mutable data:

- footprints
- attraction visits
- carvings
- journals
- journal photo metadata
- journal track points
- app settings that affect personal experience

V1 does not upload binary image files. Journal and carving image fields are synchronized as metadata paths only. A later storage pass can add object upload and remote URLs.

V1 does not sync derived achievement and score tables as source-of-truth records. Achievements, scores, and stats should be recomputed from synced source data, or synchronized later with their own settlement protocol.

## Server Model

Add a generic `sync_items` table:

```text
user_id
entity_type
entity_id
operation
payload
updated_at
deleted
```

The primary key is `(user_id, entity_type, entity_id)`. The server stores the latest version of every synced entity. Deletions are tombstones: `deleted = true`, `operation = DELETE`, and the payload may be empty.

## Protocol

`POST /sync/push`

Request:

```json
{
  "items": [
    {
      "entityType": "FOOTPRINT",
      "entityId": "u1:330000",
      "operation": "UPSERT",
      "payload": "{\"userId\":\"u1\",\"regionId\":\"330000\",\"level\":\"DEEP\",\"timestamp\":123}",
      "updatedAt": 123,
      "deleted": false
    }
  ]
}
```

Response:

```json
{
  "accepted": 1,
  "serverTime": 123
}
```

`GET /sync/pull?since=123`

Response:

```json
{
  "items": [],
  "serverTime": 456
}
```

The server must also keep backward compatibility for already existing typed footprint and attraction-visit payloads where practical, but the generic `items` field is the new primary path.

## Client Model

The local `sync_queue` remains the durable offline queue. Repositories enqueue source-data mutations after successful local writes:

- `FootprintRepository.markFootprint`
- `FootprintRepository.markAttractionVisit`
- `FootprintRepository.removeFootprint`
- `FootprintRepository.removeAttractionVisit`
- `CarvingRepository.insertCarving`
- `CarvingRepository.updateCarving`
- `CarvingRepository.deleteCarving`
- `JournalRepository.insertJournal`
- `JournalRepository.updateJournal`
- `JournalRepository.deleteJournal`
- journal photo and track point inserts/deletes
- user setting writes

Repositories should accept an optional sync queue dependency so tests and offline use remain simple. The queue must not break local-first behavior if no server exists.

## Merge Rules

- Footprint conflict: keep the higher visit level; keep the latest timestamp.
- Attraction visit conflict: keep the higher visit level; keep the latest timestamp and latest note.
- Carving, journal, photo metadata, track point, and setting conflict: latest `updatedAt` wins.
- Delete conflict: a tombstone with newer `updatedAt` wins.
- Pulling remote changes must write local tables without re-enqueuing those same changes.

## Authentication

Sync routes remain JWT protected. Local quick-start users stay local-only until they phone-login. For local server smoke tests, phone login uses the existing fixed code `123456`.

## Configuration

The API base URL must no longer be a hard-coded LAN IP. V1 should support an environment or build-time default:

- local host service: `http://127.0.0.1:8080`
- Android emulator: `http://10.0.2.2:8080`

If full platform-specific runtime configuration is too large for this pass, the Koin module should at least centralize the default in one helper function.

## Local Deployment

This machine has native PostgreSQL tooling available and Docker is not installed. Local deployment should use native PostgreSQL:

1. create or reuse database user `mapchina`
2. create or reuse database `mapchina`
3. run `./gradlew :server:run`
4. run the existing seed script after schema creation
5. verify `/health`
6. verify auth login plus `/sync/push` and `/sync/pull` over real HTTP

## Verification

Required before claiming completion:

- server route tests for push/pull and schema creation
- client sync engine tests for push, pull, conflict resolution, and queue retry
- focused repository tests proving local mutations enqueue sync items
- `./gradlew :server:test :shared:allTests :androidApp:testDebugUnitTest`
- local server deployment with PostgreSQL
- real HTTP smoke test: health, login, push, pull
- if app runtime behavior changes, Android install and emulator verification per `AGENTS.md`

