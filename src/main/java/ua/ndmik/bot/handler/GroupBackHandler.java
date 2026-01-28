package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;

@Component
public class GroupBackHandler implements CallbackHandler {

    private final GroupSelectionHandler groupSelectionHandler;
    private final UserSettingsRepository userRepository;

    public GroupBackHandler(GroupSelectionHandler groupSelectionHandler,
                            UserSettingsRepository userRepository) {
        this.groupSelectionHandler = groupSelectionHandler;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(Update update) {
        long chatId = getChatId(update);
        UserSettings user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException(String.format("User not found for chatId=%s", chatId)));
        user.setTmpGroupId(null);
        userRepository.save(user);
        groupSelectionHandler.handle(update);
    }
}
