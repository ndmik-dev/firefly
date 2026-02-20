package ua.ndmik.bot.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "daily_stats")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStats {

    @Id
    @Column(name = "stat_date")
    private LocalDate statDate;

    private long newUsers;
    private long notificationsSent;
    private long notificationsFailed;
}
