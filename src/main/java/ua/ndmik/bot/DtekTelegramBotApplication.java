package ua.ndmik.bot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ua.ndmik.bot.scheduler.ShutdownsScheduler;
import ua.ndmik.bot.telegram.DtekShutdownBot;

@SpringBootApplication
public class DtekTelegramBotApplication implements CommandLineRunner {

    private final DtekShutdownBot bot;
    private final ShutdownsScheduler scheduler;

    public DtekTelegramBotApplication(DtekShutdownBot bot,
                                      ShutdownsScheduler scheduler) {
        this.bot = bot;
        this.scheduler = scheduler;
    }

    static void main(String[] args) {
        SpringApplication.run(DtekTelegramBotApplication.class, args);
    }


    @Override
    public void run(String... args) {
//        bot.process();
        System.out.println("===== INIT DB =====");
        scheduler.processShutdowns();
        System.out.println("===== DATA LOADED ======");
//        System.out.println("Second");
//        scheduler.processShutdowns();
    }
}
