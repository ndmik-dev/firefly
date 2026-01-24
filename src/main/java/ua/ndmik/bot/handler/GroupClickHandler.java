package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;

import java.util.Optional;

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
        String groupId = extractGroupIdFromCallbackData(data);
        UserSettings user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));
        user.setGroupId(groupId);
        userRepository.save(user);
        regionHandler.handle(update);
    }

    private String extractGroupIdFromCallbackData(String data) {
        return data.substring(data.indexOf(':') + 1);
    }
}
