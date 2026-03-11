package ua.ndmik.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestClient;
import ua.ndmik.bot.exception.ApplicationExceptionReporter;

@Configuration
@EnableScheduling
public class AppConfig {

    private static final String YASNO_BASE_URL = "https://app.yasno.ua/api/blackout-service/public/shutdowns/addresses/v2";
    private static final String YASNO_REFERER_HEADER = "https://app.yasno.ua/";

    private static final String ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE_HEADER = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7,uk;q=0.6";
    private static final String CACHE_CONTROL_HEADER = "max-age=0";
    private static final String REFERER_HEADER = "https://www.dtek-krem.com.ua/ua/shutdowns";
    public static final String USER_AGENT_HEADER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36";

    @Bean
    public RestClient dtekRestClient() {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, ACCEPT_HEADER)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_HEADER)
                .defaultHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_HEADER)
                .defaultHeader(HttpHeaders.REFERER, REFERER_HEADER)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT_HEADER)
                .build();

    }

    @Bean
    public RestClient yasnoRestClient() {
        return RestClient.builder()
                .baseUrl(YASNO_BASE_URL)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_HEADER)
                .defaultHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_HEADER)
                .defaultHeader(HttpHeaders.REFERER, YASNO_REFERER_HEADER)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT_HEADER)
                .build();
    }

    @Bean
    public TaskScheduler taskScheduler(ApplicationExceptionReporter exceptionReporter) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(2);
        taskScheduler.setThreadNamePrefix("firefly-scheduler-");
        taskScheduler.setErrorHandler(throwable -> exceptionReporter.report("scheduled task execution", throwable));
        taskScheduler.initialize();
        return taskScheduler;
    }
}
