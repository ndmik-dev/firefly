package ua.ndmik.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.ndmik.bot.client.DtekClient;
import ua.ndmik.bot.converter.ScheduleResponseConverter;
import ua.ndmik.bot.model.ScheduleResponse;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    }

    //TODO: add transactions
    @Scheduled(fixedDelayString = "${scheduler.shutdowns.fixed-delay-ms}", timeUnit = TimeUnit.MINUTES)
    public void processShutdowns() {
        log.info("Running scheduler");
        ScheduleResponse scheduleResponse = dtekClient.getShutdownsSchedule();
        List<Schedule> oldSchedules = scheduleRepository.findAll();
        List<Schedule> newSchedules = converter.toSchedules(scheduleResponse);
        compareAndUpdate(oldSchedules, newSchedules);
        List<String> updatedGroupIds = scheduleRepository.findAllGroupIdsByNeedToNotifyTrue();
        for (String groupId : updatedGroupIds) {
            processGroupUpdate(groupId);
        }
    }

    private void processGroupUpdate(String groupId) {
        List<UserSettings> users = userRepository.findByGroupIdAndIsNotificationEnabledTrue(groupId);
        if (users.isEmpty()) {
            return;
        }
        //TODO: sendMessages. Fix TODAY/TOMORROW problem
        users.forEach(user -> telegramService.sendUpdate(user.getChatId()));
        List<Schedule> schedules = scheduleRepository.findAllByGroupId(groupId);
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
}
