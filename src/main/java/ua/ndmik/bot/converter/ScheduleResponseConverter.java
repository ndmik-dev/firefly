package ua.ndmik.bot.converter;

import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import ua.ndmik.bot.model.DtekArea;
import ua.ndmik.bot.model.GroupSchedule;
import ua.ndmik.bot.model.ScheduleResponse;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.ScheduleDay;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.function.Predicate.not;

@Component
public class ScheduleResponseConverter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final JsonMapper mapper;

    public ScheduleResponseConverter() {
        this.mapper = new JsonMapper();
    }

    public List<Schedule> toSchedules(ScheduleResponse response, DtekArea area) {
        List<Schedule> schedules = new ArrayList<>();
        LocalDateTime lastUpdate = parseLastUpdate(response.update());
        String todayId = response.today();
        String tomorrowId = getTomorrowId(response);

        response.data().entrySet().stream()
                .filter(entry -> entry.getKey().equals(todayId))
                .map(Map.Entry::getValue)
                .map(GroupSchedule::getGroupSchedules)
                .flatMap(entry -> entry.entrySet().stream())
                .map(entry -> buildSchedule(entry, ScheduleDay.TODAY, area, lastUpdate))
                .forEach(schedules::add);

        response.data().entrySet().stream()
                .filter(entry -> entry.getKey().equals(tomorrowId))
                .map(Map.Entry::getValue)
                .map(GroupSchedule::getGroupSchedules)
                .flatMap(entry -> entry.entrySet().stream())
                .map(entry -> buildSchedule(entry, ScheduleDay.TOMORROW, area, lastUpdate))
                .forEach(schedules::add);

        return schedules;
    }

    private Schedule buildSchedule(Map.Entry<String, Map<String, String>> entry,
                                   ScheduleDay day,
                                   DtekArea area,
                                   LocalDateTime lastUpdate) {
        String groupId = entry.getKey();
        Map<String, String> schedule = entry.getValue();
        return Schedule.builder()
                .schedule(mapper.writeValueAsString(schedule))
                .groupId(groupId.replaceAll("[^0-9.]", ""))
                .scheduleDay(day)
                .area(area)
                .lastUpdate(lastUpdate)
                .needToNotify(Boolean.TRUE)
                .build();
    }

    private LocalDateTime parseLastUpdate(String lastUpdate) {
        if (lastUpdate == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.parse(lastUpdate.trim(), FORMATTER);
    }

    private String getTomorrowId(ScheduleResponse response) {
        return response.data()
                .keySet()
                .stream()
                .filter(not(id -> id.equals(response.today())))
                .findFirst()
                .orElse("");
    }
}
