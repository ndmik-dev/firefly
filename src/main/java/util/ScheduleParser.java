package util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import ua.ndmik.bot.model.ScheduleResponse;

@Service
public class ScheduleParser {

    private static final String SHUTDOWNS_SCRIPT = "script:containsData(DisconSchedule.fact)";
    private static final String SCHEDULES_KEY = "DisconSchedule.fact";

    //TODO: retry on exception
    public static ScheduleResponse parseScheduleFromHtml(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalStateException("Empty response from DTEK");
        }
        Element script = Jsoup.parse(html)
                .select(SHUTDOWNS_SCRIPT)
                .first();
        if (script == null) {
            throw new IllegalStateException("DisconSchedule.fact not found");
        }
        String scheduleJson = ScheduleParser.parseSchedule(script.data());
        return new JsonMapper().readValue(scheduleJson, ScheduleResponse.class);
    }

    private static String parseSchedule(String textBlock) {
        int pos = textBlock.indexOf(SCHEDULES_KEY);
        int braceStart = textBlock.indexOf('{', pos);
        int depth = 0;
        for (int i = braceStart; i < textBlock.length(); i++) {
            char c = textBlock.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return textBlock.substring(braceStart, i + 1);
                }
            }
        }
        throw new IllegalStateException("Unclosed object");
    }
}
