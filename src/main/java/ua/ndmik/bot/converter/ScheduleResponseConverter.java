package ua.ndmik.bot.converter;

import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import ua.ndmik.bot.model.GroupSchedule;
import ua.ndmik.bot.model.ScheduleResponse;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.ScheduleDay;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.function.Predicate.not;

@Component
public class ScheduleResponseConverter {

    private final JsonMapper mapper;

    public ScheduleResponseConverter() {
        this.mapper = new JsonMapper();
    }

    public List<Schedule> toSchedules(ScheduleResponse response) {
        List<Schedule> schedules = new ArrayList<>();
        String lastUpdate = response.update();
        String todayId = response.today();
        String tomorrowId = getTomorrowId(response);

        response.data().entrySet().stream()
                .filter(entry -> entry.getKey().equals(todayId))
                .map(Map.Entry::getValue)
                .map(GroupSchedule::getGroupSchedules)
                .flatMap(entry -> entry.entrySet().stream())
                .map(entry -> buildSchedule(entry, ScheduleDay.TODAY, lastUpdate))
                .forEach(schedules::add);

        response.data().entrySet().stream()
                .filter(entry -> entry.getKey().equals(tomorrowId))
                .map(Map.Entry::getValue)
                .map(GroupSchedule::getGroupSchedules)
                .flatMap(entry -> entry.entrySet().stream())
                .map(entry -> buildSchedule(entry, ScheduleDay.TOMORROW, lastUpdate))
                .forEach(schedules::add);

        return schedules;
    }

    private Schedule buildSchedule(Map.Entry<String, Map<String, String>> entry,
                                   ScheduleDay day,
                                   String lastUpdate) {
        String groupId = entry.getKey();
        Map<String, String> schedule = entry.getValue();
        return Schedule.builder()
                .schedule(mapper.writeValueAsString(schedule))
                .groupId(groupId.replaceAll("[^0-9.]", ""))
                .scheduleDay(day)
                //TODO: fix conversion error
                .lastUpdate(LocalDateTime.parse(lastUpdate))
                .needToNotify(Boolean.TRUE)
                .build();
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
