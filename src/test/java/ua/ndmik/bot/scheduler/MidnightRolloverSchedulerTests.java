package ua.ndmik.bot.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;
import ua.ndmik.bot.model.DtekArea;
import ua.ndmik.bot.model.HourState;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.ScheduleDay;
import ua.ndmik.bot.repository.ScheduleRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MidnightRolloverSchedulerTests {

    private static final LocalDateTime LAST_UPDATE = LocalDateTime.of(2026, 3, 1, 10, 0);

    @Mock
    private ScheduleRepository scheduleRepository;

    @Captor
    private ArgumentCaptor<Iterable<Schedule>> savedSchedulesCaptor;

    private final JsonMapper mapper = new JsonMapper();

    @Test
    void rolloverSchedulesAtMidnight_promotesTomorrowScheduleAndResetsNotifications() {
        MidnightRolloverScheduler scheduler = new MidnightRolloverScheduler(scheduleRepository);
        Schedule tomorrow = schedule(ScheduleDay.TOMORROW, "2.1", scheduleWith(
                new HourOverride(10, HourState.NO),
                new HourOverride(11, HourState.NO)
        ));
        String tomorrowScheduleJson = tomorrow.getSchedule();

        given(scheduleRepository.findByDay(ScheduleDay.TOMORROW.name())).willReturn(List.of(tomorrow));

        scheduler.rolloverSchedulesAtMidnight();

        List<Schedule> savedSchedules = capturedSavedSchedules();
        assertThat(find(savedSchedules, ScheduleDay.TODAY, "2.1").getSchedule()).isEqualTo(tomorrowScheduleJson);
        assertThat(find(savedSchedules, ScheduleDay.TOMORROW, "2.1").getSchedule()).isEqualTo(allDayWithPowerJson());
        assertThat(find(savedSchedules, ScheduleDay.TODAY, "2.1").getNeedToNotify()).isFalse();
        assertThat(find(savedSchedules, ScheduleDay.TOMORROW, "2.1").getNeedToNotify()).isFalse();
    }

    @Test
    void rolloverSchedulesAtMidnight_deletesTodayAndSavesNothingWhenTomorrowIsMissing() {
        MidnightRolloverScheduler scheduler = new MidnightRolloverScheduler(scheduleRepository);

        given(scheduleRepository.findByDay(ScheduleDay.TOMORROW.name())).willReturn(List.of());

        scheduler.rolloverSchedulesAtMidnight();

        then(scheduleRepository).should().deleteByDay(ScheduleDay.TODAY.name());
        then(scheduleRepository).should().findByDay(ScheduleDay.TOMORROW.name());
        then(scheduleRepository).shouldHaveNoMoreInteractions();
    }

    private List<Schedule> capturedSavedSchedules() {
        then(scheduleRepository).should().saveAll(savedSchedulesCaptor.capture());

        List<Schedule> savedSchedules = new ArrayList<>();
        savedSchedulesCaptor.getValue().forEach(savedSchedules::add);
        return savedSchedules;
    }

    private Schedule find(List<Schedule> schedules, ScheduleDay day, String groupId) {
        return schedules.stream()
                .filter(schedule -> schedule.getScheduleDay() == day)
                .filter(schedule -> schedule.getGroupId().equals(groupId))
                .findFirst()
                .orElseThrow();
    }

    private Schedule schedule(ScheduleDay day, String groupId, Map<String, String> hours) {
        return Schedule.builder()
                .groupId(groupId)
                .area(DtekArea.KYIV)
                .scheduleDay(day)
                .schedule(mapper.writeValueAsString(hours))
                .lastUpdate(LAST_UPDATE)
                .needToNotify(Boolean.TRUE)
                .build();
    }

    private String allDayWithPowerJson() {
        return mapper.writeValueAsString(allDayWithPower());
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

    private record HourOverride(int hour, HourState state) {
    }
}
