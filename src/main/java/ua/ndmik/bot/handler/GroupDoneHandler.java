package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.DtekShutdownsService;
import ua.ndmik.bot.service.TelegramService;

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
        telegramService.sendMessage(update);
    }
}
