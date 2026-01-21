package ua.ndmik.bot.model;

import lombok.Getter;

@Getter
public enum HourState {
    YES("yes"),
    NO("no"),
    FIRST("first"),
    SECOND("second");

    private final String value;

    HourState(String value) {
        this.value = value;
    }

    public static HourState resolveState(String value) {
        for (HourState state : HourState.values()) {
            if (state.value.equals(value))
                return state;
        }
        throw new IllegalStateException("Unknown hour state");
    }
}
