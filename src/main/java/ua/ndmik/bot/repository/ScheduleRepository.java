package ua.ndmik.bot.repository;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ua.ndmik.bot.model.entity.Schedule;
import ua.ndmik.bot.model.entity.ScheduleId;

import java.util.List;

public interface ScheduleRepository extends CrudRepository<Schedule, ScheduleId> {

    @NonNull List<Schedule> findAll();

    List<Schedule> findAllByNeedToNotifyTrue();

    List<Schedule> findAllByGroupId(String groupId);

    @Query("select distinct s.groupId from Schedule s")
    List<String> findDistinctGroupIds();
}
