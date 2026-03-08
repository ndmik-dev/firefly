package ua.ndmik.bot.model.schedule;

import java.time.LocalTime;

public record ShutdownInterval(
        LocalTime start,
        LocalTime end
) {}
