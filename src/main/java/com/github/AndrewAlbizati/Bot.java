package com.github.AndrewAlbizati;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.util.List;

public class Bot {
    private final String token;
    private final Deck deck;

    public Bot(String token) {
        this.token = token;
        deck = new Deck(6);
        deck.shuffleDeck();
    }

    public Deck getDeck() {
        return deck;
    }

    public void run() {
        // Start Discord bot
        DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();
        System.out.println("Logged in as " + api.getYourself().getDiscriminatedName());

        // Set bot status
        api.updateStatus(UserStatus.ONLINE);
        api.updateActivity(ActivityType.PLAYING, "Type /blackjack to start a game.");

        // Create slash command (may take a few minutes to update on Discord)
        SlashCommand.with("blackjack", "Plays a game of Blackjack that you can bet points on",
                List.of(
                        SlashCommandOption.create(SlashCommandOptionType.LONG, "BET", "Amount of points you wish to bet", true)
                )).createGlobal(api).join();

        // Create slash command listener for blackjack
        api.addSlashCommandCreateListener(new CommandHandler(this));
        api.addMessageComponentCreateListener(new OnButtonPress());
    }
}
