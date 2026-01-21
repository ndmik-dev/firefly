package ua.ndmik.bot.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
@EnableScheduling
public class AppConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                //TODO: replace by env var
                .baseUrl("https://www.dtek-krem.com.ua")
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
                .build();

    }

    //TODO: leave just cookie cache
    @Bean
    public CacheManager cacheManager() {
        CaffeineCache userStates = new CaffeineCache("userStates", userStateCache());
        CaffeineCache scheduleResponses = new CaffeineCache("scheduleResponses", scheduleResponseCache());
        CaffeineCache cookies = new CaffeineCache("cookies", cookiesCache());

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(userStates, scheduleResponses, cookies));
        return cacheManager;
    }

    private Cache<Object, Object> userStateCache() {
        return Caffeine.newBuilder()
                .initialCapacity(1000)
                .build();
    }

    private Cache<Object, Object> scheduleResponseCache() {
        return Caffeine.newBuilder()
                .initialCapacity(32)
                .expireAfterWrite(Duration.ofMinutes(20))
                .build();
    }

    private Cache<Object, Object> cookiesCache() {
        return Caffeine.newBuilder()
                .initialCapacity(32)
                .expireAfterWrite(Duration.ofDays(1))
                .build();
    }
}
