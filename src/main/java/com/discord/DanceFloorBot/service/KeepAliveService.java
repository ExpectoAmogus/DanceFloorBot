package com.discord.DanceFloorBot.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class KeepAliveService {

    @Scheduled(fixedRate = 1000 * 60)
    public void reportCurrentTime() {
        System.out.println(System.currentTimeMillis());
    }
}
