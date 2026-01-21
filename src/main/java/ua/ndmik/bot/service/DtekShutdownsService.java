package ua.ndmik.bot.service;

import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import ua.ndmik.bot.model.HourState;
import ua.ndmik.bot.model.ShutdownInterval;
import ua.ndmik.bot.model.entity.Schedule;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DtekShutdownsService {

    private final JsonMapper mapper;

    public DtekShutdownsService(JsonMapper mapper) {
        this.mapper = new JsonMapper();
    }

    public Map<LocalTime, LocalTime> getShutdowns(Schedule schedule) {
        Map<String, String> scheduleMap = mapper.readValue(schedule.getSchedule(), new TypeReference<>() {});
        if (isScheduleEmpty(scheduleMap)) {
            return Map.of();
        }
        return getShutdownIntervals(scheduleMap);
    }

    private boolean isScheduleEmpty(Map<String, String> schedule) {
        return schedule
                .values()
                .stream()
                .allMatch(HourState.YES.getValue()::equals);
    }

    private Map<LocalTime, LocalTime> getShutdownIntervals(Map<String, String> hourStateMap) {
        ArrayList<ShutdownInterval> shutdownIntervals = new ArrayList<>();
        for (int hour = 1; hour <= 24; hour++) {
            HourState state = HourState.resolveState(hourStateMap.get(String.valueOf(hour)));
            ShutdownInterval shutdown = toShutdownInterval(hour, state);

            if (shutdown == null) {
                continue;
            }

            if (shutdownIntervals.isEmpty()) {
                shutdownIntervals.add(shutdown);
                continue;
            }

            ShutdownInterval lastInterval = shutdownIntervals.getLast();
            if (!shutdown.start().isAfter(lastInterval.end())) {
                LocalTime newEnd = max(shutdown.end(), lastInterval.end());
                shutdownIntervals.removeLast();
                shutdownIntervals.addLast(new ShutdownInterval(lastInterval.start(), newEnd));
            } else {
                shutdownIntervals.add(shutdown);
            }
        }

        Map<LocalTime, LocalTime> result = new LinkedHashMap<>();
        for (ShutdownInterval interval : shutdownIntervals) {
            result.put(interval.start(), interval.end());
        }
        return result;
    }

    private ShutdownInterval toShutdownInterval(int hour, HourState state) {
        LocalTime hourStart = LocalTime.of((hour - 1) % 24, 0);
        LocalTime hourEnd = (hour == 24)
                ? LocalTime.MIDNIGHT
                : LocalTime.of(hour, 0);

        return switch (state) {
            case NO -> new ShutdownInterval(hourStart, hourEnd);
            case SECOND -> new ShutdownInterval(hourStart.plusMinutes(30), hourEnd);
            case FIRST -> new ShutdownInterval(hourStart, hourStart.plusMinutes(30));
            default -> null;
        };
    }

    private LocalTime max(LocalTime first, LocalTime second) {
        if (first == LocalTime.MIDNIGHT || second == LocalTime.MIDNIGHT) {
            return LocalTime.MIDNIGHT;
        }
        return first.isAfter(second)
                ? first
                : second;
    }
}
