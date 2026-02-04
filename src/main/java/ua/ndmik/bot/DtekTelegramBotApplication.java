package ua.ndmik.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ua.ndmik.bot.scheduler.ShutdownsScheduler;

@SpringBootApplication
public class DtekTelegramBotApplication implements CommandLineRunner {

    private final ShutdownsScheduler scheduler;
    private final boolean initDbOnStartup;

    public DtekTelegramBotApplication(ShutdownsScheduler scheduler,
                                      @Value("${app.init-db-on-startup:true}")
                                      boolean initDbOnStartup) {
        this.scheduler = scheduler;
        this.initDbOnStartup = initDbOnStartup;
    }

    static void main(String[] args) {
        SpringApplication.run(DtekTelegramBotApplication.class, args);
    }

    //TODO: remove after run
    @Override
    public void run(String... args) {
        if (!initDbOnStartup) {
            return;
        }
        System.out.println("===== INIT DB =====");
        scheduler.processShutdowns();
        System.out.println("===== DATA LOADED ======");
    }
}
