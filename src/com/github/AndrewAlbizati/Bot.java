package com.github.AndrewAlbizati;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.user.UserStatus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Bot {
    public static void main(String[] args) {
        final String COMMAND_PREFIX = "!";

        // Check if bjpoints.json is present, creates the file if absent
        try {
            File pointsJSONFile = new File("bjpoints.json");
            if (pointsJSONFile.createNewFile()) {
                FileWriter writer = new FileWriter("bjpoints.json");
                writer.write("{\n\t\n}");
                writer.close();
                System.out.println("bjpoints.json has been created.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Request token from the user
        System.out.print("Please paste the Discord bot token: ");
        final String TOKEN = new Scanner(System.in).nextLine();

        try {
            final DiscordApi api = new DiscordApiBuilder().setToken(TOKEN).setAllIntents().login().join();
            System.out.println("Logged in as " + api.getYourself().getName());

            // Set bot status
            api.updateStatus(UserStatus.ONLINE);
            api.updateActivity(ActivityType.PLAYING, "Type " + COMMAND_PREFIX + "blackjack to start a game.");

            // Message listener
            api.addMessageCreateListener(event -> {
                if (event.getMessageContent().toLowerCase().startsWith(COMMAND_PREFIX + "blackjack") || event.getMessageContent().toLowerCase().startsWith(COMMAND_PREFIX + "bj")) {
                    Blackjack blackjack = new Blackjack(event, api);
                    blackjack.start();
                }
            });

        // Bot failed to start
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("There was an error! " + e.getMessage());
            System.exit(1);
        }
    }
}
