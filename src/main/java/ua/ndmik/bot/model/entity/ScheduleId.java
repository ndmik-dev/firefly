package ua.ndmik.bot.model.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ua.ndmik.bot.model.common.DtekArea;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ScheduleId implements Serializable {

    private DtekArea area;
    private String groupId;
    private ScheduleDay scheduleDay;
}
