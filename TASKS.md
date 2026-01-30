# Project Tasks

## 1) Fix immediate build blocker
- Add the missing semicolon in `DtekClient.fetchHtml`.
- Optional: remove unused imports if build treats warnings as errors.

## 2) Make notifications idempotent and transactional
- Set `needToNotify = false` after sending updates and persist.
- Wrap `processShutdowns` in a transaction to avoid partial updates.
- Preserve `needToNotify` when schedule content is unchanged.

## 3) Harden schedule parsing and error handling
- In `ShutdownsResponseProcessor`, handle missing `DisconSchedule.fact` before brace parsing.
- In `DtekClient`, handle 403 explicitly: refresh cookies and retry once; surface a clear error on failure.

## 4) Make schedule ingestion robust to partial/unknown data
- Avoid `.get()` on missing TODAY/TOMORROW; fallback to a safe message.
- In `HourState.resolveState`, handle null/unknown values without crashing the scheduler.

## 5) Clarify “no data” vs “power all day”
- Introduce explicit “no data” signal from `DtekShutdownsService`.
- Update `MessageFormatter` to render “Немає даних” when appropriate.
- Split formatter into specific methods for menu/update/shutdowns as noted in TODOs.

## 6) Refactor TODOs for consistency and config
- Move group-button formatting out of `RegionHandler` into formatter/util.
- Replace hardcoded base URL with an env var in `AppConfig`.
- Add the menu title logic in `TelegramService`.

## 7) Stabilize Playwright cookie retrieval
- Replace fixed `waitForTimeout(50000)` with condition-based waiting and reasonable timeouts.
- Add retry/backoff if cookie fetch fails.

## 8) Tests
- Unit tests for `ShutdownsResponseProcessor.parseSchedule` (missing key, malformed JSON).
- Tests for `DtekShutdownsService` interval merge logic and “no data” cases.
- Tests for `MessageFormatter` output: empty, full-day, and partial schedules.
