package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.service.TelegramService;

@Component
public class GroupDoneHandler implements CallbackHandler {

    private final TelegramService telegramService;

    public GroupDoneHandler(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @Override
    public void handle(Update update) {
        telegramService.sendMainMenu(update);
    }
}
