package ua.ndmik.bot.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import ua.ndmik.bot.model.HourState;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.repository.ScheduleRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ua.ndmik.bot.model.entity.ScheduleDay.TODAY;
import static ua.ndmik.bot.model.entity.ScheduleDay.TOMORROW;

@Service
@Slf4j
public class MidnightRolloverScheduler {

    private final ScheduleRepository scheduleRepository;
    private final JsonMapper mapper;

    public MidnightRolloverScheduler(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
        this.mapper = new JsonMapper();
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "${scheduler.shutdowns.time-zone:Europe/Kyiv}")
    public void rolloverSchedulesAtMidnight() {
        log.info("Running daily schedule rollover");
        scheduleRepository.deleteByDay(TODAY.name());
        List<Schedule> tomorrowSchedules = scheduleRepository.findByDay(TOMORROW.name());
        if (tomorrowSchedules.isEmpty()) {
            log.info("No tomorrow schedules found, rollover finished");
            return;
        }
        List<Schedule> rolledSchedules = tomorrowSchedules.stream()
                .map(schedule -> Schedule.builder()
                        .area(schedule.getArea())
                        .groupId(schedule.getGroupId())
                        .scheduleDay(TODAY)
                        .schedule(schedule.getSchedule())
                        .lastUpdate(schedule.getLastUpdate())
                        .needToNotify(Boolean.FALSE)
                        .build())
                .toList();
        String allYesScheduleJson = buildAllYesScheduleJson();
        tomorrowSchedules.forEach(schedule -> {
            schedule.setSchedule(allYesScheduleJson);
            schedule.setNeedToNotify(Boolean.FALSE);
        });
        List<Schedule> schedulesToSave = new ArrayList<>(rolledSchedules.size() + tomorrowSchedules.size());
        schedulesToSave.addAll(rolledSchedules);
        schedulesToSave.addAll(tomorrowSchedules);
        scheduleRepository.saveAll(schedulesToSave);
        log.info("Schedules were rolled over");
    }

    private String buildAllYesScheduleJson() {
        Map<String, String> schedule = new LinkedHashMap<>();
        for (int hour = 1; hour <= 24; hour++) {
            schedule.put(String.valueOf(hour), HourState.YES.getValue());
        }
        return mapper.writeValueAsString(schedule);
    }
}
