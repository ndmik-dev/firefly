package ua.ndmik.bot.repository;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ua.ndmik.bot.model.DtekArea;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.ScheduleId;
import ua.ndmik.bot.model.entity.ScheduleDay;

import java.util.List;

public interface ScheduleRepository extends CrudRepository<Schedule, ScheduleId> {

    @NonNull List<Schedule> findAll();

    @Query(value = """
            SELECT DISTINCT group_id
            FROM schedules
            WHERE need_to_notify = 1
              AND area = :area
            """, nativeQuery = true)
    List<String> findNotifyGroupIdsByArea(@Param("area") DtekArea area);

    @Query(value = """
            SELECT *
            FROM schedules
            WHERE group_id = :groupId
            """, nativeQuery = true)
    List<Schedule> findByGroup(@Param("groupId") String groupId);

    @Query(value = """
            SELECT *
            FROM schedules
            WHERE group_id = :groupId
              AND area = :area
            """, nativeQuery = true)
    List<Schedule> findByGroupAndArea(@Param("groupId") String groupId, @Param("area") DtekArea area);

    @Query(value = """
            SELECT *
            FROM schedules
            WHERE schedule_day = :day
            """, nativeQuery = true)
    List<Schedule> findByDay(@Param("day") ScheduleDay scheduleDay);

    @Modifying
    @Query(value = """
            DELETE
            FROM schedules
            WHERE schedule_day = :day
            """, nativeQuery = true)
    void deleteByDay(@Param("day") ScheduleDay day);

    @Query(value = """
            SELECT DISTINCT group_id
            FROM schedules
            """, nativeQuery = true)
    List<String> findGroupIds();
}
