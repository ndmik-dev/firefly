package ua.ndmik.bot.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.handler.CallbackHandlerResolver;
import ua.ndmik.bot.model.MenuCallback;
import ua.ndmik.bot.service.TelegramService;

@Service
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
            String chatId = String.valueOf(update.getCallbackQuery().getMessage().getChatId());
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            telegramService.setPreviousMessageId(messageId);
            //TODO: remove animation if possible
            telegramService.cleanUpOldMessage(chatId);
            handleCallback(update);
        }
    }

    private void handleMessage(Update update) {
        String text = update.getMessage().getText().trim();
        if (text.equals("/start")) {
            telegramService.sendMainMenu(update);
        }
    }

    private void handleCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        String callbackKey = data.split(":", 2)[0];
        MenuCallback callback = MenuCallback.valueOf(callbackKey);
        callbackHandlerResolver.getHandler(callback).handle(update);
    }
}
