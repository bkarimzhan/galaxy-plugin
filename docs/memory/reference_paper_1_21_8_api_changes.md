---
name: Paper 1.21.8 API surface changes
description: Non-obvious package/symbol moves in Paper 1.21.8 paper-api versus older docs/tutorials. Verified by inspecting paper-api jar with javap on 2026-05-05.
type: reference
originSessionId: ce3f8e16-efa4-45f6-8cd3-9717028d97c0
---
When writing Paper plugins for 1.21.8, these symbols are NOT where older tutorials/AI suggestions place them:

- **`PlayerInputEvent`** — now lives in `org.bukkit.event.player.PlayerInputEvent` (was previously `io.papermc.paper.event.player.PlayerInputEvent` in earlier Paper). Its payload is `org.bukkit.Input` with simple boolean accessors: `isForward/isBackward/isLeft/isRight/isJump/isSneak/isSprint`.
- **`Attribute.SCALE`** — used to be `Attribute.GENERIC_SCALE`. Same for any attribute previously prefixed `GENERIC_`. The whole prefix was dropped in Paper 1.21.x — `MAX_HEALTH`, `MOVEMENT_SPEED`, `ATTACK_DAMAGE`, etc.
- **`World.setKeepSpawnInMemory(boolean)`** is deprecated. For void/utility worlds just don't call it; for keeping a region loaded use chunk tickets instead.

Verify by `find ~/.gradle/caches/modules-2 -name 'paper-api-*.jar' | xargs -I{} javap -p -cp {} <FQN>` before guessing FQNs.
