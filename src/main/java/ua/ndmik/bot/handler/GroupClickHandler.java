package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;

import java.util.Optional;

@Component
public class GroupClickHandler implements CallbackHandler {

    private final UserSettingsRepository userRepository;

    public GroupClickHandler(UserSettingsRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void handle(Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        Optional<UserSettings> user = userRepository.findByChatId(chatId);
        String userGroupId = user.isPresent()
                ? user.get().getGroupId()
                : "";
        String clickedGroupId = extractGroupId(update.getCallbackQuery().getData());
        System.out.println();
    }

    private String extractGroupId(String buttonText) {
        return buttonText.replaceAll("[^0-9.]", "");
    }
}
