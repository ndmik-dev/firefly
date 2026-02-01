package ua.ndmik.bot.service;

import org.springframework.stereotype.Service;

@Service
public class ShutdownsResponseProcessor {

    private static final String SCHEDULES_KEY = "DisconSchedule.fact";

    public String parseSchedule(String textBlock) {
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
