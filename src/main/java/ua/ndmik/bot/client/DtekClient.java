package ua.ndmik.bot.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;
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

@Service
//TODO: refactor client and AppConfig
public class DtekClient {

    private static final String ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE_HEADER = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7,uk;q=0.6";
    private static final String CACHE_CONTROL_HEADER = "max-age=0";
    private static final String REFERER_HEADER = "https://www.dtek-krem.com.ua/ua/shutdowns";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36";

    private final RestClient restClient;
    private final ShutdownsResponseProcessor responseProcessor;
    private final JsonMapper mapper;
    //TODO: fix midnight problem according today and tomorrow changing
    //TODO: create table for this
    private ScheduleResponse cachedResponse;

    public DtekClient(RestClient restClient,
                      ShutdownsResponseProcessor responseProcessor) {
        this.restClient = restClient;
        this.responseProcessor = responseProcessor;
        this.mapper = new JsonMapper();
    }

    public ScheduleResponse getShutdownsSchedule() {
        String html = restClient.get()
                .uri("/ua/shutdowns")
                .header(HttpHeaders.ACCEPT, ACCEPT_HEADER)
                .header(HttpHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_HEADER)
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_HEADER)
                .header(HttpHeaders.REFERER, REFERER_HEADER)
                .header(HttpHeaders.COOKIE, retrieveCookies())
                .retrieve()
//                handle errors
//                .onStatus()
                .body(String.class);

        if (html == null || isRobotBlock(html)) {
            html = fetchPageHtml();
        }

        Element script = Jsoup.parse(html)
                .select("script:containsData(DisconSchedule.fact)")
                .first();
        if (script == null) {
            throw new IllegalStateException("DisconSchedule.fact not found");
        }
        String schedule = responseProcessor.parseSchedule(script.data());
        ScheduleResponse response = mapper.readValue(schedule, ScheduleResponse.class);;
        //TODO: create a copy
//        cachedResponse = response.
        return mapper.readValue(schedule, ScheduleResponse.class);
    }

    private boolean isRobotBlock(String html) {
        String normalized = html.toLowerCase(Locale.ROOT);
        return normalized.contains("noindex,nofollow") || normalized.contains("noindex, nofollow");
    }

    private String retrieveCookies() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setUserAgent(USER_AGENT)
            );
            Page page = context.newPage();
            //TODO: fix blocking call
            page.navigate("https://www.dtek-krem.com.ua/ua/shutdowns",
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
        }
    }

    private String fetchPageHtml() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions().setUserAgent(USER_AGENT)
            );
            Page page = context.newPage();
            page.navigate("https://www.dtek-krem.com.ua/ua/shutdowns",
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE)
            );
            page.waitForTimeout(2000);
            String content = page.content();
            browser.close();
            return content;
        }
    }
}
