package ua.ndmik.bot.handler;

import org.springframework.stereotype.Service;
import ua.ndmik.bot.model.MenuCallback;

import java.util.HashMap;
import java.util.Map;

@Service
public class CallbackHandlerResolver {

    private final Map<MenuCallback, CallbackHandler> handlers;

    public CallbackHandlerResolver(DefaultHandler defaultHandler,
                                   RegionHandler regionHandler,
                                   GroupClickHandler groupClickHandler,
                                   GroupBackHandler groupBackHandler) {

        this.handlers = new HashMap<>();
        handlers.put(MenuCallback.REGION, regionHandler);
        handlers.put(MenuCallback.KYIV, defaultHandler);
        handlers.put(MenuCallback.ENABLE_NOTIFICATIONS, defaultHandler);
        handlers.put(MenuCallback.DISABLE_NOTIFICATIONS, defaultHandler);
        handlers.put(MenuCallback.GROUP_CLICK, groupClickHandler);
        handlers.put(MenuCallback.GROUP_BACK, groupBackHandler);
    }

    public CallbackHandler getHandler(MenuCallback callback) {
        return handlers.get(callback);
    }
}
