package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;

@Component
public class RegionsBackHandler implements CallbackHandler {

    private final TelegramService telegramService;
    private final UserSettingsRepository userRepository;

    public RegionsBackHandler(TelegramService telegramService,
                              UserSettingsRepository userRepository) {
        this.telegramService = telegramService;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(Update update) {
        long chatId = getChatId(update);
        UserSettings userSettings = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));
        String groupId = userSettings.getGroupId();
        if (groupId != null) {
            telegramService.sendMessage(update);
        } else {
            telegramService.sendGreeting(update);
        }
    }
}
