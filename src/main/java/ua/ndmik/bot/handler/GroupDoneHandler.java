package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.DtekShutdownsService;
import ua.ndmik.bot.service.TelegramService;

import java.util.List;

@Component
public class GroupDoneHandler implements CallbackHandler {

    private final TelegramService telegramService;
    private final DtekShutdownsService dtekService;
    private final UserSettingsRepository userRepository;
    private final ScheduleRepository scheduleRepository;

    public GroupDoneHandler(TelegramService telegramService,
                            DtekShutdownsService dtekService,
                            UserSettingsRepository userRepository,
                            ScheduleRepository scheduleRepository) {
        this.telegramService = telegramService;
        this.dtekService = dtekService;
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public void handle(Update update) {
        long chatId = getChatId(update);
        UserSettings user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));
        List<Schedule> schedules = scheduleRepository.findAllByGroupId(user.getGroupId());
        String message = dtekService.getShutdownsMessage(schedules);
        //TODO: fix message is displayed after menu
        telegramService.sendMessage(update, message);
    }
}
