package ua.ndmik.bot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApplicationExceptionReporter {

    public void report(String context, Throwable throwable) {
        log.error("Unhandled exception in {}", context, throwable);
    }
}
