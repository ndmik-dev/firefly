package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ua.ndmik.bot.model.Message;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;

import java.util.List;

import static ua.ndmik.bot.model.MenuCallback.GROUP_SELECTION;

@Component
public class GroupResolvingHandler implements CallbackHandler {

    private final UserSettingsRepository userRepository;
    private final TelegramService telegramService;

    public GroupResolvingHandler(UserSettingsRepository userRepository,
                                 TelegramService telegramService) {
        this.userRepository = userRepository;
        this.telegramService = telegramService;
    }

    @Override
    public void handle(Update update) {
        long chatId = getChatId(update);
        UserSettings user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));

        user.setAwaitingAddressInput(true);
        userRepository.save(user);

        InlineKeyboardMarkup menu = telegramService.menu(List.of(
                new InlineKeyboardRow(List.of(
                        telegramService.button("⬅️ До вибору регіону", GROUP_SELECTION.name())
                ))
        ));

        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        Message message = new Message(
                messageId,
                chatId,
                """
                        📍 Надішліть адресу одним повідомленням.
                        Функція працює лише для адрес у <b>м. Київ</b>.
                        
                        Формат:
                        • вул. Хрещатик, 22
                        • вул. Хрещатик 22
                        
                        Я знайду вашу групу відключень і збережу її.
                        """,
                menu
        );
        telegramService.editMessage(message);
    }
}
