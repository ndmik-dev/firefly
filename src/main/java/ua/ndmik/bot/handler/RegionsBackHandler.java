package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.model.Message;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;

import static ua.ndmik.bot.service.TelegramService.GREETING;

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
        UserSettings user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));
        String groupId = user.getGroupId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        Message message;
        if (groupId != null) {
            message = new Message(
                    messageId,
                    chatId,
                    telegramService.formatMessage(user, ""),
                    telegramService.buildMainMenuMarkup(user)
            );
        } else {
            message = new Message(
                    messageId,
                    chatId,
                    GREETING,
                    telegramService.buildMainMenuMarkup(user)
            );
        }
        telegramService.editMessage(message);
    }
}
