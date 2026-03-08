package ua.ndmik.bot.model.callback;

import ua.ndmik.bot.model.common.DtekArea;

public record SelectionPayload(String groupId, DtekArea area, int page) {
}
