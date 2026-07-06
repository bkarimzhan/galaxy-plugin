---
name: paper-mv-spawn-fix
description: Fix Multiverse-Core spawn-location after world recreate or relocation. mv modify <world> set spawn does NOT work in MV 5.6+ — must edit data/plugins/Multiverse-Core/worlds.yml directly while paper is stopped. Resolves infinite respawn-death-loops caused by stale spawn coords pointing into unloaded chunks.
---

# Fixing Multiverse spawn after world rebuild

## When to use

- After regenerating a Multiverse-managed world (`/mv create` after `rm -rf data/<world>`).
- When `/mv tp <world>` lands the player at old/wrong coordinates.
- When player respawns on death and instantly dies again ("респавн-смерть-loop").
- When `/mv setspawn` from console fails (it requires a player target).

## Why mv-modify doesn't work

In MV-Core 5.6.x, `mv modify <world> set spawn|spawnLocation|spawn-location 0,65,0` returns `Failed to set '<x>' ... Config node not found`. The serialized `spawn-location:` block IS present in `worlds.yml` (`==: MVSpawnLocation` plus x/y/z/yaw/pitch), but it's not editable through `mv modify`. The clean path is to edit the YAML file directly with paper stopped, then start.

## Steps

```bash
# 1. Stop paper
ssh mc@host "cd /home/mc/minecraft && docker compose stop paper"

# 2. Backup worlds.yml
ssh mc@host "cp /home/mc/minecraft/data/plugins/Multiverse-Core/worlds.yml{,.bak-$(date -u +%Y%m%d-%H%M%S)}"

# 3. Patch via Python pyyaml (preserves indentation; sed/regex breaks YAML structure)
ssh mc@host "python3 <<'PYEOF'
import yaml
path = '/home/mc/minecraft/data/plugins/Multiverse-Core/worlds.yml'
with open(path) as f:
    data = yaml.safe_load(f)
data['<world>']['spawn-location'].update({
    'x': 0.0, 'y': 65.0, 'z': 0.0,
    'yaw': 0.0, 'pitch': 0.0,
})
with open(path, 'w') as f:
    yaml.safe_dump(data, f, default_flow_style=False, allow_unicode=True)
print('patched')
PYEOF"

# 4. Also set vanilla level.dat spawn for consistency (server still down — start first or do via rcon after start)
ssh mc@host "cd /home/mc/minecraft && docker compose start paper"
ssh mc@host "until docker logs paper --tail 5 2>&1 | grep -qE 'Done \(.*\)! For help'; do sleep 12; done; docker exec paper rcon-cli 'execute in minecraft:overworld run setworldspawn 0 65 0'"

# 5. Verify
ssh mc@host "docker exec paper rcon-cli 'mv info <world>' 2>&1 | grep -i 'spawn loc'"
```

## DO NOT use sed/regex on worlds.yml

A regex replace looks fine but **silently corrupts indentation** under nested YAML blocks — the spawn-location block lives inside a per-world block at depth 4, and even one misplaced space breaks the entire MV config. Always use the YAML library which preserves structure.

## Side effect: player.dat persists last-known position across world recreates

Even with MV-spawn fixed, players who logged on the OLD world still have their last position in `data/<world>/playerdata/<uuid>.dat`. After regen, they respawn at those old coords (now in unloaded chunks of new world) → fall, void death, respawn loop.

**Quick fix:** kill them while keepInventory=true is enabled — they respawn at MV-spawn (now correct):
```
docker exec paper rcon-cli 'kill <player>'
```

**Permanent fix** (only if you don't care about playerdata): delete or move `data/<world>/playerdata/*.dat`. They'll re-spawn fresh on next login.

## Bonus: create EssentialsX warp without /setwarp

`/setwarp` requires a player at the target location. From console, write the file directly:

```bash
# Get world UUID from binary uid.dat
WORLD_UUID=$(ssh mc@host "python3 -c \"
import sys
b = open('/home/mc/minecraft/data/<world>/uid.dat','rb').read()
print('-'.join([b[0:4].hex(), b[4:6].hex(), b[6:8].hex(), b[8:10].hex(), b[10:16].hex()]))
\"")

# Get any online player UUID for lastowner (from ops.json or usercache)
LASTOWNER_UUID=$(ssh mc@host "jq -r '.[0].uuid' /home/mc/minecraft/data/ops.json")

# Write warp file
ssh mc@host "cat > /home/mc/minecraft/data/plugins/Essentials/warps/<warp_name>.yml <<WARPEOF
name: <warp_name>
lastowner: $LASTOWNER_UUID
world: $WORLD_UUID
x: 0.5
y: 65.0
z: 0.5
yaw: 0.0
pitch: 0.0
WARPEOF"

# Reload Essentials
ssh mc@host "docker exec paper rcon-cli 'essentials reload'"

# Verify
ssh mc@host "docker exec paper rcon-cli 'warp'"
```

The file MUST have `world:` set to the world UUID (not name) — Essentials 2.21+ uses UUID-based world references.
