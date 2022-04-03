package com.github.AndrewAlbizati;

import org.javacord.api.entity.message.Message;
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
    private User user;
    private Deck deck;

    private Deck dealerHand;
    private Deck playerHand;

    private long bet;

    public Game(User user, long bet) {
        this.user = user;
        this.bet = bet;

        deck = new Deck(6);
        deck.shuffleDeck();

        dealerHand = new Deck(0);
        dealerHand.add(deck.remove(0));
        dealerHand.add(deck.remove(0));

        playerHand = new Deck(0);
        playerHand.add(deck.remove(0));
        playerHand.add(deck.remove(0));
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

    public void setUser(User user) {
        this.user = user;
    }

    public Deck getDeck() {
        return deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public Deck getDealerHand() {
        return dealerHand;
    }

    public void setDealerHand(Deck deck) {
        this.dealerHand = deck;
    }

    public Deck getPlayerHand() {
        return playerHand;
    }

    public void setPlayerHand(Deck deck) {
        this.playerHand = deck;
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

            // Add the user to the JSON if they're not already on file
            if (!json.containsKey(user.getIdAsString())) {
                json.put(user.getIdAsString(), 100L); // Player receives 100 points to start with
            }

            return (long) json.get(user.getIdAsString());

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

            // Add the user to the JSON if they're not already on file
            if (!json.containsKey(user.getIdAsString())) {
                json.put(user.getIdAsString(), 100L); // Player receives 100 points to start with
            }

            long userPoints = (long) json.get(user.getIdAsString());

            json.put(user.getIdAsString(), userPoints + pointAmount);

            Files.write(Paths.get(fileName), json.toJSONString().getBytes());
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
