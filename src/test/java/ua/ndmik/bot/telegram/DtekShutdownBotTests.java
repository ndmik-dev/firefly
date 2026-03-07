package ua.ndmik.bot.telegram;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ua.ndmik.bot.exception.ApplicationExceptionReporter;
import ua.ndmik.bot.handler.CallbackHandler;
import ua.ndmik.bot.handler.CallbackHandlerResolver;
import ua.ndmik.bot.model.common.DtekArea;
import ua.ndmik.bot.model.callback.MenuCallback;
import ua.ndmik.bot.model.yasno.ResolvedYasnoGroup;
import ua.ndmik.bot.model.entity.UserSettings;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;
import ua.ndmik.bot.service.YasnoGroupResolverService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class DtekShutdownBotTests {

    private final TelegramService telegramService = mock(TelegramService.class);
    private final ApplicationExceptionReporter exceptionReporter = mock(ApplicationExceptionReporter.class);
    private final CallbackHandlerResolver callbackHandlerResolver = mock(CallbackHandlerResolver.class);
    private final UserSettingsRepository userRepository = mock(UserSettingsRepository.class);
    private final YasnoGroupResolverService yasnoGroupResolverService = mock(YasnoGroupResolverService.class);
    private final DtekShutdownBot bot = new DtekShutdownBot(
            "TEST_TOKEN",
            telegramService,
            exceptionReporter,
            callbackHandlerResolver,
            userRepository,
            yasnoGroupResolverService
    );

    @Test
    void consume_resolvesAndSavesGroupWhenUserIsAwaitingAddressInput() {
        long chatId = 123L;
        UserSettings user = UserSettings.builder()
                .chatId(chatId)
                .awaitingAddressInput(true)
                .build();
        ResolvedYasnoGroup resolved = new ResolvedYasnoGroup(
                25,
                902,
                1L,
                "Хрещатик",
                2L,
                "22",
                "29.1"
        );
        given(userRepository.findByChatId(chatId)).willReturn(Optional.of(user));
        given(yasnoGroupResolverService.resolveByAddress("вул. Хрещатик, 22")).willReturn(Optional.of(resolved));

        bot.consume(messageUpdate(chatId, "вул. Хрещатик, 22"));

        assertThat(user.getGroupId()).isEqualTo("29.1");
        assertThat(user.getArea()).isEqualTo(DtekArea.KYIV);
        assertThat(user.isAwaitingAddressInput()).isFalse();
        then(userRepository).should().save(user);
        then(telegramService).should().sendUpdate(eq(user), contains("✅"));
    }

    @Test
    void consume_doesNotResolveAddressWhenUserIsNotAwaitingInput() {
        long chatId = 456L;
        UserSettings user = UserSettings.builder()
                .chatId(chatId)
                .awaitingAddressInput(false)
                .build();
        given(userRepository.findByChatId(chatId)).willReturn(Optional.of(user));

        bot.consume(messageUpdate(chatId, "вул. Хрещатик, 22"));

        then(yasnoGroupResolverService).should(never()).resolveByAddress(anyString());
        then(userRepository).should(never()).save(user);
        then(telegramService).should(never()).sendUpdate(eq(user), anyString());
    }

    @Test
    void consume_routesLegacyPageFractionCallbackToGroupPageHandler() {
        Update update = callbackUpdate(777L, "cbq-1", "1/5");
        CallbackHandler groupPageHandler = mock(CallbackHandler.class);
        given(callbackHandlerResolver.getHandler(MenuCallback.GROUP_PAGE)).willReturn(groupPageHandler);

        bot.consume(update);

        then(groupPageHandler).should().handle(update);
        then(telegramService).should().answerCallback("cbq-1");
    }

    @Test
    void consume_reportsUnhandledExceptionFromCallbackHandler() {
        Update update = callbackUpdate(888L, "cbq-2", "GROUP_PAGE:KYIV:0");
        CallbackHandler callbackHandler = mock(CallbackHandler.class);
        given(callbackHandlerResolver.getHandler(MenuCallback.GROUP_PAGE)).willReturn(callbackHandler);
        doThrow(new RuntimeException("boom")).when(callbackHandler).handle(update);

        bot.consume(update);

        then(exceptionReporter).should().report(eq("telegram update processing"), any(RuntimeException.class));
        then(telegramService).should().answerCallback("cbq-2");
    }

    private Update messageUpdate(long chatId, String text) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        given(update.hasMessage()).willReturn(true);
        given(update.getMessage()).willReturn(message);
        given(message.hasText()).willReturn(true);
        given(message.getText()).willReturn(text);
        given(message.getChatId()).willReturn(chatId);
        given(update.hasCallbackQuery()).willReturn(false);
        return update;
    }

    private Update callbackUpdate(long chatId, String callbackId, String callbackData) {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        given(update.hasMessage()).willReturn(false);
        given(update.hasCallbackQuery()).willReturn(true);
        given(update.getCallbackQuery()).willReturn(callbackQuery);
        given(callbackQuery.getData()).willReturn(callbackData);
        given(callbackQuery.getId()).willReturn(callbackId);
        given(callbackQuery.getMessage()).willReturn(message);
        given(message.getChatId()).willReturn(chatId);
        return update;
    }
}
