package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ua.ndmik.bot.model.telegram.Message;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;

import java.util.List;

import static ua.ndmik.bot.model.callback.MenuCallback.*;

@Component
public class GroupSelectionHandler implements CallbackHandler {

    private final TelegramService telegramService;
    private final UserSettingsRepository userRepository;

    public GroupSelectionHandler(TelegramService telegramService,
                                 UserSettingsRepository userRepository) {
        this.telegramService = telegramService;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(Update update) {
        long chatId = getChatId(update);
        userRepository.findByChatId(chatId).ifPresent(this::resetAwaitingAddressInput);

        InlineKeyboardRow regions = new InlineKeyboardRow(
                List.of(
                        telegramService.button("🏙️ Київ", KYIV.name()),
                        telegramService.button("🏘️ Київщина", REGION.name())
                ));
        InlineKeyboardRow resolveByAddress = new InlineKeyboardRow(
                List.of(
                        telegramService.button("📍 Знайти групу за адресою (Київ)", GROUP_RESOLVING.name())
                ));
        InlineKeyboardRow back = new InlineKeyboardRow(
                List.of(
                        telegramService.button("⬅️ Назад", REGIONS_BACK.name())
                ));
        InlineKeyboardMarkup menu = telegramService.menu(List.of(regions, resolveByAddress, back));

        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        Message message = new Message(
                messageId,
                chatId,
                "🧭 Оберіть ваш регіон, щоб налаштувати групу відключень.",
                menu
        );
        telegramService.editMessage(message);
    }

    private void resetAwaitingAddressInput(UserSettings user) {
        if (!user.isAwaitingAddressInput()) {
            return;
        }
        user.setAwaitingAddressInput(false);
        userRepository.save(user);
    }
}
