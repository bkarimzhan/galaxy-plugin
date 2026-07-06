---
name: galaxy-add-planet
description: Add a new planet to the Galaxy plugin (`/home/mc/galaxy-plugin/`) — creates the planet world + mini-sphere in `space` + gravity/label/moon registrations. Trigger when user says "добавь планету X", "создай планету X", "новый мир-планета", "add planet X to galaxy plugin".
---

# Galaxy plugin — добавление новой планеты

End-to-end рецепт для добавления планеты в `/home/mc/galaxy-plugin/`. Затрагивает 6–8 файлов и требует пересборку + рестарт сервера. Skill **обязателен** для соблюдения порядка — забыть один шаг легко, и тогда мир сгенерируется без gravity / без названия / без мини-сферы в `space`.

## 1. Параметры что нужно собрать у пользователя

Если пользователь не дал — спросить **до** редактирования файлов. Один вопрос за раз (соответствует workflow preference этого пользователя).

| Параметр | Пример | Default если пользователь не знает |
|---|---|---|
| **id** (slug, lowercase ASCII) | `pluto`, `titan` | спросить — это primary key |
| **Russian label** | `Плутон`, `Титан` | спросить |
| **Тип ландшафта** | `flat` / `terrain` / `custom` | спросить — определяет генератор |
| **Worldborder** | `500` или `1000` | `500` для дальних, `1000` если планируется база |
| **Координаты мини-сферы в space** | `[105, 100, -130]` | предложить точку на ~150 от origin, не пересекающуюся с существующими (см. `planets.yml`) |
| **Радиус мини-сферы** | `3..8` | `4` для small rocky, `7` для gas giant |
| **Палитра мини-сферы** | shell + core Material | согласовать с темой планеты |
| **Биом для planet world** | `Biome.BADLANDS` и т.д. | согласовать с landscape |
| **Gravity scale** | `0.38` (low), `1.0` (Earth), `2.5` (Jupiter) | по реальной физике |
| **Луны** | список MoonDef или нет | по умолчанию нет |

## 2. Чек-лист файлов (порядок имеет значение)

### 2.1. `src/main/resources/planets.yml`
Добавить запись по образцу:
```yaml
  pluto:
    world: planet_pluto
    sphere:
      center: [105, 100, -130]
      radius: 4
      shell: PACKED_ICE
      core: BLUE_ICE
    spawn:
      location: [0, 145, 0]
      yaw: 0.0
```

### 2.2. `src/main/java/com/galaxy/plugin/world/WorldBootstrap.java`
Добавить `public static final String PLANET_PLUTO = "planet_pluto";` рядом с другими константами.

### 2.3. `src/main/java/com/galaxy/plugin/GalaxyPlugin.java`
Три правки в этом файле:

**(a) `onEnable()`** — после других `wb.createFlatPlanet(...)`:
```java
wb.createFlatPlanet(WorldBootstrap.PLANET_PLUTO, plutoGen(), 145, 500, "icy plains");
```

**(b) `getDefaultWorldGenerator()` switch** — добавить case:
```java
case WorldBootstrap.PLANET_PLUTO -> plutoGen();
```

**(c) helper-метод**:
- Простой плоский: `return new FlatPlanetGenerator(Material.PACKED_ICE, Material.BLUE_ICE, Biome.FROZEN_PEAKS);`
- Через `TerrainPlanetGenerator` с `Profile`:
  ```java
  private static TerrainPlanetGenerator plutoGen() {
      return new TerrainPlanetGenerator(new TerrainPlanetGenerator.Profile(
          Material.PACKED_ICE, Material.BLUE_ICE, Material.ICE,
          Biome.FROZEN_PEAKS,
          85, 25,           // baseY, heightRange
          0.004, 0.5, 12.0, // scale, ridgeStrength, warpStrength
          105));            // accentMinHeight
  }
  ```
- Кастомный класс (как `MarsTerrainGenerator`) — создавать только если нужны кратеры / специфические слои, которые `TerrainPlanetGenerator.Profile` не поддерживает.

### 2.4. `src/main/java/com/galaxy/plugin/planets/PlanetTexture.java`
Добавить case в switch + private метод. Только если хочешь кастомную **раскраску мини-сферы** в `space` (полярные шапки, банды, узоры). Иначе мини-сфера будет однотонной из shell-материала из `planets.yml`.

```java
case "pluto" -> pluto(dx, dy, dz, radius);
// ...
private static Material pluto(int dx, int dy, int dz, int r) {
    int h = hash(dx, dy, dz) % 10;
    if (h < 6) return Material.PACKED_ICE;
    if (h < 9) return Material.BLUE_ICE;
    return Material.SNOW_BLOCK;
}
```

### 2.5. `src/main/java/com/galaxy/plugin/listeners/GravityListener.java`
В Map гравитации:
```java
"planet_pluto",   0.06,
```

### 2.6. `src/main/java/com/galaxy/plugin/listeners/SightTask.java`
Русский label для action-bar:
```java
Map.entry("pluto", "Плутон"),
```

### 2.7. (Опционально) `src/main/java/com/galaxy/plugin/listeners/MoonOrbitTask.java`
Если у планеты есть луны:
```java
new MoonDef("charon", "pluto", 6, 11, Material.LIGHT_GRAY_CONCRETE, 0.7f, 0),
```

## 3. Сборка

```bash
cd /home/mc/galaxy-plugin && gradle shadowJar
```
Проверить вывод: `BUILD SUCCESSFUL`. Любые `error:` или `cannot find symbol` — лечить до деплоя.

## 4. Деплой (state-changing — backup обязателен)

```bash
# 4.1 Backup ТЕКУЩИХ миров что будут пересобраны
tar -czf /home/mc/minecraft/backups/pre-add-pluto-$(date -u +%Y%m%dT%H%M%SZ).tar.gz \
  -C /home/mc/minecraft/data space

# 4.2 Stop paper для чистой замены jar
docker stop paper

# 4.3 Подмена jar + сброс флаг-файла мини-сфер (чтобы новая мини-сфера построилась)
cp /home/mc/galaxy-plugin/build/libs/Galaxy-0.1.0.jar /home/mc/minecraft/data/plugins/Galaxy-0.1.0.jar
rm -f /home/mc/minecraft/data/plugins/Galaxy/.spheres-built

# 4.4 Start paper
docker start paper

# 4.5 Подождать готовности и проверить логи
until docker exec paper rcon-cli "list" >/dev/null 2>&1; do sleep 2; done
docker logs paper --tail 200 2>&1 | grep -iE "galaxy|pluto|sphere"
```

Ожидаемые строки в логе:
- `World 'planet_pluto' ready (...)` — мир создан
- `Mini-sphere 'pluto' built at ... r=N` — мини-сфера в `space` построена
- `Loaded N planet(s) from planets.yml: [..., pluto]` — конфиг считан с новой планетой

## 5. Проверка in-game

```bash
# Телепортироваться к мини-сфере и в planet_pluto:
docker exec paper rcon-cli "execute in minecraft:space run tp <player> 105 105 -130"
docker exec paper rcon-cli "execute in minecraft:planet_pluto run tp <player> 0 145 0"
```

Проверить:
1. Мини-сфера видна и имеет правильную палитру.
2. При телепорте в planet_<id> игрок не умирает от падения (поверхность близко к spawnY).
3. Гравитация на планете соответствует заявленной (`/effect` или прыжок).
4. Action-bar показывает русское название при взгляде на сферу.

## 6. Gotchas

- **SimplexOctaveGenerator.noise(...,true) возвращает [-1, 1], не [0, 1].** При использовании в `TerrainPlanetGenerator` или кастомном генераторе нормализуй: `double n = (gen.noise(...) + 1.0) * 0.5;` иначе terrain просядет ниже baseY. См. `feedback_simplex_noise_normalize` в memory.
- **spawnY** должен быть **выше** `baseY + heightRange + 10`. Иначе игрок при первом телепорте приземлится в terrain или умрёт от падения.
- **Не использовать яркие `*_CONCRETE`** в палитре реалистичных планет — они смотрятся пластиково. Для естественных поверхностей предпочесть `TERRACOTTA`, `SAND`, `STONE`, `PACKED_ICE`, `BASALT`.
- **При изменении генератора** существующего мира: удалить `data/planet_<id>/` целиком перед рестартом, иначе старые чанки сохранятся (paper не регенерирует существующие чанки).
- **Сделать backup перед удалением мира** в `/home/mc/minecraft/backups/<dated>.tar.gz` — backup discipline.
- **Мини-сфера**: при добавлении новой планеты обязательно удалить `data/plugins/Galaxy/.spheres-built` чтобы `SphereBuilder` пересобрал. Существующие мини-сферы тоже пересоберутся — это нормально, занимает <1 секунды.
- **Координаты мини-сфер не должны пересекаться** — проверить `planets.yml`, расстояние между центрами должно быть `r1 + r2 + 5` минимум. Звезда в (0,100,0) занимает r=18.
- **Терминология пользователя**: «в космосе / мини-сфера» = шар в `space`, «планета X» сама по себе = реальный мир `planet_X`. См. `feedback_galaxy_terminology` в memory.

## 7. Откат если что-то пошло не так

```bash
docker stop paper
# Восстановить space из backup
rm -rf /home/mc/minecraft/data/space
tar -xzf /home/mc/minecraft/backups/pre-add-pluto-*.tar.gz -C /home/mc/minecraft/data
# Удалить новый мир-планету (он мусорный)
rm -rf /home/mc/minecraft/data/planet_pluto
# Откатить jar к предыдущей версии (или собрать с git checkout)
cd /home/mc/galaxy-plugin && git stash && gradle shadowJar
cp build/libs/Galaxy-0.1.0.jar /home/mc/minecraft/data/plugins/Galaxy-0.1.0.jar
docker start paper
```
