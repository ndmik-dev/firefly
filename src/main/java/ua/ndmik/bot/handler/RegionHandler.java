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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        Optional<UserSettings> user = userRepository.findByChatId(chatId);
        String userGroupId = user.isPresent()
                ? user.get().getGroupId()
                : "";
        List<String> groupIds = scheduleRepository.findDistinctGroupIds()
                .stream()
                .sorted()
                .toList();
        List<InlineKeyboardButton> buttons = groupIds.stream()
                .map(groupId -> telegramService.buildButton(formatButton(groupId, userGroupId), GROUP_CLICK.name()))
                .toList();
        List<List<InlineKeyboardButton>> buttonChunks = toChunksList(buttons, 2);
        List<InlineKeyboardRow> rows = buttonChunks.stream()
                .map(InlineKeyboardRow::new)
                .toList();
        InlineKeyboardMarkup menu = telegramService.buildMenu(rows);
        telegramService.sendMessage("Виберіть групу", menu, chatId);
    }

    private List<List<InlineKeyboardButton>> toChunksList(List<InlineKeyboardButton> buttons, int chunkSize) {
        AtomicInteger counter = new AtomicInteger();
        Map<Integer, List<InlineKeyboardButton>> mapOfChunks = buttons.stream()
                .collect(Collectors.groupingBy(_ -> counter.getAndIncrement() / chunkSize));
        return new ArrayList<>(mapOfChunks.values());
    }

    private String formatButton(String groupId, String userGroupId) {
        return groupId.equals(userGroupId)
                ? "✅ " + groupId
                : "⚪ " + groupId;
    }
}
