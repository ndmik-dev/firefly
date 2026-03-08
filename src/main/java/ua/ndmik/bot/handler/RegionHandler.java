package ua.ndmik.bot.handler;

import ua.ndmik.bot.model.common.DtekArea;
import ua.ndmik.bot.repository.ScheduleRepository;
import ua.ndmik.bot.repository.UserSettingsRepository;
import ua.ndmik.bot.service.TelegramService;

import org.springframework.stereotype.Component;

@Component
public class RegionHandler extends AbstractAreaGroupHandler {

    public RegionHandler(TelegramService telegramService,
                         ScheduleRepository scheduleRepository,
                         UserSettingsRepository userRepository) {
        super(telegramService, scheduleRepository, userRepository);
    }

    @Override
    protected DtekArea targetArea() {
        return DtekArea.KYIV_REGION;
    }
}
