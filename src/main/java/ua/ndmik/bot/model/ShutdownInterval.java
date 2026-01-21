package ua.ndmik.bot.model;

import java.time.LocalTime;

public record ShutdownInterval(
        LocalTime start,
        LocalTime end
) {}
