---
name: paper-itzg-config
description: Persistently change Paper server settings (view-distance, memory, max-players, difficulty, motd) on an itzg/minecraft-server docker container. The image regenerates server.properties from docker-compose env on every start, so manual sed of server.properties is wiped on restart.
---

# Configuring Paper running under itzg/minecraft-server

## When to use

User asks to change a `server.properties` value, JVM heap size, or any Paper runtime setting that's reset on container restart. Common triggers: "raise view distance", "increase max-players", "set MOTD", "give server more RAM", "change difficulty".

## Critical fact

`itzg/minecraft-server` (the docker image used here) **regenerates `server.properties` from environment variables on every container start**. Editing `data/server.properties` with `sed` or by hand persists only until the next restart, then it's overwritten back to whatever's in the docker-compose env block. The same applies to `eula.txt`, JVM flags, and a few other files.

This trips up everyone the first time. View-distance bumped via `sed` reverts to compose default after the next restart, with no warning.

## Workflow

```bash
# 1. Edit docker-compose.yml env block
ssh mc@host "sed -i 's/VIEW_DISTANCE: \"8\"/VIEW_DISTANCE: \"12\"/' /home/mc/minecraft/docker-compose.yml"

# 2. Verify
ssh mc@host "grep -E 'VIEW|MEMORY|MAX_PLAYERS' /home/mc/minecraft/docker-compose.yml"

# 3. Force recreate (NOT just restart — env changes need recreate)
ssh mc@host "cd /home/mc/minecraft && docker compose up -d --force-recreate paper"

# 4. Wait for boot, verify the env propagated to server.properties
ssh mc@host "until docker logs paper --tail 5 2>&1 | grep -qE 'Done \(.*\)! For help'; do sleep 12; done; grep -E '^(view-distance|simulation-distance|max-players|difficulty)=' /home/mc/minecraft/data/server.properties"
```

## Common env vars in itzg image

| Compose var | server.properties key / effect |
|---|---|
| `VIEW_DISTANCE` | view-distance |
| `SIMULATION_DISTANCE` | simulation-distance |
| `MAX_PLAYERS` | max-players |
| `DIFFICULTY` | difficulty (peaceful, easy, normal, hard) |
| `MOTD` | motd |
| `MEMORY` | both -Xms and -Xmx (e.g. "5G" → -Xms5G -Xmx5G) |
| `INIT_MEMORY`, `MAX_MEMORY` | override -Xms / -Xmx separately |
| `USE_AIKAR_FLAGS` | adds Aikar's G1GC tuning flags |
| `WHITELIST` | comma-separated names → whitelist.json |
| `OPS` | comma-separated names → ops.json |
| `SERVER_PORT` | server-port |

For the full list see itzg's docs at `https://docker-minecraft-server.readthedocs.io/`.

## Things that ARE persistent (no need to use compose env)

- Plugin configs in `data/plugins/<Plugin>/config.yml` and similar — entrypoint never touches these.
- World data (`data/<world>/level.dat`, regions, playerdata).
- Bukkit/Spigot/Paper config files (`bukkit.yml`, `spigot.yml`, `paper-global.yml`, `paper-world-defaults.yml`, `paper-world.yml`).
- Datapacks in `data/<world>/datapacks/`.
- WG `regions.yml`, MV `worlds.yml`, Essentials `warps/`.

So edits to plugin and world config files survive restarts and don't need to go via docker-compose env.

## Avoid

- ❌ `docker exec paper sh -c 'echo "view-distance=12" >> /data/server.properties'` — wiped on next restart.
- ❌ `docker compose restart paper` after env change — that does NOT pick up new env vars. Must use `up -d --force-recreate paper`.
- ❌ Hard-coded values in compose that conflict with explicit user requests in chat — always confirm what to set first.

## Memory budget on this server (Hetzner CPX21, 7.6 GiB total)

Safe heap range: **4G–5G**. With 6G heap + JVM overhead the host has only ~200 MiB free → no page cache → join lag. After dropping to 5G the host has 1.4 GiB available, no GC pressure.
