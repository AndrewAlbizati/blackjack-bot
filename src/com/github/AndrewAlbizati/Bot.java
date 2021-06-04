package com.github.AndrewAlbizati;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.user.UserStatus;

import java.util.Scanner;

public class Bot {
    public static void main(String[] args) {
        String prefix = "!";
        System.out.print("Token: ");
        String token = new Scanner(System.in).nextLine();

        DiscordApi api = new DiscordApiBuilder().setToken(token).setAllIntents().login().join();
        System.out.println("Logged in as " + api.getYourself().getName());

        // Set bot status
        api.updateStatus(UserStatus.ONLINE);
        api.updateActivity(ActivityType.PLAYING, "Type !blackjack to start a game.");

        api.addMessageCreateListener(event -> {
            if (event.getMessageContent().equalsIgnoreCase(prefix + "blackjack") || event.getMessage().getContent().equalsIgnoreCase(prefix + "bj")) {
                Blackjack blackjack = new Blackjack(event, api);
                blackjack.start();
            }
        });
    }
}
