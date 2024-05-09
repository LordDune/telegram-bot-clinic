package ru.ld.tgBotClinic.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@Data
@Configuration
@EnableScheduling // включение элементов, подлежащих автоматическому запуску
@PropertySource("application.properties") // местонахождение переменных, которые мы добавляем в код ${}
public class BotConfig {

    @Value("${bot.name}")
    String botName;

    @Value("${bot.token}")
    String token;

    @Value("${bot.owner}")
    Long ownerId;

    @Value("${bot.ownerTelegram}")
    String ownerTelegram;
}
