package ua.ndmik.bot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface CallbackHandler {

    void handle(Update update);

    default long getChatId(Update update) {
        return update.getCallbackQuery().getMessage().getChatId();
    }
}
