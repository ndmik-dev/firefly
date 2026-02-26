package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.model.DtekArea;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;

@Component
public class KyivHandler implements CallbackHandler {
    private final UserSettingsRepository userRepository;
    private final RegionHandler regionHandler;

    public KyivHandler(UserSettingsRepository userRepository,
                       RegionHandler regionHandler) {
        this.userRepository = userRepository;
        this.regionHandler = regionHandler;
    }

    @Override
    public void handle(Update update) {
        long chatId = getChatId(update);
        UserSettings user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));
        regionHandler.reprint(update, user.getGroupId(), RegionHandler.GROUP_SELECTION_TEXT, DtekArea.KYIV, 0);
    }
}
