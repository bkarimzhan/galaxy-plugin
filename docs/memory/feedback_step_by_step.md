---
name: Step-by-step execution with confirmation
description: User wants one command at a time with explanation before and confirmation between
type: feedback
originSessionId: 304ff0c2-6267-435e-89d5-30c101acfe0c
---
For server setup / sysadmin work, run **one command at a time**. Explain what the command does and why **before** executing, then wait for explicit confirmation. After each command, show the output and interpret the result before proposing the next step.

**Why:** User explicitly requested this for VPS hardening work — they want visibility and control over each change, especially for security-sensitive operations like SSH config changes and firewall rules where a mistake can lock them out.

**How to apply:** Applies to multi-step infrastructure/admin tasks. Don't batch independent commands "for efficiency" in this context. For pure read-only diagnostic batches it's OK to combine into one informational SSH call (as we did with the initial system survey), but for any state-changing command, it's strictly one at a time.

**Update 2026-05-04 (MVP2 planning):** For larger feature work like MVP2, the "next" gating extends to whole **stages** (not just commands). User estimates work in **hours**, not weeks/days. They are co-developing alongside, working at their own pace. Plan output: name stages + hour estimates, present overview, then user says `next` to advance to the next stage. Within a stage, the one-command-at-a-time rule still applies for state-changing operations.
