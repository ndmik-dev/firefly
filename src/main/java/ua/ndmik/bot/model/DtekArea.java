package ua.ndmik.bot.model;

import static ua.ndmik.bot.util.Constants.*;

public enum DtekArea {
    KYIV(DTEK_REM_URL),
    KYIV_REGION(DTEK_KREM_URL);

    private final String baseUrl;

    DtekArea(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getShutdownsUrl() {
        return baseUrl + SHUTDOWNS_PATH;
    }
}
