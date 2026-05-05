# Galaxy plugin — Paper plugin для Minecraft 1.21.8

Цель: кастомный плагин, добавляющий механику «Солнечная система»: planet-миры + космос-void + entity-корабль для перелётов.

## Текущая задача

**Stage 0 — скелет проекта.** Создать build.gradle.kts / plugin.yml / пустой `GalaxyPlugin.java` так, чтобы `gradle shadowJar` выдавал валидный jar. Кода механики ещё нет, проверяем что инфраструктура работает.

Файл-структура (запланирована):

```
/home/mc/galaxy-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .gitignore
├── README.md
└── src/main/
    ├── java/com/galaxy/plugin/
    │   ├── GalaxyPlugin.java   ← onEnable / onDisable
    │   ├── config/             ← PlanetsConfig (Stage 2)
    │   ├── world/              ← SpaceChunkGenerator, DesertChunkGenerator
    │   ├── planets/            ← Planet record, PlanetManager, SphereBuilder
    │   ├── teleport/           ← TeleportService
    │   ├── ship/               ← ShipController
    │   ├── listeners/          ← ProximityTask (BukkitRunnable, не PlayerMoveEvent!)
    │   └── commands/           ← /space /ship /leaveplanet
    └── resources/
        ├── plugin.yml
        └── planets.yml         ← конфиг планет
```

## Дизайн механики (актуальный, 2026-05-06)

### Поток игрока

1. **Спавн на `planet_earth`** — ванильный normal overworld, **worldborder 1000** (от −500 до +500). Тут добываются ресурсы и зарабатываются Кредиты.
2. **Покупка корабля за кредиты** (в MVP — `/ship` без покупки, экономика отложена). Корабль = entity, не блочная конструкция.
3. **Подъём на корабле** — игрок летит вверх. При **Y > 300** в любом `planet_*` мире → телепорт в `space` рядом с мини-сферой соответствующей планеты.
4. **В `space`** — void-мир, в центре звезда (beacon на iron-пирамиде в (0, 100, 0)), вокруг на орбитах **мини-сферы планет** (декоративные блочные шары радиус 15-30, не настоящие миры).
5. **Подлёт к мини-сфере** на дистанцию `radius + 3` → телепорт в реальный планетный мир (`planet_<name>`) на surface spawn.
6. **С планеты обратно** — снова летим вверх, Y > 300 → возврат в `space` около мини-сферы.
7. `/leaveplanet` — fallback для застрявших игроков (без корабля).

### Миры MVP

| Мир | Тип | Worldborder | Назначение |
|---|---|---|---|
| `planet_earth` | vanilla normal overworld | 1000 | Спавн новых игроков, базовый сурв, экономика. Только ванильные руды. |
| `space` | void (custom ChunkGenerator) | 5000+ | Звезда + мини-сферы. Транзит между планетами. |
| `planet_mars` | flat-desert (custom ChunkGenerator) | 1000 | Пустыня, red_sand. |

В MVP — 2 мини-сферы в `space`: `mini_earth` (зелёная, ведёт в `planet_earth`) и `mini_mars` (красная, ведёт в `planet_mars`).

### Корабль (entity-based)

- **Невидимая osedланная свинья** (`Pig`) с `setGravity(false)`, помечена через PersistentDataContainer.
- Опциональный визуал: `BlockDisplay` с iron_block над свиньёй (placeholder, потом кастомная модель в RP).
- Tick-задача читает `rider.getLocation().getDirection()` + jump/sneak (через PlayerInputEvent в Paper API) → задаёт velocity свиньи.
- `/ship` спавнит корабль перед игроком, садит игрока.

### Future (НЕ MVP)

- Покупка корабля за кредиты через EconomyShopGUI.
- Кастомные модели в Resource Pack.
- **Блочные корабли** для перевозки команды и груза (Movecraft-стиль).
- **Galaxy jumps** — переход из одной void-вселенной в другую (`galaxy_<name>`), каждая со своим набором планет.
- Программируемые Smart-items (long-term roadmap).

### Архитектурные решения

- **Build system:** Gradle Kotlin DSL.
- **Java:** 21 (Corretto на dev, OpenJDK 21.0.10 на VPS).
- **Paper API:** `io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT`.
- **Shadow plugin:** `com.gradleup.shadow` 8.3.5.
- **Plugin descriptor:** `plugin.yml` (не `paper-plugin.yml`, проще для MVP).
- **Proximity check** — `BukkitScheduler.runTaskTimer` каждые 5 тиков, **НЕ `PlayerMoveEvent`**.
- **Управление мирами** — плагин владеет своими мирами через `Bukkit.createWorld(WorldCreator)`, без Multiverse.
- **Идемпотентность сфер** — флаг-файл в `plugin data folder`, чтобы не пересоздавать.
- **Все строки** в конфигах, не хардкодить.
- **Логирование** через `plugin.getLogger()`.

## Сборка и деплой

```bash
cd /home/mc/galaxy-plugin
gradle shadowJar
# выходной jar: build/libs/Galaxy-0.1.0.jar
cp build/libs/Galaxy-0.1.0.jar /home/mc/minecraft/data/plugins/
docker restart paper
```

## Сервер: доступ и команды

- **Хост:** `62.238.21.160` (Hetzner, Ubuntu 24.04). Этот файл уже на VPS.
- **Paper:** `itzg/minecraft-server` docker container, имя `paper`.
- **RCON:** `docker exec paper rcon-cli "<команда>"`.
- **Логи:** `docker logs paper --tail 100 -f`.
- **Конфиги/данные:** `/home/mc/minecraft/data/`.
- **server.properties** регенерируется из docker-compose env при каждом старте → менять через переменные окружения в `docker-compose.yml`, не sed-ом.

## Аутентификация и игроки (без изменений с MVP1)

`online-mode=false` + `white-list=true` + AuthMe + FastLogin.

| Ник | Тип | Auth | Op | Пароль |
|---|---|---|---|---|
| `kz_bazelevs` | premium (Mojang) | FastLogin → sessionserver | level 4 | — (Mojang token) |
| `Leekuan16` | cracked | AuthMe `/login <pwd>` | level 4 | `G@laxy2026!Cosmos` |

**Добавление новых игроков — только через RCON:**

```bash
# cracked:
docker exec paper rcon-cli "authme register <ник> <пароль>"
docker exec paper rcon-cli "whitelist add <ник>"
docker exec paper rcon-cli "op <ник>"

# premium:
docker exec paper rcon-cli "whitelist add <ник>"
# op-ить ТОЛЬКО после первого входа (иначе offline-UUID запишется в ops.json)
```

## Стек плагинов на сервере (live)

AuthMe 5.7.0, Essentials 2.21.2, EssentialsChat 2.21.2, FastAsyncWorldEdit 2.15.0, FastLogin 1.12-SNAPSHOT, LuckPerms 5.5.42, Multiverse-Core 5.6.1, ProtocolLib 5.4.0, Vault 1.7.3, WorldGuard 7.0.14, EconomyShopGUI 7.0.3, DeluxeMenus 1.14.1, PlaceholderAPI 2.11.7.

**Удалить перед деплоем плагина:** Multiverse-NetherPortals (новый дизайн без nether-связок).

## Server-side миграция (когда плагин готов)

1. Backup `/home/mc/minecraft/` в датированный tarball в `/home/mc/minecraft/backups/`.
2. Stop paper container.
3. Удалить старые миры из `data/`: `earth`, `earth_nether`, `earth_the_end`, `mars`, `mars2`, `test_mars`, `mars_nether`, `world`, `world_nether`, `world_the_end`.
4. Удалить плагин Multiverse-NetherPortals + его данные.
5. Очистить ссылки на старые миры в конфигах: `EconomyShopGUI/shops.yml`, `DeluxeMenus/gui_menus/galaxy_map.yml`, `WorldGuard/worlds/<old>/`, MV `worlds.yml`.
6. Положить `Galaxy-X.Y.jar` в `data/plugins/`.
7. Start paper. Плагин создаёт `space`, `planet_earth`, `planet_mars` на первом загрузе.

## Lessons learned (важное, не повторять)

### SSH polling и fail2ban
Никогда не polling SSH чаще раза в 10 секунд — fail2ban забанит. `maxretry=6`, `bantime=10m` уже настроено в `/etc/fail2ban/jail.d/custom.conf`. Админский IP `45.130.7.107` в whitelist.

### Drop-in конфиги
Все правки sshd / fail2ban / sysctl — в `*.d/*.conf` файлах, не в основных конфигах (затираются при apt-update).

### Backup discipline
Перед любой state-changing операцией — `tar -czf /home/mc/minecraft/backups/<dated>.tar.gz` или `cp <file> /home/mc/minecraft/backups/<dated>.<ext>`.

### itzg compose env
`server.properties` перезаписывается из docker-compose env на каждом рестарте → правки sed-ом затираются. Используй `VIEW_DISTANCE`, `MEMORY`, `DIFFICULTY` env vars и `docker compose up -d --force-recreate`.

### Pending cleanup
Временный NOPASSWD sudo для `mc` в `/etc/sudoers.d/mc-nopasswd-temp` нужно вернуть на password-sudo до production-ready.
