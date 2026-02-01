package ua.ndmik.bot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ua.ndmik.bot.scheduler.ShutdownsScheduler;

@SpringBootApplication
public class DtekTelegramBotApplication implements CommandLineRunner {

    private final ShutdownsScheduler scheduler;

    public DtekTelegramBotApplication(ShutdownsScheduler scheduler) {
        this.scheduler = scheduler;
    }

    static void main(String[] args) {
        SpringApplication.run(DtekTelegramBotApplication.class, args);
    }

    //TODO: remove after run
    @Override
    public void run(String... args) {
        System.out.println("===== INIT DB =====");
        scheduler.processShutdowns();
        System.out.println("===== DATA LOADED ======");
    }
}
