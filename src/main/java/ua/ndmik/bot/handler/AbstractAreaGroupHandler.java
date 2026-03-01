package ua.ndmik.bot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ua.ndmik.bot.model.DtekArea;
import ua.ndmik.bot.model.Message;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;

import java.util.List;

import static ua.ndmik.bot.model.MenuCallback.GROUP_BACK;
import static ua.ndmik.bot.model.MenuCallback.GROUP_CLICK;
import static ua.ndmik.bot.model.MenuCallback.GROUP_DONE;
import static ua.ndmik.bot.model.MenuCallback.GROUP_PAGE;

public abstract class AbstractAreaGroupHandler implements CallbackHandler {
    private static final int PAGE_SIZE = 12;
    public static final String GROUP_SELECTION_TEXT = "🧩 Оберіть вашу групу відключень.\n\nПісля вибору натисніть «✅ Підтвердити».";

    private final TelegramService telegramService;
    private final ScheduleRepository scheduleRepository;
    private final UserSettingsRepository userRepository;

    protected AbstractAreaGroupHandler(TelegramService telegramService,
                                       ScheduleRepository scheduleRepository,
                                       UserSettingsRepository userRepository) {
        this.telegramService = telegramService;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
    }

    protected abstract DtekArea targetArea();

    @Override
    public void handle(Update update) {
        UserSettings user = requireUser(getChatId(update));
        reprint(update, user.getGroupId(), GROUP_SELECTION_TEXT, targetArea(), 0);
    }

    public void reprint(Update update, String userGroupId, String text) {
        UserSettings user = requireUser(getChatId(update));
        DtekArea area = user.getTmpArea() != null
                ? user.getTmpArea()
                : (user.getArea() != null ? user.getArea() : DtekArea.KYIV_REGION);
        reprint(update, userGroupId, text, area, 0);
    }

    public void reprint(Update update, String userGroupId, String text, DtekArea area, int page) {
        UserSettings user = requireUser(getChatId(update));
        if (user.getTmpArea() != area) {
            user.setTmpArea(area);
            userRepository.save(user);
        }
        editGroupSelection(update, userGroupId, text, area, page);
    }

    private void editGroupSelection(Update update, String selectedGroupId, String text, DtekArea area, int page) {
        long chatId = getChatId(update);
        List<String> groupIds = scheduleRepository.findGroupIdsByArea(area.name())
                .stream()
                .sorted()
                .toList();
        int totalPages = Math.max(1, (groupIds.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int normalizedPage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = normalizedPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, groupIds.size());
        List<String> pageGroupIds = groupIds.subList(fromIndex, toIndex);

        List<InlineKeyboardButton> buttons = pageGroupIds.stream()
                .map(groupId -> telegramService.button(
                        formatButton(groupId, selectedGroupId),
                        GROUP_CLICK.name() + ":" + groupId + ":" + area.name() + ":" + normalizedPage)
                )
                .toList();
        List<InlineKeyboardRow> rows = telegramService.chunkButtons(buttons, 2);
        rows.add(buildPaginationRow(area, normalizedPage, totalPages));
        rows.add(new InlineKeyboardRow(List.of(
                telegramService.button("⬅️ Назад", GROUP_BACK.name()),
                telegramService.button("✅ Підтвердити", GROUP_DONE.name()))
        ));
        InlineKeyboardMarkup menu = telegramService.menu(rows);

        String pagedText = text + "\n\n📄 Сторінка " + (normalizedPage + 1) + "/" + totalPages;
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        Message message = new Message(
                messageId,
                chatId,
                pagedText,
                menu
        );
        telegramService.editMessage(message);
    }

    private InlineKeyboardRow buildPaginationRow(DtekArea area, int page, int totalPages) {
        String areaName = area.name();
        String leftCallback = page > 0
                ? GROUP_PAGE.name() + ":" + areaName + ":" + (page - 1)
                : GROUP_PAGE.name() + ":" + areaName + ":" + page;
        String rightCallback = page < totalPages - 1
                ? GROUP_PAGE.name() + ":" + areaName + ":" + (page + 1)
                : GROUP_PAGE.name() + ":" + areaName + ":" + page;
        return new InlineKeyboardRow(List.of(
                telegramService.button("⬅️", leftCallback),
                telegramService.button((page + 1) + "/" + totalPages, GROUP_PAGE.name() + ":" + areaName + ":" + page),
                telegramService.button("➡️", rightCallback)
        ));
    }

    private String formatButton(String groupId, String userGroupId) {
        return groupId.equals(userGroupId)
                ? "✅ " + groupId
                : "⚪ " + groupId;
    }

    private UserSettings requireUser(long chatId) {
        return userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));
    }
}
