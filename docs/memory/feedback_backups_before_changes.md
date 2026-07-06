---
name: Always backup before state-changing operations
description: User wants explicit snapshot/backup of affected files BEFORE any change so rollback is always possible
type: feedback
originSessionId: 304ff0c2-6267-435e-89d5-30c101acfe0c
---
Before any state-changing operation on the Minecraft server (plugin install/update, config edit, compose.yml change, server.properties change, world manipulation, etc.), **create a snapshot first** so rollback is always trivial.

**Standard pattern:**
- `mkdir -p /home/mc/minecraft/backups`
- `TS=$(date +%Y%m%d-%H%M%S); tar czf /home/mc/minecraft/backups/<scope>-pre-<op>-$TS.tgz -C /home/mc/minecraft <path>`
- Then perform the change
- If something breaks: `tar xzf <backup> -C /home/mc/minecraft && docker compose restart paper`

**Granularity:**
- Single file edit (config.yml, compose.yml) → `cp file file.orig` is sufficient (this is what Python scripts already do)
- Plugin add/remove/update → tar the whole `data/plugins/` dir
- Mode switch / world ops → tar `data/` dir (or just world subdirs if scoped)

**Why:** User explicitly asked 2026-05-03 during ProtocolLib install. Rationale: server state is stateful and partly unrecoverable (worlds), and even reversible-looking ops can have side effects (e.g., FastLogin DB schema changes between versions). A 30-second tar is cheap insurance.

**How to apply:** Default ON for any operation that modifies files in `/home/mc/minecraft/data/`, the compose.yml, or plugin jars. Mention the backup explicitly in the proposal so user sees it. Skip only for pure read-only diagnostics.
