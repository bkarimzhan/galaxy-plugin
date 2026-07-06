---
name: THE_VOID biome fog gotcha in custom END worlds
description: Paper 1.21 applies biome fog_color even inside THE_END dimension — THE_VOID biome paints a pale-blue haze around blocks. Use Biome.THE_END for "space" world chunks instead.
type: feedback
originSessionId: ce3f8e16-efa4-45f6-8cd3-9717028d97c0
---
When writing a custom void/space ChunkGenerator that returns `Biome.THE_VOID`, players in the world will see a pale-blue haze (`#c0d8ff`) around any blocks that are loaded. This happens even in `World.Environment.THE_END` dimensions where you'd expect the dimension's own ambient to dominate.

**Why:** Paper 1.21 / Mojang 1.18+ apply biome `fog_color` from the biome registry to the player's surroundings, regardless of dimension type. THE_VOID's fog_color is pale blue, intended for the natural void area below the world.

**How to apply:** For "space" or "void" custom generators, return `Biome.THE_END` (fog_color `#a080a0`, the standard End ambient purple) — closer to dark/space and consistent with THE_END dimension. Don't reach for THE_VOID just because the world has no terrain.

**Caveat:** biome is baked at chunk generation time. After changing the BiomeProvider in the generator, existing chunks keep the old biome — must regenerate (delete world folder or use `/locate` + `/seed` shenanigans).

Found 2026-05-05 during Galaxy-plugin development; user reported "blue fog appears around the sun, gone when far away" — turned out to be the biome fog rendering against the densely-built sun, not the sun itself.
