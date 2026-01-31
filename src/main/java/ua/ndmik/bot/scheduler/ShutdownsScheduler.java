package ua.ndmik.bot.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.ndmik.bot.client.DtekClient;
import ua.ndmik.bot.converter.ScheduleResponseConverter;
import ua.ndmik.bot.model.HourState;
import ua.ndmik.bot.model.ScheduleResponse;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.DtekShutdownsService;
import ua.ndmik.bot.service.MessageFormatter;
import ua.ndmik.bot.service.TelegramService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static ua.ndmik.bot.model.entity.ScheduleDay.TODAY;
import static ua.ndmik.bot.model.entity.ScheduleDay.TOMORROW;

@Service
@Slf4j
public class ShutdownsScheduler {

    private final DtekClient dtekClient;
    private final DtekShutdownsService dtekService;
    private final ScheduleRepository scheduleRepository;
    private final UserSettingsRepository userRepository;
    private final ScheduleResponseConverter converter;
    private final TelegramService telegramService;
    private final MessageFormatter messageFormatter;
    private final JsonMapper mapper;

    public ShutdownsScheduler(DtekClient dtekClient,
                              DtekShutdownsService dtekService,
                              ScheduleRepository scheduleRepository,
                              UserSettingsRepository userRepository,
                              ScheduleResponseConverter converter,
                              TelegramService telegramService,
                              MessageFormatter messageFormatter) {
        this.dtekClient = dtekClient;
        this.dtekService = dtekService;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.converter = converter;
        this.telegramService = telegramService;
        this.messageFormatter = messageFormatter;
        this.mapper = new JsonMapper();
    }

    @Scheduled(fixedDelayString = "${scheduler.shutdowns.fixed-delay-ms}", timeUnit = TimeUnit.MINUTES)
    public void processShutdowns() {
        if (isBlockedWindow()) {
            log.info("Scheduler skipped (blocked window 23:55-00:05)");
            return;
        }
        log.info("Running scheduler");
        ScheduleResponse scheduleResponse = dtekClient.getShutdownsSchedule();
        List<Schedule> oldSchedules = scheduleRepository.findAll();
        List<Schedule> newSchedules = converter.toSchedules(scheduleResponse);
        Set<String> tomorrowArrivedGroupIds = findTomorrowArrivedGroups(oldSchedules, newSchedules);
        compareAndUpdate(oldSchedules, newSchedules);
        List<String> updatedGroupIds = scheduleRepository.findAllGroupIdsByNeedToNotifyTrue();
        for (String groupId : updatedGroupIds) {
            processGroupUpdate(groupId, tomorrowArrivedGroupIds.contains(groupId));
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void rolloverSchedulesAtMidnight() {
        log.info("Running daily schedule rollover");
        scheduleRepository.deleteByScheduleDay(TODAY);
        List<Schedule> tomorrowSchedules = scheduleRepository.findAllByScheduleDay(TOMORROW);
        for (Schedule schedule : tomorrowSchedules) {
            schedule.setScheduleDay(TODAY);
        }
        scheduleRepository.saveAll(tomorrowSchedules);
    }

    private void processGroupUpdate(String groupId, boolean tomorrowArrived) {
        List<UserSettings> users = userRepository.findByGroupIdAndIsNotificationEnabledTrue(groupId);
        if (tomorrowArrived) {
            users.forEach(user -> telegramService.sendUpdate(user.getChatId(), "📅 Графік на завтра зʼявився"));
        } else {
            users.forEach(user -> telegramService.sendUpdate(user.getChatId()));
        }
        List<Schedule> schedules = scheduleRepository.findAllByGroupId(groupId);
        //TODO: uncomment
//        schedules.forEach(schedule -> schedule.setNeedToNotify(Boolean.FALSE));
        scheduleRepository.saveAll(schedules);
    }

    private void compareAndUpdate(List<Schedule> oldSchedules, List<Schedule> newSchedules) {
        for (Schedule newSchedule : newSchedules) {
            Optional<Schedule> oldSchedule = findSchedule(oldSchedules, newSchedule);
            oldSchedule.ifPresentOrElse(
                    schedule -> updateExistingSchedule(schedule, newSchedule),
                    () -> scheduleRepository.save(newSchedule));
        }
    }

    private void updateExistingSchedule(Schedule oldSchedule, Schedule newSchedule) {
        if (!oldSchedule.getSchedule().equals(newSchedule.getSchedule())) {
            scheduleRepository.save(newSchedule);
        }
    }

    private Optional<Schedule> findSchedule(List<Schedule> oldSchedules, Schedule newSchedule) {
        return oldSchedules.stream()
                .filter(schedule -> schedule.getScheduleDay().equals(newSchedule.getScheduleDay()))
                .filter(schedule -> schedule.getGroupId().equals(newSchedule.getGroupId()))
                .findFirst();
    }

    private Set<String> findTomorrowArrivedGroups(List<Schedule> oldSchedules, List<Schedule> newSchedules) {
        Set<String> result = new HashSet<>();
        for (Schedule newSchedule : newSchedules) {
            if (!TOMORROW.equals(newSchedule.getScheduleDay())) {
                continue;
            }
            Optional<Schedule> oldSchedule = findSchedule(oldSchedules, newSchedule);
            if (oldSchedule.isPresent()
                    && isScheduleEmpty(oldSchedule.get().getSchedule())
                    && !isScheduleEmpty(newSchedule.getSchedule())) {
                result.add(newSchedule.getGroupId());
            }
        }
        return result;
    }

    private boolean isScheduleEmpty(String scheduleJson) {
        Map<String, String> scheduleMap = mapper.readValue(scheduleJson, new TypeReference<>() {});
        return scheduleMap.values()
                .stream()
                .allMatch(HourState.YES.getValue()::equals);
    }

    private boolean isBlockedWindow() {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(23, 55);
        LocalTime end = LocalTime.of(0, 5);
        return now.isAfter(start) || now.isBefore(end);
    }
}
