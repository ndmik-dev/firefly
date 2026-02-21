package ua.ndmik.bot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.ndmik.bot.model.entity.DailyStats;
import ua.ndmik.bot.repository.DailyStatsRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;

@Service
public class StatsService {

    private final DailyStatsRepository dailyStatsRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final ZoneId zoneId;

    public StatsService(DailyStatsRepository dailyStatsRepository,
                        UserSettingsRepository userSettingsRepository,
                        @Value("${scheduler.shutdowns.time-zone:Europe/Kyiv}") String timeZone) {
        this.dailyStatsRepository = dailyStatsRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.zoneId = ZoneId.of(timeZone);
    }

    @Transactional
    public synchronized void recordNewUser() {
        incrementTodayStats(stats -> stats.setNewUsers(stats.getNewUsers() + 1));
    }

    @Transactional
    public synchronized void recordNotificationSent() {
        incrementTodayStats(stats -> stats.setNotificationsSent(stats.getNotificationsSent() + 1));
    }

    @Transactional
    public synchronized void recordNotificationFailed() {
        incrementTodayStats(stats -> stats.setNotificationsFailed(stats.getNotificationsFailed() + 1));
    }

    private void incrementTodayStats(Consumer<DailyStats> mutator) {
        DailyStats stats = getOrCreate(today());
        mutator.accept(stats);
        dailyStatsRepository.save(stats);
    }

    @Transactional(readOnly = true)
    public String buildTodayStatsMessage() {
        LocalDate date = today();
        DailyStats stats = dailyStatsRepository.findById(date)
                .orElseGet(() -> DailyStats.builder().statDate(date).build());
        return buildSingleDayMessage(date, stats);
    }

    @Transactional(readOnly = true)
    public String buildWeeklyStatsMessage() {
        LocalDate to = today();
        LocalDate from = to.minusDays(6);
        List<DailyStats> weeklyStats = dailyStatsRepository.findByStatDateBetweenOrderByStatDateAsc(from, to);

        long newUsers = weeklyStats.stream().mapToLong(DailyStats::getNewUsers).sum();
        long sent = weeklyStats.stream().mapToLong(DailyStats::getNotificationsSent).sum();
        long failed = weeklyStats.stream().mapToLong(DailyStats::getNotificationsFailed).sum();

        return buildAggregatedMessage(from, to, newUsers, sent, failed);
    }

    private String buildSingleDayMessage(LocalDate date, DailyStats stats) {
        long sent = stats.getNotificationsSent();
        long failed = stats.getNotificationsFailed();
        long attempts = sent + failed;
        double successRate = attempts == 0
                ? 100.0
                : (sent * 100.0) / attempts;

        long totalUsers = userSettingsRepository.count();
        long usersWithGroup = userSettingsRepository.countByGroupIdIsNotNull();
        long usersWithNotifications = userSettingsRepository.countByIsNotificationEnabledTrue();

        return """
                📊 <b>Статистика %s</b>
                📅 Дата: <b>%s</b>

                👤 Нові користувачі: <b>%d</b>
                🔔 Відправлено сповіщень: <b>%d</b>
                ❌ Невдалі спроби: <b>%d</b>
                ✅ Успішність: <b>%.1f%%</b>

                👥 Користувачів всього: <b>%d</b>
                🧩 Обрали групу: <b>%d</b>
                🔔 Увімкнули сповіщення: <b>%d</b>
                """.formatted(
                "за сьогодні",
                date,
                stats.getNewUsers(),
                sent,
                failed,
                successRate,
                totalUsers,
                usersWithGroup,
                usersWithNotifications
        );
    }

    private String buildAggregatedMessage(LocalDate from, LocalDate to, long newUsers, long sent, long failed) {
        long attempts = sent + failed;
        double successRate = attempts == 0
                ? 100.0
                : (sent * 100.0) / attempts;

        return """
                📈 <b>Статистика за 7 днів</b>
                📅 Період: <b>%s — %s</b>

                👤 Нові користувачі: <b>%d</b>
                🔔 Відправлено сповіщень: <b>%d</b>
                ❌ Невдалі спроби: <b>%d</b>
                ✅ Успішність: <b>%.1f%%</b>
                """.formatted(from, to, newUsers, sent, failed, successRate);
    }

    private DailyStats getOrCreate(LocalDate date) {
        return dailyStatsRepository.findById(date)
                .orElseGet(() -> DailyStats.builder()
                        .statDate(date)
                        .newUsers(0)
                        .notificationsSent(0)
                        .notificationsFailed(0)
                        .build());
    }

    private LocalDate today() {
        return LocalDate.now(zoneId);
    }
}
