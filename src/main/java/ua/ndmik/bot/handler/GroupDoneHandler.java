package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.model.Message;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;

@Component
public class GroupDoneHandler implements CallbackHandler {

    private final TelegramService telegramService;
    private final UserSettingsRepository userRepository;
    private final RegionHandler regionHandler;

    public GroupDoneHandler(TelegramService telegramService,
                            UserSettingsRepository userRepository,
                            RegionHandler regionHandler) {
        this.telegramService = telegramService;
        this.userRepository = userRepository;
        this.regionHandler = regionHandler;
    }

    @Override
    public void handle(Update update) {
        long chatId = getChatId(update);
        UserSettings user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));
        String groupId = user.getTmpGroupId();
        if (groupId == null) {
            regionHandler.reprint(update, null, "⚠️ Щоб зберегти вибір, спочатку оберіть групу зі списку нижче.");
            return;
        }
        user.setGroupId(groupId);
        userRepository.save(user);

        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        Message message = new Message(
                messageId,
                chatId,
                telegramService.formatMessage(user, "✅ Групу відключень збережено"),
                telegramService.buildMainMenuMarkup(user)
        );
        telegramService.editMessage(message);
    }
}
