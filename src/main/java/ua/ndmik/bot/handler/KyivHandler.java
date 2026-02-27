package ua.ndmik.bot.handler;

import org.springframework.stereotype.Component;
import ua.ndmik.bot.model.DtekArea;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;

@Component
public class KyivHandler extends AbstractAreaGroupHandler {

    public KyivHandler(TelegramService telegramService,
                       ScheduleRepository scheduleRepository,
                       UserSettingsRepository userRepository) {
        super(telegramService, scheduleRepository, userRepository);
    }

    @Override
    protected DtekArea targetArea() {
        return DtekArea.KYIV;
    }
}
