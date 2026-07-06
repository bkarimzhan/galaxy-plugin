# Galaxy — архитектура плагина

Paper-плагин для Minecraft 1.21.8, реализующий механику «Солнечная система»: 9 планетных миров + void-космос с декоративными мини-сферами планет + entity-корабль для перелётов. Java 21, Gradle Kotlin DSL, shadow-jar (`build/libs/Galaxy-0.1.0.jar`).

Документ описывает фактическое состояние кода на HEAD (`77c2bef`, тег `v5-planet-terrain` + Mars-генератор). Старый `CLAUDE.md` («Stage 0») устарел — не ориентироваться на него.

---

## 1. Обзор механики (поток игрока)

1. **Спавн** — новый игрок появляется на кварцевой платформе (0, 80, 0) мира `planet_earth` (`SpawnPlatform` + `SpawnListener`). `planet_earth` — обычный ванильный overworld с worldborder 1000, но «безжизненный» (mob-спавн выключен) и без структур.
2. **Корабль** — `/ship` спавнит перед игроком невидимую свинью-корабль с визуалом BlockDisplay (железный блок) и сажает игрока. Управление: взгляд + WASD, Space — вверх, Shift — вниз. Экономики/покупки пока нет — корабль бесплатный.
3. **Взлёт** — `ProximityTask` (каждые 5 тиков) следит за игроками. На любой планете при **Y > 300** игрок телепортируется в мир `space` — на точку возле мини-сферы этой планеты (за 12 блоков от края). Корабль деспавнится и респавнится на месте прибытия (cross-world перенос пассажира ненадёжен, поэтому despawn-and-respawn). При **Y > 180** ещё на планете игроку выставляется персональное ночное небо (`setPlayerTime`) — эффект «выхода в стратосферу».
4. **Космос** (`space`) — void-мир с THE_END-окружением. В центре (0, 100, 0) — солнце r=18 из shroomlight/glowstone, вокруг в радиусе ~150 блоков — мини-сферы 8 планет (r=3..8) с процедурными текстурами, кольцом Сатурна и 10 лунами-BlockDisplay на орбитах. Фоново: звёзды-частицы END_ROD вокруг игрока, вечная ночь, night vision, боссбар дракона переименован в «Солнечная система ★».
5. **Наведение** — `SightTask` рейтрейсом показывает в action bar русское имя планеты и дистанцию, когда игрок смотрит на сферу.
6. **Посадка** — подлёт к мини-сфере ближе `radius + 6` блоков → телепорт в соответствующий планетный мир на точку `spawn` из `planets.yml` (поднятую до поверхности). Корабль деспавнится.
7. **Возврат** — снова вверх до Y > 300 → назад в `space` у сферы планеты, корабль автоспавнится.
8. **Навигация без полёта** — `/planets` (список с дистанциями), `/goto <planet>` (варп в орбиту сферы, дальше подлёт вручную), `/leaveplanet` (fallback: с планеты — в космос; в космосе — на ближайшую планету), `/space` (admin-телепорт в спавн космоса), `/unship` (слезть и убрать корабль).
9. **Гравитация** — `GravityListener` масштабирует `Attribute.GRAVITY` игрока по миру: Марс/Меркурий 0.38, Юпитер 2.36, Солнце 5.0 и т.д.

Мир Солнца — отдельная «планета»: лавовый океан (посадка возможна, выжить трудно — гравитация ×5).

---

## 2. Карта модулей / классов

### `com.galaxy.plugin`
- **`GalaxyPlugin`** — главный класс. В `onEnable` порядок: бутстрап миров → загрузка `planets.yml` → одноразовая постройка сфер и спавн-платформы → регистрация слушателей/задач → команды. Также содержит фабрики генераторов планет (профили Меркурия, Венеры и т.д.) и `getDefaultWorldGenerator()` — маппинг «имя мира → генератор», чтобы миры корректно подхватывались при повторной загрузке.

### `world` — миры и генераторы
- **`WorldBootstrap`** — создание/загрузка всех 10 миров (`space` + 9 `planet_*`) через `WorldCreator`; константы имён миров; worldborder, gamerules «безжизненности» (`DO_MOB_SPAWNING=false` и др.), чистка залётных мобов (`purgeAllMobs`, корабли по PDC-тегу не трогает). Для `space` дополнительно: THE_END-окружение, view distance 32, вечная ночь, удаление автоспавненного дракона, ре-лейбл боссбара DragonBattle.
- **`SpaceChunkGenerator`** — пустой void-генератор (ничего не ставит), биом THE_END.
- **`TerrainPlanetGenerator`** — универсальный шумовой terrain engine (см. раздел 3). Параметризуется рекордом `Profile`.
- **`MarsTerrainGenerator`** — специализированный генератор Марса: тот же шумовой каркас + детерминированное поле кратеров + многослойная палитра поверхности/недр.
- **`FlatPlanetGenerator`** — плоская «палуба» на Y=64 из двух материалов; используется для газовых гигантов (Юпитер, Сатурн).
- **`LavaChunkGenerator`** — мир Солнца: лава от бедрока до Y=62, magma block на Y=63, биом NETHER_WASTES.
- **`DesertChunkGenerator`** — легаси-генератор красной пустыни (первый Марс); в текущей сборке не используется.
- **`SingleBiomeProvider`** — BiomeProvider, возвращающий один биом на весь мир; использовался этапом «ванильный рельеф + форс-биом» (`createPlanetMars`/`createVanillaBiomePlanet` в `WorldBootstrap` — сейчас не вызываются).
- **`SpawnPlatform`** — одноразовая (флаг-файл `.spawn-platform-built`) постройка спавн-платформы 9×9 в `planet_earth`: кварц, glowstone-маяки, невидимый LIGHT-блок, полая каменная опора.
- **`DatapackInstaller`** — при старте распаковывает `galaxy-datapack/` из jar в `<world-container>/planet_earth/datapacks/galaxy` (именно в level-name мир — датапаки в MC живут per-save). См. раздел 5.

### `planets` — модель солнечной системы
- **`Planet`** (record) — id, имя мира, центр/радиус/материалы мини-сферы, флаг `skipSphereBuild`, точка спавна на планете.
- **`PlanetManager`** — грузит `planets.yml` (копирует дефолт из jar при первом запуске), парсит в `Planet`, даёт lookup по id и по имени мира.
- **`SphereBuilder`** — одноразовая (флаг-файл `.spheres-built`) постройка в `space`: солнце r=18 (shroomlight/glowstone), полые оболочки мини-сфер с текстурами из `PlanetTexture`, кольцо Сатурна, предварительная зачистка END-структур. Метод `scatterStars` — мёртвый код (звёзды теперь частицы).
- **`PlanetTexture`** — процедурные «текстуры» сфер: детерминированный int-hash + широтные полосы (полярные шапки Земли/Марса, полосы Юпитера с «красным пятном», ленты Сатурна и т.п.).

### `ship` — корабль
- **`ShipController`** — весь корабль: спавн невидимой неуязвимой свиньи (gravity off, `MOVEMENT_SPEED=0`, `SCALE=0.5`, PDC-тег `galaxy_ship`) + BlockDisplay-визуал, следующий за ней телепортом. Читает `PlayerInputEvent` в вектор (forward/strafe/vert), тикает физику раз в тик: `v = v*0.85 + look*f*0.6 + right*s*0.6; v.y += vert*0.55`, лимит скорости 1.4. Спешивание блокируется (`EntityDismountEvent` отменяется — Shift трактуется как «вниз»), сойти можно только через `dismountAndDespawn` (используется `/unship` и телепортами). `shutdown()` убирает все корабли во всех мирах.

### `teleport`
- **`TeleportService`** — три перехода: `toSpaceFromPlanet` (в космос к сфере планеты + автоспавн корабля), `toPlanet` (на планету, Y поднимается до `getHighestBlockYAt`+1), `toOrbit` (для `/goto`). Везде паттерн «снять с корабля → деспавн → телепорт → новый корабль в точке прибытия».

### `listeners` — задачи и события
- **`ProximityTask`** — сердце переходов (BukkitRunnable, 5 тиков): в космосе проверяет подлёт к сферам (`radius+6`), на планетах — порог взлёта Y>300 и «стратосферное» ночное небо Y>180.
- **`GravityListener`** — на join/смену мира выставляет `Attribute.GRAVITY` = 0.08 × фактор планеты (хардкод-мапа имён миров).
- **`MoonOrbitTask`** — 10 лун (Луна, Фобос/Деймос, 4 галилеевых, Титан, Титания, Тритон) как BlockDisplay; каждый тик телепортирует по круговой орбите вокруг родительской сферы (радиус/период/фаза в хардкод-списке `MoonDef`). PDC-тег `galaxy:moon`, зачистка старых при рестарте.
- **`StarParticleTask`** — каждые 10 тиков рисует по 100 END_ROD-частиц равномерно по сфере r=30..240 вокруг каждого игрока в космосе (force=true, чтобы видны издалека).
- **`SightTask`** — каждые 5 тиков raytrace до 320 блоков; попадание в блок около центра сферы (radius+12) → action bar «Марс · 87 блоков» (русские имена в хардкод-мапе).
- **`SpawnListener`** — первый вход → спавн-платформа; повторный вход в «транзитных» мирах (`space`, `planet_sun`) без транспорта → спасение на платформу; respawn без кровати/якоря → платформа.
- **`NightVisionListener`** — бесконечный night vision в космосе, снятие при выходе.
- **`DragonGuard`** — отменяет любой спавн EnderDragon в `space` и гасит его взрывы/порчу блоков.
- **`EndStructureCleanup`** — борьба с регенерацией END-структур в `space`: на загрузке чанков (0,0) и (6,0) и таймером раз в 200 тиков вычищает обсидиановую платформу, exit-портал, bedrock-фонтан, факелы, dragon egg; блоки внутри радиуса солнца заменяет на shroomlight, снаружи — на воздух.

### `commands`
- **`PlayerCommand`** — абстрактная база «команда только для игрока».
- **`SpaceCommand`** (`/space`, op) — телепорт в спавн космоса.
- **`ShipCommand`** / **`UnshipCommand`** — вызвать/убрать корабль.
- **`GotoCommand`** (`/goto <planet>` + tab-complete) — варп в орбиту сферы.
- **`PlanetsCommand`** — список планет, в космосе отсортирован по дистанции.
- **`LeavePlanetCommand`** — fallback-эвакуация для застрявших.

---

## 3. Генераторы миров и terrain engine

Все генераторы — Bukkit `ChunkGenerator`, всё строится в `generateNoise()`; остальные фазы (surface/caves/decorations/mobs/structures) отключены переопределением `shouldGenerate*() → false`. Каждый генератор отдаёт `getDefaultBiomeProvider` с одним биомом на мир — биом подбирается ради климата/цветов неба (FROZEN_PEAKS для Урана, BADLANDS для Марса и т.д.).

### Распределение по мирам

| Мир | Генератор | Рельеф |
|---|---|---|
| `space` | `SpaceChunkGenerator` | чистый void, биом THE_END |
| `planet_earth` | ванильный NORMAL (без кастомного генератора) | обычный overworld, структуры отключены |
| `planet_sun` | `LavaChunkGenerator` | лавовый океан до Y62 + magma на Y63 |
| `planet_mars` | `MarsTerrainGenerator` | шумовой рельеф + кратеры |
| `planet_mercury` | `TerrainPlanetGenerator` | скалы: deepslate/stone, пики из базальта, biome STONY_PEAKS |
| `planet_venus` | `TerrainPlanetGenerator` | вулканические холмы: tuff/granite, magma-пики, SAVANNA |
| `planet_uranus` | `TerrainPlanetGenerator` | ледяные пики: packed/blue ice, FROZEN_PEAKS |
| `planet_neptune` | `TerrainPlanetGenerator` | замёрзший океан: blue/packed ice, DEEP_FROZEN_OCEAN |
| `planet_jupiter`, `planet_saturn` | `FlatPlanetGenerator` | плоская «палуба газового гиганта» на Y64 |

`GalaxyPlugin.getDefaultWorldGenerator()` возвращает тот же генератор по имени мира — это нужно, чтобы Bukkit/сторонние плагины при загрузке мира получали правильный генератор, а не ванильный.

### Terrain engine (`TerrainPlanetGenerator`)

Профиль планеты — record `Profile(surface, foundation, accent, biome, baseY, heightRange, terrainScale, ridgeStrength, warpStrength, accentMinHeight)`.

Высота колонки считается из трёх слоёв `SimplexOctaveGenerator` (сиды разведены XOR-константами, ре-инициализация при смене сида мира):

1. **Base** — 6 октав, scale = `terrainScale` — базовые холмы.
2. **Ridge** — 4 октавы, scale ×1.5: `r = (1 − |noise|)²` — «хребтовый» шум, даёт острые гребни; смешивается с base по весу `ridgeStrength`.
3. **Domain warp** — два 3-октавных шума `warpX`/`warpZ`, scale ×0.5: входные координаты искажаются на ±`warpStrength` блоков — ломает «плюшевую» регулярность симплекса.

Итог: `height = baseY + (combined − 0.5) · 2 · heightRange`. Колонка: bedrock на −64 → `foundation` до высоты → верхний блок `surface`, а выше `accentMinHeight` — `accent` (снежные/базальтовые пики).

### Марс (`MarsTerrainGenerator`)

Тот же трёхслойный шумовой каркас (base 6 октав нормализуется в [0,1] — см. комментарий в коде), поверх — **детерминированное поле кратеров** без Random:

- Плоскость разбита на ячейки 64×64; целочисленный hash `mix(cx,cz)` решает, есть ли в ячейке кратер (~65%), и задаёт его радиус (8–22), глубину (3–7) и позицию внутри ячейки. Проверяются 3×3 соседних ячейки.
- Внутри радиуса — параболическая чаша (`−depth·(1−t²)`), в поясе до 1.18·r — приподнятый вал (+1.5 блока).
- Зона 18 блоков вокруг (0,0) свободна от кратеров — защита спавна.
- Материалы: равнины — red sand с выходами терракоты; дно кратеров — обнажённая порода (терракота без песка); плато выше Y115 — тёмный «базальтовый» верх; недра — 2 блока orange terracotta → 3 red terracotta → гранит/красный песчаник/камень.

---

## 4. Конфиг `planets.yml`

Дефолт лежит в jar (`src/main/resources/planets.yml`); при первом запуске копируется в `plugins/Galaxy/planets.yml` и дальше читается **только оттуда** (правки в jar не подхватятся, пока файл существует).

Формат:

```yaml
planets:
  <id>:                     # id планеты (mars, earth, …) — ключ для /goto, текстур, лун
    world: planet_<id>      # имя мира-планеты
    sphere:                 # мини-сфера в мире space
      center: [x, y, z]     # центр сферы (int)
      radius: 5             # радиус (default 18)
      shell: RED_CONCRETE   # материал оболочки (fallback, если нет PlanetTexture)
      core: REDSTONE_BLOCK  # блок в центре сферы
      skipBuild: true       # опционально: не строить (у Солнца — его строит buildStar)
    spawn:                  # точка посадки в мире планеты
      location: [x, y, z]   # double; Y поднимается до поверхности при телепорте
      yaw: 0.0
```

Текущая раскладка — «компактная система»: всё в боевом радиусе ~150 блоков от начала координат, Y всех сфер = 100.

| id | world | центр сферы | r | оболочка |
|---|---|---|---|---|
| sun | planet_sun | 0, 100, 0 | 18 | SHROOMLIGHT (skipBuild) |
| mercury | planet_mercury | 22, 100, 15 | 3 | STONE |
| venus | planet_venus | −13, 100, 36 | 5 | ORANGE_TERRACOTTA |
| earth | planet_earth | −49, 100, −18 | 6 | GREEN_CONCRETE |
| mars | planet_mars | 11, 100, −64 | 3 | RED_CONCRETE |
| jupiter | planet_jupiter | 55, 100, 65 | 8 | ORANGE_TERRACOTTA |
| saturn | planet_saturn | −67, 100, 80 | 7 | SMOOTH_SANDSTONE (+кольцо) |
| uranus | planet_uranus | −96, 100, −80 | 5 | LIGHT_BLUE_CONCRETE |
| neptune | planet_neptune | 91, 100, −109 | 5 | BLUE_CONCRETE |

Спавн-точки планет — (0, 66, 0), кроме Земли (0, 81, 0 — платформа) и Солнца (0, 80, 0). Гравитация, русские подписи (`SightTask`) и луны (`MoonOrbitTask`) привязаны к id/имени мира в коде, не в yml.

---

## 5. Datapack (`galaxy-datapack`)

В jar лежит `src/main/resources/galaxy-datapack/` (pack_format 48). `DatapackInstaller.installFor(plugin, "planet_earth")` при каждом старте (до создания миров) распаковывает его в `planet_earth/datapacks/galaxy` — именно в **level-name мир** (на сервере itzg стоит `LEVEL: planet_earth`), потому что Minecraft грузит датапаки только из папки основного сейва, а действуют они на все миры сервера.

Содержимое — **20 переопределений `data/minecraft/worldgen/structure_set/*.json`**, у каждого пустой список структур:

```json
{ "structures": [], "placement": { "type": "minecraft:random_spread", "spacing": 32, "separation": 8, "salt": 0 } }
```

Зачем: гарантированно отключить генерацию всех ванильных структур (деревни, крепости, trial chambers, end cities и т.д.) во **всех** мирах. `WorldCreator.generateStructures(false)` действует только в момент создания мира и записывается в level.dat — для уже существующих на диске миров и для миров с ванильным генератором это ненадёжно. Переопределение structure_set пустым списком — единственный способ выключить структуры на уровне worldgen-реестра, не ломая валидацию реестров Paper (сами файлы удалить нельзя — ванильные ссылки должны резолвиться; поэтому список структур опустошается, а placement остаётся валидным).

Исторически датапак также нёс кастомный биом `galaxy:space` с чёрным туманом — от этого отказались (туман сейчас убирается no-op fog-шейдером в resource pack, отдельный репозиторий `galaxy-resourcepack`), и в текущем датапаке биома нет.

---

## 6. Жизненный цикл

### `onEnable` (load: POSTWORLD)

1. **`WorldBootstrap`**:
   - `createSpace()` — сначала `DatapackInstaller.installFor(plugin, PLANET_EARTH)` (датапак должен лежать до генерации), затем создание `space` (THE_END env + `SpaceChunkGenerator`), worldborder 5000, вечная ночь, view distance 32, удаление дракона, ре-лейбл боссбара.
   - `createPlanetEarth()` — ванильный NORMAL, border 1000.
   - `createFlatPlanet(...)` ×7 — Марс, Меркурий, Венера, Юпитер, Сатурн, Уран, Нептун со своими генераторами.
   - `createPlanetSun()` — лавовый мир.
   - Для каждого мира: lifeless-gamerules + purge мобов. Провал бутстрапа ключевых миров → `disablePlugin`.
2. **`PlanetManager.loadFromFile`** — копия дефолтного `planets.yml` при первом запуске, парсинг; пустой список → `disablePlugin`.
3. **`SphereBuilder.buildAllOnce`** — солнце + мини-сферы + кольцо Сатурна (однократно, флаг-файл).
4. **`SpawnPlatform.buildOnce`** + `setSpawnLocation` Земли (однократно, флаг-файл).
5. Регистрация событийных слушателей: `SpawnListener`, `DragonGuard`, `EndStructureCleanup` (+ немедленный `wipeNow`), `NightVisionListener`, `GravityListener`.
6. **`TeleportService`** → **`ShipController`** (регистрируется и как listener; связывается с TeleportService).
7. Запуск периодических задач: `ProximityTask` (5t), `StarParticleTask` (10t), `SightTask` (5t), `MoonOrbitTask` (1t; чистит старые луны и спавнит 10 новых).
8. Привязка 6 команд из `plugin.yml`.

### `onDisable`

Отмена всех задач; `MoonOrbitTask.shutdown()` удаляет луны-BlockDisplay; `ShipController.shutdown()` останавливает тик физики и деспавнит все корабли во всех мирах.

### Одноразовые постройки

Сферы и платформа охраняются флаг-файлами в `plugins/Galaxy/` (`.spheres-built`, `.spawn-platform-built`). Чтобы перестроить (например, после правки `planets.yml`) — удалить флаг-файл и перезапустить сервер.

---

## 7. Известные ограничения

- **Экономика не реализована** — `/ship` бесплатный; покупка за кредиты (EconomyShopGUI) осталась в планах.
- **Много хардкода вне конфига**: факторы гравитации (`GravityListener`), русские названия (`SightTask`), луны (`MoonOrbitTask.MOONS`), процедурные текстуры (`PlanetTexture`), профили рельефа (фабрики в `GalaxyPlugin`) — привязаны к id/именам миров в коде. Добавление планеты требует правок в ~6 местах (есть скилл `galaxy-add-planet`, автоматизирующий это).
- **Дублирование позиции Солнца**: центр (0,100,0) и r=18 захардкожены в `SphereBuilder.buildStar` и `EndStructureCleanup`, хотя есть в `planets.yml`.
- **Несогласованная нормализация шума**: `MarsTerrainGenerator` нормализует base-шум из [−1,1] в [0,1], `TerrainPlanetGenerator` — нет; из-за этого у обычных terrain-планет фактический размах высот вдвое больше номинального `heightRange` и смещён относительно `baseY`.
- **Мёртвый/легаси-код**: `DesertChunkGenerator`, `SphereBuilder.scatterStars`, `WorldBootstrap.createPlanetMars` / `createVanillaBiomePlanet`, `ShipController.teleportShipWithRider` (RETAIN_PASSENGERS-путь заменён на despawn-respawn), неиспользуемый импорт `DesertChunkGenerator` в `GalaxyPlugin`.
- **`ProximityTask` не проверяет наличие корабля** — любой игрок выше Y=300 на планете улетит в космос (в т.ч. поднятый эндер-жемчугом/элитрами); посадка тоже срабатывает на «пешехода» в космосе.
- **Борьба с THE_END-окружением — вечная**: сервер регенерирует обсидиановую платформу и exit-портал в `space`, поэтому `EndStructureCleanup` вынужден чистить их на chunk-load и таймером каждые 200 тиков. Смена окружения на NORMAL была опробована и отклонена (визуально хуже).
- **Боссбар — от DragonBattle**: заголовок «Солнечная система» живёт на боссбаре битвы дракона END-мира; это трюк, зависящий от внутреннего поведения Paper.
- **`planets.yml` читается один раз на старте**; hot-reload нет, перестройка сфер — только через удаление флаг-файла.
- **Debug-шум в логах**: `ShipController.onInput` пишет `[ship-debug]` на каждое изменение ввода.
- **Туман в космосе** убирается только внешним resource pack (no-op fog-шейдер) — сам плагин чёрное небо/отсутствие тумана не гарантирует.