package ua.ndmik.bot.converter;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDateTimeSqliteConverterTests {

    private final LocalDateTimeSqliteConverter converter = new LocalDateTimeSqliteConverter();

    @Test
    void convertToEntityAttribute_readsLegacyEpochMillis() {
        LocalDateTime value = converter.convertToEntityAttribute("1772344020000");

        assertThat(value).isEqualTo(LocalDateTime.of(2026, 3, 1, 5, 47));
    }

    @Test
    void convertToEntityAttribute_readsFormattedTimestamp() {
        LocalDateTime value = converter.convertToEntityAttribute("2026-03-01 20:43:00.000");

        assertThat(value).isEqualTo(LocalDateTime.of(2026, 3, 1, 20, 43));
    }

    @Test
    void convertToDatabaseColumn_writesStableSqliteTimestampFormat() {
        String value = converter.convertToDatabaseColumn(LocalDateTime.of(2026, 3, 1, 20, 43, 0));

        assertThat(value).isEqualTo("2026-03-01 20:43:00.000");
    }
}
