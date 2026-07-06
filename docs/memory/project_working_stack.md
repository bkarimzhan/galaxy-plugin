---
name: Verified working plugin stack (2026-05-03)
description: Specific component versions confirmed working together on Paper for the mixed-mode setup
type: project
originSessionId: 304ff0c2-6267-435e-89d5-30c101acfe0c
---
**Verified working as of 2026-05-03** (Leekuan16 + fartit25 cracked logins tested live; kz_bazelevs premium login deferred but architecture intact):

| Component | Version | Source |
|---|---|---|
| Paper | **1.21.8** (build 60) | itzg image auto-downloads from PaperMC |
| Java runtime | 21 (Temurin) | itzg/minecraft-server:java21 |
| AuthMe Reloaded | **5.7.0** (b2660) | github.com/AuthMe/AuthMeReloaded/releases/5.7.0 |
| FastLogin (Bukkit) | **1.12-SNAPSHOT-65a379c** | github.com/TuxCoding/FastLogin tag `1.12-kick-toggle` |
| ProtocolLib | **5.4.0** (stable) | github.com/dmulloy2/ProtocolLib/releases/5.4.0 |
| LuckPerms (Bukkit) | **5.5.42** | download.luckperms.net |
| Vault | **1.7.3-b131** (2020 stable) | github.com/MilkBowl/Vault |
| FastAsyncWorldEdit (Paper) | **2.15.0+59372c5** | github.com/IntellectualSites/FastAsyncWorldEdit |
| EssentialsX core + Chat | **2.21.2** | github.com/EssentialsX/Essentials (officially supports Paper 1.21.8) |
| Multiverse-Core | **5.6.1** | github.com/Multiverse/Multiverse-Core |
| Citizens | **NOT INSTALLED** | build 4171 incompat with 1.21.8 (built for newer Paper). Need older Jenkins build |

**Why this exact stack:** ProtocolLib `dev-build` requires Java 25 (class file 69), our Java 21 supports up to class file 65. Stable 5.4.0 only supports Paper 1.21.4–1.21.8 officially, so Paper was downgraded from 1.21.11 → 1.21.8 to match. FastLogin needs ProtocolLib for direct (no-proxy) connection servers. AuthMe 5.7.0 is the latest stable, works fine on 1.21.8.

**Critical confirmation in logs**: `[FastLogin] Hooking into auth plugin: AuthMeHook` — this line means FastLogin successfully integrated with AuthMe and will route premium players through Mojang auth while letting cracked players hit AuthMe.

**How to apply:** When upgrading Paper, recheck ProtocolLib official-support matrix. If wanting newer Paper (1.21.9+), need either ProtocolLib dev-build (requires Java 25 → upgrade Java in compose) or wait for new ProtocolLib stable. Don't blindly bump versions.

**Pending follow-ups** (not blockers, but tracked):
- Temporary NOPASSWD sudo `/etc/sudoers.d/mc-nopasswd-temp` still active — to be removed once setup phase is fully done

**Resolution as of 2026-05-03 (later in same session):** Both prior open decisions resolved.

1. **FastLogin `secondAttemptCracked`** — kept `false` (max protection). Resolved by removing `fartit25` from the roster entirely, eliminating the cracked-fallback trade-off.

2. **Citizens** — deferred to MVP3. Incompat jar parked in `backups/`, decision documented in `CLAUDE.md` under "Отложенные решения". Will reassess with fresher Jenkins build or alternatives (ZNPCsPlus, FancyNPCs, CitizensReimagined).

MVP1 auth architecture frozen — see `project_mixed_server_plan` memory and `CLAUDE.md` "Финальная архитектура аутентификации (MVP1)" for canonical state.
