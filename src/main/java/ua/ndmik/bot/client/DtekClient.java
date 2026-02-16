package ua.ndmik.bot.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ua.ndmik.bot.model.ScheduleResponse;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static ua.ndmik.bot.config.AppConfig.USER_AGENT_HEADER;
import static util.Constants.DTEK_KREM_URL;
import static util.Constants.SHUTDOWNS_PATH;
import static util.ScheduleParser.parseScheduleFromHtml;

@Service
@Slf4j
public class DtekClient {

    private static final String SHUTDOWNS_SCRIPT = "script:containsData(DisconSchedule.fact)";
    private static final int NAVIGATION_TIMEOUT_MS = 45_000;
    private static final int NETWORK_IDLE_WAIT_MS = 5_000;

    private final RestClient restClient;
    private volatile String cachedCookies;

    public DtekClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<ScheduleResponse> getSchedules() {
        String html = fetchHtmlWithRetry();
        if (isBlockedOrMissing(html)) {
            log.warn("Failed to fetch valid schedules after retry, skipping this cycle.");
            return Optional.empty();
        }
        try {
            return Optional.of(parseScheduleFromHtml(html));
        } catch (RuntimeException e) {
            log.warn("Failed to parse schedules from response", e);
            return Optional.empty();
        }
    }

    private String fetchHtmlWithRetry() {
        String html = executeRequest();
        if (!isBlockedOrMissing(html)) {
            return html;
        }
        log.warn("Some error during schedule extracting, trying to fetch cookies.");
        if (!refreshCookies()) {
            return html;
        }
        return executeRequest();
    }

    private String executeRequest() {
        RestClient.RequestHeadersSpec<?> request = restClient.get().uri(SHUTDOWNS_PATH);
        if (cachedCookies != null && !cachedCookies.isBlank()) {
            request = request.header(HttpHeaders.COOKIE, cachedCookies);
        }
        return request.exchange((_, res) -> {
            if (res.getStatusCode().is4xxClientError()) {
                log.warn("Client error during executing request statusCode={}, body={}", res.getStatusCode(), res.getBody());
                return null;
            }
            return res.bodyTo(String.class);
        });
    }

    private boolean refreshCookies() {
        String freshCookies = retrieveCookies();
        if (freshCookies == null || freshCookies.isBlank()) {
            log.warn("Cookies were not retrieved, keeping previous cookie cache.");
            return false;
        }
        cachedCookies = freshCookies;
        return true;
    }

    private String retrieveCookies() {
        log.info("Retrieving cookies");
        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                 BrowserContext context = browser.newContext(new Browser.NewContextOptions().setUserAgent(USER_AGENT_HEADER));
                 Page page = context.newPage()) {
                page.navigate(DTEK_KREM_URL + SHUTDOWNS_PATH,
                        new Page.NavigateOptions()
                                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                                .setTimeout((double) NAVIGATION_TIMEOUT_MS)
                );
                try {
                    page.waitForLoadState(LoadState.NETWORKIDLE,
                            new Page.WaitForLoadStateOptions().setTimeout(NETWORK_IDLE_WAIT_MS));
                } catch (PlaywrightException e) {
                    log.debug("Network idle was not reached quickly while retrieving cookies: {}", e.getMessage());
                }

                List<Cookie> cookies = context.cookies();
                return cookies.stream()
                        .map(c -> c.name + "=" + c.value)
                        .collect(Collectors.joining("; "));
            }
        } catch (RuntimeException e) {
            log.warn("Failed to retrieve cookies", e);
            return null;
        }
    }

    private boolean isBlockedOrMissing(String html) {
        return html == null || isRobotBlock(html) || missingSchedule(html);
    }

    private boolean missingSchedule(String html) {
        if (html == null) {
            return true;
        }
        return Jsoup.parse(html)
                .selectFirst(SHUTDOWNS_SCRIPT) == null;
    }

    private boolean isRobotBlock(String html) {
        String normalized = html.toLowerCase(Locale.ROOT);
        return normalized.contains("noindex,nofollow") || normalized.contains("noindex, nofollow");
    }
}
