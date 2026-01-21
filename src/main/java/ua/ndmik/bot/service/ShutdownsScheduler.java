package ua.ndmik.bot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.ndmik.bot.client.DtekClient;
import ua.ndmik.bot.converter.ScheduleResponseConverter;
import ua.ndmik.bot.model.ScheduleResponse;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ShutdownsScheduler {

    private final DtekClient dtekClient;
    private final DtekShutdownsService dtekService;
    private final TelegramClient telegramClient;
    private final ScheduleRepository scheduleRepository;
    private final UserSettingsRepository userRepository;
    private final ScheduleResponseConverter converter;

    public ShutdownsScheduler(DtekClient dtekClient,
                              DtekShutdownsService dtekService,
                              @Value("${telegram.bot-token}") String botToken,
                              ScheduleRepository scheduleRepository,
                              UserSettingsRepository userRepository,
                              ScheduleResponseConverter converter) {
        this.dtekClient = dtekClient;
        this.dtekService = dtekService;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.converter = converter;
    }

    //TODO: add transactions
    @Scheduled(fixedDelayString = "${scheduler.shutdowns.fixed-delay-ms}")
    public void run() {
        ScheduleResponse scheduleResponse = dtekClient.getShutdownsSchedule();
        List<Schedule> oldSchedules = scheduleRepository.findAll();
        List<Schedule> newSchedules = converter.toSchedules(scheduleResponse);
        compareAndUpdate(oldSchedules, newSchedules);
        List<Schedule> updatedSchedules = scheduleRepository.findAllByNeedToNotifyTrue();
        for (Schedule schedule : updatedSchedules) {
            List<UserSettings> users = userRepository.findByGroupIdAndIsNotificationEnabledTrue(schedule.getGroupId());
            //TODO: sendMessages
            schedule.setNeedToNotify(Boolean.FALSE);
            scheduleRepository.save(schedule);
        }
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
            oldSchedule.setSchedule(newSchedule.getSchedule());
            oldSchedule.setLastUpdate(newSchedule.getLastUpdate());
            oldSchedule.setNeedToNotify(Boolean.TRUE);
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
