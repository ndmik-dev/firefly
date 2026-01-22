package ua.ndmik.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@Slf4j
public class DefaultHandler implements CallbackHandler {
    @Override
    public void handle(Update update) {
        log.info("HANDLER FOR THIS ACTION IS NOT IMPLEMENTED");
    }
}
