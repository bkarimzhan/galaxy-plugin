---
name: Pending cleanup after VPS hardening
description: Items that were temporarily set during initial VPS hardening session and must be reverted before the server is considered production-ready
type: project
originSessionId: 304ff0c2-6267-435e-89d5-30c101acfe0c
---
During the 2026-05-03 initial VPS hardening session for the Minecraft server, the user chose temporary NOPASSWD sudo for `mc` to speed up agent-driven setup. **Decision 2026-05-03:** keep NOPASSWD active across hardening + Docker install + Java install + Paper setup phases since more admin work remains. Revert only once all setup is complete and the server enters steady-state operation.

**Pending action:** Remove `/etc/sudoers.d/mc-nopasswd-temp` to restore password-required sudo for the `mc` user. Verify with `sudo -K && sudo whoami` (should prompt for password). Do this **after** Docker, Java, and Paper are installed/configured — not before.

**Why:** Password sudo is the chosen long-term posture (option B from the A/B/C choice). NOPASSWD was granted to avoid repeated password prompts during a controlled multi-phase setup driven by the agent. Removing it mid-setup would force interactive password entry for every subsequent sudo, which is the friction the user explicitly traded away.

**How to apply:** When the user signals the setup is complete (Paper running, world configured, ops set, etc.), propose `sudo rm /etc/sudoers.d/mc-nopasswd-temp` then verify with `sudo -l -U mc` that only the password-protected `(ALL : ALL) ALL` rule from sudo group membership remains.
