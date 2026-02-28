package ua.ndmik.bot.client;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ua.ndmik.bot.model.DtekArea;
import ua.ndmik.bot.model.ScheduleResponse;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static ua.ndmik.bot.util.ScheduleParser.parseScheduleFromHtml;

@Service
@Slf4j
public class DtekClient {

    private static final String SHUTDOWNS_SCRIPT = "script:containsData(DisconSchedule.fact)";
    private static final int FETCH_ATTEMPTS = 3;
    private static final Set<String> ROBOT_BLOCK_MARKERS = Set.of("noindex,nofollow", "noindex, nofollow");

    private final RestClient dtekClient;
    private final DtekCookieProvider cookieProvider;

    public DtekClient(@Qualifier("dtekRestClient") RestClient dtekClient,
                      DtekCookieProvider cookieProvider) {
        this.dtekClient = dtekClient;
        this.cookieProvider = cookieProvider;
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
            boolean attachCookies = attempt > 1;
            html = executeRequest(area, attachCookies);
            if (!isBlockedOrMissing(html)) {
                return html;
            }
            if (attempt < FETCH_ATTEMPTS) {
                log.warn("Some error during schedule extracting for area={}. Retrying, attachCookies={}",
                        area,
                        attachCookies);
            }
        }
        return html;
    }

    private String executeRequest(DtekArea area, boolean attachCookies) {
        RestClient.RequestHeadersSpec<?> request = dtekClient.get()
                .uri(area.getShutdownsUrl())
                .header(HttpHeaders.REFERER, area.getShutdownsUrl())
                .header("Origin", area.getBaseUrl());
        request = addCookiesHeaderIfPresent(request, area, attachCookies);
        return request.exchange((_, res) -> {
            if (res.getStatusCode().is4xxClientError()) {
                log.warn("Client error during executing request statusCode={}, body={}", res.getStatusCode(), res.getBody());
                return null;
            }
            return res.bodyTo(String.class);
        });
    }

    private RestClient.RequestHeadersSpec<?> addCookiesHeaderIfPresent(RestClient.RequestHeadersSpec<?> request,
                                                                       DtekArea area,
                                                                       boolean attachCookies) {
        if (!attachCookies) {
            return request;
        }
        Optional<String> cookies = cookieProvider.getCookies(area);
        if (cookies.isPresent() && !cookies.get().isBlank()) {
            return request.header(HttpHeaders.COOKIE, cookies.get());
        }
        return request;
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
