package ua.ndmik.bot.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ua.ndmik.bot.model.entity.DailyStats;

import java.time.LocalDate;
import java.util.List;

public interface DailyStatsRepository extends CrudRepository<DailyStats, LocalDate> {

    @Query(value = """
            SELECT *
            FROM daily_stats
            WHERE stat_date BETWEEN :from AND :to
            ORDER BY stat_date ASC
            """, nativeQuery = true)
    List<DailyStats> findRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
