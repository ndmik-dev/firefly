# Firefly

Telegram bot (Spring Boot) that fetches DTEK outage schedules for Kyiv and Kyiv region, stores user settings in SQLite, and sends schedule updates.

## Stack
- Java 25
- Spring Boot 4.0.1
- SQLite + Spring Data JPA + Flyway
- Telegram Bots API (long polling)
- Playwright + Jsoup for DTEK data extraction

## Bot Features
- `/start` with inline menu and per-user settings persistence.
- Manual group selection by region:
  - `🏙️ Київ`
  - `🏘️ Київщина`
- Group auto-detection by address via YASNO:
  - trigger: `📍 Знайти групу за адресою (Київ)`
  - supported formats: `вул. Хрещатик, 22` or `вул. Хрещатик 22`
  - current scope: **only city Kyiv addresses**
  - resolved group is saved for the user with `area=KYIV`
- Notification toggle per user.
- Admin stats commands:
  - `/stats_today`
  - `/stats_week`
- Scheduler:
  - periodic refresh for Kyiv and Kyiv region schedules
  - blocked window: `23:55-00:05` (`Europe/Kyiv`)
  - midnight rollover: moves `tomorrow` schedules into `today`

## Configuration
Spring loads `.env` automatically via:
`spring.config.import: optional:file:.env[.properties]`

Required:
- `TELEGRAM_BOT_TOKEN`: Telegram bot token

Optional:
- `TELEGRAM_ADMIN_CHAT_IDS`: comma-separated chat IDs allowed to run `/stats_*`
- `SPRING_DATASOURCE_URL`: datasource URL  
  default: `jdbc:sqlite:src/main/resources/db/app.db`
- `scheduler.shutdowns.fixed-delay-ms`: polling interval value for refresh scheduler  
  note: despite the name, value is interpreted in **minutes** (`@Scheduled(..., timeUnit = TimeUnit.MINUTES)`)  
  default: `10`
- `scheduler.shutdowns.time-zone`: scheduler timezone  
  default: `Europe/Kyiv`

Database notes:
- Flyway migrations are enabled.
- JPA `ddl-auto` is `none`.

## Run Locally
1. Set env vars (example):

```bash
export TELEGRAM_BOT_TOKEN=your_token_here
```

2. Install Chromium for Playwright (first run only):

```bash
./gradlew playwrightInstallChromium
```

3. Start app:

```bash
./gradlew bootRun
```

App listens on `8080`.

## Build and Test
```bash
./gradlew clean
./gradlew test
./gradlew build
```

## Run Released JAR (`v1.0.0`)
```bash
export TELEGRAM_BOT_TOKEN=your_token_here
java -jar build/libs/firefly-1.0.0.jar
```

## YASNO Address Lookup Script
Script: `scripts/yasno_group_by_address.sh`

Purpose:
- Resolve outage group by address via YASNO API (`street -> house -> group`).
- Works with default `region_id=25`, `dso_id=902`.

Prerequisites:
- `httpie` (`http`)
- `jq`

Usage:
```bash
./scripts/yasno_group_by_address.sh 'вул. Богдана Хмельницького, 11'
./scripts/yasno_group_by_address.sh 'вул. Богдана Хмельницького 11'
./scripts/yasno_group_by_address.sh '<address>' [region_id] [dso_id]
```

Output:
- `stdout`: resolved group id (for example `29.1`)
- `stderr`: debug payloads (`streets`, `houses`, `group`) and resolved IDs

## Docker
Build local image:
```bash
docker build -t firefly:local .
```

Run local container:
```bash
mkdir -p ./db
touch ./db/app.db

docker run --rm \
  -e TELEGRAM_BOT_TOKEN=your_token_here \
  -e SPRING_DATASOURCE_URL=jdbc:sqlite:/app/db/app.db \
  -v "$(pwd)/db:/app/db" \
  -p 8080:8080 \
  firefly:local
```

`docker-compose.yml` is deployment-oriented and uses:
- image `ghcr.io/ndmik-dev/firefly:latest`
- DB mount `/opt/firefly/db:/app/db`

## CI/CD
- `.github/workflows/build.yml`: runs `./gradlew build` on `push` and `pull_request`.
- `.github/workflows/deploy.yml`: on merged PR into `main`, builds `bootJar`, pushes GHCR image, then triggers Dokploy deployment.
