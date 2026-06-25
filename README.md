![Platform](https://img.shields.io/badge/platform-Paper%20%7C%20Folia-green.svg)
![Java](https://img.shields.io/badge/java-17%2B-orange.svg)
![Version minecraft](https://img.shields.io/badge/Version_Minecraft_1.18_--_26.x-red.svg)
[![Support me](https://img.shields.io/badge/Support%20me-Ko--fi-ff5f5f?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/erotoro)

# ServerScope

English and Russian user guide for the ServerScope Paper/Folia plugin.

Author: Erotoro

---

# English

## What ServerScope Is

ServerScope is a monitoring and observability plugin for Paper and Folia Minecraft servers. It is designed to give server owners and administrators a clearer view of runtime health, performance behavior, and operational stability through a built-in diagnostics platform.

It helps you answer questions like:
- Why is the server lagging right now?
- Which chunks are heavy?
- Are entities growing too fast?
- Are alerts firing because of a real issue or just noise?
- Which profiled events/plugins look suspicious?

Main features:
- live server metrics
- historical trends and charts (TPS, MSPT, players, entities, chunks)
- chunk and world diagnostics
- basic event and plugin profiling
- alert system
- built-in web dashboard
- JSON API
- in-game admin commands
- SQLite storage

---

## Requirements

- Java 17 or newer (use the Java version your Minecraft release itself requires — Java 17 for 1.18–1.20.4, Java 21 for 1.20.5–1.21.x, Java 25 for 26.x)
- Paper or Folia, Minecraft 1.18 up to the current 26.x line

### Compatibility

ServerScope reads runtime data (TPS, MSPT, chunk and entity counts, Folia schedulers)
through reflection with safe fallbacks, so a single build runs across the whole supported
range without version-specific jars.

| Minecraft | Status |
| --- | --- |
| 1.18.2 | Supported target |
| 1.20.4 | Supported target |
| 1.20.6 | Supported target |
| 1.21.4 / 1.21.11 | Supported target |
| 26.x (year-based) | Supported target |
| other 1.18+ releases | Best-effort support (same stable API surface) |

The plugin is compiled to Java 17 bytecode, so 1.18 is the lowest supported Minecraft
version (it is the oldest release that runs on Java 17). Older versions (1.16 needs Java 8,
1.17 needs Java 16) would require dropping the modern code base and are intentionally not
supported.

---

## How To Install

1. Download the ready release jar.
2. Put `ServerScope-1.1.0.jar` into your server `plugins/` folder.
3. Start the server.
4. Wait until ServerScope creates its folder and config.
5. Stop the server if you want to review the config first.
6. Open:

```text
plugins/ServerScope/config.yml
```

---

## Default Behavior

By default, ServerScope is already useful after the first start.

Enabled by default:
- storage
- collectors
- alerts
- profiling
- web dashboard

Web defaults are safe:
- host: `127.0.0.1`
- port: `8080`
- token: auto-generated per server
- reverse proxy mode: off

That means:
- the dashboard is on by default
- it is local-only by default
- it does not expose itself to the internet unless you change the host/network setup

---

## First Start: What Happens

On first start, ServerScope will:
- create `plugins/ServerScope/config.yml`
- create a unique web token for this server
- start collectors
- start storage
- start diagnostics and alerts
- start the local web dashboard on `127.0.0.1:8080`

You do not need to invent a token manually.

The token is generated automatically and stored in:

```text
plugins/ServerScope/config.yml
```

Look for:

```yml
web:
  auth-token: ...
```

---

## How To Open The Web Dashboard

If you are on the same machine as the server, open:

```text
http://127.0.0.1:8080/
```

Then:
1. Copy the value of `web.auth-token` from `config.yml`
2. Paste it into the dashboard token field
3. Save the token in the UI

If the page opens but data does not load, the token is usually wrong.

---

## Web Config Explained

Default block:

```yml
web:
  enabled: true
  host: 127.0.0.1
  port: 8080
  auth-token: auto-generated-per-server
  cors-enabled: false
  cors-allowed-origin: http://127.0.0.1
  reverse-proxy-enabled: false
  max-requests-per-window: 120
  rate-limit-window-millis: 10000
  max-request-uri-length: 2048
  max-response-bytes: 2097152
```

What each option means:
- `enabled`: turns the built-in web server on or off
- `host`: network interface to bind to
- `port`: dashboard/API port
- `auth-token`: secret token used by the dashboard and API
- `cors-enabled`: allows cross-origin browser requests
- `cors-allowed-origin`: allowed browser origin when CORS is enabled
- `reverse-proxy-enabled`: trust `X-Forwarded-*` headers from your own proxy
- `max-requests-per-window`: per-IP request limit
- `rate-limit-window-millis`: rate-limit window
- `max-request-uri-length`: maximum URI length
- `max-response-bytes`: maximum JSON response size

---

## Minecraft Hosting: Direct Web Panel Setup

If your server is on a Minecraft hosting provider and you want to open the built-in web panel from your browser, you usually need a separate free port for the panel.

Use this checklist:
1. Ask the hosting provider whether your plan allows an extra TCP/HTTP port for plugins.
2. Choose a free port for ServerScope, for example `25656`.
3. Find the public IP or domain of your server in the hosting panel.
4. Put that port into `web.port`.
5. Set `web.host` to `0.0.0.0` if the panel must be reachable from outside the machine.
6. Restart the server.
7. Open the panel in your browser using `http://YOUR_IP_OR_DOMAIN:YOUR_FREE_PORT/`.

Example for direct external access:

```yml
web:
  enabled: true
  host: 0.0.0.0
  port: YOUR_FREE_PORT
  auth-token: auto-generated-per-server
  cors-enabled: false
  cors-allowed-origin: http://YOUR_IP_OR_DOMAIN:YOUR_FREE_PORT/
  reverse-proxy-enabled: false
  max-requests-per-window: 120
  rate-limit-window-millis: 10000
  max-request-uri-length: 2048
  max-response-bytes: 2097152
```

What to put into each important field:
- `host: 0.0.0.0` means "listen on all network interfaces" so the panel can be reached from outside
- `port` must be a free port that the hosting provider allows
- `auth-token: auto-generated-per-server` means ServerScope will generate a unique token for this server automatically
- `cors-allowed-origin` should match the exact address you open in the browser, including the port, if you enable CORS later

Open the panel in your browser:
- if your host gives you an IP, use `http://YOUR_SERVER_IP:FREE_PORT/`
- if your host gives you a domain, use `http://YOUR_DOMAIN:FREE_PORT/`

Where to get the token:
1. Start the server once with ServerScope installed.
2. Open `plugins/ServerScope/config.yml`.
3. Find `web.auth-token`.
4. Copy that value.
5. Open the web panel.
6. Paste the token into the token field in the dashboard.

If you exposed the token publicly, rotate it with:

```text
/serverscope web regenerate-token
```

What to ask your hosting provider if the panel does not open:

```text
I use a Minecraft plugin with a built-in web panel.
Please confirm whether my server can accept external TCP/HTTP connections on port 25636.
If custom ports are blocked, do you provide port forwarding, reverse proxy, or another way to expose a plugin web panel?
```

How to understand common problems:
- page does not open at all: the port is blocked or not forwarded by the host
- page opens but API says `401`: wrong token
- page opens on the same machine but not from your PC or phone: external access is blocked by the host or firewall
- you changed `config.yml` but nothing changed: restart the server or run `/serverscope reload`

Important:
- do not post your token publicly
- direct HTTP access is not encrypted, so HTTPS through a reverse proxy is safer
- if the hosting provider does not allow custom ports, the built-in panel may be unavailable from the internet

---

## Remote Access: Recommended Setup

If you want to open the panel from another machine, the best way is:
- keep ServerScope on a private/local address
- put Nginx or Caddy in front of it
- use HTTPS on the reverse proxy

ServerScope config:

```yml
web:
  enabled: true
  host: 127.0.0.1
  port: 8080
  auth-token: auto-generated-per-server
  cors-enabled: false
  cors-allowed-origin: https://panel.example.com
  reverse-proxy-enabled: true
  max-requests-per-window: 120
  rate-limit-window-millis: 10000
  max-request-uri-length: 2048
  max-response-bytes: 2097152
```

Minimal Nginx example:

```nginx
server {
    listen 443 ssl http2;
    server_name panel.example.com;

    ssl_certificate /path/fullchain.pem;
    ssl_certificate_key /path/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

Do not expose the raw ServerScope HTTP port to the public internet unless you understand the risk.

---

## Commands

Main commands:

```text
/serverscope
/serverscope status
/serverscope reload
/serverscope report
/serverscope findings
/serverscope alerts
/serverscope web regenerate-token
```

What they do:
- `/serverscope`: quick summary
- `/serverscope status`: runtime/module status
- `/serverscope reload`: reload config and runtime
- `/serverscope report`: quick health report
- `/serverscope findings`: active diagnostic findings
- `/serverscope alerts`: active alerts
- `/serverscope web regenerate-token`: create a new web token and reload web runtime

---

## History And Trends

The dashboard includes a **History** page that turns the stored server-health samples into time-series charts:
- TPS and MSPT over time
- players, entities and loaded chunks over time
- min/avg TPS and max MSPT summary for the selected window

Pick the time window (15m, 30m, 1h, 3h, 12h, 24h) from the selector. The data is read back from SQLite, so charts survive restarts as long as the samples are within the retention period.

The same data is available over the JSON API:

```text
GET /api/history?minutes=30&limit=500
```

- `minutes`: window size in minutes (1–1440, default 30)
- `limit`: maximum number of points (default 100, max 500)
- requires the same auth token as the rest of the API

If storage is disabled, the endpoint returns an empty point list instead of failing.

---

## Permissions

- `serverscope.admin`
- `serverscope.command.status`
- `serverscope.command.reload`
- `serverscope.command.report`
- `serverscope.command.findings`
- `serverscope.command.alerts`
- `serverscope.command.web`
- `serverscope.alerts`

Default:
- all admin permissions are `op`
- in-game alert receiving is `op`

---

## What Profiling Does

Profiling watches selected Bukkit/Paper events and builds summaries.

It helps show:
- slow events
- frequent events
- suspicious bursts
- approximate plugin impact

Default profiled events:
- `player_interact`
- `block_break`
- `block_place`
- `entity_damage`
- `inventory_click`
- `creature_spawn`

Important:
- this is not a full JVM profiler
- numbers are approximate
- it is meant for live diagnostics, not deep Java profiling

---

## What Alerts Mean

Alerts are notifications produced by the analyzer.

Examples:
- low TPS
- high MSPT
- too many entities
- hot chunk
- unstable server trend

Current behavior:
- `CRITICAL` can notify admins in-game
- `WARN` stays in console and web UI
- repeated spam was reduced

This is intentional so that admins are not flooded in chat by low-quality warnings.

Alerts are also persisted to storage, so the dashboard shows an **Alert History** panel that
survives restarts. The same data is available over the API:

```text
GET /api/alerts/history?limit=30&severity=WARN&status=ACTIVE
```

If storage is disabled the endpoint simply returns an empty list.

---

## Safe Reload

`/serverscope reload` (and `/serverscope web regenerate-token`) apply config changes with a
rollback safety net: if the new configuration fails to start, ServerScope restores the previous
working runtime instead of disabling the plugin. A bad config edit no longer takes your
monitoring offline — you get a warning in chat and the console, and the last good setup keeps
running.

---

## Storage

ServerScope stores data in SQLite.

Default file:

```text
plugins/ServerScope/serverscope-mvp.db
```

Storage is:
- async
- batched
- bounded

The plugin also prevents SQLite from being configured outside the plugin folder.

---

## How To Rotate The Web Token

Use:

```text
/serverscope web regenerate-token
```

This will:
- generate a new token
- save it to `config.yml`
- reload the runtime

After that:
- old browser/API sessions using the old token stop working
- you must use the new token

---

## Troubleshooting

### The dashboard opens, but data does not load

Usually the token is wrong.

Fix:
1. open `plugins/ServerScope/config.yml`
2. copy `web.auth-token`
3. paste it into the dashboard

### I get `401 Unauthorized`

Your token is missing or incorrect.

### I get `429 Too Many Requests`

You are hitting the API too fast.

Wait a few seconds or reduce polling from external tools.

### The dashboard does not open at all

Check:
- `web.enabled: true`
- `web.host`
- `web.port`
- firewall rules
- host restrictions

### It works locally, but not from another device

Most likely:
- `host` is `127.0.0.1`
- or the port is blocked

### My hosting provider does not allow custom ports

Then the embedded dashboard may not be reachable externally.

Options:
- use it locally only
- use SSH tunnel
- use a reverse proxy/tunnel supported by your environment

---

## Safe Production Advice

- keep `host: 127.0.0.1` unless remote access is really needed
- use a reverse proxy for public access
- use HTTPS externally
- never publish the token
- rotate the token if you think it leaked
- leave CORS disabled unless you know why you need it

---

## Quick Start For A Complete Beginner

If you just want it to work:

1. Download the ready ServerScope jar
2. Put the jar into `plugins/`
3. Start the server
4. Open `plugins/ServerScope/config.yml`
5. Find `web.auth-token`
6. Open `http://127.0.0.1:8080/`
7. Paste the token into the dashboard
8. Use:
   - `/serverscope status`
   - `/serverscope report`
   - `/serverscope findings`
   - `/serverscope alerts`

That is enough to start using the plugin.

---

# Русский

Автор: Erotoro

## Что такое ServerScope

ServerScope это плагин мониторинга и observability для Minecraft-серверов на Paper и Folia. Он создан для того, чтобы владельцы и администраторы сервера могли лучше видеть состояние сервера, его производительность и общую стабильность через встроенную диагностическую платформу.

Он нужен, чтобы быстро понимать:
- почему сервер лагает прямо сейчас
- какие чанки тяжёлые
- не растёт ли количество сущностей слишком быстро
- настоящая ли это проблема или просто шумный warning
- какие события и плагины выглядят подозрительно

Основные функции:
- живые метрики сервера
- исторические тренды и графики (TPS, MSPT, игроки, сущности, чанки)
- диагностика миров и чанков
- базовое профилирование событий и плагинов
- система alert-ов
- встроенная web-панель
- JSON API
- игровые админ-команды
- хранение данных в SQLite

---

## Требования

- Java 17 или новее (используй ту Java, которую требует твоя версия Minecraft — Java 17 для 1.18–1.20.4, Java 21 для 1.20.5–1.21.x, Java 25 для 26.x)
- Paper или Folia, Minecraft 1.18 до текущей линейки 26.x

### Совместимость

ServerScope читает рантайм-данные (TPS, MSPT, счётчики чанков и сущностей, планировщики Folia)
через рефлексию с безопасными fallback-ами, поэтому один и тот же сборочный jar работает на всём
поддерживаемом диапазоне без отдельных сборок под версию.

| Minecraft | Статус |
| --- | --- |
| 1.18.2 | Поддерживаемая цель |
| 1.20.4 | Поддерживаемая цель |
| 1.20.6 | Поддерживаемая цель |
| 1.21.4 / 1.21.11 | Поддерживаемая цель |
| 26.x (year-based) | Поддерживаемая цель |
| прочие 1.18+ релизы | Поддержка по возможности (тот же стабильный API) |

Плагин компилируется в байткод Java 17, поэтому 1.18 — нижняя поддерживаемая версия (это самый
старый релиз на Java 17). Более старые (1.16 требует Java 8, 1.17 — Java 16) потребовали бы отказа
от современной кодовой базы и сознательно не поддерживаются.

---

## Как установить

1. Скачай готовый release jar.
2. Положи `ServerScope-1.1.0.jar` в папку `plugins/`.
3. Запусти сервер.
4. Дождись, пока ServerScope создаст свою папку и конфиг.
5. При желании останови сервер и открой конфиг.
6. Конфиг лежит здесь:

```text
plugins/ServerScope/config.yml
```

---

## Что включено по умолчанию

Сразу после первого запуска уже включены:
- storage
- collectors
- alerts
- profiling
- web-панель

Web по умолчанию настроен безопасно:
- host: `127.0.0.1`
- port: `8080`
- токен: генерируется автоматически для этого сервера
- reverse proxy режим: выключен

Это значит:
- панель включена сразу
- она доступна только локально
- наружу сама по себе она не торчит, пока ты сам это не изменишь

---

## Что происходит при первом запуске

При первом запуске ServerScope:
- создаёт `plugins/ServerScope/config.yml`
- генерирует уникальный web-токен именно для этого сервера
- запускает collectors
- запускает storage
- запускает диагностику и alerts
- запускает локальную web-панель на `127.0.0.1:8080`

Тебе не нужно вручную придумывать токен.

Токен автоматически записывается в:

```text
plugins/ServerScope/config.yml
```

Ищи здесь:

```yml
web:
  auth-token: ...
```

---

## Как открыть web-панель

Если ты находишься на той же машине, где работает сервер, открой:

```text
http://127.0.0.1:8080/
```

Дальше:
1. открой `config.yml`
2. скопируй `web.auth-token`
3. вставь его в поле токена в панели
4. сохрани токен в UI

Если страница открывается, но данные не грузятся, почти всегда причина в неправильном токене.

---

## Что значит каждый web-параметр

Текущий дефолтный блок:

```yml
web:
  enabled: true
  host: 127.0.0.1
  port: 8080
  auth-token: auto-generated-per-server
  cors-enabled: false
  cors-allowed-origin: http://127.0.0.1
  reverse-proxy-enabled: false
  max-requests-per-window: 120
  rate-limit-window-millis: 10000
  max-request-uri-length: 2048
  max-response-bytes: 2097152
```

Что это значит:
- `enabled`: включает или выключает встроенный web-сервер
- `host`: на какой сетевой интерфейс он слушает
- `port`: порт панели и API
- `auth-token`: секретный токен для панели и API
- `cors-enabled`: разрешить cross-origin запросы из браузера
- `cors-allowed-origin`: какой origin разрешён при включённом CORS
- `reverse-proxy-enabled`: доверять ли заголовкам `X-Forwarded-*` от твоего reverse proxy
- `max-requests-per-window`: лимит запросов на IP
- `rate-limit-window-millis`: окно для rate limit
- `max-request-uri-length`: максимальная длина URI
- `max-response-bytes`: максимальный размер JSON-ответа

---

## Minecraft-хостинг: как открыть встроенную web-панель

Если сервер стоит на обычном Minecraft-хостинге и ты хочешь открыть встроенную web-панель в браузере, обычно нужен отдельный свободный порт под эту панель.

Делай так:
1. Уточни у хостинга, можно ли открыть дополнительный TCP/HTTP-порт для плагина.
2. Выбери свободный порт, например `25656`.
3. Найди внешний IP или домен сервера в панели хостинга.
4. Укажи этот порт в `web.port`.
5. Поставь `web.host: 0.0.0.0`, если панель должна открываться извне.
6. Перезапусти сервер.
7. Открой панель по адресу `http://IP_ИЛИ_ДОМЕН:СВОБОДНЫЙ_ПОРТ/`.

Пример конфига для прямого внешнего доступа:

```yml
web:
  enabled: true
  host: 0.0.0.0
  port: СВОБОДНЫЙ_ПОРТ
  auth-token: auto-generated-per-server
  cors-enabled: false
  cors-allowed-origin: http://IP_ИЛИ_ДОМЕН:СВОБОДНЫЙ_ПОРТ/
  reverse-proxy-enabled: false
  max-requests-per-window: 120
  rate-limit-window-millis: 10000
  max-request-uri-length: 2048
  max-response-bytes: 2097152
```

Что указывать в важных полях:
- `host: 0.0.0.0` означает, что панель слушает все сетевые интерфейсы и может быть доступна извне
- `port` это отдельный свободный порт, который должен разрешать твой хостинг
- `auth-token: auto-generated-per-server` означает, что плагин сам создаст уникальный токен для этого сервера
- `cors-allowed-origin` должен совпадать с точным адресом панели в браузере, включая порт, если ты потом включишь CORS

Открывай панель в браузере:
- используй `http://IP_СЕРВЕРА:СВОБОДНЫЙ_PORT/`
- если хостинг дал домен, используй `http://DOMAIN_СЕРВЕРА:СВОБОДНЫЙ_PORT/`

Где взять токен:
1. Один раз запусти сервер с установленным ServerScope.
2. Открой `plugins/ServerScope/config.yml`.
3. Найди `web.auth-token`.
4. Скопируй это значение.
5. Открой web-панель.
6. Вставь токен в поле токена в интерфейсе.

Если токен засветился или ты отправил его кому-то, сразу смени его командой:

```text
/serverscope web regenerate-token
```

Что написать в поддержку хостинга, если панель не открывается:

```text
У меня Minecraft-плагин поднимает встроенную web-панель.
Подскажите, доступен ли на моём тарифе внешний TCP/HTTP-порт 25636 для входящих подключений?
Если произвольные порты запрещены, есть ли у вас port forwarding, reverse proxy или другой способ открыть web-панель плагина?
```

Как понять типичную проблему:
- страница вообще не открывается: хостинг не пробросил порт или его режет firewall
- страница открывается, но API отвечает `401`: неверный токен
- на самой машине панель доступна, а с ПК или телефона нет: внешний доступ блокируется хостингом или firewall
- ты поменял `config.yml`, но ничего не изменилось: перезапусти сервер или выполни `/serverscope reload`

Важно:
- не публикуй токен в чатах, скриншотах и тикетах
- прямой HTTP-доступ без HTTPS менее безопасен
- если хостинг не разрешает отдельные порты, встроенная панель может быть недоступна извне

---

## Лучший вариант для удалённого доступа

Если хочешь открывать панель с другого устройства, правильнее всего:
- держать сам ServerScope на локальном/внутреннем адресе
- поставить перед ним Nginx или Caddy
- наружу отдавать уже HTTPS через reverse proxy

Конфиг ServerScope:

```yml
web:
  enabled: true
  host: 127.0.0.1
  port: 8080
  auth-token: auto-generated-per-server
  cors-enabled: false
  cors-allowed-origin: https://panel.example.com
  reverse-proxy-enabled: true
  max-requests-per-window: 120
  rate-limit-window-millis: 10000
  max-request-uri-length: 2048
  max-response-bytes: 2097152
```

Минимальный пример Nginx:

```nginx
server {
    listen 443 ssl http2;
    server_name panel.example.com;

    ssl_certificate /path/fullchain.pem;
    ssl_certificate_key /path/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

Не стоит просто так открывать сырой HTTP-порт ServerScope в интернет.

---

## Команды

Основные команды:

```text
/serverscope
/serverscope status
/serverscope reload
/serverscope report
/serverscope findings
/serverscope alerts
/serverscope web regenerate-token
```

Что делает каждая:
- `/serverscope`: короткая сводка
- `/serverscope status`: статус runtime и модулей
- `/serverscope reload`: перезагрузка конфига и runtime
- `/serverscope report`: быстрый отчёт по состоянию сервера
- `/serverscope findings`: активные диагностические findings
- `/serverscope alerts`: активные alerts
- `/serverscope web regenerate-token`: создать новый web-токен и перезагрузить web runtime

---

## История и тренды

В панели есть страница **История**, которая превращает сохранённые снимки состояния сервера в графики во времени:
- TPS и MSPT во времени
- игроки, сущности и загруженные чанки во времени
- сводка по выбранному периоду: мин./средн. TPS и макс. MSPT

Период выбирается селектором (15м, 30м, 1ч, 3ч, 12ч, 24ч). Данные читаются обратно из SQLite, поэтому графики переживают перезапуск сервера, пока точки не вышли за период хранения (retention).

Те же данные доступны через JSON API:

```text
GET /api/history?minutes=30&limit=500
```

- `minutes`: размер окна в минутах (1–1440, по умолчанию 30)
- `limit`: максимум точек (по умолчанию 100, максимум 500)
- требует тот же auth-токен, что и остальной API

Если storage выключен, эндпоинт возвращает пустой список точек, а не ошибку.

---

## Права

- `serverscope.admin`
- `serverscope.command.status`
- `serverscope.command.reload`
- `serverscope.command.report`
- `serverscope.command.findings`
- `serverscope.command.alerts`
- `serverscope.command.web`
- `serverscope.alerts`

По умолчанию:
- все админ-права для `op`
- игровые alert-уведомления тоже для `op`

---

## Что делает Profiling

Profiling следит за выбранными событиями Bukkit/Paper и строит сводки.

Он помогает увидеть:
- медленные события
- частые события
- подозрительные всплески
- примерное влияние плагинов

По умолчанию профилируются:
- `player_interact`
- `block_break`
- `block_place`
- `entity_damage`
- `inventory_click`
- `creature_spawn`

Важно:
- это не полноценный JVM profiler
- цифры приблизительные
- он нужен для live-диагностики, а не для глубокой Java-профилировки

---

## Что означают Alerts

Alerts это уведомления, которые создаёт анализатор.

Примеры:
- низкий TPS
- высокий MSPT
- слишком много сущностей
- горячий чанк
- нестабильный тренд состояния сервера

Текущее поведение:
- `CRITICAL` может приходить администраторам в игру
- `WARN` остаётся в консоли и web UI
- спам повторяющихся сообщений сильно уменьшен

Это сделано специально, чтобы чат админов не засыпало мусором.

Алерты также сохраняются в storage, поэтому в панели есть блок **История алертов**, который
переживает рестарт. Те же данные доступны через API:

```text
GET /api/alerts/history?limit=30&severity=WARN&status=ACTIVE
```

Если storage выключен, эндпоинт просто возвращает пустой список.

---

## Безопасная перезагрузка

`/serverscope reload` (и `/serverscope web regenerate-token`) применяют изменения конфига с
защитным откатом: если новый конфиг не стартует, ServerScope восстанавливает предыдущий рабочий
runtime, а не отключает плагин целиком. Опечатка в конфиге больше не кладёт мониторинг — ты
получаешь предупреждение в чат и консоль, а последняя рабочая конфигурация продолжает работать.

---

## Storage

ServerScope хранит данные в SQLite.

Файл по умолчанию:

```text
plugins/ServerScope/serverscope-mvp.db
```

Storage здесь:
- асинхронный
- батчевый
- ограниченный по нагрузке

Плагин также не даёт вынести SQLite-файл за пределы папки плагина.

---

## Как поменять web-токен

Используй:

```text
/serverscope web regenerate-token
```

Что произойдёт:
- создастся новый токен
- он запишется в `config.yml`
- runtime перезагрузится

После этого:
- старый токен перестанет работать
- нужно будет использовать новый

---

## Частые проблемы

### Панель открывается, но данные не грузятся

Обычно неверный токен.

Что делать:
1. открыть `plugins/ServerScope/config.yml`
2. скопировать `web.auth-token`
3. вставить его в панель

### Получаю `401 Unauthorized`

Токен отсутствует или неверный.

### Получаю `429 Too Many Requests`

Ты слишком часто дёргаешь API.

Подожди немного или уменьши polling во внешнем инструменте.

### Панель вообще не открывается

Проверь:
- `web.enabled: true`
- `web.host`
- `web.port`
- firewall
- ограничения хостинга

### Локально работает, а с другого устройства нет

Скорее всего:
- стоит `host: 127.0.0.1`
- или порт заблокирован

### Хостинг не разрешает дополнительные порты

Тогда встроенная панель может быть недоступна извне.

Варианты:
- использовать только локально
- использовать SSH tunnel
- использовать reverse proxy/туннель, если это позволяет окружение

---

## Безопасные советы для продакшена

- оставляй `host: 127.0.0.1`, если удалённый доступ не нужен
- для публичного доступа используй reverse proxy
- наружу лучше отдавать только HTTPS
- никому не показывай токен
- если думаешь, что токен утёк, ротируй его
- не включай CORS без причины

---

## Быстрый старт для совсем новичка

Если хочется просто запустить и пользоваться:

1. Скачай готовый jar ServerScope
2. Положи jar в `plugins/`
3. Запусти сервер
4. Открой `plugins/ServerScope/config.yml`
5. Найди `web.auth-token`
6. Открой `http://127.0.0.1:8080/`
7. Вставь токен в панель
8. Используй:
   - `/serverscope status`
   - `/serverscope report`
   - `/serverscope findings`
   - `/serverscope alerts`

Этого уже достаточно, чтобы начать пользоваться плагином.

---

Supported me - https://ko-fi.com/erotoro
Thx <3
