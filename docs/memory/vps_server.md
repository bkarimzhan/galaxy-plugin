---
name: VPS server (Hetzner)
description: Hardware, access, and dev environment for the Galaxy plugin server VPS. Live state.
type: project
---

Hetzner VPS hosting the Paper Minecraft server + Galaxy plugin development environment.

**Access:**
- SSH: `ssh mc@62.238.21.160` (key-based only, no password)
- Root login disabled, password auth disabled (sshd hardened via `/etc/ssh/sshd_config.d/99-hardening.conf`)
- Sudo for `mc` is currently NOPASSWD (`/etc/sudoers.d/mc-nopasswd-temp`) — see `project_pending_cleanup.md`, must be reverted before production-ready

**Hardware:**
- Hostname: `minecraft`
- OS: Ubuntu 24.04 LTS, kernel 6.8.x
- CPU: 4 vCPU AMD EPYC-Genoa @ 2.0GHz
- RAM: 7.6 GiB + 2 GB swap (`/swapfile`, `vm.swappiness=10` via `/etc/sysctl.d/99-swappiness.conf`)
- Disk: 150 GB ext4 root

**Network/security:**
- ufw: default deny incoming, allows `22/tcp` + `25565/tcp` (v4+v6)
- fail2ban: `sshd` jail (maxretry=6, findtime=10m, bantime=10m) + `recidive` jail (maxretry=3, findtime=1d, bantime=1w). Whitelist: admin IP `<ADMIN_IP>` in `ignoreip`. All config in single drop-in `/etc/fail2ban/jail.d/custom.conf`
- If locked out: Hetzner Cloud Console → Console (web VNC) → `sudo fail2ban-client unban <IP>`

**Java heap budget:** 5–6 GB safely available for Paper. Currently `MEMORY: "5G"` in `docker-compose.yml`.

**Dev environment installed (for Galaxy plugin development on VPS):**
- Node.js 20.20.2 + npm 10.8.2 (for Claude Code)
- Claude Code 2.1.128 (`/usr/bin/claude`)
- OpenJDK 21.0.10 (Ubuntu)
- Gradle 8.10 via SDKMAN (`source ~/.sdkman/bin/sdkman-init.sh`)
- zip, unzip

**RAM caution during plugin builds:**
Paper Xmx=5G + Claude+Node ~300MB + gradle build ~1G ≈ tight. During `gradle shadowJar` consider stopping paper to avoid GC pressure: `docker stop paper`, build, `docker start paper`. Or build in Docker (`gradle:jdk21` image) without host JDK contention.

**How to apply:** when sizing JVM, planning ports, or running builds — anchor to these specs. Always SSH as `mc`, never root. Memory edits/scp use this host.
