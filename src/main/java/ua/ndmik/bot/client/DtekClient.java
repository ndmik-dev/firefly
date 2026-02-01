package ua.ndmik.bot.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import ua.ndmik.bot.model.ScheduleResponse;
import ua.ndmik.bot.service.ShutdownsResponseProcessor;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static ua.ndmik.bot.config.AppConfig.USER_AGENT_HEADER;
import static util.Constants.DTEK_KREM_URL;
import static util.Constants.SHUTDOWNS_PATH;

@Service
@Slf4j
public class DtekClient {

    private static final String SHUTDOWNS_SCRIPT = "script:containsData(DisconSchedule.fact)";

    private final RestClient restClient;
    private final ShutdownsResponseProcessor responseProcessor;
    private final JsonMapper mapper;
    private volatile String cachedCookies;

    public DtekClient(RestClient restClient,
                      ShutdownsResponseProcessor responseProcessor) {
        this.restClient = restClient;
        this.responseProcessor = responseProcessor;
        this.mapper = new JsonMapper();
    }

    public ScheduleResponse getSchedules() {
        if (cachedCookies == null || cachedCookies.isBlank()) {
            cachedCookies = retrieveCookies();
        }
        String html = fetchHtml();
        return parseScheduleFromHtml(html);
    }

    private String fetchHtml() {
        String html = executeRequest();
        if (isBlockedOrMissing(html)) {
            cachedCookies = retrieveCookies();
            html = executeRequest();
        }
        return html;
    }

    private String executeRequest() {
        RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri(SHUTDOWNS_PATH)
                .header(HttpHeaders.COOKIE, cachedCookies);
        return request.exchange((_, res) -> {
            if (res.getStatusCode().is4xxClientError()) {
                return null;
            }
            return res.bodyTo(String.class);
        });
    }

    private String retrieveCookies() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setUserAgent(USER_AGENT_HEADER)
            );
            Page page = context.newPage();
            page.navigate(DTEK_KREM_URL + SHUTDOWNS_PATH,
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE)
            );
            // Wait a bit to ensure all cookies are set
            page.waitForTimeout(2000);
            // Extract all cookies
            List<Cookie> cookies = context.cookies();

            browser.close();

            return cookies.stream()
                    .map(c -> c.name + "=" + c.value)
                    .collect(Collectors.joining("; "));
        } catch (RuntimeException e) {
            log.warn("Failed to retrieve cookies: {}", e.getMessage());
            throw e;
        }
    }

    private ScheduleResponse parseScheduleFromHtml(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalStateException("Empty response from DTEK");
        }
        Element script = Jsoup.parse(html)
                .select(SHUTDOWNS_SCRIPT)
                .first();
        if (script == null) {
            throw new IllegalStateException("DisconSchedule.fact not found");
        }
        String scheduleJson = responseProcessor.parseSchedule(script.data());
        return mapper.readValue(scheduleJson, ScheduleResponse.class);
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
