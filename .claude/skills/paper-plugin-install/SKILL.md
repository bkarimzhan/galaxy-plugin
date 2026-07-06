---
name: paper-plugin-install
description: Install or upgrade a plugin on a Paper Minecraft server (itzg/minecraft-server docker image). Handles finding direct .jar URL via Modrinth/Hangar/spiget, stop/install/start cycle, and verification.
---

# Paper Plugin Install

## When to use

User asks to install, add, upgrade, or update a Bukkit/Paper plugin. Trigger phrases: "install plugin", "поставить плагин", "обновить плагин", plugin name + version.

## Workflow

1. **Find direct .jar URL.** Public registries in priority order:
   - **Modrinth API** (preferred, has direct CDN, no auth):
     ```bash
     curl -s 'https://api.modrinth.com/v2/project/<slug>/version' | python3 -c "
     import json, sys
     for v in json.load(sys.stdin):
         if '<MC_VERSION>' in v['game_versions']:
             print(v['version_number'], v['files'][0]['url'])
             break
     "
     ```
   - **Hangar API** (PaperMC official): `https://hangar.papermc.io/api/v1/projects/<owner>/<project>/versions`
   - **Spiget proxy** (cookie-free Spigot mirror): `https://api.spiget.org/v2/resources/<id>/download`
   - **GitHub releases** (some projects): `https://api.github.com/repos/<owner>/<repo>/releases/latest`

2. **Confirm 1.21.8 (or current MC version) compat explicitly** in `game_versions` — don't trust "1.21+".

3. **Known paid-only plugins (require user to manually upload)** — flag and stop:
   - Iris (Volmit), Nexo (Discord/Stripe), Oraxen, ItemsAdder, Lands premium, ShopGUI+, etc.

4. **Snapshot before installing** (call paper-backup-snapshot skill).

5. **Stop Paper, download to `plugins/`, restart.**
   ```bash
   ssh mc@host "set -e
   cd /home/mc/minecraft
   docker compose stop paper
   cd data/plugins
   curl -fsSL -o <Plugin>-<Version>.jar '<URL>'
   ls -lh <Plugin>-<Version>.jar
   sha256sum <Plugin>-<Version>.jar
   cd /home/mc/minecraft
   docker compose start paper"
   ```

6. **Wait for boot (poll `Done (`), verify enable line + no errors.**
   ```bash
   for i in 1..12; do sleep 5; docker logs paper --tail 60 | grep -q 'Done (' && break; done
   docker logs paper 2>&1 | grep -iE 'Enabling <PluginName>' | tail -5
   docker logs paper --since 2m 2>&1 | grep -iE 'severe|error' | grep -v 'GeoLite' || echo '(none)'
   ```

7. **Record version + SHA-256 in CLAUDE.md** under "Plugin stack" section.

## Gotchas

- itzg image runs Paper inside container at `/data/plugins/`, mapped to host `/home/mc/minecraft/data/plugins/`. Use HOST path for file ops, CONTAINER path inside `docker exec`.
- Some plugins need restart, not `/reload` — Paper plugins have hard-deps that need fresh classloader.
- `docker compose` requires `cd` to the directory with `docker-compose.yml` first (or use `-f /path/to/docker-compose.yml`).
- Compatibility of FastAsyncWorldEdit (FAWE) covers WorldEdit dep — don't install plain WorldEdit alongside.
- Multiverse-NetherPortals 5.x must match Multiverse-Core 5.x major version.
