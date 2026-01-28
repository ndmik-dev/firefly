package ua.ndmik.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ua.ndmik.bot.model.MenuCallback.GROUP_SELECTION;
import static ua.ndmik.bot.model.MenuCallback.NOTIFICATION_CLICK;

@Service
@Slf4j
public class TelegramService {

    private final TelegramClient telegramClient;
    private final UserSettingsRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final DtekShutdownsService dtekService;

    public TelegramService(@Value("${telegram.bot-token}") String botToken,
                           UserSettingsRepository userRepository,
                           ScheduleRepository scheduleRepository,
                           DtekShutdownsService dtekService) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
        this.dtekService = dtekService;
    }

    public void sendGreeting(Update update) {
        String greeting = """
                ⚡️ DTEK Shutdowns Bot
                
                Привіт! Я надішлю тобі графіки відключень і попереджу, якщо щось змінилось.
                
                Обери свою групу 🧩 та дивись графік на сьогодні/завтра 📅
                """;
        sendMessage(update, greeting);
    }

    public void sendMessage(Update update) {
        //TODO: add title instead of menu text
        String menuTemplate = """
                🏠 Меню
                
                🧩 Група: %s
                🔔 Сповіщення: %s
                
                %s
                """;
        UserSettings user = getOrCreateUser(update);
        String groupInfo = formatGroupInfo(user.getGroupId());
        List<Schedule> schedules = scheduleRepository.findAllByGroupId(user.getGroupId());
        String shutdowns = dtekService.getShutdownsMessage(schedules);
        String notificationInfo = formatNotificationInfo(user.isNotificationEnabled());
        sendMessage(update, String.format(menuTemplate, groupInfo, notificationInfo, shutdowns));
    }

    public void sendMessage(Update update, String text) {
        InlineKeyboardMarkup menu = buildMainMenuMarkup(getOrCreateUser(update));
        sendMessage(update, text, menu);
    }

    public void sendMessage(Update update, String text, InlineKeyboardMarkup markup) {
        if (update.getCallbackQuery() != null && update.getCallbackQuery().getMessage() != null) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            editMessage(text, markup, chatId, messageId);
            return;
        }
        if (update.getMessage() != null) {
            long chatId = update.getMessage().getChatId();
            sendMessage(text, markup, chatId);
        }
    }

    public void sendMessage(String text, InlineKeyboardMarkup markup, long chatId) {
        SendMessage message = SendMessage
                .builder()
                .text(text)
                .chatId(chatId)
                .replyMarkup(markup)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Exception while sending message, chatId={}", chatId, e);
        }
    }

    public InlineKeyboardMarkup menu(List<InlineKeyboardRow> rows) {
        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardButton button(String text, String callback) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callback)
                .build();
    }

    public List<InlineKeyboardRow> chunkButtons(List<InlineKeyboardButton> buttons, int chunkSize) {
        AtomicInteger counter = new AtomicInteger();
        Map<Integer, List<InlineKeyboardButton>> mapOfChunks = buttons.stream()
                .collect(Collectors.groupingBy(_ -> counter.getAndIncrement() / chunkSize));

        return mapOfChunks.values()
                .stream()
                .map(InlineKeyboardRow::new)
                .collect(Collectors.toList());
    }

    private UserSettings createNewUser(Long chatId) {
        return userRepository.save(UserSettings.builder()
                .chatId(chatId)
                .isNotificationEnabled(true)
                .build());
    }

    private UserSettings getOrCreateUser(Update update) {
        long chatId = update.getMessage() != null
                ? update.getMessage().getChatId()
                : update.getCallbackQuery().getMessage().getChatId();
        return userRepository.findByChatId(chatId)
                .orElseGet(() -> createNewUser(chatId));
    }

    private InlineKeyboardMarkup buildMainMenuMarkup(UserSettings user) {
        if (user.getGroupId() != null) {
            InlineKeyboardRow group = new InlineKeyboardRow(List.of(
                    button("🧩 Змінити групу", GROUP_SELECTION.name())
            ));
            String notificationText = user.isNotificationEnabled()
                    ? "🔕 Вимкнути сповіщення"
                    : "🔔 Увімкнути сповіщення";
            InlineKeyboardRow notifications = new InlineKeyboardRow(List.of(
                    button(notificationText, NOTIFICATION_CLICK.name())
            ));
            return menu(List.of(group, notifications));
        } else {
            InlineKeyboardRow group = new InlineKeyboardRow(List.of(
                    button("🧩 Обрати групу", GROUP_SELECTION.name())
            ));
            return menu(List.of(group));
        }
    }

    private String formatGroupInfo(String groupId) {
        return Strings.isNotEmpty(groupId)
                ? groupId
                : "Оберіть групу відключень нижче";
    }

    private String formatNotificationInfo(boolean isEnabled) {
        return isEnabled
                ? "✅ Увімкнено"
                : "❌ Вимкнено";
    }

    private void editMessage(String text, InlineKeyboardMarkup markup, long chatId, int messageId) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .replyMarkup(markup)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Exception while editing message, chatId={}, messageId={}", chatId, messageId, e);
        }
    }
}
