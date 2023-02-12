package com.discord.DanceFloorBot;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DanceFloorBotApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(DanceFloorBotApplication.class)
                .build()
                .run(args);
    }

}
