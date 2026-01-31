package ua.ndmik.bot.service;

import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MessageFormatter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    private static final int MINUTES_PER_DAY = 24 * 60;
    public String format(Map<LocalTime, LocalTime> todayShutdowns,
                         Map<LocalTime, LocalTime> tomorrowShutdowns,
                         LocalDate today) {
        StringBuilder sb = new StringBuilder();
        sb.append("💡 <b>Світло буде</b>\n\n");
        sb.append(formatDay("Сьогодні", today, todayShutdowns));
        sb.append('\n');
        sb.append(formatDay("Завтра", today.plusDays(1), tomorrowShutdowns));
        return sb.toString().trim();
    }

    private String formatDay(String label, LocalDate date, Map<LocalTime, LocalTime> shutdowns) {
        StringBuilder sb = new StringBuilder();
        sb.append("📅 <b>")
                .append(label)
                .append("</b>: ")
                .append("<b>")
                .append(date.format(DATE_FORMATTER))
                .append("</b>")
                .append('\n');
        List<LightInterval> intervals = toLightIntervals(shutdowns);
        if (intervals.isEmpty()) {
            sb.append("⚠️ <i>Немає даних</i>\n");
            return sb.toString();
        }
        for (LightInterval interval : intervals) {
            sb.append("✅ ")
                    .append("<b>")
                    .append(formatMinutes(interval.startMinutes, false))
                    .append("</b>")
                    .append("–")
                    .append("<b>")
                    .append(formatMinutes(interval.endMinutes, true))
                    .append("</b>")
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
