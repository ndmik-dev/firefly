package ua.ndmik.bot.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ua.ndmik.bot.model.MenuCallback.*;

@Service
@Slf4j
@Setter
public class TelegramService {

    private final TelegramClient telegramClient;
    private final UserSettingsRepository userRepository;
    private Integer previousMessageId;

    public TelegramService(@Value("${telegram.bot-token}") String botToken,
                           UserSettingsRepository userRepository) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.userRepository = userRepository;
    }

    public void sendMainMenu(Update update) {
        long chatId = update.getMessage() != null
                ? update.getMessage().getChatId()
                : update.getCallbackQuery().getMessage().getChatId();

        UserSettings user = userRepository.findByChatId(chatId)
                .orElseGet(() -> createNewUser(chatId));
//        boolean needToCreate = update.getMessage() != null && userOpt.isEmpty();

        InlineKeyboardRow regions = new InlineKeyboardRow(
                List.of(
                        button("Київ", KYIV.name()),
                        button("Київщина", REGION.name())
                ));
        String notificationText = user.isNotificationEnabled()
                ? "Вимкнути сповіщення"
                : "Увімкнути сповіщення";
        InlineKeyboardRow notifications = new InlineKeyboardRow(List.of(
                button(notificationText, NOTIFICATION_CLICK.name())
        ));
        InlineKeyboardMarkup menu = menu(List.of(regions, notifications));
        sendMessage("Вітаю, оберіть дію", menu, chatId);
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

    public void cleanUpOldMessage(String chatId) {
        if (previousMessageId == null) {
            return;
        }
        DeleteMessage deleteMessage = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(previousMessageId)
                .build();
        try {
            telegramClient.execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Exception while deleting message, chatId={}, messageId={}", chatId, previousMessageId, e);
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
}
