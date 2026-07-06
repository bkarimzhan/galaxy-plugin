---
name: Multiverse spawn-location must be edited via worlds.yml, not mv modify
description: `mv modify <world> set spawn|spawnLocation|spawn-location <x,y,z>` all fail with "Config node not found". Edit worlds.yml directly with paper stopped.
type: feedback
originSessionId: cd18e1ec-2075-4688-bdd4-e97f7766f3ec
---
**Rule:** to change a Multiverse-Core 5.x world's spawn after world rebuild or relocation, do NOT try `mv modify <world> set spawn ...` — that property name isn't exposed via mv-modify in MV 5.6.x. Edit `data/plugins/Multiverse-Core/worlds.yml` directly while Paper is stopped, then start.

**Why:** confirmed 2026-05-05. Tried `mv modify earth set spawn 0,65,0`, `... set spawnLocation`, `... set spawn-location` — all returned `Failed to set '<x>': Config node not found`. The serialized `spawn-location:` block IS in worlds.yml (with `==: MVSpawnLocation` plus x/y/z/yaw/pitch), but mv-modify only edits a subset of nodes.

If MV-spawn is wrong after a world regen, players who die get a respawn loop: MV teleports them to the (stale) spawn coords, which may be in unloaded chunks → void death → respawn → loop.

**How to apply:**
1. `cd /home/mc/minecraft && docker compose stop paper`
2. Use Python pyyaml (NOT regex/sed — indentation matters and YAML lib preserves it):
   ```python
   import yaml
   path = '/home/mc/minecraft/data/plugins/Multiverse-Core/worlds.yml'
   data = yaml.safe_load(open(path))
   data['<world>']['spawn-location'].update({'x': X, 'y': Y, 'z': Z, 'yaw': 0.0, 'pitch': 0.0})
   yaml.safe_dump(data, open(path, 'w'), default_flow_style=False, allow_unicode=True)
   ```
3. `docker compose start paper`
4. Verify with `mv info <world>` → `Spawn Location:` line.

Also do `setworldspawn X Y Z` via rcon for vanilla level.dat consistency.

**Bonus:** for player-friendly TP, create EssentialsX warp by writing `data/plugins/Essentials/warps/<name>.yml` directly (server running OK):
```yaml
name: <name>
lastowner: <some-player-uuid>
world: <world-uuid-from-data/<world>/uid.dat>
x: 0.5
y: 65.0
z: 0.5
yaw: 0.0
pitch: 0.0
```
Then `essentials reload` via rcon. World UUID extraction from binary uid.dat:
```python
b = open('data/<world>/uid.dat','rb').read()
print('-'.join([b[0:4].hex(), b[4:6].hex(), b[6:8].hex(), b[8:10].hex(), b[10:16].hex()]))
```
