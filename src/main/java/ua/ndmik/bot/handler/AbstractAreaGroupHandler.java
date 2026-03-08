package ua.ndmik.bot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ua.ndmik.bot.exception.UserNotFoundException;
import ua.ndmik.bot.model.common.DtekArea;
import ua.ndmik.bot.model.telegram.Message;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;
import ua.ndmik.bot.util.GroupIdComparator;

import java.util.List;

import static ua.ndmik.bot.model.callback.MenuCallback.GROUP_BACK;
import static ua.ndmik.bot.model.callback.MenuCallback.GROUP_CLICK;
import static ua.ndmik.bot.model.callback.MenuCallback.GROUP_DONE;
import static ua.ndmik.bot.model.callback.MenuCallback.GROUP_PAGE;

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
        reprint(update, user.getGroupId(), user.getArea(), GROUP_SELECTION_TEXT, targetArea(), 0);
    }

    public void reprint(Update update, String selectedGroupId, DtekArea selectedArea, String text) {
        UserSettings user = requireUser(getChatId(update));
        DtekArea area = user.getTmpArea() != null
                ? user.getTmpArea()
                : (user.getArea() != null ? user.getArea() : DtekArea.KYIV_REGION);
        reprint(update, selectedGroupId, selectedArea, text, area, 0);
    }

    public void reprint(Update update, String selectedGroupId, DtekArea selectedArea, String text, DtekArea area, int page) {
        UserSettings user = requireUser(getChatId(update));
        if (user.getTmpArea() != area) {
            user.setTmpArea(area);
            user.setTmpGroupId(null);
            userRepository.save(user);
        }
        editGroupSelection(update, selectedGroupId, selectedArea, text, area, page);
    }

    private void editGroupSelection(Update update,
                                    String selectedGroupId,
                                    DtekArea selectedArea,
                                    String text,
                                    DtekArea area,
                                    int page) {
        long chatId = getChatId(update);
        List<String> groupIds = scheduleRepository.findGroupIdsByArea(area.name())
                .stream()
                .sorted(GroupIdComparator.INSTANCE)
                .toList();
        int totalPages = Math.max(1, (groupIds.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int normalizedPage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = normalizedPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, groupIds.size());
        List<String> pageGroupIds = groupIds.subList(fromIndex, toIndex);
        List<InlineKeyboardButton> buttons = pageGroupIds.stream()
                .map(groupId -> telegramService.button(
                        formatButton(groupId, selectedGroupId, selectedArea, area),
                        GROUP_CLICK.name() + ":" + groupId + ":" + area.name() + ":" + normalizedPage)
                )
                .toList();
        List<InlineKeyboardRow> rows = telegramService.chunkButtons(buttons, 2);
        if (totalPages > 1) {
            rows.add(buildPaginationRow(area, normalizedPage, totalPages));
        }
        rows.add(new InlineKeyboardRow(List.of(
                telegramService.button("⬅️ Назад", GROUP_BACK.name()),
                telegramService.button("✅ Підтвердити", GROUP_DONE.name()))
        ));
        InlineKeyboardMarkup menu = telegramService.menu(rows);

        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        Message message = new Message(
                messageId,
                chatId,
                text,
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

    private String formatButton(String groupId, String selectedGroupId, DtekArea selectedArea, DtekArea area) {
        return isSelectedForArea(groupId, selectedGroupId, selectedArea, area)
                ? "✅ " + groupId
                : "⚪ " + groupId;
    }

    static boolean isSelectedForArea(String groupId, String selectedGroupId, DtekArea selectedArea, DtekArea area) {
        return selectedArea == area && groupId.equals(selectedGroupId);
    }

    private UserSettings requireUser(long chatId) {
        return userRepository.findByChatId(chatId)
                .orElseThrow(() -> new UserNotFoundException(chatId));
    }
}
