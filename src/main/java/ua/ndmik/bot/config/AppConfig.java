package ua.ndmik.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import static util.Constants.DTEK_KREM_URL;

@Configuration
@EnableScheduling
public class AppConfig {

    private static final String ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE_HEADER = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7,uk;q=0.6";
    private static final String CACHE_CONTROL_HEADER = "max-age=0";
    private static final String REFERER_HEADER = "https://www.dtek-krem.com.ua/ua/shutdowns";
    public static final String USER_AGENT_HEADER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36";

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(DTEK_KREM_URL)
                .defaultHeader(HttpHeaders.ACCEPT, ACCEPT_HEADER)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_HEADER)
                .defaultHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_HEADER)
                .defaultHeader(HttpHeaders.REFERER, REFERER_HEADER)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT_HEADER)
                .build();

    }
}
