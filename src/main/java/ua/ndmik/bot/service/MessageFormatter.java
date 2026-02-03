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

    //TODO: debug and refactor, rm extra code
    public String format(Map<LocalTime, LocalTime> todayShutdowns,
                         Map<LocalTime, LocalTime> tomorrowShutdowns,
                         LocalDate today) {
        return  "💡 <b>Світло буде</b>\n\n" +
                formatDay("Сьогодні", today, todayShutdowns) +
                '\n' +
                formatDay("Завтра", today.plusDays(1), tomorrowShutdowns);
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
        if (shutdowns == null || shutdowns.isEmpty()) {
            sb.append("⚠️ <i>Графік ще не створено</i>\n");
            return sb.toString();
        }
        List<LightInterval> intervals = toLightIntervals(shutdowns);
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

    private record LightInterval(int startMinutes, int endMinutes) {}
}
