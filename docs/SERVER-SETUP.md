# Galaxy Minecraft Server — руководство по развёртыванию с нуля

Документ описывает, как воссоздать сервер «Галактика» (Paper 1.21.8 в Docker) на новом VPS. Текущий сервер (Hetzner, Ubuntu 24.04) будет удалён — этот документ является единственным источником для восстановления инфраструктуры (данные миров восстанавливаются из бэкапов, см. раздел 8).

**Все секреты в документе заменены плейсхолдерами вида `<RCON_PASSWORD>` — реальные значения хранить отдельно (менеджер паролей).**

---

## 1. Требования к VPS

- **ОС:** Ubuntu 24.04 LTS (текущий сервер — Hetzner).
- **RAM:** минимум 8 GB (контейнер Paper использует `MEMORY: 5G` + оверхед JVM/ОС).
- **CPU:** 2–4 vCPU.
- **Диск:** 40+ GB SSD (миры + бэкапы).
- **ПО на хосте:**
  - Docker (текущая версия: **Docker 29.5.3**). Java на хост ставить **не нужно** — она внутри образа `itzg/minecraft-server:java21`.
  - `fail2ban` (см. раздел 7).
  - Для сборки Galaxy-плагина на этой же машине: Gradle + JDK 21 (OpenJDK 21). Можно собирать и на dev-машине и копировать jar.
- **Пользователь:** непривилегированный `mc`, домашний каталог `/home/mc`. Sudo — по паролю (не оставлять NOPASSWD-костыли в `/etc/sudoers.d/`).
- **Открытые порты:** 22 (SSH), 25565 (Minecraft). RCON-порт 25575 наружу **не** пробрасывается (доступ только через `docker exec paper rcon-cli`).

Структура каталогов:

```
/home/mc/
├── minecraft/
│   ├── docker-compose.yml
│   ├── data/            ← том контейнера: миры, plugins/, configs
│   └── backups/         ← датированные tarball'ы
├── galaxy-plugin/       ← исходники кастомного плагина (git-репо)
└── docs/
```

## 2. docker-compose и первый запуск

Образ — `itzg/minecraft-server`. **Ключевая особенность:** образ регенерирует `server.properties` из env-переменных compose-файла при **каждом** старте контейнера. Любые правки `server.properties` sed-ом затираются — все настройки менять только через `docker-compose.yml` + `docker compose up -d --force-recreate`.

`/home/mc/minecraft/docker-compose.yml` (полностью, секретов не содержит):

```yaml
services:
  paper:
    image: itzg/minecraft-server:java21
    container_name: paper
    restart: unless-stopped
    ports:
      - "25565:25565"
    environment:
      EULA: "TRUE"
      TYPE: "PAPER"
      VERSION: "1.21.8"
      MEMORY: "5G"
      USE_AIKAR_FLAGS: "true"
      DIFFICULTY: "normal"
      MODE: "survival"
      ONLINE_MODE: "FALSE"
      MOTD: "Galaxy Server [DEV]"
      VIEW_DISTANCE: "12"
      SIMULATION_DISTANCE: "6"
      LEVEL: "planet_earth"
      TZ: "Asia/Almaty"
    volumes:
      - ./data:/data
    tty: true
    stdin_open: true
```

Первый запуск:

```bash
mkdir -p /home/mc/minecraft/data
cd /home/mc/minecraft
docker compose up -d
docker logs paper -f     # дождаться "Done (...s)!"
```

### server.properties — важные поля

Часть полей не покрывается env-переменными compose и настраивается отдельно (через env `RCON_PASSWORD`/`ENABLE_WHITELIST` itzg-образа или правку после остановки — предпочтительно env):

| Поле | Значение | Комментарий |
|---|---|---|
| `level-name` | `planet_earth` | задаётся через env `LEVEL` |
| `online-mode` | `false` | обязательно для схемы AuthMe + FastLogin |
| `white-list` | `true` | вход только по whitelist |
| `enable-rcon` | `true`, порт 25575 | пароль — `<RCON_PASSWORD>`, наружу не открывать |
| `resource-pack` | `https://bkarimzhan.github.io/galaxy-resourcepack/pack.zip` | GitHub Pages, см. раздел 6 |
| `resource-pack-sha1` | `<SHA1 текущего pack.zip>` | обновляется при каждом изменении пака (`sha1sum pack.zip`) |
| `resource-pack-prompt` | `{"text":"Скачать Resource Pack для сервера Галактика?","color":"yellow"}` | |
| `require-resource-pack` | `false` | |

Управление сервером:

```bash
docker exec paper rcon-cli "<команда>"   # RCON
docker logs paper --tail 100 -f          # логи
docker restart paper                     # рестарт
```

## 3. Установка плагинов

Плагины кладутся jar-файлами в `/home/mc/minecraft/data/plugins/` при остановленном (или с последующим рестартом) сервере. Источники: Modrinth / Hangar / SpigotMC / страницы проектов.

Текущий рабочий стек (проверен на Paper 1.21.8):

| Плагин | Версия | Назначение |
|---|---|---|
| AuthMe | 5.7.0 | регистрация/логин cracked-игроков |
| FastLogin (Bukkit) | 1.12-SNAPSHOT-65a379c (тег `1.12-kick-toggle` репо TuxCoding/FastLogin — обычного релиза 1.12 может не быть) | автологин premium-игроков через Mojang sessionserver |
| ProtocolLib | 5.4.0 | зависимость FastLogin |
| LuckPerms (Bukkit) | 5.5.42 | права |
| Vault | 1.7.3 | economy/permissions API |
| EssentialsX | 2.21.2 | базовые команды, экономика |
| EssentialsXChat | 2.21.2 | чат |
| Multiverse-Core | 5.6.1 | управление мирами (nether/end для planet_earth) |
| FastAsyncWorldEdit (Paper) | 2.15.0 | редактирование мира |
| WorldGuard | 7.0.14 | защита регионов |
| PlaceholderAPI | 2.11.7 | плейсхолдеры для меню |
| EconomyShopGUI (free) | 7.0.3 | магазин |
| DeluxeMenus | 1.14.1 | GUI-меню (galaxy_map) |
| **Galaxy** | 0.1.0 | кастомный плагин, см. раздел 4 |

Порядок установки: сначала библиотеки-зависимости (ProtocolLib, Vault, PlaceholderAPI, LuckPerms), затем остальное, рестарт после каждой пачки. **Multiverse-NetherPortals не ставить** — новый дизайн миров без nether-связок.

Точные версии, источники сборок и обоснование стека (почему Paper именно 1.21.8, почему ProtocolLib 5.4.0, а не dev-build) — в `docs/memory/project_working_stack.md`.

### Ключевые отличия конфигов от дефолтов

После первого запуска плагины генерируют дефолтные конфиги в `data/plugins/<Имя>/`. Изменить:

**AuthMe** (`plugins/AuthMe/config.yml`):
- `DataSource.backend: SQLITE` (не MySQL; поля mySQL* игнорируются).
- `settings.sessions.enabled: false` — сессии выключены, логин при каждом входе.
- `settings.restrictions.maxRegPerIp: 3`, `kickNonRegistered: true`, `kickOnWrongPassword: true`.
- Регистрация игроков — только админом через RCON (`authme register`), см. раздел 5.

**FastLogin** (`plugins/FastLogin/config.yml`):
- `autoRegister: true`, `autoLogin: true`, `premiumUuid: true`, `forwardSkin: true` — premium-игроки входят без пароля с настоящим UUID и скином.
- `driver: sqlite` (локальная `FastLogin.db`).
- `premium-warning: false`.

**Multiverse** (`plugins/Multiverse-Core/worlds.yml`):
- Управляет тройкой `planet_earth` / `planet_earth_nether` / `planet_earth_the_end`. Миры `space`, `planet_mars` и другие планеты создаёт и владеет ими Galaxy-плагин (без Multiverse).
- **Важно (MV 5.6+):** `mv modify <world> set spawn` не работает. Точка спавна правится напрямую в `worlds.yml` (блок `spawn-location`) при остановленном сервере. Текущий спавн `planet_earth`: `x: -96, y: 66, z: 32`; seed мира: `996381052428111964` (нужен только если пересоздавать мир без бэкапа).

**EconomyShopGUI** (`plugins/EconomyShopGUI/`): стандартные секции/шопы (`sections/*.yml`, `shops/*.yml`), кастомизация минимальна — при переносе достаточно скопировать каталог из бэкапа.

**DeluxeMenus** (`plugins/DeluxeMenus/gui_menus/`): содержит `galaxy_map.yml` (карта галактики; на момент фиксации — заготовка на базе дефолтного меню, `open_command: menu`) + примерные меню. Копировать каталог из бэкапа.

## 4. Деплой Galaxy-плагина

Исходники: `/home/mc/galaxy-plugin/` (git-репозиторий). Стек: Java 21, Gradle Kotlin DSL, Paper API `1.21.8-R0.1-SNAPSHOT`, shadow-плагин `com.gradleup.shadow` 8.3.5.

```bash
cd /home/mc/galaxy-plugin
gradle shadowJar
# выходной jar: build/libs/Galaxy-0.1.0.jar

# бэкап предыдущей версии, затем деплой:
cp /home/mc/minecraft/data/plugins/Galaxy-0.1.0.jar \
   /home/mc/minecraft/backups/Galaxy-0.1.0.jar.$(date +%Y%m%d-%H%M%S).bak 2>/dev/null || true
cp build/libs/Galaxy-0.1.0.jar /home/mc/minecraft/data/plugins/
docker restart paper
```

Плагин на первом запуске сам создаёт свои миры (`space` — void с мини-сферами планет, `planet_mars` и т.д.) и владеет ими через `Bukkit.createWorld` — регистрировать их в Multiverse не нужно. Идемпотентность построек (сферы) обеспечивается флаг-файлами в data folder плагина — `plugins/Galaxy/` тоже переносить из бэкапа.

Механика: worldborder 1000 на планетах, подъём выше Y=300 → телепорт в `space`, подлёт к мини-сфере → телепорт на планету, `/ship` — корабль, `/leaveplanet` — fallback.

## 5. Аутентификация и добавление игроков

Схема: `online-mode=false` + `white-list=true` + **AuthMe** (cracked) + **FastLogin** (premium). FastLogin проверяет ник через Mojang sessionserver: лицензионные игроки входят автоматически с premium-UUID, пиратские — регистрируются в AuthMe и вводят `/login <пароль>`.

Текущие игроки (пароли — плейсхолдеры):

| Ник | Тип | Auth | Op |
|---|---|---|---|
| `kz_bazelevs` | premium (Mojang) | FastLogin, без пароля | level 4 |
| `Leekuan16` | cracked | AuthMe, пароль `<LEEKUAN16_PASSWORD>` | level 4 |

**Добавление новых игроков — только через RCON:**

```bash
# cracked-игрок:
docker exec paper rcon-cli "authme register <ник> <пароль>"
docker exec paper rcon-cli "whitelist add <ник>"
docker exec paper rcon-cli "op <ник>"        # если нужен op

# premium-игрок:
docker exec paper rcon-cli "whitelist add <ник>"
# ВАЖНО: op давать ТОЛЬКО после первого входа игрока,
# иначе в ops.json запишется offline-UUID и op не сработает.
```

При переносе сервера скопировать из бэкапа: `data/whitelist.json`, `data/ops.json`, `data/plugins/AuthMe/` (SQLite-база паролей), `data/plugins/FastLogin/FastLogin.db`.

## 6. Ресурспак (GitHub Pages)

Пак хостится в репозитории **`bkarimzhan/galaxy-resourcepack`** на GitHub Pages:

- URL пака: `https://bkarimzhan.github.io/galaxy-resourcepack/pack.zip`
- В `server.properties`: `resource-pack` = URL выше, `resource-pack-sha1` = SHA-1 актуального zip.

Процесс обновления пака:

1. Изменить содержимое, пересобрать `pack.zip` (zip корня пака, `pack.mcmeta` в корне архива).
2. `sha1sum pack.zip` → новый SHA-1.
3. Закоммитить и запушить в репо (auth — GitHub PAT `<GITHUB_PAT>`; не оставлять PAT в remote-URL после пуша).
4. Подождать обновления CDN GitHub Pages (проверять `curl -sI <url>` / скачиванием и сверкой sha1 — CDN кэширует, может занять несколько минут).
5. Обновить `resource-pack-sha1` на сервере и перезапустить контейнер. Если SHA-1 не совпадает с фактическим файлом, клиенты будут перекачивать пак при каждом входе или отклонять его.

## 7. Безопасность VPS

Главный принцип: **все правки sshd / fail2ban / sysctl — только в drop-in каталогах `*.d/*.conf`**, не в основных конфигах (основные затираются при apt-обновлениях).

### fail2ban

`/etc/fail2ban/jail.d/custom.conf`:

```ini
[sshd]
enabled = true
backend = systemd
maxretry = 6
findtime = 10m
bantime = 10m

[recidive]
enabled = true
backend = polling
maxretry = 3
findtime = 1d
bantime = 1w
```

`/etc/fail2ban/jail.d/00-ignoreip.local` — whitelist доверенных IP (обновляется скриптом, вручную не править):

```ini
[DEFAULT]
ignoreip = 127.0.0.1/8 ::1 <ДОВЕРЕННЫЕ_IP_АДМИНА>
```

**Урок:** не опрашивать SSH чаще раза в 10 секунд (скрипты/мониторинг) — fail2ban забанит даже успешные подключения по частоте попыток. Админский IP держать в `ignoreip`.

### sshd drop-ins (`/etc/ssh/sshd_config.d/`)

`99-hardening.conf`:

```
X11Forwarding no
```

`20-tunnel-keepalive.conf`:

```
ClientAliveInterval 30
ClientAliveCountMax 3
```

Рекомендуется также: вход только по ключам (`PasswordAuthentication no`), `PermitRootLogin no` — добавить в тот же `99-hardening.conf` на новом VPS после раскладки ключей.

### ufw

```bash
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp
ufw allow 25565/tcp
ufw enable
```

### swap

На VPS с 8 ГБ RAM (Paper 5G + gradle-сборки) нужен swap 2 ГБ:

```bash
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
echo 'vm.swappiness=10' > /etc/sysctl.d/99-swap.conf && sysctl --system
```

### Прочее

- RCON (25575) не публиковать наружу — в compose проброшен только 25565.
- Не держать NOPASSWD-sudo для пользователя `mc` (на старом сервере был временный `/etc/sudoers.d/mc-nopasswd-temp` — на новом не создавать).
- Детали настройки старого VPS (включая исходные значения) — в `docs/memory/vps_server.md`.

## 8. Бэкапы

Каталог: `/home/mc/minecraft/backups/`.

**Дисциплина:** перед **любой** state-changing операцией (установка/обновление плагина, правка конфига, изменение/удаление мира) — датированный снапшот:

```bash
# полный снапшот (сервер желательно остановить или выполнить save-off/save-all):
docker exec paper rcon-cli "save-off" && docker exec paper rcon-cli "save-all flush"
tar -czf /home/mc/minecraft/backups/pre-<операция>-$(date +%Y%m%d-%H%M%S).tar.gz \
    -C /home/mc/minecraft data
docker exec paper rcon-cli "save-on"

# точечный бэкап одного файла:
cp <файл> /home/mc/minecraft/backups/<имя>.$(date +%Y%m%d-%H%M%S).bak
```

Восстановление на новом VPS: развернуть по разделам 1–2, остановить контейнер, распаковать последний полный tarball в `/home/mc/minecraft/` (каталог `data/`), запустить контейнер. Бэкапы обязательно копировать **за пределы** VPS (scp/rclone на другую машину или объектное хранилище) — иначе удаление сервера уничтожит и их.

> **ВНИМАНИЕ (актуально на 2026-07-06):** лежащие в `backups/` tarball'ы устарели. Последний полный снапшот `data/` — `pre-terrain-rewrite-20260506` (2026-05-06); он старше пересоздания Марса (2026-05-08) и ~2 месяцев игры. Бэкапы от 2026-05-08 покрывают только `planet_mars` и `space`. Ни один бэкап не скопирован за пределы VPS.

**Обязательный финальный шаг перед сносом сервера** — свежий полный снапшот и вывоз с VPS:

```bash
docker exec paper rcon-cli "save-off" && docker exec paper rcon-cli "save-all flush"
tar -czf /home/mc/minecraft/backups/final-$(date +%Y%m%d-%H%M%S).tar.gz -C /home/mc/minecraft data docker-compose.yml
docker exec paper rcon-cli "save-on"
# затем с ЛОКАЛЬНОЙ машины:
scp mc@<IP>:/home/mc/minecraft/backups/final-*.tar.gz .
```

Что критично сохранить перед удалением старого сервера:

- полный `tar` каталога `/home/mc/minecraft/data/` (миры, plugins с конфигами и базами AuthMe/FastLogin, whitelist.json, ops.json);
- `docker-compose.yml`;
- git-репо `/home/mc/galaxy-plugin/` (запушить на удалённый remote);
- репо `galaxy-resourcepack` (уже на GitHub);
- секреты: `<RCON_PASSWORD>`, пароль AuthMe игрока Leekuan16, GitHub PAT — в менеджер паролей;
- drop-in конфиги из `/etc/fail2ban/jail.d/` и `/etc/ssh/sshd_config.d/` (содержимое приведено в разделе 7).
