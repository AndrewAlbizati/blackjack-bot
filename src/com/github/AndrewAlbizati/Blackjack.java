package com.github.AndrewAlbizati;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Blackjack implements CommandExecutor {
    private static HashMap<String, KnownCustomEmoji> emojiHashMap = getEmojisMap();
    public Blackjack() {

    }

    @Command(aliases = {"!blackjack", "!bj"}, async = true, description = "Plays a game of Blackjack.")
    public void onCommand(Server server, TextChannel channel, Message message) throws IOException {
        if (message.getContent().split(" ").length != 2) {
            return;
        }

        if (!isInt(message.getContent().split(" ")[1])) {
            return;
        }

        int bet = Integer.parseInt(message.getContent().split(" ")[1]);


        try {
            // Initialize the game
            Deck deck = new Deck(52);
            deck.shuffleDeck();
            Deck[] hands = deck.deal(2, 2);
            Deck dealerHand = hands[0];
            Deck playerHand = hands[1];
            EmbedBuilder eb = new EmbedBuilder().setTitle("Blackjack")
                    .setDescription("**Commands**" +
                            "\nType **\"hit\"** to hit" +
                            "\nType **\"stand\"** to stand" +
                            "\nYour bet: **" + bet + "**")
                    .setThumbnail("https://cdn.discordapp.com/attachments/810716354977726504/847610923023597568/counting-cards-black-jack.png")
                    .setColor(new Color(155, 89, 182));
            eb.addField("Dealer", cardToString(dealerHand.get(0)));


            String title = "Your Hand (" + getScore(playerHand) + ")";
            eb.addField(title, cardsToString(playerHand));
            Message embedMessage = channel.sendMessage(eb).join();

            if (getScore(playerHand) == 21) {
                channel.sendMessage("Blackjack! 3/2");
                return;
            }

            if (dealerHand.get(0).getValue() == 1 && getScore(dealerHand) == 21) {
                channel.sendMessage("Dealer has blackjack!");
                return;
            }

            // Message listener for hit/stand
            AtomicBoolean roundFinished = new AtomicBoolean(false);
            Bot.getApi().addMessageCreateListener(messageCreateEvent -> {
                if (roundFinished.get()) {
                    return;
                }

                Message m = messageCreateEvent.getMessage();
                if (m.getReadableContent().equalsIgnoreCase("hit")) {
                    playerHand.add(deck.remove(0));
                    EmbedBuilder embedBuilder = embedMessage.getEmbeds().get(0).toBuilder();
                    embedBuilder.removeAllFields();
                    embedBuilder.addField("Dealer", cardToString(dealerHand.get(0)));
                    String title2 = "Your Hand (" + getScore(playerHand) + ")";
                    embedBuilder.addField(title2, cardsToString(playerHand));
                    embedMessage.edit(embedBuilder).join();

                    if (getScore(playerHand) >= 21) {
                        roundFinished.set(true);
                    }
                } else if (m.getReadableContent().equalsIgnoreCase("stand")) {
                    roundFinished.set(true);
                }
            }).removeAfter(2, TimeUnit.MINUTES).addRemoveHandler(() -> {
                roundFinished.set(true);
            });

            // Wait until the player is finished
            while (!roundFinished.get()) {
                Thread.sleep(100);
            }

            // Show dealer's hand
            EmbedBuilder embedBuilder = embedMessage.getEmbeds().get(0).toBuilder();
            embedBuilder.removeAllFields();
            embedBuilder.addField("Dealer (" + getScore(dealerHand) + ")", cardsToString(dealerHand));
            title = "Your Hand (" + getScore(playerHand) + ")";
            embedBuilder.addField(title, cardsToString(playerHand));
            embedMessage.edit(embedBuilder).join();

            // Player busts
            if (getScore(playerHand) > 21) {
                channel.sendMessage("You busted!");
                return;
            }

            // Dealer hits until they get 17
            while (getScore(dealerHand) < 17) {
                dealerHand.add(deck.remove(0));
                embedBuilder = embedMessage.getEmbeds().get(0).toBuilder();
                embedBuilder.removeAllFields();
                embedBuilder.addField("Dealer (" + getScore(dealerHand) + ")", cardsToString(dealerHand));
                title = "Your Hand (" + getScore(playerHand) + ")";
                embedBuilder.addField(title, cardsToString(playerHand));
                embedMessage.edit(embedBuilder).join();
                Thread.sleep(1000);
            }

            // Dealer busts
            if (getScore(dealerHand) > 21) {
                channel.sendMessage("Dealer busted!");
                return;
            }

            // Determine winner
            if (getScore(dealerHand) > getScore(playerHand)) {
                channel.sendMessage("Dealer wins!");
            } else if (getScore(playerHand) > getScore(dealerHand)) {
                channel.sendMessage("Player wins!");
            } else {
                channel.sendMessage("Tie!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isInt(String s) {
        try {
            int i = Integer.parseInt(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String cardsToString(Deck d) {
        StringBuilder cards = new StringBuilder();
        for (Card c : d) {
            cards.append(emojiHashMap.get(c.getId()).getMentionTag() + " " + c.getName() + "\n");
        }

        return cards.toString();
    }

    private static String cardToString(Card c) {
        return emojiHashMap.get(c.getId()).getMentionTag() + " " + c.getName();
    }

    private static HashMap<String, KnownCustomEmoji> getEmojisMap() {
        try {
            HashMap<String, KnownCustomEmoji> map = new HashMap<>();

            InputStream jsonPath = Blackjack.class.getResourceAsStream("resources/cardsids.json");
            JSONParser parser = new JSONParser();
            JSONObject cards = (JSONObject) parser.parse(new InputStreamReader(jsonPath, "UTF-8"));
            for (Object o : cards.keySet()) {
                if (cards.get(o).toString().length() != 18) {
                    System.out.println(o.toString());
                }

                map.put(o.toString(), Bot.getApi().getCustomEmojiById(cards.get(o).toString()).get());
            }

            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ArrayList<Integer> calculateScore(Deck d) {
        d.sortDeck();
        d.reverseDeck();
        ArrayList<Integer> score = new ArrayList<>();
        int scoreWithoutAce = 0;
        for (int i = 0; i < d.size(); i++) {
            Card c = d.get(i);

            switch (c.getValue()) {
                case 1:
                    if (scoreWithoutAce + 1 <= 21) {
                        score.add(scoreWithoutAce + 1);
                    }

                    if (scoreWithoutAce + 11 <= 21) {
                        score.add(scoreWithoutAce + 11);
                    }
                    break;

                case 11:

                case 12:

                case 13:
                    scoreWithoutAce += 10;
                    break;

                default:
                    scoreWithoutAce += c.getValue();
                    break;
            }
        }
        if (score.isEmpty()) {
            score.add(scoreWithoutAce);
        }

        if (score.contains(21)) {
            score.clear();
            score.add(21);
        }

        return score;
    }

    public static int getScore(Deck d) {
        int score = 0;

        Deck d2 = new Deck();
        for (Card c : d) {
            d2.add(c);
        }

        d2.sortDeck();
        d2.reverseDeck();

        for (Card c : d2) {
            switch (c.getValue()) {
                case 1:
                    if (score + 11 <= 21) {
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
}
