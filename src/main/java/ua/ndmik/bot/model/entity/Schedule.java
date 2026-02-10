package ua.ndmik.bot.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    private ScheduleDay scheduleDay;
    @JdbcTypeCode(SqlTypes.JSON)
    private String schedule;
    private LocalDateTime lastUpdate;
    private Boolean needToNotify;
}
