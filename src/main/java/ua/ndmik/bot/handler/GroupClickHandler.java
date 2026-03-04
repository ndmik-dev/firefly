package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.model.DtekArea;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;

@Component
public class GroupClickHandler implements CallbackHandler {

    private final UserSettingsRepository userRepository;
    private final RegionHandler regionHandler;

    public GroupClickHandler(UserSettingsRepository userRepository,
                             RegionHandler regionHandler) {
        this.userRepository = userRepository;
        this.regionHandler = regionHandler;
    }

    @Override
    public void handle(Update update) {
        long chatId = getChatId(update);
        String data = update.getCallbackQuery().getData();
        SelectionPayload payload = parsePayload(data);
        UserSettings user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));
        user.setTmpGroupId(payload.groupId());
        user.setTmpArea(payload.area());
        userRepository.save(user);
        regionHandler.reprint(
                update,
                payload.groupId(),
                payload.area(),
                "✅ Групу обрано. Натисніть «✅ Підтвердити», щоб зберегти вибір.",
                payload.area(),
                payload.page()
        );
    }

    private SelectionPayload parsePayload(String data) {
        String[] parts = data.split(":");
        String groupId = parts.length > 1 ? parts[1] : "";
        DtekArea area = DtekArea.KYIV_REGION;
        int page = 0;

        if (parts.length > 2) {
            try {
                area = DtekArea.valueOf(parts[2]);
            } catch (IllegalArgumentException ignored) {
                area = DtekArea.KYIV_REGION;
            }
        }
        if (parts.length > 3) {
            try {
                page = Integer.parseInt(parts[3]);
            } catch (NumberFormatException ignored) {
                page = 0;
            }
        }
        return new SelectionPayload(groupId, area, Math.max(page, 0));
    }

    private record SelectionPayload(String groupId, DtekArea area, int page) {
    }
}
