package ua.ndmik.bot.service;

import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class MessageFormatter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    //TODO: create methods for different messages to notify about shutdowns
    public String format(Map<LocalTime, LocalTime> intervals) {
        if (intervals.isEmpty()) {
            return "✅ <b>Відключень не заплановано</b>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("⚡ <b>Графік відключень світла</b>\n");
        sb.append("🕒 <i>Час місцевий</i>\n\n");

        intervals.forEach((start, end) -> {
            sb.append("🔌 ")
                    .append(formatTime(start))
                    .append(" — ")
                    .append(formatTime(end))
                    .append(" <i>(")
                    .append(formatDurationMinutes(start, end))
                    .append(")</i>")
                    .append('\n');
        });

        return sb.toString();
    }

    private static String formatTime(LocalTime time) {
        return "<b>" + time.format(TIME_FORMATTER) + "</b>";
    }

    private static String formatDurationMinutes(LocalTime start, LocalTime end) {
        int startMinutes = start.getHour() * 60 + start.getMinute();
        int endMinutes = end.getHour() * 60 + end.getMinute();
        // Treat 00:00 as end of day when interval crosses midnight.
        if (endMinutes == 0 && startMinutes > 0) {
            endMinutes = 24 * 60;
        }
        int minutes = Math.max(0, endMinutes - startMinutes);
        int hoursPart = minutes / 60;
        int minutesPart = minutes % 60;

        if (hoursPart == 0) {
            return minutesPart + " хв";
        }
        if (minutesPart == 0) {
            return hoursPart + " год";
        }
        return hoursPart + " год " + minutesPart + " хв";
    }
}
