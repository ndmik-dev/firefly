package ua.ndmik.bot.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import ua.ndmik.bot.model.DtekArea;
import ua.ndmik.bot.model.HourState;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.ScheduleDay;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DtekShutdownsServiceTests {

    private static final String GROUP_ID = "1.1";
    private static final LocalDateTime LAST_UPDATE = LocalDateTime.of(2026, 3, 1, 10, 0);

    private final DtekShutdownsService service = new DtekShutdownsService(new MessageFormatter(), "Europe/Kyiv");
    private final JsonMapper mapper = new JsonMapper();

    @Test
    void getShutdownsMessage_usesPlaceholderForAllDayYesSchedules() {
        Schedule today = schedule(ScheduleDay.TODAY, allDayWithPower());
        Schedule tomorrow = schedule(ScheduleDay.TOMORROW, allDayWithPower());

        String message = service.getShutdownsMessage(List.of(today, tomorrow));

        assertThat(message)
                .contains("📅 <b>Сьогодні</b>")
                .contains("📅 <b>Завтра</b>");
        assertOccurrences(message, "Графік ще не створено", 2);
    }

    @Test
    void getShutdownsMessage_formatsLightIntervalsAroundShutdowns() {
        Schedule today = schedule(ScheduleDay.TODAY, scheduleWith(
                new HourOverride(1, HourState.NO),
                new HourOverride(24, HourState.NO)
        ));
        Schedule tomorrow = schedule(ScheduleDay.TOMORROW, allDayWithPower());

        String message = service.getShutdownsMessage(List.of(today, tomorrow));

        assertThat(message).contains("✅ <b>01:00</b>–<b>23:00</b>");
    }

    @Test
    void getShutdownsMessage_mergesAdjacentHalfHourOutagesIntoSingleGap() {
        Schedule today = schedule(ScheduleDay.TODAY, scheduleWith(
                new HourOverride(10, HourState.SECOND),
                new HourOverride(11, HourState.FIRST)
        ));
        Schedule tomorrow = schedule(ScheduleDay.TOMORROW, allDayWithPower());

        String message = service.getShutdownsMessage(List.of(today, tomorrow));

        assertThat(message).contains("✅ <b>00:00</b>–<b>09:30</b>");
        assertThat(message).contains("✅ <b>10:30</b>–<b>24:00</b>");
    }

    private Schedule schedule(ScheduleDay day, Map<String, String> hours) {
        return Schedule.builder()
                .groupId(GROUP_ID)
                .area(DtekArea.KYIV)
                .scheduleDay(day)
                .schedule(mapper.writeValueAsString(hours))
                .lastUpdate(LAST_UPDATE)
                .needToNotify(Boolean.FALSE)
                .build();
    }

    private Map<String, String> allDayWithPower() {
        Map<String, String> hours = new LinkedHashMap<>();
        for (int hour = 1; hour <= 24; hour++) {
            hours.put(String.valueOf(hour), HourState.YES.getValue());
        }
        return hours;
    }

    private Map<String, String> scheduleWith(HourOverride... overrides) {
        Map<String, String> hours = allDayWithPower();
        for (HourOverride override : overrides) {
            hours.put(String.valueOf(override.hour()), override.state().getValue());
        }
        return hours;
    }

    private void assertOccurrences(String text, String expectedPart, int expectedCount) {
        int occurrences = text.split(expectedPart, -1).length - 1;
        assertThat(occurrences).isEqualTo(expectedCount);
    }

    private record HourOverride(int hour, HourState state) {
    }
}
