package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.model.Message;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;

@Component
public class NotificationsClickHandler implements CallbackHandler {

    private final UserSettingsRepository userRepository;
    private final TelegramService telegramService;

    public NotificationsClickHandler(UserSettingsRepository userRepository,
                                     TelegramService telegramService) {
        this.userRepository = userRepository;
        this.telegramService = telegramService;
    }

    @Override
    public void handle(Update update) {
        long chatId = getChatId(update);
        userRepository.findByChatId(chatId)
                .ifPresent(user -> {
                    user.setNotificationEnabled(!user.isNotificationEnabled());
                    userRepository.save(user);
                    int messageId = update.getCallbackQuery().getMessage().getMessageId();
                    Message message = new Message(
                            messageId,
                            chatId,
                            telegramService.formatMessage(user, ""),
                            telegramService.buildMainMenuMarkup(user)
                    );
                    telegramService.editMessage(message);
                });
    }
}
