package ua.ndmik.bot.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.ndmik.bot.handler.CallbackHandlerResolver;
import ua.ndmik.bot.model.common.DtekArea;
import ua.ndmik.bot.model.callback.MenuCallback;
import ua.ndmik.bot.model.yasno.ResolvedYasnoGroup;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;
import ua.ndmik.bot.service.YasnoGroupResolverService;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Profile("!test")
public class DtekShutdownBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private static final Pattern LEGACY_PAGE_CALLBACK = Pattern.compile("^\\d+/\\d+$");

    private final String botToken;
    private final TelegramService telegramService;
    private final CallbackHandlerResolver callbackHandlerResolver;
    private final UserSettingsRepository userRepository;
    private final YasnoGroupResolverService yasnoGroupResolverService;

    public DtekShutdownBot(@Value("${telegram.bot-token}") String botToken,
                           TelegramService telegramService,
                           CallbackHandlerResolver callbackHandlerResolver,
                           UserSettingsRepository userRepository,
                           YasnoGroupResolverService yasnoGroupResolverService) {
        this.botToken = botToken;
        this.telegramService = telegramService;
        this.callbackHandlerResolver = callbackHandlerResolver;
        this.userRepository = userRepository;
        this.yasnoGroupResolverService = yasnoGroupResolverService;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
            return;
        }

        if (update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }

    private void handleMessage(Update update) {
        String text = update.getMessage().getText().trim();
        if (isCommand(text, "/start")) {
            telegramService.sendGreeting(update);
            return;
        }
        if (isCommand(text, "/stats_today")) {
            telegramService.sendTodayStats(update.getMessage().getChatId());
            return;
        }
        if (isCommand(text, "/stats_week")) {
            telegramService.sendWeeklyStats(update.getMessage().getChatId());
            return;
        }

        handleAddressLookup(update, text);
    }

    private void handleCallback(Update update) {
        try {
            String data = update.getCallbackQuery().getData();
            MenuCallback callback = resolveCallback(data);
            callbackHandlerResolver.getHandler(callback).handle(update);
        } finally {
            telegramService.answerCallback(update.getCallbackQuery().getId());
        }
    }

    private MenuCallback resolveCallback(String data) {
        String callbackKey = data.split(":", 2)[0];
        try {
            return MenuCallback.valueOf(callbackKey);
        } catch (IllegalArgumentException e) {
            if (LEGACY_PAGE_CALLBACK.matcher(data).matches()) {
                return MenuCallback.GROUP_PAGE;
            }
            return MenuCallback.DEFAULT;
        }
    }

    private boolean isCommand(String text, String command) {
        return text.equals(command) || text.startsWith(command + "@");
    }

    private void handleAddressLookup(Update update, String text) {
        long chatId = update.getMessage().getChatId();
        Optional<UserSettings> userOptional = userRepository.findByChatId(chatId);
        if (userOptional.isEmpty()) {
            return;
        }

        UserSettings user = userOptional.get();
        if (!user.isAwaitingAddressInput()) {
            return;
        }

        Optional<ResolvedYasnoGroup> resolved;
        try {
            resolved = yasnoGroupResolverService.resolveByAddress(text);
        } catch (RuntimeException ex) {
            telegramService.sendUpdate(
                    user,
                    """
                            ⚠️ Не вдалося обробити адресу.
                            Надішліть адресу у форматі «вулиця, будинок».
                            Підтримуються лише адреси у м. Київ.
                            """
            );
            return;
        }

        if (resolved.isEmpty()) {
            telegramService.sendUpdate(
                    user,
                    """
                            ⚠️ Групу за цією адресою не знайдено.
                            Перевірте формат «вулиця, будинок», наприклад: «вул. Хрещатик, 22».
                            Працює лише для адрес у м. Київ.
                            """
            );
            return;
        }

        user.setGroupId(resolved.get().groupId());
        user.setArea(DtekArea.KYIV);
        user.setTmpGroupId(null);
        user.setTmpArea(null);
        user.setAwaitingAddressInput(false);
        userRepository.save(user);
        telegramService.sendUpdate(user, "✅ Групу для м. Київ знайдено і збережено: <b>%s</b>".formatted(user.getGroupId()));
    }
}
