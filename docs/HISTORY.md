# История разработки Galaxy Plugin

Архивная выжимка всех сессий разработки с Claude Code (май 2026). Составлена 2026-07-06 перед удалением сервера.

## Как читать этот документ

Это конденсат всех сессий разработки плагина Galaxy: хронология работ, сводка архитектурных решений, накопленные «грабли» и точное состояние проекта на момент архивации. Разделы 3–6 самодостаточны — чтобы продолжить проект, достаточно прочитать их, а хронология (раздел 2) нужна для контекста «почему сделано именно так».

---

## 1. Хронология сессий

Две первые сессии 2026-05-05 (случайный ввод; план Stage 0 — скелет Gradle-проекта) тривиальны и пропущены — их результат целиком вошёл в марафон ниже.

### 2026-05-05 → 05-06 (марафон ~4,5 часа) — Galaxy MVP: полная реализация и живая отладка

За одну сессию с нуля реализован и задеплоен на VPS весь MVP плагина (Paper 1.21.8, Gradle Kotlin DSL + shadow, Java 21), стадии 0–9:

- **Миры**: `space` (THE_END environment, void-генератор, world border 5000), `planet_earth` (vanilla, LEVEL-мир), `planet_sun` (лавовый мир), 8 планет. Конфиг `planets.yml` + `PlanetManager`.
- **Космос**: мини-сферы планет строит `SphereBuilder` (идемпотентно через флаг-файл `.spheres-built`); `ProximityTask` каждые 5 тиков (Y>300 на планете → космос, подлёт к мини-сфере → телепорт на планету); звёзды — частицы END_ROD с `force=true` и равномерным сферическим распределением; NIGHT_VISION игрокам в space.
- **Корабль**: невидимая свинья + BlockDisplay-визуал, управление через `PlayerInputEvent`; слезть только через `/unship`.
- **Команды**: `/space`, `/ship`, `/unship`, `/leaveplanet`, `/goto`, `/planets`.
- **Сервер**: очищен от старых миров (earth/mars и их nether/end) и Multiverse-NetherPortals; `LEVEL=planet_earth` в docker-compose; бэкапы урезаны с 11G до ~100M.

Затем длинная итеративная отладка с игроком (kz_bazelevs) в реальном времени: починен полёт корабля (setAI/седло/strafe/shift — см. «Грабли»), cross-world транзит через despawn+respawn, убит авто-спавнящийся Ender-дракон, зачистка регенерирующихся End-структур (`EndStructureCleanup`), найден источник «голубого тумана» (биом THE_VOID), no-fog шейдер в ресурспаке на GitHub Pages, солнце как лава-мир, 8 планет с текстурами и кольцом Сатурна, вся система сжата в 150-блочный радиус (обход END battle-ambience), 10 спутников (BlockDisplay-орбиты, `MoonOrbitTask`), реальная гравитация через `Attribute.GRAVITY`, рельеф планет через vanilla-генерацию + `SingleBiomeProvider` (кастомные noise-генераторы отвергнуты как «плоские»).

Пользователь ввёл схему именованных тегов: за сессию созданы `v1-solar-system` … `v4-gravity-empty-planets`. Репозиторий `github.com/bkarimzhan/galaxy-plugin`, ~35 коммитов.

### 2026-05-06 (19:11–19:21) — Отключение мобов на всех планетах

- На planet_earth (единственной, где мобы ещё были) по подтверждению пользователя отключены **все** мобы, включая пассивных; `purgeMonsters` → `purgeAllMobs` (чистит всех `Mob`, но пропускает сущности с PDC-ключом `galaxy_ship` — корабль это Pig).
- Обнаружено, что мобы из уже сгенерированных чанков спавнятся при загрузке чанка (белый медведь на Нептуне) — создан листенер `LifelessWorldGuard` (отмена `CreatureSpawnEvent` кроме reason=CUSTOM + чистка на `ChunkLoadEvent`), заменивший точечный `SpaceMobGuard`.
- Два деплоя jar с бэкапами; после первого вычищено 100+ мобов по мирам.

### 2026-05-06, вечер (19:23–20:38) — Безжизненные миры, отключение структур, кастомный рельеф

- `LifelessWorldGuard` заменён набором gamerules (`DO_MOB_SPAWNING`, `DO_PATROL_SPAWNING`, `DO_TRADER_SPAWNING`, `DO_INSOMNIA`, `DISABLE_RAIDS`) + разовый `purgeAllMobs()` при старте.
- Выяснено: `WorldCreator.generateStructures(false)` в Paper 1.21.x игнорируется движком 1.18+ — шахты/деревни отключены датапаком с 20 пустыми override-файлами `structure_set`; `DatapackInstaller` переписан на рекурсивный обход jar.
- Кастомный биом `galaxy:space` рендерился голубым — **откат на ванильный `Biome.THE_END`** (это финальное состояние; мир space пересоздан).
- `TerrainPlanetGenerator` v2 (6-октавный Simplex fBm + ridge + domain warp, без пещер) — Mars/Mercury/Venus/Uranus/Neptune переведены на него; Земля осталась ванильной (пещеры/руды для майнинга).
- Плюс: safe-spawn при телепорте (`getHighestBlockYAt+1`), ночное небо клиенту выше Y=180, Солнце с твёрдой поверхностью MAGMA_BLOCK и source-лавой, `DragonGuard` против Ender-дракона в space.
- Многократные wipe регионов всех миров с бэкапами; коммит `286447e`, push 56 коммитов в GitHub.

### 2026-05-08, session-1 (17:22–17:36) — Диагностика коннекта и тег v5

- Игрок kz_bazelevs не мог зайти: сервер здоров (FastLogin/ProtocolLib работают), клиент дисконнектится до ответа на ENCRYPTION_REQUEST — проблема на стороне клиента (протухший Microsoft-токен / версия ≠ 1.21.8 / VPN).
- Создан и запушен annotated-тег `v5-planet-terrain` («Кастомный рельеф планет») на `286447e`. PAT давал пользователь в чате, после push вычищен из reflog.

### 2026-05-08, session-2 (17:39–18:08) — Проверка деплоя v5 и диагноз «листа Мёбиуса»

- Подтверждено, что v5 задеплоен и работает (jar от 05-06, paper Up 45h, игроки летали без ошибок).
- Новый дефект: рельеф Марса «как лист Мёбиуса» — диагноз: ridge-шум `(1-|noise|)²` при ridgeStrength=0.7 + domain warp 12 даёт узкие извивающиеся ленты. Правка ridgeStrength 0.7→0.3 внесена в код, но не задеплоена (в следующей сессии подход заменён целиком).

### 2026-05-08, session-3 (18:09–18:38) — Реалистичный Марс: MarsTerrainGenerator

- Сначала Claude по ошибке перекрасил мини-сферу в space вместо мира planet_mars — откатили; терминология «мини-сфера vs мир планеты» закреплена в памяти.
- Написан `MarsTerrainGenerator`: ударные кратеры (сетка 64×64, ~65% покрытие, r=8–22, глубина 3–7, приподнятые края), слои red_sand → orange/red terracotta → granite/red_sandstone/stone, тёмные базальтовые пики (brown_terracotta при Y≥115), safe-zone r=18 у спавна, без пещер.
- Найден и исправлен системный баг: Bukkit `SimplexOctaveGenerator.noise(..., normalized=true)` возвращает **[-1,1]**, а не [0,1] — из-за этого рельеф проседал (поверхность Y=50–58 вместо ~105). После нормализации `(n+1)*0.5` — высоты 90–107.
- planet_mars дважды пересоздан; дизайн одобрен пользователем («запомни такой марс»). Создан скилл `galaxy-add-planet` (end-to-end чек-лист добавления планеты: 7 файлов, деплой, gotchas). Коммит `77c2bef` (не запушен на момент сессии).

---

## 2. Ключевые архитектурные решения

**Плагин и код**

- Один плагин Galaxy (`com.galaxy.plugin`), независимые `ChunkGenerator`-ы на мир, без общей базы; DI через конструкторы (сервисы получают зависимости, не лезут в `Bukkit.getWorld` каждый раз).
- `ProximityTask` — `BukkitRunnable` раз в 5 тиков вместо `PlayerMoveEvent` (на порядок дешевле).
- Идемпотентность построек — флаг-файлы в data-папке плагина (`.spheres-built`, `.spawn-platform-built`); пересборка = удалить флаг + рестарт.

**Корабль**

- Невидимая свинья: AI **включён**, но `MOVEMENT_SPEED=0` и `FOLLOW_RANGE=0`, **без седла** (setAI(false) замораживает velocity, седло зануляет горизонтальную скорость). BlockDisplay-визуал трекается отдельно, не пассажиром.
- Dismount-пакет (Shift) отменяется и трактуется как «вниз»; слезть только через `/unship`.
- Cross-world транзит: despawn старого корабля + телепорт игрока + spawn нового (`teleport(RETAIN_PASSENGERS)` между мирами молча не работает).
- Спавн ship-pig идёт через `world.spawnEntity()` = `SpawnReason.CUSTOM` — все анти-моб механики обязаны пропускать CUSTOM и сущности с PDC-ключом `galaxy_ship`.

**Космос (мир space)**

- THE_END environment (нет солнца/луны/облаков); биом — ванильный `Biome.THE_END` (кастомный `galaxy:space` и `THE_VOID` отвергнуты — оба дают голубой туман).
- Вся система сжата в 150-блочный радиус от (0,0,0) — END battle-рендер захардкожен в клиенте и не отключается через API; boss-bar переименован в «Солнечная система ★».
- Звёзды = одиночные частицы END_ROD, `spawnParticle(..., force=true)`, равномерное сферическое сэмплирование `acos(2u-1)`; блоки-звёзды/glowstone отвергнуты (светящиеся блоки в void дают «туман»-ореол).
- Туман у клиента убран ресурспаком на GitHub Pages: override `assets/minecraft/shaders/include/fog.glsl` (no-op).
- `EndStructureCleanup`: End-структуры регенерируются vanilla при каждом телепорте — зачистка на `ChunkLoadEvent` + периодически; `DragonGuard` блокирует дракона.

**Планеты**

- Терминология: «Марс» = мир `planet_mars`; «в космосе / мини-сфера» = декоративный шар в `space`. При неоднозначности — уточнять.
- Земля — vanilla NORMAL (пещеры/руды для майнинга), LEVEL-мир сервера, спавн-платформа 9×9 кварц на (0,80,0).
- Терраподобные (Mercury/Venus/Uranus/Neptune) — `TerrainPlanetGenerator` v2 (6 октав Simplex fBm + ridge + domain warp, per-planet профили в `GalaxyPlugin.java`, без пещер).
- Марс — отдельный `MarsTerrainGenerator` (кратеры + слоистая корка + базальтовые пики; одобренные параметры: BASE_Y=95, HEIGHT_RANGE=30, TERRAIN_SCALE=0.0035, RIDGE_STRENGTH=0.45, WARP_STRENGTH=14) — эталонный дизайн зафиксирован в `docs/memory/project_mars_terrain_design.md`.
- Jupiter/Saturn — flat (газовые гиганты, `FlatPlanetGenerator`); Солнце — «планета» `planet_sun` (`LavaChunkGenerator`: MAGMA_BLOCK-поверхность, source-лава ниже, визуал мини-сферы строит `buildStar`).
- Структуры (шахты/деревни) отключены **только датапаком** с пустыми `structure_set` (20 файлов) — `generateStructures(false)` не работает.
- «Безжизненность» — gamerules (`DO_MOB_SPAWNING`, `DO_PATROL_SPAWNING`, `DO_TRADER_SPAWNING`, `DO_INSOMNIA`=false, `DISABLE_RAIDS`=true) + разовый `purgeAllMobs()` при старте; отдельные листенеры для мобов удалены.
- Гравитация — `Attribute.GRAVITY` игрока по мирам с реальными коэффициентами (Mercury/Mars 0.38x, Jupiter 2.36x, Sun cap 5x), ставится на Join/ChangedWorld.
- Спутники — BlockDisplay-сущности с PDC-тегом `galaxy:moon`, per-tick орбиты (10 лун); движение самих планет отложено.
- Навигация: `/goto` телепортирует на **орбиту** (в space за сферой, на свежем корабле), не на поверхность; `SightTask` — action-bar с русским именем планеты по ray-trace; safe-spawn при посадке — clamp к `getHighestBlockYAt+1`.

**Инфраструктура и процесс**

- Не переходить на NMS-хаки/Fabric/форк Paper — plugin API + datapack покрывают нужды; NMS reflection только точечно.
- Git-процесс: чекпойнт-коммит после каждого зелёного билда; «хорошие состояния» = annotated-теги `vN-slug` с русским заголовком (по слову пользователя «коммит <название>»); GitHub PAT нигде не хранится — только inline в push-URL, потом scrub reflog.
- Бэкап-дисциплина: tar-снапшот в `/home/mc/minecraft/backups/` перед любой state-changing операцией.
- Настройки сервера — только через env в `docker-compose.yml` (itzg-образ регенерирует server.properties на каждом старте).
- Рабочий процесс с пользователем: пошагово, одна state-changing команда за раз с объяснением; крупная работа — стадиями по «next».

---

## 3. Грабли и уроки

**Paper API / Bukkit**

- Paper 1.21.8: `PlayerInputEvent` в `org.bukkit.event.player` (не `io.papermc.paper...`); у `Attribute` убран префикс `GENERIC_` (`SCALE`, `MAX_HEALTH`...). Проверять сигнатуры через `javap` по paper-api jar из `~/.gradle/caches`.
- `setAI(false)` полностью замораживает моба — игнорируются и `setVelocity`, и NBT `Motion`. Для «носителя» — AI включён + `MOVEMENT_SPEED=0`.
- Оседланная свинья зануляет горизонтальную скорость райдера (`Pig.travel()`) — для летающего маунта седло не ставить.
- `Entity.teleport(..., RETAIN_PASSENGERS)` **молча** не работает между мирами — только despawn/respawn.
- Bukkit `SimplexOctaveGenerator.noise(..., normalized=true)` возвращает **[-1,1]**, не [0,1] — нормализовать `(n+1)*0.5`, иначе рельеф систематически проседает. (`TerrainPlanetGenerator.heightAt()` имеет этот баг, компенсированный высоким BASE_Y.)
- Ridge-шум в квадрате + ridgeStrength>0.6 + сильный warp → узкие ленты-хребты («лист Мёбиуса»).
- Кастомный value-noise (даже 3 октавы) ощущается плоским при ходьбе — vanilla noise + `SingleBiomeProvider` даёт лучший результат бесплатно; для узнаваемости нужен спец-генератор (кратеры Марса).
- `WorldCreator.generateStructures(false)` — legacy-флаг, движок структур 1.18+ его игнорирует; единственный надёжный способ — datapack с пустыми `structure_set`.
- `DO_MOB_SPAWNING=false` не покрывает патрули, странствующего торговца, фантомов, рейды и Ender-дракона (DragonBattle — отдельная система, нужен листенер).
- Мобы, сохранённые в уже сгенерированных чанках, спавнятся при загрузке чанка — нужен `ChunkLoadEvent`-хэндлер; мобы, заспавнившиеся во время initial chunk-gen до gamerules, требуют стартового purge.
- Y-спавн из конфига нельзя хардкодить — на горах/айсбергах игрок оказывается внутри блоков; всегда clamp к highest block.
- Bukkit может ставить лаву не source-блоком — жидкая поверхность «плывёт»; верх звезды должен быть твёрдым (MAGMA_BLOCK).

**Биомы, датапаки, клиент**

- Биом `THE_VOID` даёт бледно-голубой туман даже в END-дименшене; кастомный биом с нулевыми effects тоже рендерится голубым — для космоса использовать ванильный `THE_END`. Биом запекается при генерации чанка — смена требует пересоздания мира.
- World-датапаки Paper читает **только** из папки LEVEL-мира (`level-name` из server.properties), не из папок других миров.
- Установленный в существующий мир датапак НЕ включается автоматически (level.dat хранит список) — нужен `datapack enable "file/galaxy"` (с кавычками в rcon), затем regen чанков, сгенерированных до включения.
- Отключение руд через датапак: не ссылаться на `minecraft:ore_diamond` и т.п. напрямую (Unbound values → сервер не стартует) — свой no-op `configured_feature` и все override'ы на него.
- Частицы дальше ~32 блоков требуют `spawnParticle(..., force=true)`.
- END battle-ambience захардкожен в клиенте в ~150 блоках от (0,0,0) — не отключается через API.

**rcon / операции с мирами**

- `kill`/`damage @e` могут не убить дракона — работает `data merge entity @s {Health:0f}`.
- `fill` лимит 32768 блоков — резать на слэбы; `setblock <block> keep` — неразрушающий probe; НЕ использовать `setblock destroy` на живом мире.
- `locate` через rcon: `execute in minecraft:<мир> run minecraft:locate structure ...` (краткая форма конфликтует с плагинами; главный мир = `minecraft:overworld` при level-name=planet_earth).
- Проверка блоков: `execute in <dim> if block X Y Z minecraft:<mat>` (Test passed/failed); `data get block` работает только с tile-entity; чанки нужно `forceload` (и снимать потом).
- При смене генератора мира — удалять папку мира целиком при остановленном paper; при wipe не забывать флаг-файлы плагина, иначе сферы/платформа не перестроятся.
- `tar` работающего мира может падать с «file changed as we read it» — стопить paper или принять первый удачный архив.
- Проверка содержимого jar без `strings`: `unzip -l | grep` по именам классов.

**Инфраструктура / процесс**

- Claude Code работает **прямо на VPS** (hostname `minecraft`) — не пытаться ssh на 62.238.21.160.
- itzg/minecraft-server регенерирует server.properties из compose-env на каждом старте — правки через `docker-compose.yml` + `up -d --force-recreate`; конфиги плагинов не трогаются.
- MV 5.6+: `mv modify <world> set spawn` не работает — править `plugins/Multiverse-Core/worlds.yml` напрямую (pyyaml, paper остановлен), иначе возможен respawn-death-loop.
- Паттерн premium-логина в логах: START → encryption → verified; если после START сразу lost connection — проблема у клиента (токен лаунчера / версия ≠ 1.21.8 / VPN), не у сервера. ViaVersion не установлен — строго клиент 1.21.8.
- Секреты: PAT только inline в push-URL с немедленным scrub reflog; не писать в файлы/коммиты; после использования напоминать отозвать.
- Игроков добавлять/удалять только через RCON (op до первого premium-логина ломает premium-вход — записывается offline UUID).
- RAM на VPS впритык (Paper 5G + gradle ~1G): при `gradle shadowJar` рассмотреть остановку paper.
- Harness Claude Code блокирует паттерн `sleep N && команда` — ожидание старта paper делать фоновым until-loop по строке `Done (` в `docker logs paper`.

---

## 4. Состояние на момент архивации (2026-07-06)

**Работает на сервере (VPS Hetzner, hostname `minecraft`)**

- Контейнер `paper` — Up 3+ недели, healthy. Задеплоен `Galaxy-0.1.0.jar` от **2026-05-08 18:23** = коммит `77c2bef` (включает `MarsTerrainGenerator`).
- 12 миров в `/home/mc/minecraft/data/`: `space`, `planet_earth` (+ неиспользуемые `planet_earth_nether`/`planet_earth_the_end`), `planet_sun`, `planet_mercury`, `planet_venus`, `planet_mars`, `planet_jupiter`, `planet_saturn`, `planet_uranus`, `planet_neptune`.
- `LEVEL=planet_earth` в docker-compose; датапак `galaxy` (20 пустых structure_set) в `planet_earth/datapacks/`, включён.
- Ресурспак: репозиторий `github.com/bkarimzhan/galaxy-resourcepack` → GitHub Pages, no-fog шейдер `assets/minecraft/shaders/include/fog.glsl`; sha1 прописан в server.properties.
- Игровое состояние: компактная солнечная система (всё в 150-блочном радиусе), 9 мини-сфер + кольцо Сатурна + 10 лун, звёзды-частицы, корабль летает по всем мирам, гравитация по планетам, мобов и структур на планетах (кроме Земли — там только структуры в старых чанках) нет, Марс с кратерным реалистичным рельефом одобрен пользователем.
- Аутентификация: `online_mode=false` + AuthMe + FastLogin + ProtocolLib; ростер из 2 op-игроков: `Leekuan16` (cracked/AuthMe) и `kz_bazelevs` (premium/FastLogin). Полный стек версий — в `docs/memory/project_working_stack.md`.

**Git (`/home/mc/galaxy-plugin`, remote `github.com/bkarimzhan/galaxy-plugin`)**

- `origin/main` = `286447e` (= тег `v5-planet-terrain`). Локальный `main` = `77c2bef` («MarsTerrainGenerator: realistic surface for planet_mars») — **впереди origin на 1 коммит, не запушен и не затеган**.
- Теги (все запушены): `v1-solar-system`, `v2-compact-solar-system` («Компактная солнечная система»), `v3-moons» («Спутники»), `v4-gravity-empty-planets» («Гравитация и пустые планеты»), `v5-planet-terrain» («Кастомный рельеф планет»). Откат: `git reset --hard vN-slug`.
- Локально не закоммичены: правка `.gitignore`, новые `docs/` (ARCHITECTURE.md, SERVER-SETUP.md, memory/, этот файл), `server/docker-compose.yml`, `.claude/`.

**Структура кода (`src/main/java/com/galaxy/plugin/`)**

- `GalaxyPlugin.java` — точка входа, per-planet профили рельефа;
- `commands/` — Goto, LeavePlanet, Planets, Ship, Space, Unship (+PlayerCommand);
- `listeners/` — DragonGuard, EndStructureCleanup, GravityListener, MoonOrbitTask, NightVisionListener, ProximityTask, SightTask, SpawnListener, StarParticleTask;
- `planets/` — Planet, PlanetManager, PlanetTexture, SphereBuilder;
- `ship/ShipController.java`; `teleport/TeleportService.java`;
- `world/` — WorldBootstrap, DatapackInstaller, SpaceChunkGenerator, FlatPlanetGenerator, LavaChunkGenerator, DesertChunkGenerator, TerrainPlanetGenerator, MarsTerrainGenerator, SingleBiomeProvider, SpawnPlatform.

**Вспомогательное**

- Скиллы Claude Code: `galaxy-add-planet` (чек-лист добавления планеты), `paper-plugin-scaffold`, `paper-backup-snapshot`, `paper-plugin-install`, `paper-itzg-config`, `paper-mv-spawn-fix`, `paper-rp-github-pages`, `mc-rcon-build`.
- Файлы памяти проекта скопированы в `docs/memory/` — при противоречиях они авторитетнее пересказов. **Исключение:** `project_galaxy_plugin_state.md` частично устарел (описывает откаченные решения: кастомный биом `galaxy:space`, листенер SpaceMobGuard — см. пометку в самом файле); при конфликте верить этому документу.

---

## 5. Открытые задачи и roadmap

**Немедленное (гигиена репозитория)**

- Запушить `77c2bef` в origin/main; спросить у пользователя русское название и создать тег `v6-*` для реалистичного Марса (дизайн одобрен, тега нет).
- Настроить постоянную GitHub-авторизацию (credential helper / SSH-ключ / gh CLI) — сейчас push делается разовым PAT из чата.
- Закоммитить архив целиком: `docs/` (включая `docs/memory/`), `.claude/skills/` (8 скиллов — `.gitignore` их специально разрешает через `!.claude/skills/`), `server/docker-compose.yml`, правку `.gitignore`. *(Сделано архивным коммитом 2026-07-06.)*

**Известные мелкие дефекты / хвосты**

- `TerrainPlanetGenerator.heightAt()` — тот же баг нормализации [-1,1] (компенсирован высоким BASE_Y); нормализовать, если нужны точные диапазоны высот у Mercury/Venus/Uranus/Neptune.
- Возможные «ленты Мёбиуса» на Uranus (ridgeStrength 0.65) и Mercury (0.6) — снизить по образцу диагностики Марса или перевести на реалистичные генераторы по образцу `MarsTerrainGenerator` (через скилл `galaxy-add-planet`).
- Debug-лог `[ship-debug]` на каждое нажатие пишется на уровне INFO — убрать/понизить.
- No-fog шейдер глобальный (убирает туман и на планетах) — при желании сделать условным.
- Neptune = FROZEN_OCEAN (вода) — пользователь может захотеть сменить биом.
- Старые чанки planet_earth всё ещё содержат шахты/деревни; полная очистка = пересоздание мира (сотрёт постройки игроков) — решение не принято.
- `planet_earth_nether`/`planet_earth_the_end` не используются; space и большинство planet_* не импортированы в Multiverse.

**Отложенные фичи**

- Движение планет по орбитам (спутники двигаются, планеты статичны); варианты: rebuild каждые N сек (рывки) или BlockDisplay (потеря текстур).
- «Мини-копия реального Марса»: тематические landmarks (Olympus Mons, Valles Marineris, полярные шапки) поверх генератора; NASA MOLA heightmap — второй заход (при border 1000 нужно усиление вертикали ×3–5).
- Roadmap генератора: Уровень 2 — vanilla-style 3D density (continentalness/erosion/PV, overhangs, surface rules); Уровень 3 — терра-профили планет в YAML, кастомные руды/флора.
- Огнестойкость/броня для посещения Солнца (planet_sun) — идея, не реализована.
- Команда `/observatory` (точка обзора системы сверху) — предложение без ответа пользователя.

**Крупные направления (выбор за пользователем)**

- Экономика (EconomyShopGUI + покупка корабля за кредиты) **или** блочные корабли (Movecraft-стиль).
- Образовательный roadmap для сына (Leekuan16, 9 лет): MVP2 — command blocks, MVP3 — Skript + Smart-items, MVP4+ — реальный Java (см. `docs/memory/project_education_roadmap.md`).

**Серверная гигиена (вне плагина)**

- **Перед сносом сервера — свежий полный бэкап миров и вывоз с VPS** (последний полный tarball от 2026-05-06 устарел на ~2 месяца игры; процедура — в SERVER-SETUP.md, раздел 8).
- Убрать временный NOPASSWD sudo (`/etc/sudoers.d/mc-nopasswd-temp`) при переходе в steady-state.
- Убедиться, что все PAT, светившиеся в чатах сессий, отозваны в GitHub Settings.
- **Пароль AuthMe игрока Leekuan16 засвечен в истории git**: tracked-файл CLAUDE.md входит в запушенные коммиты (в т.ч. `61c5552`, `286447e` = origin/main) и теги v1–v5 публичного репозитория. В рабочей копии заменён плейсхолдером, но история хранит оригинал. При восстановлении сервера из бэкапа (база AuthMe вернётся) — сменить пароль (`authme changepassword`); этот пароль нигде не переиспользовать. Радикальный вариант — переписать историю репо (`git filter-repo`) с перезаливкой тегов.
