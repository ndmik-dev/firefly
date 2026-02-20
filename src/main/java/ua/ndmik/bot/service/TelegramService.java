package ua.ndmik.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.ndmik.bot.model.Message;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final StatsService statsService;
    private final Set<Long> adminChatIds;

    public static String GREETING = """
                ⚡️ <b>Firefly • Графіки відключень ДТЕК</b>

                Показую, коли буде світло за вашою групою відключень.
                Оберіть групу, щоб переглядати графік і керувати сповіщеннями.
                """;

    public TelegramService(@Value("${telegram.bot-token}") String botToken,
                           @Value("${telegram.admin-chat-ids:}") String adminChatIdsRaw,
                           UserSettingsRepository userRepository,
                           ScheduleRepository scheduleRepository,
                           DtekShutdownsService dtekService,
                           StatsService statsService) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
        this.dtekService = dtekService;
        this.statsService = statsService;
        this.adminChatIds = parseAdminChatIds(adminChatIdsRaw);
    }

    public void sendGreeting(Update update) {
        UserSettings user = getOrCreateUser(update);
        InlineKeyboardMarkup menu = buildMainMenuMarkup(user);
        Message message = new Message(
                null,
                user.getChatId(),
                GREETING,
                menu
        );
        sendNewMessage(message);
    }

    public void sendUpdate(UserSettings user, String header) {
        Message message = new Message(
                null,
                user.getChatId(),
                formatMessage(user, header),
                buildMainMenuMarkup(user)
        );
        boolean sent = sendNewMessage(message);
        if (sent) {
            statsService.recordNotificationSent();
        } else {
            statsService.recordNotificationFailed();
        }
    }

    public void sendTodayStats(long chatId) {
        if (!isAdminChat(chatId)) {
            sendText(chatId, "⛔️ Команда доступна тільки адміну.");
            return;
        }
        sendText(chatId, statsService.buildTodayStatsMessage());
    }

    public void sendWeeklyStats(long chatId) {
        if (!isAdminChat(chatId)) {
            sendText(chatId, "⛔️ Команда доступна тільки адміну.");
            return;
        }
        sendText(chatId, statsService.buildWeeklyStatsMessage());
    }

    public void sendDailySummaryToAdmins() {
        if (adminChatIds.isEmpty()) {
            log.debug("No admin chat IDs configured, skipping daily summary.");
            return;
        }
        String summary = statsService.buildTodayStatsMessage();
        adminChatIds.forEach(chatId -> sendText(chatId, summary));
    }

    private void sendText(long chatId, String text) {
        Message message = new Message(null, chatId, text, null);
        sendNewMessage(message);
    }

    private boolean sendNewMessage(Message message) {
        SendMessage sendMessage = SendMessage
                .builder()
                .text(message.text())
                .chatId(message.chatId())
                .replyMarkup(message.menu())
                .parseMode(ParseMode.HTML)
                .build();
        try {
            log.info("Sending message to chatId={}", message.chatId());
            telegramClient.execute(sendMessage);
            return true;
        } catch (TelegramApiException e) {
            log.error("Exception while sending message, chatId={}", message.chatId(), e);
            return false;
        }
    }

    public void editMessage(Message message) {
        EditMessageText editMessage = EditMessageText
                .builder()
                .chatId(message.chatId())
                .messageId(message.messageId())
                .text(message.text())
                .replyMarkup(message.menu())
                .parseMode(ParseMode.HTML)
                .build();
        try {
            log.info("Editing message in chatId={}, messageId={}", message.chatId(), message.messageId());
            telegramClient.execute(editMessage);
        } catch (TelegramApiException e) {
            log.error("Exception while editing message, chatId={}, messageId={}", message.chatId(), message.messageId(), e);
        }
    }

    public void answerCallback(String callbackQueryId) {
        AnswerCallbackQuery answerCallbackQuery = AnswerCallbackQuery
                .builder()
                .callbackQueryId(callbackQueryId)
                .build();
        try {
            telegramClient.execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            log.error("Exception while answering callbackQueryId={}", callbackQueryId, e);
        }
    }

    public String formatMessage(UserSettings user, String header) {
        String template = """
                %s
                ⚙️ <b>Ваші налаштування</b>
                
                🧩 Група відключень: <b>%s</b>
                🔔 Сповіщення: <b>%s</b>

                %s
                """;
        header = Strings.isNotBlank(header)
                ? (header + "\n")
                : "";
        String groupId = user.getGroupId();
        String displayGroupId = formatGroupInfo(groupId);
        String notificationStatus = formatNotificationInfo(user.isNotificationEnabled());
        List<Schedule> schedules = scheduleRepository.findAllByGroupId(groupId);
        String shutdowns = dtekService.getShutdownsMessage(schedules);
        return String.format(template, header, displayGroupId, notificationStatus, shutdowns);
    }

    private UserSettings getOrCreateUser(Update update) {
        long chatId = update.getMessage() != null
                ? update.getMessage().getChatId()
                : update.getCallbackQuery().getMessage().getChatId();
        log.info("Finding user with chatId={}", chatId);
        return userRepository.findByChatId(chatId)
                .orElseGet(() -> createNewUser(chatId));
    }

    private UserSettings createNewUser(Long chatId) {
        log.info("Creating new user with chatId={}", chatId);
        UserSettings user = userRepository.save(UserSettings.builder()
                .chatId(chatId)
                .isNotificationEnabled(true)
                .build());
        statsService.recordNewUser();
        return user;
    }

    public InlineKeyboardMarkup buildMainMenuMarkup(UserSettings user) {
        InlineKeyboardRow group = new InlineKeyboardRow(List.of(
                button(groupButtonText(user), GROUP_SELECTION.name())
        ));
        if (user.getGroupId() == null) {
            return menu(List.of(group));
        }
        InlineKeyboardRow notifications = new InlineKeyboardRow(List.of(
                button(notificationButtonText(user), NOTIFICATION_CLICK.name())
        ));
        return menu(List.of(group, notifications));
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

    private String groupButtonText(UserSettings user) {
        return user.getGroupId() != null
                ? "🧩 Змінити групу"
                : "🧩 Обрати групу";
    }

    private String notificationButtonText(UserSettings user) {
        return user.isNotificationEnabled()
                ? "🔕 Вимкнути сповіщення"
                : "🔔 Увімкнути сповіщення";
    }

    private String formatNotificationInfo(boolean isEnabled) {
        return isEnabled
                ? "✅ Увімкнено"
                : "❌ Вимкнено";
    }

    private String formatGroupInfo(String groupId) {
        return Strings.isBlank(groupId)
                ? "❗ Не обрано"
                : groupId;
    }

    private boolean isAdminChat(long chatId) {
        return adminChatIds.contains(chatId);
    }

    private Set<Long> parseAdminChatIds(String raw) {
        if (Strings.isBlank(raw)) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(Strings::isNotBlank)
                .map(this::parseAdminChatId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Long parseAdminChatId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid telegram.admin-chat-ids value skipped: {}", value);
            return null;
        }
    }
}
