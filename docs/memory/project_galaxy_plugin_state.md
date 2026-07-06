---
name: Galaxy plugin — current state (2026-05-08)
description: Where Galaxy plugin development is at. Live MVP on VPS with compact solar system, 9 planet worlds, RP with no-fog shader, 5 named version tags. Custom planet-terrain engine + dragon guard landed in v5.
type: project
originSessionId: ce3f8e16-efa4-45f6-8cd3-9717028d97c0
---
> ⚠️ **ЧАСТИЧНО УСТАРЕЛО** (снапшот от 2026-05-08 с неточностями): кастомный биом `galaxy:space` откачен в пользу ванильного `THE_END` ещё 2026-05-06; листенер `SpaceMobGuard` удалён (заменён на DragonGuard + gamerules); миров 12, не 9 (добавь `planet_earth_nether`/`planet_earth_the_end` и `planet_mars` пересоздан 2026-05-08 под MarsTerrainGenerator). Актуальное состояние — `docs/HISTORY.md`, раздел 4.

**Repo:** github.com/bkarimzhan/galaxy-plugin (HTTPS, push via PAT in URL then scrub).

**Tag scheme (user-driven):** annotated tags `v<n>-<slug>` with Russian titles. User names "good states" by message — I run `git tag -a vN-slug -m "Russian Title\n\n<details>" <commit-hash>` and push. Existing tags:
- `v1-solar-system` — wide orbits
- `v2-compact-solar-system` — все в 150-block END battle radius
- `v3-moons` — спутники планет
- `v4-gravity-empty-planets` — реальная гравитация + покой нелетающих миров
- `v5-planet-terrain` — кастомный рельеф планет (TerrainPlanetGenerator + structure_set отключение + DragonGuard)

**Current architecture (head ~`61c5552`):**
- 9 worlds: `space` (THE_END env, custom void chunkgen, custom biome `galaxy:space` via datapack, fog_color=0), `planet_earth` (vanilla NORMAL, has spawn-platform at 0,80,0), `planet_sun` (lava ocean), `planet_jupiter`/`planet_saturn` (FlatPlanetGenerator gas-giant deck), `planet_mercury`/`venus`/`mars`/`uranus`/`neptune` (NORMAL world type with `SingleBiomeProvider` forcing themed biomes — STONY_PEAKS, WINDSWEPT_HILLS, ERODED_BADLANDS, FROZEN_PEAKS, FROZEN_OCEAN respectively).
- Compact solar system layout (planets.yml): all centers within 150-block radius from (0,100,0); Sun r=18, planets r=3..8 at varied orbital angles.
- Per-tick listeners: `ProximityTask`, `MoonOrbitTask` (10 BlockDisplay moons), `StarParticleTask` (END_ROD particles, force=true), `SightTask` (action-bar planet labels via raytrace), `EndStructureCleanup` (wipes regen'd obsidian platform + exit portal pillars).
- Per-event listeners: `ShipController`, `SpawnListener` (rescue → planet_earth platform), `NightVisionListener` (infinite night-vision in space), `GravityListener` (Attribute.GRAVITY scaled to real values per world), `SpaceMobGuard` (no dragon).
- Commands: /space /ship /unship /leaveplanet /goto /planets.

**Resource pack:** repo `github.com/bkarimzhan/galaxy-resourcepack` → github pages → server pushes via `resource-pack=` in server.properties. Currently has `assets/minecraft/shaders/include/fog.glsl` no-op override killing distance fog globally. SHA1 `be0e3d50...`.

**Last closed issue (2026-05-08):** terrestrial-planet flatness — fixed by v5 TerrainPlanetGenerator. v5 jar deployed to VPS 2026-05-06 20:30, paper running 45h+ with active playtests across mars/uranus/mercury/neptune — no errors, user confirmed.

**Server-side state on VPS:**
- `data/space/`, `data/planet_<name>/` worlds (created by plugin)
- `data/planet_earth/datapacks/galaxy/` — biome+fog datapack (auto-installed by plugin)
- itzg compose has `LEVEL: planet_earth` so spawn world is the spawn-platform world
- Backups dir cleaned, only `pre-galaxy-deploy-20260505` retained

**Tokens:** GitHub PAT used in this conversation only. Not stored anywhere on disk.
