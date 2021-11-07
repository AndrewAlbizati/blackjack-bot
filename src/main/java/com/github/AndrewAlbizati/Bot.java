package com.github.AndrewAlbizati;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class Bot {
    public static void main(String[] args) {
        // Check if bjpoints.json and config.properties are present, creates new files if absent
        try {
            File pointsJSONFile = new File("bjpoints.json");
            if (pointsJSONFile.createNewFile()) {
                FileWriter writer = new FileWriter("bjpoints.json");
                writer.write("{}"); // Empty JSON object
                writer.close();
                System.out.println("bjpoints.json has been created.");
            }

            File config = new File("config.properties");
            if (config.createNewFile()) {
                FileWriter writer = new FileWriter("config.properties");
                writer.write("token=");
                writer.close();
                System.out.println("config.properties has been created.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Get token from config.properties
        // Raise exception if not found
        String token = "";
        try {
            Properties prop = new Properties();
            FileInputStream ip = new FileInputStream("config.properties");
            prop.load(ip);
            ip.close();

            token = prop.getProperty("token");

            if (token.length() == 0)
                throw new NullPointerException("Please add a Discord bot token into config.properties");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Token not found!");
            System.exit(1);
        }

        // Start Discord bot
        try {
            final DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();
            System.out.println("Logged in as " + api.getYourself().getDiscriminatedName());

            // Set bot status
            api.updateStatus(UserStatus.ONLINE);
            api.updateActivity(ActivityType.PLAYING, "Type /blackjack to start a game.");

            // Create slash command (may take a few mins to update on Discord)
            SlashCommand.with("blackjack", "Plays a game of Blackjack that you can bet points on",
                    Arrays.asList(
                            SlashCommandOption.create(SlashCommandOptionType.INTEGER, "BET", "Amount of points you wish to bet", true)
                    )).createGlobal(api).join();

            // Create slash command listener for blackjack
            api.addSlashCommandCreateListener(new BlackjackCommandHandler(api));

            api.addMessageComponentCreateListener(new OnButtonPress(api));
        // Bot failed to start
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("There was an error! " + e.getMessage());
            System.exit(1);
        }
    }
}
