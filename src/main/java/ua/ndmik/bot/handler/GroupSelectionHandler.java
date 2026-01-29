package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ua.ndmik.bot.service.TelegramService;

import java.util.List;

import static ua.ndmik.bot.model.MenuCallback.*;

@Component
public class GroupSelectionHandler implements CallbackHandler {

    private final TelegramService telegramService;

    public GroupSelectionHandler(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @Override
    public void handle(Update update) {
        InlineKeyboardRow regions = new InlineKeyboardRow(
                List.of(
                        telegramService.button("Київ (поки не робе)", KYIV.name()),
                        telegramService.button("Київщина", REGION.name())
                ));
        InlineKeyboardRow back = telegramService.backRow(REGIONS_BACK.name());
        InlineKeyboardMarkup menu = telegramService.menu(List.of(regions, back));
        telegramService.sendMessage(update, "Оберіть групу відключень", menu);
    }
}
