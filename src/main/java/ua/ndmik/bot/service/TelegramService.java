package ua.ndmik.bot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
public class TelegramService {

    private final TelegramClient telegramClient;

    public TelegramService(@Value("${telegram.bot-token}") String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }


}
