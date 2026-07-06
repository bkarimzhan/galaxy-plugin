---
name: User names good states as Russian git tags
description: When the user says "коммит <Russian Name>" or "назови коммит <name>", create an annotated git tag `vN-slug` with that title on the current head and push to remote.
type: feedback
originSessionId: ce3f8e16-efa4-45f6-8cd3-9717028d97c0
---
User periodically says things like "коммит Спутники" or "Назови этот коммит «Компактная солнечная система»" — these mean: tag the latest relevant commit so we can roll back to it.

**How to apply:**
1. Pick the commit hash that delivered the named feature (usually HEAD or last feature commit).
2. `git tag -a vN-slug -m "<Russian Title>\n\n<short details>" <hash>` — increment N from existing tags.
3. Push the tag with the temp-token-in-URL pattern: `git push https://<TOKEN>@... vN-slug`.
4. Reply with the tag name and the rollback command `git reset --hard vN-slug`.

**Why:** the tags are how the user navigates "good states" of the project — they want to be able to return to any named version if a later change ruins something. Don't skip tagging when they ask, even if HEAD is identical to a previous commit (just point the tag).

Slug naming: ASCII, kebab-case, English approximation of the Russian title. So far: v1-solar-system, v2-compact-solar-system, v3-moons, v4-gravity-empty-planets.
