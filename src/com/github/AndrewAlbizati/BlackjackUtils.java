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

    public static String cardsToString(Deck d) {
        StringBuilder deckString = new StringBuilder();
        for (Card c : d) {
            deckString.append(c.getName() + "\n");
        }
        return deckString.toString();
    }

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
        switch(suitStr) {
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

    public static Card randomCard() {
        Random rand = new Random();
        Suit[] suits = {Suit.SPADE, Suit.DIAMOND, Suit.HEART, Suit.CLUB};
        Card c = new Card(rand.nextInt(13) + 1, suits[rand.nextInt(suits.length)]);
        return c;
    }

    public static long getPlayerPointAmount(String id) {
        long playerPointAmount;
        try {
            String jsonPath = "bjpoints.json";
            FileReader reader = new FileReader(jsonPath);
            JSONParser parser = new JSONParser();
            JSONObject points = (JSONObject)parser.parse(reader);
            reader.close();

            JSONObject userPoints = (JSONObject) points.get(id);
            if (userPoints != null) {
                playerPointAmount = (long) userPoints.get("points");
            } else {
                // Player receives 100 points to start with
                givePoints(id, 100);
                playerPointAmount = 100;
            }

            return playerPointAmount;

        } catch (ParseException | IOException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public static void givePoints(String id, long pointAmount) {
        try {
            String jsonPath = "bjpoints.json";
            FileReader reader = new FileReader(jsonPath);
            JSONParser parser = new JSONParser();
            JSONObject points = (JSONObject)parser.parse(reader);
            reader.close();

            if (!points.containsKey(id)) {
                points.put(id, new JSONObject());
            }

            JSONObject userPoints = (JSONObject) points.get(id);
            if (!userPoints.containsKey("points")) {
                userPoints.put("points", 0L);
            }

            userPoints.put("points", (long) userPoints.get("points") + pointAmount);

            points.put(id, userPoints);

            Files.write(Paths.get(jsonPath), points.toJSONString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
