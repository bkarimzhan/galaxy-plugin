---
name: Datapack ore-disable must use no-op fallback feature, not raw vanilla refs
description: Overriding placed_feature/<ore>.json with feature reference like minecraft:ore_diamond crashes Paper at boot with "Unbound values in registry"
type: feedback
originSessionId: cd18e1ec-2075-4688-bdd4-e97f7766f3ec
---
**Rule:** when writing a datapack to disable ore generation in 1.21+, do NOT reference vanilla configured_feature names like `minecraft:ore_diamond`, `minecraft:ore_gold_extra`, `minecraft:ore_gold_nether` directly in placed_feature override `feature` field. Some configured_feature names you'd expect (e.g. `minecraft:ore_diamond`) don't actually exist as direct registry entries — vanilla uses `ore_diamond_buried`, `ore_diamond_large`, etc. instead.

**Why:** Paper boots a chunk loader that validates the worldgen registry. Placing a placed_feature override with `feature: minecraft:ore_diamond` makes Paper search for that configured_feature; if not found it logs "Unbound values in registry ResourceKey[minecraft:root / minecraft:worldgen/configured_feature]: [...]" and **fails to load datapacks, blocking server startup**. Confirmed 2026-05-05 — datapack `no_top_ores` crashed Paper boot loop.

**How to apply:** the safe pattern is to ship a custom no-op configured_feature in your own namespace and have all overrides point to it:

```
data/<your_pack>/worldgen/configured_feature/empty.json:
{
  "type": "minecraft:no_op"
}

data/minecraft/worldgen/placed_feature/ore_diamond.json:
{
  "feature": "<your_pack>:empty",
  "placement": []
}
```

Empty placement means it's never placed; `<your_pack>:empty` is guaranteed to resolve because it's in your own pack. Apply same pattern for every placed_feature you want to disable.

**Recovery if it crashes:** `docker compose stop paper`, `rm -rf data/<world>/datapacks/<pack>`, `docker compose start paper`.
