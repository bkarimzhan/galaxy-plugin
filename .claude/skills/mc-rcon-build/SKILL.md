---
name: mc-rcon-build
description: Build structures, place blocks, signs, buttons, command_blocks on a Paper Minecraft server via rcon-cli (no player needed). Handles /fill 32768 limit, NBT quoting hell for signs with Cyrillic, force-load patterns, and block-state syntax for wall buttons/signs.
---

# Building via RCON on Paper

## When to use

Constructing things in-game from a console/SSH context — no player required. For Paper server using itzg's docker image with rcon enabled.

Triggers: "build platform/spawn/cosmodrome", "place sign/button", "/fill", "/setblock", "paint area".

## Core workflow pattern

```bash
ssh mc@host 'bash -s' << 'OUTER'
docker exec paper rcon-cli 'execute in minecraft:<world> run forceload add <x1> <z1> <x2> <z2>' >/dev/null
sleep 1

cat > /tmp/build.commands << 'CMDEND'
execute in minecraft:<world> run fill <x1> <y1> <z1> <x2> <y2> <z2> minecraft:<block>
execute in minecraft:<world> run setblock <x> <y> <z> minecraft:<block>[<state>]{<NBT>}
...
execute in minecraft:<world> run forceload remove all
CMDEND

while IFS= read -r line; do
  [ -z "$line" ] && continue
  out=$(docker exec paper rcon-cli "$line" 2>&1 | sed 's/\x1b\[[0-9;]*[a-zA-Z]//g' | head -1)
  short=$(echo "$line" | head -c 80)
  echo ">> ${short}..."
  echo "   ${out}"
done < /tmp/build.commands
OUTER
```

Why this pattern:
- Single-quoted heredoc `<< 'OUTER'` and `<< 'CMDEND'` — bash doesn't expand `$`, no escape hell
- Lines fed individually to rcon-cli, each is one self-contained MC command
- Cyrillic UTF-8 passes through SSH→bash→rcon→Paper transparently
- One rcon call per command — readable output, easy to debug failures

## Critical gotchas

### 1. `/fill` 32768 block limit

`/fill` fails with `Too many blocks in the specified area` if (x2-x1+1) × (y2-y1+1) × (z2-z1+1) > 32768. Split large fills.

A 41×41×30 air-clear (50,430) won't fit. Split by Z or Y:
```
/fill X1 Y1 Z1 X2 Y2 Z_mid <block>      # half 1
/fill X1 Y1 Z_mid+1 X2 Y2 Z2 <block>    # half 2
```

A 200×200 single-layer area = 40,000 → split into 2× 200×100 (20,000 each).

### 2. Force-load chunks before far-from-spawn ops

`/fill` and `/setblock` outside the loaded chunk radius can silently fail or leave partial state. Always:
```
execute in minecraft:<world> run forceload add <x1> <z1> <x2> <z2>
sleep 1
... do work ...
execute in minecraft:<world> run forceload remove all
```

`/forceload add` accepts BLOCK coords (auto-converts to chunk pairs). Block ranges over multi-chunk: it loads the whole rectangle of chunks.

### 3. Wall block-state direction is OPPOSITE of `facing`

For `stone_button[face=wall,facing=<dir>]` and `oak_wall_sign[facing=<dir>]`:
- `facing` = direction the BLOCK FACE POINTS (where player presses or reads from)
- The wall block it's ATTACHED to is in the OPPOSITE direction

Example: button on the SOUTH wall of a booth, visible from inside (north of wall):
- Button position: 1 block north of wall (in interior air)
- `facing=north` (button points north, into interior)
- Wall block is at position+south (= the actual south wall)

This trips up everyone. Verify with `data get block <pos> attached_to` if available, or test by interacting.

### 4. Sign NBT format (1.20+) for Cyrillic text

```
setblock X Y Z minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Линия 1"}','{"text":"Линия 2"}','{"text":""}','{"text":""}']}}
```

- Standing sign: `oak_sign[rotation=0..15]`. 0=south, 4=west, 8=north, 12=east. The number indicates which way the SIGN'S FRONT FACES.
- Wall sign: `oak_wall_sign[facing=<dir>]`. Same opposite-of-wall rule.
- Messages array MUST have exactly 4 entries (sign has 4 lines). Use `'{"text":""}'` for blank lines.
- Each message is a JSON text component string, single-quoted in NBT, with double-quoted JSON keys/values inside.
- Add color: `'{"text":"...","color":"yellow"}'`.

### 5. Block verification probes

`data get block X Y Z` only works for BLOCK ENTITIES (chests, signs, command_blocks). For regular blocks (stone, dirt, air) it returns "The target block is not a block entity".

For probing arbitrary blocks, use `execute if block`:
```
execute if block X Y Z minecraft:<block_id>      → "Test passed"/"Test failed"
execute if block X Y Z #minecraft:<tag>          → tag-based (sand/stone/dirt/etc.)
```

Useful tags: `#minecraft:base_stone_overworld`, `#minecraft:dirt`, `#minecraft:sand`, `#minecraft:logs`, `#minecraft:leaves`, `#minecraft:terracotta`.

### 6. To inspect terrain Y-level (find ground)

Loop Y descending, check first non-air:
```bash
for y in 120 100 90 80 75 70 65 64 63 60; do
  if ! docker exec paper rcon-cli "execute in minecraft:<world> if block X $y Z minecraft:air" 2>&1 | grep -q 'Test passed'; then
    echo "ground at Y=$y"
    break
  fi
done
```

### 7. command_block placement requires `enable-command-block=true`

In `server.properties`. Add via:
```bash
sed -i 's|^enable-command-block=false|enable-command-block=true|' server.properties
```
Then restart Paper.

`/setblock <pos> minecraft:command_block{Command:"<cmd>"}` — the command runs as console-equivalent when redstone-powered. Use `@p[distance=..6]` selector to target nearest player at button.

### 8. Always verify after build

Probe key features at end of build:
```bash
for spec in 'X Y Z:expected_block' '...:...'; do
  IFS=: read pos block <<<"$spec"
  echo -n "  ($pos) $block? "
  docker exec paper rcon-cli "execute in minecraft:<world> if block $pos minecraft:$block" 2>&1
done
```

### 9. Snapshot region files before large builds

Edits to chunks not easily reversible. Before large /fill in known coords:
```bash
docker exec paper rcon-cli 'save-all flush' >/dev/null  # write dirty chunks
sleep 2
cp data/<world>/region/r.X.Y.mca backups/<reason>-$(date -u +%Y%m%d-%H%M%S)/
```

Region files are 512×512 blocks each: `r.X.Y.mca` covers blocks `X*512..X*512+511, Y*512..Y*512+511`. For build at block (13497, 2061), region is r.26.4.

## Color codes in display text

For sign + chat text use `&` codes (auto-converted on signs in newer versions, or `§` directly):
- `§0` black, `§1` blue, `§4` red, `§6` gold, `§7` gray, `§a` green, `§e` yellow
- `§l` bold, `§o` italic, `§n` underline, `§r` reset

In sign JSON, use `"color":"yellow"` etc., not `§` codes.
