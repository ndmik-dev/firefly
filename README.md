# DTEK Telegram Bot

Spring Boot Telegram bot that fetches DTEK shutdown schedules and serves them via a bot API. It uses Playwright (Chromium) to retrieve cookies and then parses the schedule HTML.

## Requirements (Local)
- Java 25
- Gradle (or use `./gradlew`)
- Node is bundled with Playwright; no separate Node install is required

## Configuration
Set the Telegram bot token via environment variable:

```bash
export TELEGRAM_BOT_TOKEN=your_token_here
```

SQLite database is expected at `./db/app.db` (mounted into the container or used locally via `SPRING_DATASOURCE_URL`).

## Run Locally (without Docker)
1. Prepare DB file:

```bash
mkdir -p db
[ -f db/app.db ] || touch db/app.db
```

2. (First time only) Download Playwright Chromium:

```bash
./gradlew playwrightInstallChromium
```

3. Run the app:

```bash
SPRING_DATASOURCE_URL=jdbc:sqlite:./db/app.db \
TELEGRAM_BOT_TOKEN=your_token_here \
./gradlew bootRun
```

## Run Locally (Docker)
1. Create `.env` with the bot token:

```env
TELEGRAM_BOT_TOKEN=your_token_here
```

2. Prepare DB folder:

```bash
mkdir -p db
[ -f db/app.db ] || touch db/app.db
```

3. Start:

```bash
docker compose up --build
```