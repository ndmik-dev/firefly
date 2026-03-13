# Changelog

All notable changes to this project are documented in this file.

## [1.0.0] - 2026-03-13

### Added
- Initial stable Firefly release.
- Telegram bot `/start` flow with inline menu and persisted user settings.
- Manual outage group selection for Kyiv and Kyiv region.
- YASNO-based address-to-group resolution for Kyiv addresses.
- Per-user notification toggle.
- Admin stats commands: `/stats_today`, `/stats_week`.
- Scheduler refresh for outage schedules and midnight rollover handling.

### Technical
- Spring Boot 4.0.1, Java 25.
- SQLite persistence with Spring Data JPA and Flyway migrations.
- DTEK/YASNO integrations using Playwright + Jsoup.

