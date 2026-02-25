package ua.ndmik.bot.client;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ua.ndmik.bot.model.DtekArea;
import ua.ndmik.bot.model.ScheduleResponse;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static ua.ndmik.bot.config.AppConfig.USER_AGENT_HEADER;
import static ua.ndmik.bot.util.ScheduleParser.parseScheduleFromHtml;

@Service
@Slf4j
public class DtekClient {

    private static final String SHUTDOWNS_SCRIPT = "script:containsData(DisconSchedule.fact)";
    private static final int NAVIGATION_TIMEOUT_MS = 45_000;
    private static final int NETWORK_IDLE_WAIT_MS = 5_000;
    private static final int FETCH_ATTEMPTS = 3;
    private static final Set<String> ROBOT_BLOCK_MARKERS = Set.of("noindex,nofollow", "noindex, nofollow");

    private final RestClient dtekClient;

    public DtekClient(@Qualifier("dtekRestClient") RestClient dtekClient) {
        this.dtekClient = dtekClient;
    }

    public Optional<ScheduleResponse> getKyivSchedules() {
        return getSchedules(DtekArea.KYIV);
    }

    public Optional<ScheduleResponse> getKyivRegionSchedules() {
        return getSchedules(DtekArea.KYIV_REGION);
    }

    private Optional<ScheduleResponse> getSchedules(DtekArea area) {
        String html = fetchHtml(area);
        if (isBlockedOrMissing(html)) {
            log.warn("Failed to fetch valid schedules after retry for area={}, skipping this cycle.", area);
            return Optional.empty();
        }
        try {
            return Optional.of(parseScheduleFromHtml(html));
        } catch (RuntimeException e) {
            log.warn("Failed to parse schedules from response for area={}", area, e);
            return Optional.empty();
        }
    }

    private String fetchHtml(DtekArea area) {
        String html = null;
        for (int attempt = 1; attempt <= FETCH_ATTEMPTS; attempt++) {
            html = executeRequest(area);
            if (!isBlockedOrMissing(html)) {
                return html;
            }
            if (attempt < FETCH_ATTEMPTS) {
                log.warn("Some error during schedule extracting for area={}. Retrying with fresh cookies.", area);
            }
        }
        return html;
    }

    private String executeRequest(DtekArea area) {
        RestClient.RequestHeadersSpec<?> request = dtekClient.get()
                .uri(area.getShutdownsUrl())
                .header(HttpHeaders.REFERER, area.getShutdownsUrl())
                .header("Origin", area.getBaseUrl());
        request = addCookiesHeaderIfPresent(request, area);
        return request.exchange((_, res) -> {
            if (res.getStatusCode().is4xxClientError()) {
                log.warn("Client error during executing request statusCode={}, body={}", res.getStatusCode(), res.getBody());
                return null;
            }
            return res.bodyTo(String.class);
        });
    }

    private RestClient.RequestHeadersSpec<?> addCookiesHeaderIfPresent(RestClient.RequestHeadersSpec<?> request, DtekArea area) {
        Optional<String> cookies = retrieveCookies(area);
        if (cookies.isPresent() && !cookies.get().isBlank()) {
            return request.header(HttpHeaders.COOKIE, cookies.get());
        }
        return request;
    }

    private Optional<String> retrieveCookies(DtekArea area) {
        log.info("Retrieving cookies for area={}", area);
        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                 BrowserContext context = browser.newContext(new Browser.NewContextOptions().setUserAgent(USER_AGENT_HEADER));
                 Page page = context.newPage()) {
                navigateForCookies(page, area);
                List<Cookie> cookies = context.cookies();
                String cookieHeader = cookies.stream()
                        .map(c -> c.name + "=" + c.value)
                        .collect(Collectors.joining("; "));
                return cookieHeader.isBlank()
                        ? Optional.empty()
                        : Optional.of(cookieHeader);
            }
        } catch (RuntimeException e) {
            log.warn("Failed to retrieve cookies for area={}", area, e);
            return Optional.empty();
        }
    }

    private void navigateForCookies(Page page, DtekArea area) {
        page.navigate(area.getShutdownsUrl(),
                new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(NAVIGATION_TIMEOUT_MS)
        );
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(NETWORK_IDLE_WAIT_MS));
        } catch (PlaywrightException e) {
            log.debug("Network idle was not reached quickly while retrieving cookies: {}", e.getMessage());
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
        return ROBOT_BLOCK_MARKERS.stream().anyMatch(normalized::contains);
    }
}
