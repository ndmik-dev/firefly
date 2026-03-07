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
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua.ndmik.bot.model.common.DtekArea;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static ua.ndmik.bot.config.AppConfig.USER_AGENT_HEADER;

@Component
@Slf4j
public class DtekCookieProvider {

    private static final int NAVIGATION_TIMEOUT_MS = 15_000;
    private static final int DOM_CONTENT_LOADED_WAIT_MS = 3_000;
    private static final int NETWORK_IDLE_WAIT_MS = 2_000;
    private static final Duration COOKIE_REFRESH_FAILURE_COOLDOWN = Duration.ofMinutes(15);

    private final Object browserLock;
    private final Map<DtekArea, Instant> refreshBlockedUntil;

    private Playwright playwright;
    private Browser browser;

    public DtekCookieProvider() {
        this.browserLock = new Object();
        this.refreshBlockedUntil = new EnumMap<>(DtekArea.class);
    }

    public Optional<String> getCookies(DtekArea area) {
        Instant blockedUntil = refreshBlockedUntil.get(area);
        if (blockedUntil != null && Instant.now().isBefore(blockedUntil)) {
            log.warn("Cookie refresh is cooling down for area={} until={}", area, blockedUntil);
            return Optional.empty();
        }

        synchronized (browserLock) {
            return refreshCookies(area);
        }
    }

    private Optional<String> refreshCookies(DtekArea area) {
        log.info("Refreshing cookies for area={}", area);
        try (BrowserContext context = getBrowser().newContext(new Browser.NewContextOptions().setUserAgent(USER_AGENT_HEADER));
             Page page = context.newPage()) {
            navigateForCookies(page, area);
            List<Cookie> cookies = context.cookies();
            String cookieHeader = cookies.stream()
                    .map(cookie -> cookie.name + "=" + cookie.value)
                    .collect(Collectors.joining("; "));
            if (cookieHeader.isBlank()) {
                rememberFailure(area);
                return Optional.empty();
            }
            refreshBlockedUntil.remove(area);
            return Optional.of(cookieHeader);
        } catch (RuntimeException e) {
            rememberFailure(area);
            log.warn("Failed to retrieve cookies for area={}", area, e);
            return Optional.empty();
        }
    }

    private Browser getBrowser() {
        if (browser != null && browser.isConnected()) {
            return browser;
        }
        closeResources();
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        return browser;
    }

    private void rememberFailure(DtekArea area) {
        refreshBlockedUntil.put(area, Instant.now().plus(COOKIE_REFRESH_FAILURE_COOLDOWN));
    }

    private void navigateForCookies(Page page, DtekArea area) {
        page.navigate(area.getShutdownsUrl(),
                new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.COMMIT)
                        .setTimeout(NAVIGATION_TIMEOUT_MS)
        );
        try {
            page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(DOM_CONTENT_LOADED_WAIT_MS));
        } catch (PlaywrightException e) {
            log.debug("DOM content loaded was not reached quickly while retrieving cookies: {}", e.getMessage());
        }
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(NETWORK_IDLE_WAIT_MS));
        } catch (PlaywrightException e) {
            log.debug("Network idle was not reached quickly while retrieving cookies: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        synchronized (browserLock) {
            closeResources();
        }
    }

    private void closeResources() {
        if (browser != null) {
            try {
                browser.close();
            } catch (RuntimeException e) {
                log.debug("Failed to close Playwright browser cleanly", e);
            } finally {
                browser = null;
            }
        }
        if (playwright != null) {
            try {
                playwright.close();
            } catch (RuntimeException e) {
                log.debug("Failed to close Playwright cleanly", e);
            } finally {
                playwright = null;
            }
        }
    }
}
