package ua.ndmik.bot.model.entity;

import jakarta.persistence.*;
import lombok.*;
import ua.ndmik.bot.model.DtekArea;

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

    private String tmpGroupId;

    @Enumerated(EnumType.STRING)
    private DtekArea area;

    @Enumerated(EnumType.STRING)
    private DtekArea tmpArea;

    private boolean awaitingAddressInput;

    private boolean isNotificationEnabled;
}
