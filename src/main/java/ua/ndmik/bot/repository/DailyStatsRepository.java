package ua.ndmik.bot.repository;

import org.springframework.data.repository.CrudRepository;
import ua.ndmik.bot.model.entity.DailyStats;

import java.time.LocalDate;
import java.util.List;

public interface DailyStatsRepository extends CrudRepository<DailyStats, LocalDate> {

    List<DailyStats> findByStatDateBetweenOrderByStatDateAsc(LocalDate from, LocalDate to);
}
