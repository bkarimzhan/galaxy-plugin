---
name: Educational roadmap MVP2-6
description: Java-learning arc for the 9-year-old son via the cosmic-MMO server. Each MVP introduces a new abstraction layer.
type: project
originSessionId: e387786d-2b2d-4842-a8b2-6595bc7dc316
---
**Goal**: 9yo `Leekuan16` learns Java through the family cosmic-MMO. Difficulty rises gradually; each MVP exposes a new "programming surface".

**Roadmap (months are calendar months from MVP2 start = 2026-05):**

| MVP | Months | Educational layer | Examples |
|---|---|---|---|
| MVP2 | 1-3 | Vanilla command blocks + basic `/give`, `/tp`, `/setblock`. No code. Goal: "the world reacts to instructions." | Build a redstone door, write a simple command-block sign |
| MVP3 | 4-6 | Skript (event-based scripting, English-like) + first **Smart Lamp Controller** item (writes/edits a `.java`/`.skript` file via in-game UI) | Lamp turns on at night, off at day; player edits the rule |
| MVP4 | 7-9 | **Smart Farm Bot** + **Mining Drone** programmable items (real Java, sandboxed). Plus **Claude bot "Клавдия"** in chat for hints/debugging | "Bot, plant wheat in this 5x5 plot when sapling grows" |
| MVP5-6 | 10+ | Advanced programmable items: shipboard AI, planet-scanner, automated trading agent. Real OOP + APIs. | Player composes a small class hierarchy for a custom ship subsystem |

**How to apply:** MVP2 research and feature proposals must NOT introduce real coding for the kid yet — command blocks only. Skript and Smart-items are MVP3+. When picking plugins for MVP2, prefer ones that don't lock us out of adding Skript/Smart-items later (e.g., don't pick a shop plugin that requires its own DSL conflicting with Skript).

**Father (`kz_bazelevs`)** is a Middle Java developer — can implement Smart-item infrastructure himself in MVP3-4. The kid is the learner, the dad is the platform.
