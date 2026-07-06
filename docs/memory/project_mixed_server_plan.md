---
name: Final auth architecture (MVP1, post-fartit-removal)
description: MVP1 final auth state — only kz_bazelevs (premium) and Leekuan16 (cracked) remain; fartit25 fully removed 2026-05-03
type: project
originSessionId: 304ff0c2-6267-435e-89d5-30c101acfe0c
---
**As of 2026-05-03 (updated)**: roster reduced from 3 to 2 players. `fartit25` fully removed (deop'd, whitelist remove, authme unregister). MVP1 architecture frozen.

**Final admin roster (canonical):**
- `Leekuan16` — cracked, AuthMe `/login <AUTHME_PASSWORD>`, op level 4
- `kz_bazelevs` — premium (Mojang), FastLogin auto-login via Mojang sessionserver, op level 4

**Stack unchanged:** `online_mode=false` + AuthMe Reloaded + FastLogin (`secondAttemptCracked: false`, max protection for premium nick).

**Why fartit25 removed:** simplifies architecture and maximizes security for premium nick `kz_bazelevs`. Bonus: `fartit25` is a real Mojang account name owned by someone else — keeping it cracked-side risked impersonation games. Removal sidesteps the whole `secondAttemptCracked` trade-off.

**AuthMe table contents (expected, do NOT treat kz_bazelevs entry as a bug):**
- `leekuan16` — real registration (cracked admin password)
- `kz_bazelevs` — auto-registered by FastLogin via `forceRegister` API on first premium login, with random password. This bypasses `registration.enabled=false`. Safety entry, by design.

**How to apply:** When asked to add players, NEVER do it through the game — use RCON only. Cracked: `authme register <name> <pass>` then `whitelist add` then optionally `op`. Premium: `whitelist add` first, login first, THEN `op` (op'ing before first login records offline UUID and breaks premium login). When removing players, use the 3-step RCON sequence: deop, whitelist remove, authme unregister.

**Reference**: full architecture documented in `D:\projects\minecraft\CLAUDE.md` under "Финальная архитектура аутентификации (MVP1)".
