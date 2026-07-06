# Galaxy Plugin

Paper-плагин для Minecraft 1.21.8: механика «Солнечная система» — 9 миров-планет с кастомным рельефом и гравитацией, void-космос с мини-сферами планет и спутниками, entity-корабль для перелётов между мирами.

**Статус:** MVP реализован и обкатан на живом сервере (май 2026). Проект архивирован 2026-07-06 перед сносом VPS — всё нужное для продолжения лежит в этом репозитории.

## С чего начать

| Документ | Что внутри |
|---|---|
| [docs/HISTORY.md](docs/HISTORY.md) | История разработки: решения, грабли, состояние, открытые задачи |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Актуальная архитектура плагина по коду |
| [docs/SERVER-SETUP.md](docs/SERVER-SETUP.md) | Воссоздание сервера с нуля на новом VPS |
| [docs/SESSION-DIGESTS.md](docs/SESSION-DIGESTS.md) | Дайджесты всех сессий разработки с Claude |
| [docs/memory/](docs/memory/) | Файлы памяти агента: конденсированные решения и справки |
| [.claude/skills/](.claude/skills/) | Скиллы Claude Code (рабочие процессы: деплой, rcon, планеты, бэкапы) |
| [CLAUDE.md](CLAUDE.md) | Контекст для Claude Code при работе в репозитории |

## Сборка

```bash
gradle shadowJar   # → build/libs/Galaxy-0.1.0.jar (Java 21)
```

Деплой: положить jar в `plugins/` Paper-сервера 1.21.8 и перезапустить. Плагин сам создаёт миры (`space`, `planet_*`), строит мини-сферы и устанавливает датапак.

## Связанные репозитории

- [galaxy-resourcepack](https://github.com/bkarimzhan/galaxy-resourcepack) — ресурспак (no-fog шейдер) на GitHub Pages.
