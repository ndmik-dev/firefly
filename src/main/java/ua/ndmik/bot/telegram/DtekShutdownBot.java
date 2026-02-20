package ua.ndmik.bot.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.handler.CallbackHandlerResolver;
import ua.ndmik.bot.model.MenuCallback;
import ua.ndmik.bot.service.TelegramService;

@Service
@Profile("!test")
public class DtekShutdownBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String botToken;
    private final TelegramService telegramService;
    private final CallbackHandlerResolver callbackHandlerResolver;

    public DtekShutdownBot(@Value("${telegram.bot-token}") String botToken,
                           TelegramService telegramService,
                           CallbackHandlerResolver callbackHandlerResolver) {
        this.botToken = botToken;
        this.telegramService = telegramService;
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
        String text = update.getMessage().getText().trim();
        if (isCommand(text, "/start")) {
            telegramService.sendGreeting(update);
            return;
        }
        if (isCommand(text, "/stats_today")) {
            telegramService.sendTodayStats(update.getMessage().getChatId());
            return;
        }
        if (isCommand(text, "/stats_week")) {
            telegramService.sendWeeklyStats(update.getMessage().getChatId());
        }
    }

    private void handleCallback(Update update) {
        try {
            String data = update.getCallbackQuery().getData();
            String callbackKey = data.split(":", 2)[0];
            MenuCallback callback;
            try {
                callback = MenuCallback.valueOf(callbackKey);
            } catch (IllegalArgumentException e) {
                callback = MenuCallback.DEFAULT;
            }
            callbackHandlerResolver.getHandler(callback).handle(update);
        } finally {
            telegramService.answerCallback(update.getCallbackQuery().getId());
        }
    }

    private boolean isCommand(String text, String command) {
        return text.equals(command) || text.startsWith(command + "@");
    }
}
