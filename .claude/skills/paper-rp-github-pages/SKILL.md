---
name: paper-rp-github-pages
description: Update a Minecraft Resource Pack hosted on GitHub Pages. Handles repackaging pack.zip, computing SHA-1, GitHub authentication via PAT in URL (with cleanup), polling Pages CDN until updated, and syncing server.properties + restart.
---

# Resource Pack Update via GitHub Pages

## When to use

User wants to update visuals/textures/sounds in the server's Resource Pack. Workflow assumes RP repo at `<user>/galaxy-resourcepack` (or similar) hosted on GitHub Pages, with `pack.zip` at repo root, source files in `src/`.

Triggers: "update RP", "обновить ресурспак", "push pack", new texture added.

## Workflow

### 1. Edit source files locally

Source files are in `src/`. Common edits:
- `src/pack.mcmeta` (description, pack_format)
- `src/assets/minecraft/textures/...` (block/item textures)
- `src/assets/minecraft/lang/<locale>.json` (translations including item rename)
- `src/assets/minecraft/sounds/...`
- Skybox PNGs

### 2. Repackage to `pack.zip` at repo root

```powershell
# Windows PowerShell
Set-Location <repo-path>
Compress-Archive -Path "src\*" -DestinationPath "pack.zip" -Force
```

```bash
# Linux/macOS
cd <repo-path>/src
zip -r ../pack.zip * && cd ..
```

`pack.zip` MUST have `pack.mcmeta` at the ROOT (not nested in a subdirectory). Verify: `unzip -l pack.zip | head`.

### 3. Compute SHA-1

```powershell
(Get-FileHash pack.zip -Algorithm SHA1).Hash.ToLower()
```
```bash
sha1sum pack.zip
```

Save the value — it goes into `server.properties`.

### 4. Git commit + push (with token handling)

GitHub blocks password auth. Use a Personal Access Token (PAT) with `repo` scope. Token is sensitive — **NEVER write to project files, .git/config, or memory**. Use only inline.

Pattern: temporarily set token in remote URL, push, then immediately clean URL.

```powershell
git add src/ pack.zip
git commit -m "<description>"
git remote set-url origin "https://oauth2:<TOKEN>@github.com/<user>/<repo>.git"
git push origin main
git remote set-url origin "https://github.com/<user>/<repo>.git"   # immediately clean
```

Verify cleanup:
```powershell
git remote -v
# expected: https://github.com/<user>/<repo>.git (no token)
```

Token will appear in:
- Tool input transcript (acceptable — ephemeral session log)
- PowerShell readline history (low risk — local machine, expires in 90d)

Token must NOT appear in:
- `.git/config` (cleaned via second `set-url`)
- Any project file (CLAUDE.md, memory, .env)
- Commit messages

### 5. Poll GitHub Pages until CDN serves new SHA

Pages typically deploys in 5-30 sec, but CDN cache can take ~10 min worst case.

```powershell
$expected = "<NEW_SHA1>"
$url = "https://<user>.github.io/<repo>/pack.zip"
$tmp = "verify.zip"
for ($i = 1; $i -le 25; $i++) {
  Start-Sleep -Seconds 4
  Invoke-WebRequest -Uri $url -OutFile $tmp -UseBasicParsing
  $actual = (Get-FileHash $tmp -Algorithm SHA1).Hash.ToLower()
  if ($actual -eq $expected) { Write-Output "Updated on attempt ${i}"; break }
  Write-Output "  attempt ${i} stale sha=${actual}"
}
Remove-Item $tmp
```

Note: PowerShell variable interpolation breaks with `$i:` (parsed as drive). Use `${i}` in strings.

### 6. Sync server.properties on VPS

```bash
ssh mc@host "set -e
cd /home/mc/minecraft
sed -i 's|^resource-pack=.*|resource-pack=https://<user>.github.io/<repo>/pack.zip|' data/server.properties
sed -i 's|^resource-pack-sha1=.*|resource-pack-sha1=<NEW_SHA1>|' data/server.properties
docker compose restart paper"
```

For `resource-pack-prompt`, must be a JSON text component (not plain text):
```
resource-pack-prompt={"text":"...","color":"yellow"}
```

Plain text causes `JsonSyntaxException: malformed JSON at line 1 column 1` at boot (non-fatal but noisy).

### 7. Verify clean boot

```bash
ssh mc@host "for i in 1..12; do sleep 5; docker logs paper --tail 60 | grep -q 'Done (' && break; done
docker logs paper --since 90s 2>&1 | grep -iE 'severe|gson|jsonsyntax' | head -10"
```

Expected warnings (safe to ignore):
- `resource-pack-id missing, using default of <UUID>` — Paper auto-gens
- `Could not download GeoLiteAPI database` — AuthMe MaxMind, unrelated

## .gitignore must include token-leak protections

```
.env
.env.*
*.secret
*.token
*.key
*.pem
.netrc
auth.json
credentials.json
secrets.*
```

## Gotchas

- **`require-resource-pack=true`** kicks players who decline. Set `false` if pack is empty/optional. Set `true` only when content is mandatory for visual fidelity.
- **CDN cache**: after `git push`, Pages serves OLD SHA for ~10 min worst case. If you update server.properties immediately with new SHA, clients fail to download (SHA mismatch). Either wait, or accept brief mismatch window.
- **`pack_format`** for resource pack is DIFFERENT from datapack `pack_format`. RP for 1.21.8 uses `pack_format: 55`. Datapack uses `pack_format: 81`. Don't confuse.
- **`supported_formats`** field can be a range `[55, 99]` to support multiple MC versions in one pack — useful for forward-compat.
- **First push to a new repo** uses different flow (auto_init=true, GitHub Pages enable via API). See related skill `paper-rp-github-pages-init` if creating a fresh RP repo.
