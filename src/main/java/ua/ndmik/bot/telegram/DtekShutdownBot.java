package ua.ndmik.bot.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.ndmik.bot.client.DtekClient;
import ua.ndmik.bot.converter.ScheduleResponseConverter;
import ua.ndmik.bot.handler.CallbackHandlerResolver;
import ua.ndmik.bot.model.MenuCallback;
import ua.ndmik.bot.model.ScheduleResponse;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.model.entity.UserState;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.DtekShutdownsService;
import ua.ndmik.bot.service.ShutdownTelegramFormatter;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static ua.ndmik.bot.model.MenuCallback.KYIV;
import static ua.ndmik.bot.model.MenuCallback.REGION;
import static ua.ndmik.bot.model.entity.ScheduleDay.TODAY;
import static ua.ndmik.bot.model.entity.ScheduleDay.TOMORROW;

@Service
public class DtekShutdownBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final DtekClient dtekClient;
    private final DtekShutdownsService shutdownsService;
    private final ShutdownTelegramFormatter formatter;
    private final String botToken;
    private final TelegramClient telegramClient;
    private final UserSettingsRepository userRepository;
    private final ScheduleResponseConverter converter;
    private final ScheduleRepository scheduleRepository;
    private final CallbackHandlerResolver callbackHandlerResolver;

    public DtekShutdownBot(DtekClient dtekClient,
                           DtekShutdownsService shutdownsService,
                           ShutdownTelegramFormatter formatter,
                           UserSettingsRepository userRepository,
                           @Value("${telegram.bot-token}") String botToken,
                           ScheduleResponseConverter converter,
                           ScheduleRepository scheduleRepository,
                           CallbackHandlerResolver callbackHandlerResolver) {
        this.dtekClient = dtekClient;
        this.shutdownsService = shutdownsService;
        this.formatter = formatter;
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(getBotToken());
        this.userRepository = userRepository;
        this.converter = converter;
        this.scheduleRepository = scheduleRepository;
        this.callbackHandlerResolver = callbackHandlerResolver;
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
        long chatId = update.getMessage().getChatId();
        UserSettings user = userRepository.findByChatId(chatId)
                .orElse(newUser(chatId));

        String text = update.getMessage().getText().trim();
        UserState state = user.getState();

        if (text.equals("/start") && UserState.NONE.equals(state)) {
            user.setState(UserState.WAITING_FOR_GROUP);
            userRepository.save(user);
            InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(
                            List.of(
                                    InlineKeyboardButton.builder()
                                            .text("Київ")
                                            .callbackData(KYIV.name())
                                            .build(),
                                    InlineKeyboardButton.builder()
                                            .text("Київщина")
                                            .callbackData(REGION.name())
                                            .build()
                            )
                    ))
                    .build();
            sendMenu(markup, chatId);
//            sendMessage("Надішліть номер групи відключень у форматі \"4.1\".", chatId);
            return;
        }

        if (text.equals("/edit")) {
            user.setGroupId(null);
            user.setState(UserState.WAITING_FOR_GROUP);
            userRepository.save(user);
            sendMessage("Надішліть новий номер групи відключень у форматі \"4.1\".", chatId);
            return;
        }

        if (text.equals("/disable")) {
            user.setNotificationEnabled(false);
            userRepository.save(user);
            sendMessage("Сповіщення про відключення вимкнено", chatId);
            return;
        }

        if (text.equals("/enable")) {
            user.setNotificationEnabled(true);
            userRepository.save(user);
            sendMessage("Спровіщення про відключення увімкнено", chatId);
            return;
        }

        if (UserState.WAITING_FOR_GROUP.equals(state)) {
            if (isGroupValid(text)) {
                user.setGroupId(text);
                user.setState(UserState.READY);
                userRepository.save(user);
                List<Schedule> schedules = scheduleRepository.findAllByGroupId(text);
                Schedule todaySchedule = schedules.stream()
                        .filter(schedule -> TODAY.equals(schedule.getScheduleDay()))
                        .toList()
                        .getFirst();
                Schedule tomorrowSchedule = schedules.stream()
                        .filter(schedule -> TOMORROW.equals(schedule.getScheduleDay()))
                        .toList()
                        .getFirst();
                Map<LocalTime, LocalTime> todayShutdowns = shutdownsService.getShutdowns(todaySchedule);
                Map<LocalTime, LocalTime> tomorrowShutdowns = shutdownsService.getShutdowns(tomorrowSchedule);

                String todayShutdownsFormatted = formatter.format(todayShutdowns);
                String tomorrowShutdownsFormatted = formatter.format(tomorrowShutdowns);

                sendMessage(todayShutdownsFormatted, chatId);
                sendMessage(tomorrowShutdownsFormatted, chatId);
//                sendMessage("Триматиму в курсі всіх змін у графіках", chatId);
                return;
            }
            sendMessage("Невірно введено групу відключень, повторіть спробу", chatId);
        }
    }

    private void handleCallback(Update update) {
        MenuCallback callback = MenuCallback.valueOf(update.getCallbackQuery().getData());
        callbackHandlerResolver.getHandler(callback).handle(update);
    }

    private UserSettings newUser(Long chatId) {
        return userRepository.save(UserSettings.builder()
                .chatId(chatId)
                .state(UserState.NONE)
                .isNotificationEnabled(true)
                .build());
    }

    private void sendMenu(InlineKeyboardMarkup markup, long chatId) {
        SendMessage message = SendMessage // Create a message object
                .builder()
                .text("Вітаю")
                .chatId(chatId)
                .replyMarkup(markup)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            IO.println("Exception while sending message");
        }
    }

    private void sendMessage(String text, long chatId) {
        SendMessage message = SendMessage // Create a message object
                .builder()
                .chatId(chatId)
                .text(text)
                .parseMode(ParseMode.HTML)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            IO.println("Exception while sending message");
        }
    }

    private boolean isGroupValid(String group) {
        return group != null && group.matches("\\b[1-6]\\.[1-2]\\b");
    }

    //TODO: rm | just for test
    public void process() {
        ScheduleResponse scheduleResponse = dtekClient.getShutdownsSchedule();
        String lastShutdownsUpdate = scheduleResponse.update();
        List<Schedule> scheduleList = converter.toSchedules(scheduleResponse);
//        Map<LocalTime, LocalTime> todayShutdowns = shutdownsService.getShutdowns(scheduleResponse, "2.1");
//        Map<LocalTime, LocalTime> tomorrowShutdowns = shutdownsService.getTomorrowShutdowns(scheduleResponse, "2.1");

//        String todayShutdownsFormatted = formatter.format(todayShutdowns);
//        String tomorrowShutdownsFormatted = formatter.format(tomorrowShutdowns);

        IO.println(String.format("lastUpdate=%s", lastShutdownsUpdate));
        IO.println("===TODAY SHUTDOWNS SCHEDULE===");
//        IO.println(todayShutdownsFormatted);
        IO.println("===TOMORROW SHUTDOWNS SCHEDULE===");
//        IO.println(tomorrowShutdownsFormatted);
    }
}
