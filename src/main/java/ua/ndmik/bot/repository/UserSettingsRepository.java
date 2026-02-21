package ua.ndmik.bot.repository;

import org.springframework.data.repository.CrudRepository;
import ua.ndmik.bot.model.entity.UserSettings;

import java.util.List;
import java.util.Optional;

public interface UserSettingsRepository extends CrudRepository<UserSettings, Long> {

    Optional<UserSettings> findByChatId(Long id);
    List<UserSettings> findByGroupIdAndIsNotificationEnabledTrue(String id);
    long countByGroupIdIsNotNull();
    long countByIsNotificationEnabledTrue();
}
