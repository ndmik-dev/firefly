package ua.ndmik.bot.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ua.ndmik.bot.handler.CallbackHandlerResolver;
import ua.ndmik.bot.model.MenuCallback;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;

import java.util.List;

import static ua.ndmik.bot.model.MenuCallback.KYIV;
import static ua.ndmik.bot.model.MenuCallback.REGION;

@Service
public class DtekShutdownBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String botToken;
    private final TelegramService telegramService;
    private final UserSettingsRepository userRepository;
    private final CallbackHandlerResolver callbackHandlerResolver;

    public DtekShutdownBot(@Value("${telegram.bot-token}") String botToken,
                           TelegramService telegramService,
                           UserSettingsRepository userRepository,
                           CallbackHandlerResolver callbackHandlerResolver) {
        this.botToken = botToken;
        this.telegramService = telegramService;
        this.userRepository = userRepository;
        this.callbackHandlerResolver = callbackHandlerResolver;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
            return;
        }

        if (update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }

    private void handleMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        if (text.equals("/start")) {
            UserSettings user = userRepository.findByChatId(chatId)
                    .orElse(newUser(chatId));
            InlineKeyboardRow row = new InlineKeyboardRow(
                    List.of(
                            telegramService.button("Київ", KYIV.name()),
                            telegramService.button("Київщина", REGION.name())
                    ));
            InlineKeyboardMarkup menu = telegramService.menu(List.of(row));
            telegramService.sendMessage("Вітаю", menu, chatId);
        }
    }

    private void handleCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        String callbackKey = data.split(":", 2)[0];
        MenuCallback callback = MenuCallback.valueOf(callbackKey);
        callbackHandlerResolver.getHandler(callback).handle(update);
    }

    private UserSettings newUser(Long chatId) {
        return userRepository.save(UserSettings.builder()
                .chatId(chatId)
                .isNotificationEnabled(true)
                .build());
    }
}
