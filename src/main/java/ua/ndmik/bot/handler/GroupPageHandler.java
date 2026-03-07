package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.exception.UserNotFoundException;
import ua.ndmik.bot.model.callback.PagePayload;
import ua.ndmik.bot.model.common.DtekArea;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;

@Component
public class GroupPageHandler implements CallbackHandler {

    private final RegionHandler regionHandler;
    private final UserSettingsRepository userRepository;

    public GroupPageHandler(RegionHandler regionHandler,
                            UserSettingsRepository userRepository) {
        this.regionHandler = regionHandler;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(Update update) {
        long chatId = getChatId(update);
        String data = update.getCallbackQuery().getData();
        UserSettings user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new UserNotFoundException(chatId));
        DtekArea fallbackArea = user.getTmpArea() != null
                ? user.getTmpArea()
                : (user.getArea() != null ? user.getArea() : DtekArea.KYIV_REGION);
        PagePayload payload = parsePayload(data, fallbackArea);
        String selectedGroupId = user.getTmpGroupId() != null
                ? user.getTmpGroupId()
                : user.getGroupId();
        DtekArea selectedArea = user.getTmpGroupId() != null
                ? user.getTmpArea()
                : user.getArea();
        regionHandler.reprint(
                update,
                selectedGroupId,
                selectedArea,
                AbstractAreaGroupHandler.GROUP_SELECTION_TEXT,
                payload.area(),
                payload.page()
        );
    }

    private PagePayload parsePayload(String data, DtekArea fallbackArea) {
        if (data.matches("^\\d+/\\d+$")) {
            return new PagePayload(fallbackArea, parseLegacyPageIndex(data));
        }

        String[] parts = data.split(":");
        DtekArea area = fallbackArea;
        int page = 0;

        if (parts.length > 1) {
            try {
                area = DtekArea.valueOf(parts[1]);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (parts.length > 2) {
            try {
                page = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {
            }
        }
        return new PagePayload(area, Math.max(page, 0));
    }

    private int parseLegacyPageIndex(String data) {
        String[] legacyParts = data.split("/");
        if (legacyParts.length == 0) {
            return 0;
        }
        try {
            return Math.max(Integer.parseInt(legacyParts[0]) - 1, 0);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
