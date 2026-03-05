package ua.ndmik.bot.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import ua.ndmik.bot.client.DtekClient;
import ua.ndmik.bot.converter.ScheduleResponseConverter;
import ua.ndmik.bot.model.DtekArea;
import ua.ndmik.bot.model.ScheduleResponse;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ua.ndmik.bot.model.entity.ScheduleDay.TOMORROW;
import static ua.ndmik.bot.util.ScheduleStateUtils.isAllDayWithPower;

@Service
@Slf4j
public class ShutdownsScheduler {
    private static final String TOMORROW_SCHEDULE_APPEARED_MESSAGE = "🔔 Графік на завтра зʼявився";
    private static final String SCHEDULE_CHANGED_MESSAGE = "🔄 Графік змінився";

    private final DtekClient dtekClient;
    private final ScheduleRepository scheduleRepository;
    private final UserSettingsRepository userRepository;
    private final ScheduleResponseConverter converter;
    private final TelegramService telegramService;
    private final JsonMapper mapper;
    private final ZoneId zoneId;

    public ShutdownsScheduler(DtekClient dtekClient,
                              ScheduleRepository scheduleRepository,
                              UserSettingsRepository userRepository,
                              ScheduleResponseConverter converter,
                              TelegramService telegramService,
                              @Value("${scheduler.shutdowns.time-zone:Europe/Kyiv}") String timeZone) {
        this.dtekClient = dtekClient;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.converter = converter;
        this.telegramService = telegramService;
        this.mapper = new JsonMapper();
        this.zoneId = ZoneId.of(timeZone);
    }

    @Scheduled(fixedDelayString = "${scheduler.shutdowns.fixed-delay-ms}", timeUnit = TimeUnit.MINUTES)
    public void processShutdowns() {
        if (isBlockedWindow()) {
            log.info("Scheduler skipped (blocked window 23:55-00:05)");
            return;
        }
        log.info("Shutdowns Scheduler started");
        processShutdownsForArea(DtekArea.KYIV);
        processShutdownsForArea(DtekArea.KYIV_REGION);
        log.info("Shutdowns Scheduler finished");
    }

    private void processShutdownsForArea(DtekArea area) {
        log.info("Starting fetch schedules for area={}", area);
        Optional<ScheduleResponse> scheduleResponseOpt = fetchSchedules(area);
        if (scheduleResponseOpt.isEmpty()) {
            log.warn("Failed to fetch schedules for area={}, skipping current run.", area);
            return;
        }
        ScheduleResponse scheduleResponse = scheduleResponseOpt.get();
        log.info("Schedules for area={} extracted, data={}", area, toJson(scheduleResponse));
        List<Schedule> oldSchedules = scheduleRepository.findAll();
        List<Schedule> newSchedules = converter.toSchedules(scheduleResponse, area);
        Set<String> tomorrowAppearedIds = getTomorrowAppearedIds(oldSchedules, newSchedules);
        compareAndUpdate(oldSchedules, newSchedules);
        List<String> updatedGroupIds = scheduleRepository.findNotifyGroupIdsByArea(area.name());
        if (updatedGroupIds.isEmpty()) {
            log.info("Nothing has changed for area={}, any updates.", area);
        }
        updatedGroupIds.forEach(groupId -> processGroupUpdate(area, groupId, tomorrowAppearedIds.contains(groupId)));
    }

    private Optional<ScheduleResponse> fetchSchedules(DtekArea area) {
        return switch (area) {
            case KYIV -> dtekClient.getKyivSchedules();
            case KYIV_REGION -> dtekClient.getKyivRegionSchedules();
        };
    }

    private boolean isBlockedWindow() {
        LocalTime now = LocalTime.now(zoneId);
        LocalTime start = LocalTime.of(23, 55);
        LocalTime end = LocalTime.of(0, 5);
        return now.isAfter(start) || now.isBefore(end);
    }

    private Set<String> getTomorrowAppearedIds(List<Schedule> oldSchedules, List<Schedule> newSchedules) {
        Set<String> result = new HashSet<>();
        for (Schedule newSchedule : newSchedules) {
            if (!TOMORROW.equals(newSchedule.getScheduleDay())) {
                continue;
            }
            Optional<Schedule> oldSchedule = findSchedule(oldSchedules, newSchedule);
            boolean isScheduleAppeared = oldSchedule.isPresent()
                    && isScheduleEmpty(oldSchedule.get().getSchedule())
                    && !isScheduleEmpty(newSchedule.getSchedule());
            if (isScheduleAppeared) {
                result.add(newSchedule.getGroupId());
            }
        }
        return result;
    }

    private boolean isScheduleEmpty(String scheduleJson) {
        if (scheduleJson == null || scheduleJson.isBlank()) {
            return true;
        }
        try {
            Map<String, String> scheduleMap = mapper.readValue(scheduleJson, new TypeReference<>() {});
            return isAllDayWithPower(scheduleMap);
        } catch (RuntimeException e) {
            log.warn("Invalid schedule payload, treating as empty. payload={}", scheduleJson);
            return true;
        }
    }

    private void compareAndUpdate(List<Schedule> oldSchedules, List<Schedule> newSchedules) {
        for (Schedule newSchedule : newSchedules) {
            Optional<Schedule> oldSchedule = findSchedule(oldSchedules, newSchedule);
            oldSchedule.ifPresentOrElse(
                    schedule -> updateExistingSchedule(schedule, newSchedule),
                    () -> {
                        log.info("Saving new schedule, data={}", newSchedule);
                        scheduleRepository.save(newSchedule);
                    });
        }
    }

    private Optional<Schedule> findSchedule(List<Schedule> oldSchedules, Schedule newSchedule) {
        return oldSchedules.stream()
                .filter(schedule -> schedule.getArea().equals(newSchedule.getArea()))
                .filter(schedule -> schedule.getScheduleDay().equals(newSchedule.getScheduleDay()))
                .filter(schedule -> schedule.getGroupId().equals(newSchedule.getGroupId()))
                .findFirst();
    }

    private void updateExistingSchedule(Schedule oldSchedule, Schedule newSchedule) {
        if (!oldSchedule.getSchedule().equals(newSchedule.getSchedule())) {
            log.info("Updating existing schedule for groupId={}, scheduleDay={}, oldSchedule={}, newSchedule={}",
                    oldSchedule.getGroupId(),
                    oldSchedule.getScheduleDay(),
                    oldSchedule,
                    newSchedule);
            scheduleRepository.save(newSchedule);
        }
    }

    private void processGroupUpdate(DtekArea area, String groupId, boolean tomorrowArrived) {
        List<UserSettings> users = userRepository.findNotifiableByGroupAndArea(groupId, area.name());
        if (tomorrowArrived) {
            log.info("Tomorrow schedule appeared for area={}, groupId={}. Sending updates", area, groupId);
            users.forEach(user -> telegramService.sendUpdate(user, TOMORROW_SCHEDULE_APPEARED_MESSAGE));
        } else {
            log.info("Schedule changed for area={}, groupId={}. Sending updates", area, groupId);
            users.forEach(user -> telegramService.sendUpdate(user, SCHEDULE_CHANGED_MESSAGE));
        }
        List<Schedule> schedules = scheduleRepository.findByGroupAndArea(groupId, area.name());
        schedules.forEach(schedule -> schedule.setNeedToNotify(Boolean.FALSE));
        scheduleRepository.saveAll(schedules);
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (RuntimeException e) {
            log.warn("Failed to serialize payload to JSON, type={}", value.getClass().getSimpleName());
            return String.valueOf(value);
        }
    }
}
