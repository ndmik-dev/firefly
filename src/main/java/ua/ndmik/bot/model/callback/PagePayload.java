package ua.ndmik.bot.model.callback;

import ua.ndmik.bot.model.common.DtekArea;

public record PagePayload(DtekArea area, int page) {
}
