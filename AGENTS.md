# Repository Guidelines

## Project Structure & Module Organization
- Source code lives in `src/main/java/ua/ndmik/bot/...` (Spring Boot app, Telegram bot, JPA models, repositories).
- Resources are in `src/main/resources/` with `application.yml` and the SQLite file at `src/main/resources/db/app.db`.
- Tests live in `src/test/java/...` (currently a single `*Tests` class).
- Gradle wrapper scripts are at the repo root: `./gradlew`, `gradlew.bat`.

## Build, Test, and Development Commands
- `./gradlew build` builds the project and runs tests.
- `./gradlew test` runs JUnit 5 tests only.
- `./gradlew bootRun` starts the Spring Boot app locally on port `8080`.
- `./gradlew clean` clears build outputs under `build/`.

## Coding Style & Naming Conventions
- Java with 4-space indentation; match existing formatting in `src/main/java/...`.
- Packages are lowercase (`ua.ndmik.bot`), classes are `PascalCase`, methods and fields are `camelCase`.
- Lombok annotations are used for boilerplate (`@Getter`, `@Setter`, `@Builder`); avoid hand-written accessors unless needed.
- No formatter or linter is configured; keep changes small and consistent with nearby code.

## Testing Guidelines
- Testing uses JUnit 5 via `spring-boot-starter-test`.
- Test classes follow `*Tests` naming (see `src/test/java/ua/ndmik/bot/DtekTelegramBotApplicationTests.java`).
- No explicit coverage thresholds are enforced; add focused tests for new behavior.

## Commit & Pull Request Guidelines
- Git history is not available in this workspace, so no established commit message convention can be inferred.
- Use clear, imperative commit subjects (e.g., "Fix SQLite column mapping").
- PRs should include a concise description, rationale, and any behavioral changes; link issues if relevant.
- Include screenshots/log excerpts when UI or runtime behavior changes.

## Configuration & Data Notes
- Database path and schema settings are in `src/main/resources/application.yml`.
- `spring.jpa.hibernate.ddl-auto` is set to `create`, so local runs will recreate schema; adjust if you need persistence.
