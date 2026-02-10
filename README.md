# Firefly

Telegram bot built with Spring Boot that fetches DTEK outage schedules and sends updates to users.

## Stack
- Java 25
- Spring Boot 4
- SQLite + Spring Data JPA
- Telegram Bots API (long polling)
- Playwright + Jsoup for DTEK data extraction

## Features
- `/start` flow with interactive inline menu
- Region/group selection and persistence per chat
- Notification toggle per user
- Scheduled refresh of outage schedules
- Daily rollover of `tomorrow` schedules into `today`

## Configuration
Spring also loads `.env` automatically (`spring.config.import: optional:file:.env[.properties]`).

Required:
- `TELEGRAM_BOT_TOKEN` - Telegram bot token

Optional:
- `SPRING_DATASOURCE_URL` - overrides DB URL (default: `jdbc:sqlite:src/main/resources/db/app.db`)
- `scheduler.shutdowns.fixed-delay-ms` - schedule polling interval in minutes (default: `10`)

## Run Locally
1. Set token in `.env` or export it:

```bash
export TELEGRAM_BOT_TOKEN=your_token_here
```

2. Install Chromium for Playwright (first run only):

```bash
./gradlew playwrightInstallChromium
```

3. Start the app:

```bash
./gradlew bootRun
```

The app starts on port `8080`.

## Build and Test
```bash
./gradlew clean
./gradlew test
./gradlew build
```

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

Note: `docker-compose.yml` is currently deployment-oriented and references `ghcr.io/ndmik-dev/firefly:latest` with host volume `/opt/firefly/db:/app/db`.

## CI/CD
- `.github/workflows/build.yml`: build on pushes to `main` (currently marked as temporary in the file comment).
- `.github/workflows/deploy.yml`: on pushes to `dev`, builds `bootJar`, builds and pushes `ghcr.io/${{ github.repository }}:latest`, then triggers Dokploy deployment.
