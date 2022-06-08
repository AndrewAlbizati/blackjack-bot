package com.github.AndrewAlbizati;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Game {
    private Message message;

    private final Server server;
    private final User user;
    private long bet;

    private final Deck deck;
    private final Deck dealerHand;
    private final Deck playerHand;

    public Game(Bot bot, Server server, User user, long bet) {
        this.server = server;
        this.user = user;
        this.bet = bet;

        this.deck = bot.getDeck();

        playerHand = new Deck(0);
        dealerHand = new Deck(0);

        playerHand.add(deck.deal());
        dealerHand.add(deck.deal());

        playerHand.add(deck.deal());
        dealerHand.add(deck.deal());
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public Deck getDeck() {
        return deck;
    }

    public Deck getDealerHand() {
        return dealerHand;
    }

    public Deck getPlayerHand() {
        return playerHand;
    }

    public long getBet() {
        return bet;
    }

    public void setBet(long bet) {
        this.bet = bet;
    }

    /**
     * Gets the amount of points that a specific player has.
     * Data stored in bjpoints.json
     *
     * @return The long value of the amount of points the player has.
     */
    public long getPlayerPointAmount() {
        try {
            String fileName = "bjpoints.json";
            FileReader reader = new FileReader(fileName);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(reader);
            reader.close();

            JSONObject serverPoints = (JSONObject) json.get(server.getIdAsString());

            // Add the user to the JSON if they're not already on file
            if (!serverPoints.containsKey(user.getIdAsString())) {
                serverPoints.put(user.getIdAsString(), 100L); // Player receives 100 points to start with
            }

            return (long) serverPoints.get(user.getIdAsString());

        } catch (ParseException | IOException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * Adds or subtracts points from a user's score.
     * Data stored in bjpoints.json
     *
     * @param pointAmount The amount of points that will be added/removed.
     */
    public void givePoints(long pointAmount) {
        try {
            String fileName = "bjpoints.json";
            FileReader reader = new FileReader(fileName);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(reader);
            reader.close();

            JSONObject serverPoints = (JSONObject) json.get(server.getIdAsString());

            // Add the user to the JSON if they're not already on file
            if (!serverPoints.containsKey(user.getIdAsString())) {
                serverPoints.put(user.getIdAsString(), 100L); // Player receives 100 points to start with
            }

            long userPoints = (long) serverPoints.get(user.getIdAsString());

            serverPoints.put(user.getIdAsString(), userPoints + pointAmount);
            json.put(server.getIdAsString(), serverPoints);

            Files.write(Paths.get(fileName), json.toJSONString().getBytes());
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
