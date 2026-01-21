package ua.ndmik.bot.model;

import lombok.Getter;
import ua.ndmik.bot.handler.CallbackHandler;
import ua.ndmik.bot.handler.DefaultHandler;
import ua.ndmik.bot.handler.RegionHandler;

@Getter
public enum MenuCallback {
    KYIV(new DefaultHandler()),
    REGION(new RegionHandler()),
    ENABLE_NOTIFICATIONS(new DefaultHandler()),
    DISABLE_NOTIFICATIONS(new DefaultHandler()),
    GROUP_SELECTION_BACK(new DefaultHandler()),
    GROUP_SELECTION_DONE(new DefaultHandler());

    private final CallbackHandler handler;

    MenuCallback(CallbackHandler handler) {
        this.handler = handler;
    }
}
