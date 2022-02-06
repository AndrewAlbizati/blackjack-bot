package com.github.AndrewAlbizati;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class BlackjackUtils {
    /**
     * Checks if a deck is soft (contains an ace valued at 11).
     *
     * @param d The deck that will be checked.
     * @return Whether or not the deck is soft.
     */
    public static boolean isSoft(Deck d) {
        int aceCount = 0;
        for (Card c : d) {
            if (c.getValue() == 1) {
                aceCount++;
            }
        }
        // Hands without aces can't be soft
        if (aceCount == 0) {
            return false;
        }

        int scoreWithoutAce = 0;
        for (Card c : d) {
            switch (c.getValue()) {
                case 1:
                    break;

                case 11:
                case 12:
                case 13:
                    scoreWithoutAce += 10;
                    break;

                default:
                    scoreWithoutAce += c.getValue();
            }
        }

        if (scoreWithoutAce > 9 && aceCount > 1) {
            return false;
        }

        return scoreWithoutAce < 11;
    }

    /**
     * Evaluates the score of the deck.
     * Face cards are worth 10 points, aces are worth 1 or 11 points.
     *
     * @param d That deck that will be checked.
     * @return The Blackjack score of the deck.
     */
    public static int getScore(Deck d) {
        int score = 0;

        Deck d2 = new Deck(0);
        d2.addAll(d);

        d2.sortDeck();
        d2.reverseDeck();

        for (Card c : d2) {
            switch (c.getValue()) {
                case 1:
                    // Next card is an Ace
                    if (score + 11 <= 21) {
                        if (d2.size() > d2.indexOf(c) + 1) {
                            if (d2.get(d2.indexOf(c) + 1).getValue() == 1) {
                                score += 1;
                                break;
                            }
                        }

                        score += 11;
                        break;
                    }

                    score += 1;
                    break;

                case 11:
                case 12:
                case 13:
                    score += 10;
                    break;

                default:
                    score += c.getValue();
                    break;
            }
        }

        return score;
    }

    /**
     * Converts a deck into a user-friendly string.
     *
     * @param d Deck that will be converted.
     * @return A string that lists the name of each card in the deck.
     */
    public static String cardsToString(Deck d) {
        StringBuilder deckString = new StringBuilder();
        for (Card c : d) {
            deckString.append(c.getName() + "\n");
        }
        return deckString.toString();
    }

    /**
     * Converts the name of a card to an instance of the card class.
     *
     * @param name The name of the card (e.g. "5 of Spades").
     * @return A new card with the same value and suit as the name provided.
     */
    public static Card nameToCard(String name) {
        String valueStr = name.split(" ")[0].toLowerCase();
        String suitStr = name.split(" ")[2].toLowerCase();

        int value;
        switch (valueStr) {
            case "jack":
                value = 11;
                break;
            case "queen":
                value = 12;
                break;
            case "king":
                value = 13;
                break;
            case "ace":
                value = 1;
                break;
            default:
                value = Integer.parseInt(valueStr);
                break;
        }

        Suit suit;
        switch (suitStr) {
            case "spades":
                suit = Suit.SPADE;
                break;
            case "diamonds":
                suit = Suit.DIAMOND;
                break;
            case "hearts":
                suit = Suit.HEART;
                break;
            case "clubs":
                suit = Suit.CLUB;
                break;
            default:
                suit = null;
                break;
        }

        return new Card(value, suit);
    }

    /**
     * Generates a random card with 52 different options.
     *
     * @return A random instance of the card class.
     */
    public static Card randomCard() {
        Random rand = new Random();
        Suit[] suits = {Suit.SPADE, Suit.DIAMOND, Suit.HEART, Suit.CLUB};
        return new Card(rand.nextInt(13) + 1, suits[rand.nextInt(suits.length)]);
    }

    /**
     * Gets the amount of points that a specific player has.
     * Data stored in bjpoints.json
     *
     * @param id The Discord id of the user.
     * @return The long value of the amount of points the player has.
     */
    public static long getPlayerPointAmount(String id) {
        try {
            String fileName = "bjpoints.json";
            FileReader reader = new FileReader(fileName);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(reader);
            reader.close();

            // Add the user to the JSON if they're not already on file
            if (!json.containsKey(id)) {
                json.put(id, 100L); // Player receives 100 points to start with
            }

            return (long) json.get(id);

        } catch (ParseException | IOException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * Adds or subtracts points from a user's score.
     * Data stored in bjpoints.json
     *
     * @param id The Discord id of the user.
     * @param pointAmount The amount of points that will be added/removed.
     */
    public static void givePoints(String id, long pointAmount) {
        try {
            String fileName = "bjpoints.json";
            FileReader reader = new FileReader(fileName);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(reader);
            reader.close();

            // Add the user to the JSON if they're not already on file
            if (!json.containsKey(id)) {
                json.put(id, 100L); // Player receives 100 points to start with
            }

            long userPoints = (long) json.get(id);

            json.put(id, userPoints + pointAmount);

            Files.write(Paths.get(fileName), json.toJSONString().getBytes());
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}

