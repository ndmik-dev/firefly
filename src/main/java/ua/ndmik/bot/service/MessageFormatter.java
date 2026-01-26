package ua.ndmik.bot.service;

import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MessageFormatter {

    //TODO: refactor class
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MINUTES_PER_DAY = 24 * 60;
    //TODO: create methods for different messages to notify about shutdowns
    public String format(Map<LocalTime, LocalTime> todayShutdowns,
                         Map<LocalTime, LocalTime> tomorrowShutdowns) {
        StringBuilder sb = new StringBuilder();
        sb.append("💡 Світло буде\n");
        sb.append("🕒 Час місцевий\n\n");
        sb.append(formatDay("Сьогодні", todayShutdowns));
        sb.append('\n');
        sb.append(formatDay("Завтра", tomorrowShutdowns));
        return sb.toString().trim();
    }

    private String formatDay(String label, Map<LocalTime, LocalTime> shutdowns) {
        StringBuilder sb = new StringBuilder();
        sb.append("📅 ").append(label).append('\n');
        List<LightInterval> intervals = toLightIntervals(shutdowns);
        if (intervals.isEmpty()) {
            sb.append("🚫 Графік ще не сформований\n");
            return sb.toString();
        }
        for (LightInterval interval : intervals) {
            sb.append("✅ ")
                    .append(formatMinutes(interval.startMinutes, false))
                    .append(" — ")
                    .append(formatMinutes(interval.endMinutes, true))
                    .append('\n');
        }
        return sb.toString();
    }

    private List<LightInterval> toLightIntervals(Map<LocalTime, LocalTime> shutdowns) {
        List<LightInterval> intervals = new ArrayList<>();
        List<Map.Entry<LocalTime, LocalTime>> outages = shutdowns.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
        int cursor = 0;
        for (Map.Entry<LocalTime, LocalTime> outage : outages) {
            int startMinutes = toMinutes(outage.getKey());
            int endMinutes = toMinutes(outage.getValue());
            if (outage.getValue().equals(LocalTime.MIDNIGHT)) {
                endMinutes = MINUTES_PER_DAY;
            }
            if (cursor < startMinutes) {
                intervals.add(new LightInterval(cursor, startMinutes));
            }
            cursor = Math.max(cursor, endMinutes);
        }
        if (cursor < MINUTES_PER_DAY) {
            intervals.add(new LightInterval(cursor, MINUTES_PER_DAY));
        }
        return intervals;
    }

    private String formatMinutes(int minutes, boolean isEnd) {
        if (isEnd && minutes == MINUTES_PER_DAY) {
            return "24:00";
        }
        LocalTime time = LocalTime.of(minutes / 60, minutes % 60);
        return time.format(TIME_FORMATTER);
    }

    private int toMinutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    private static class LightInterval {
        private final int startMinutes;
        private final int endMinutes;

        private LightInterval(int startMinutes, int endMinutes) {
            this.startMinutes = startMinutes;
            this.endMinutes = endMinutes;
        }
    }
}
