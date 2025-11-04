package org.example.nagievtgbot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class NagievTgBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(NagievTgBotApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(NagievTgBot bot) {
        return args -> {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);  // Теперь bot — Spring-бин с внедрёнными зависимостями
        };
    }
}