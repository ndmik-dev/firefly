package ua.ndmik.bot.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class GroupSchedule {

    private final Map<String, Map<String, String>> groupSchedules = new HashMap<>();

    @JsonAnySetter
    public void putGroupSchedule(String group, Map<String, String> hourStateMap) {
        groupSchedules.put(group, hourStateMap);
    }
    public Map<String, String> hourStateMap(String group) {
        return groupSchedules.getOrDefault(group, Map.of());
    }
}
