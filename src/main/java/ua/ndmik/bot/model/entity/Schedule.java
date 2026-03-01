package ua.ndmik.bot.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ua.ndmik.bot.converter.LocalDateTimeSqliteConverter;
import ua.ndmik.bot.model.DtekArea;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedules")
@IdClass(ScheduleId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Schedule {

    @Id
    private String groupId;
    @Id
    @Enumerated(EnumType.STRING)
    private DtekArea area;
    @Id
    @Enumerated(EnumType.STRING)
    private ScheduleDay scheduleDay;
    @JdbcTypeCode(SqlTypes.JSON)
    private String schedule;
    @Convert(converter = LocalDateTimeSqliteConverter.class)
    private LocalDateTime lastUpdate;
    private Boolean needToNotify;
}
