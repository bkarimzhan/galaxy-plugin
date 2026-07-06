---
name: itzg image regenerates server.properties from compose env
description: itzg/minecraft-server overwrites server.properties on each start from docker-compose env vars. Manual sed gets wiped on restart.
type: feedback
originSessionId: cd18e1ec-2075-4688-bdd4-e97f7766f3ec
---
**Rule:** for `itzg/minecraft-server` (Paper image), edit persistent server settings via `docker-compose.yml` `environment:` block, NOT via `sed` of `server.properties` directly.

**Why:** the entrypoint script regenerates `server.properties` on every container start from environment variables (`VIEW_DISTANCE`, `MEMORY`, `DIFFICULTY`, `MAX_PLAYERS`, etc.). Manual edits to `data/server.properties` are lost on restart. Confirmed 2026-05-05 — bumped view-distance to 12 via `sed`, restart reset it back to 8 because compose had `VIEW_DISTANCE: "8"`.

**How to apply:** when user wants to change view-distance, max-players, difficulty, server.properties values:
1. Edit `/home/mc/minecraft/docker-compose.yml` env block.
2. Restart with `docker compose up -d --force-recreate paper` to pick up env changes.
3. Verify with `grep '^<setting>=' data/server.properties` after boot.

**Exception:** plugin configs (`plugins/<Plugin>/config.yml`) are NOT touched by entrypoint — direct edits there are persistent.
