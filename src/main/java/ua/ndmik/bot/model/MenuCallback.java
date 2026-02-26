package ua.ndmik.bot.model;

import lombok.Getter;

@Getter
public enum MenuCallback {
    KYIV,
    REGION,
    REGIONS_BACK,
    NOTIFICATION_CLICK,
    GROUP_CLICK,
    GROUP_PAGE,
    GROUP_BACK,
    GROUP_DONE,
    GROUP_SELECTION,
    GROUP_RESOLVING,
    DEFAULT
}
