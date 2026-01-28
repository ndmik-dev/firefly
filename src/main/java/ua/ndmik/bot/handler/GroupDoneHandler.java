package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.DtekShutdownsService;
import ua.ndmik.bot.service.TelegramService;

import java.util.List;

import static ua.ndmik.bot.model.MenuCallback.*;

@Component
public class GroupDoneHandler implements CallbackHandler {

    private final TelegramService telegramService;
    private final DtekShutdownsService dtekService;
    private final UserSettingsRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final RegionHandler regionHandler;

    public GroupDoneHandler(TelegramService telegramService,
                            DtekShutdownsService dtekService,
                            UserSettingsRepository userRepository,
                            ScheduleRepository scheduleRepository,
                            RegionHandler regionHandler) {
        this.telegramService = telegramService;
        this.dtekService = dtekService;
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
        this.regionHandler = regionHandler;
    }

    @Override
    public void handle(Update update) {
        long chatId = getChatId(update);
        UserSettings user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));
        String groupId = user.getTmpGroupId();
        if (groupId == null) {
            regionHandler.reprint(update, null, "! Ви не обрали групу відключень");
        }
        user.setGroupId(groupId);
        userRepository.save(user);
        telegramService.sendMessage(update);
    }
}
