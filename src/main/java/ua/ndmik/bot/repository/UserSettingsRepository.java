package ua.ndmik.bot.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ua.ndmik.bot.model.DtekArea;
import ua.ndmik.bot.model.entity.UserSettings;

import java.util.List;
import java.util.Optional;

public interface UserSettingsRepository extends CrudRepository<UserSettings, Long> {

    Optional<UserSettings> findByChatId(Long id);

    @Query(value = """
            SELECT *
            FROM user_settings
            WHERE group_id = :groupId
              AND area = :area
              AND is_notification_enabled = 1
            """, nativeQuery = true)
    List<UserSettings> findNotifiableByGroupAndArea(@Param("groupId") String groupId, @Param("area") DtekArea area);

    @Query(value = """
            SELECT COUNT(*)
            FROM user_settings
            WHERE group_id IS NOT NULL
            """, nativeQuery = true)
    long countWithGroup();

    @Query(value = """
            SELECT COUNT(*)
            FROM user_settings
            WHERE is_notification_enabled = 1
            """, nativeQuery = true)
    long countWithNotifications();
}
