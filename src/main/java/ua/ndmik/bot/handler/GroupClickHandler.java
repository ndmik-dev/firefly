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
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();
        String groupId = extractGroupIdFromCallbackData(data);
        Optional<UserSettings> user = userRepository.findByChatId(chatId);
        user.ifPresent(settings -> {
            settings.setGroupId(groupId);
            userRepository.save(settings);
        });
        regionHandler.handle(update);
    }

    private String extractGroupIdFromCallbackData(String data) {
        return data.substring(data.indexOf(':') + 1);
    }
}
