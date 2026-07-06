---
name: paper-backup-snapshot
description: Create a timestamped snapshot of a Paper Minecraft server before state-changing operations. Use before plugin installs, world edits, config changes, world deletions, or any irreversible action.
---

# Paper Backup Snapshot

## When to use

Before ANY state-changing operation on the Paper server. The standing rule (from project memory): "always create tar/cp snapshot to /home/mc/minecraft/backups/ BEFORE any state-changing operation".

Specific triggers:
- Plugin install/uninstall/upgrade
- World creation/deletion via Multiverse
- DataPack changes
- worlds.yml edits
- server.properties edits
- Manual world building (large /fill operations)
- Any sed on plugin configs

## Workflow

### Standard full snapshot (recommended for plugin/config changes)

```bash
ssh mc@host "set -e
cd /home/mc/minecraft
docker compose stop paper          # for clean state — Paper might be writing
TS=\$(date -u +%Y%m%d-%H%M%S)
tar -czf backups/<reason>-\$TS.tar.gz data/ docker-compose.yml
ls -lh backups/<reason>-\$TS.tar.gz
docker compose start paper
"
```

Replace `<reason>` with a descriptive prefix: `pre-stage1`, `pre-mvp3`, `pre-fartit-removal`, etc.

### Targeted backup (for specific scope; faster)

When changing only:
- **Plugin configs**: `tar -czf backups/<r>-\$TS.tar.gz data/plugins/<plugin-name>/`
- **One world**: `tar -czf backups/<r>-\$TS.tar.gz data/<world-name>/`
- **Few region files** (for localized world edits): `cp data/<world>/region/r.X.Y.mca backups/<dir>/`
- **MV worlds.yml only**: `cp data/plugins/Multiverse-Core/worlds.yml.bak-<reason>`

### Player data migration backup

Before deleting/moving player.dat:
```bash
find /home/mc/minecraft/data -path '*/playerdata/*' \( -name "<UUID1>*" -o -name "<UUID2>*" \) -exec cp -v {} backups/playerdata-pre-<reason>-\$TS/ \;
```

## Restore

If something goes wrong:
```bash
docker compose stop paper
tar -xzf backups/<file>.tar.gz -C /tmp/restore/
# inspect, then move what's needed back
docker compose start paper
```

## Gotchas

- **Snapshot while Paper running may catch dirty chunks** mid-write. For world data, prefer Paper-stopped or run `docker exec paper rcon-cli 'save-all flush'` first.
- **Don't backup logs/** unless debugging — they grow large.
- **Backup directory `/home/mc/minecraft/backups/`** is the canonical location per project convention. Don't use `/tmp/` (gets cleared on reboot).
- **Disk space**: full Paper data (with worlds) can be 5+ GB. Check `df -h` before tar. The `data/` dir is the bulk; `data/<earth-world>/` alone might be 4 GB.
- **Naming convention**: `backups/<scope-or-reason>-<UTC-timestamp>.tar.gz`. The UTC timestamp `$(date -u +%Y%m%d-%H%M%S)` sorts naturally and avoids timezone confusion.
