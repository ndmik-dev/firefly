package ua.ndmik.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DtekTelegramBotApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"));
        SpringApplication.run(DtekTelegramBotApplication.class, args);
    }
}
