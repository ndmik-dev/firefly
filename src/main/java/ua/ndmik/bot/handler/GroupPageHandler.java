package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.model.DtekArea;
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
        PagePayload payload = parsePayload(data);
        UserSettings user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));
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

    private PagePayload parsePayload(String data) {
        String[] parts = data.split(":");
        DtekArea area = DtekArea.KYIV_REGION;
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

    private record PagePayload(DtekArea area, int page) {
    }
}
