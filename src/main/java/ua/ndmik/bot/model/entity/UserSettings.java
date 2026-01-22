package ua.ndmik.bot.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Id
    private Long chatId;

    private String groupId;

    private boolean isNotificationEnabled;
}
