package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;

import java.util.List;

import static ua.ndmik.bot.model.MenuCallback.GROUP_DONE;
import static ua.ndmik.bot.model.MenuCallback.GROUP_CLICK;

@Component
public class RegionHandler implements CallbackHandler {

    private final TelegramService telegramService;
    private final ScheduleRepository scheduleRepository;
    private final UserSettingsRepository userRepository;

    public RegionHandler(TelegramService telegramService,
                         ScheduleRepository scheduleRepository,
                         UserSettingsRepository userRepository) {
        this.telegramService = telegramService;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(Update update) {
        long chatId = getChatId(update);
        UserSettings user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));
        String userGroupId = user.getGroupId();
        List<String> groupIds = scheduleRepository.findDistinctGroupIds()
                .stream()
                .sorted()
                .toList();
        List<InlineKeyboardButton> buttons = groupIds.stream()
                .map(groupId -> telegramService.button(
                        formatButton(groupId, userGroupId), GROUP_CLICK.name() + ":" + groupId)
                )
                .toList();
        List<InlineKeyboardRow> rows = telegramService.chunkButtons(buttons, 2);
        rows.add(new InlineKeyboardRow(List.of(telegramService.button("✅ Готово", GROUP_DONE.name()))));
        InlineKeyboardMarkup menu = telegramService.menu(rows);
        telegramService.sendMessage(update, "Виберіть групу", menu);
    }

    //TODO: move to formatter
    private String formatButton(String groupId, String userGroupId) {
        return groupId.equals(userGroupId)
                ? "✅ " + groupId
                : "⚪ " + groupId;
    }
}
