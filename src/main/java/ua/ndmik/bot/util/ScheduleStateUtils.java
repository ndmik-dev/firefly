package ua.ndmik.bot.util;

import ua.ndmik.bot.model.HourState;

import java.util.Map;

public class ScheduleStateUtils {

    public static boolean isAllDayWithPower(Map<String, String> schedule) {
        return schedule.values()
                .stream()
                .allMatch(HourState.YES.getValue()::equals);
    }
}
