package ua.ndmik.bot.model;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public record Message(
        Integer messageId,
        Long chatId,
        String text,
        InlineKeyboardMarkup menu
) {
}
