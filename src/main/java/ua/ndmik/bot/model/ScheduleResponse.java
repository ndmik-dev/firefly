package ua.ndmik.bot.model;

import java.util.Map;

public record ScheduleResponse(
        Map<String, GroupSchedule> data,
        String update,
        String today
) {}