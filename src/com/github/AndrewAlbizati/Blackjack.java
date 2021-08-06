package com.github.AndrewAlbizati;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Blackjack extends Thread {

    private final MessageCreateEvent event;
    private final DiscordApi api;

    public Blackjack(MessageCreateEvent event, DiscordApi api) {
        this.event = event;
        this.api = api;
    }

    public void run() {
        Message message = event.getMessage();
        TextChannel channel = event.getChannel();

        long playerPointAmount;
        try {
            String jsonPath = "bjpoints.json";
            FileReader reader = new FileReader(jsonPath);
            JSONParser parser = new JSONParser();
            JSONObject stats = (JSONObject)parser.parse(reader);
            reader.close();

            JSONObject userstats = (JSONObject) stats.get(message.getAuthor().getIdAsString());
            if (userstats != null) {
                playerPointAmount = (long) userstats.get("points");
            } else {
                // Player receives 100 points to start with
                givePoints(message.getAuthor().getIdAsString(), 100);
                playerPointAmount = 100;
            }

        } catch (ParseException | IOException e) {
            e.printStackTrace();
            return;
        }

        long bet;
        try {
            bet = Long.parseLong(message.getContent().split(" ")[1]);
        } catch (Exception e) {
            channel.sendMessage("You must bet at least 1 point. You have **" + playerPointAmount + "** point" + (playerPointAmount != 1 ? "s" : "") +".");
            return;
        }

        // Prevent players from betting 0 or fewer points
        if (bet <= 0) {
            channel.sendMessage("Please bet at least 1 point.");
            return;
        }

        // Player bets more than points than they have
        if (bet > playerPointAmount) {
            channel.sendMessage("Sorry, you need **" + (bet - playerPointAmount) + "** more point" + (bet - playerPointAmount != 1 ? "s" : "") + " .");
            return;
        }


        // Initialize the game with 6 decks shuffled together
        Deck deck = new Deck(6);
        deck.shuffleDeck();
        Deck[] hands = deck.deal(2, 2);
        Deck dealerHand = hands[0];
        Deck playerHand = hands[1];

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Blackjack");
        eb.setDescription("**Commands**\n" +
                "Type **\"hit\"** to hit\n" +
                "Type **\"stand\"** to stand\n" +
                "Type **\"dd\"** to double down\n" +
                "Your bet: **" + bet + "**\n" +
                "You have **" + (playerPointAmount - bet) + "** points left\n\n" +
                "**Rules**\n" +
                "Dealer must hit soft 17\n" +
                "Blackjack pays 3 to 2\n" +
                "Splitting is **not** allowed");
        eb.setColor(new Color(184, 0, 9));
        eb.setFooter("Game with " + message.getAuthor().getDisplayName(), message.getAuthor().getAvatar());
        try {
            eb.setThumbnail("https://the-datascientist.com/wp-content/uploads/2020/05/counting-cards-black-jack.png");
        } catch (Exception e) {
            // Image not found
        }

        eb.addField("Dealer's Hand", dealerHand.get(0).getName());
        eb.addField("Your Hand (" + (isSoft(playerHand) ? "Soft " : "") + getScore(playerHand) + ")", cardsToString(playerHand));

        Message embedMessage = channel.sendMessage(eb).join();

        // Player and dealer are dealt Blackjacks
        if (getScore(playerHand) == 21 && getScore(dealerHand) == 21) {
            eb.removeAllFields();
            eb.addField("Dealer's Hand (" + (isSoft(dealerHand) ? "Soft " : "") + getScore(dealerHand) + ")", cardsToString(dealerHand));
            eb.addField("Your Hand (" + (isSoft(playerHand) ? "Soft " : "") + getScore(playerHand) + ")", cardsToString(playerHand));
            eb.setDescription("**You and the dealer have blackjacks! You don't win or lose any points.**");
            eb.setFooter(message.getAuthor().getDisplayName() + " pushed!", message.getAuthor().getAvatar());

            channel.sendMessage(eb).join();
            return;
        }


        // Player is dealt a Blackjack
        if (getScore(playerHand) == 21) {
            EmbedBuilder winningEmbed = embedMessage.getEmbeds().get(0).toBuilder();
            winningEmbed.removeAllFields();
            winningEmbed.addField("Dealer's Hand (" + (isSoft(dealerHand) ? "Soft " : "") + getScore(dealerHand) + ")", cardsToString(dealerHand));
            winningEmbed.addField("Your Hand (" + (isSoft(playerHand) ? "Soft " : "") + getScore(playerHand) + ")", cardsToString(playerHand));
            winningEmbed.setDescription("**You have a blackjack!**");
            winningEmbed.setFooter(message.getAuthor().getDisplayName() + " won!", message.getAuthor().getAvatar());

            embedMessage.edit(winningEmbed);
            givePoints(message.getAuthor().getIdAsString(), (long) Math.ceil(bet * 1.5));
            return;
        }

        // Dealer is dealt a Blackjack
        if (dealerHand.get(0).getValue() == 1 && getScore(dealerHand) == 21) {
            EmbedBuilder winningEmbed = embedMessage.getEmbeds().get(0).toBuilder();
            winningEmbed.removeAllFields();
            winningEmbed.addField("Dealer's Hand (" + (isSoft(dealerHand) ? "Soft " : "") + getScore(dealerHand) + ")", cardsToString(dealerHand));
            winningEmbed.addField("Your Hand (" + (isSoft(playerHand) ? "Soft " : "") + getScore(playerHand) + ")", cardsToString(playerHand));

            winningEmbed.setDescription("**Dealer has a blackjack!**");
            winningEmbed.setFooter(message.getAuthor().getDisplayName() + " lost!", message.getAuthor().getAvatar());

            embedMessage.edit(winningEmbed).join();
            givePoints(message.getAuthor().getIdAsString(), -bet);
            return;
        }

        AtomicBoolean playerFinished = new AtomicBoolean(false);
        AtomicLong betIncrease = new AtomicLong(0); // Bet increases if a player double downs
        long finalBet = bet; // Create finalBet so it can be referenced in lambda function

        final AtomicReference<Message> embedMessageReference = new AtomicReference<>();
        embedMessageReference.set(embedMessage);
        // Message listener for player actions
        api.addMessageCreateListener(messageCreateEvent -> {
            if (playerFinished.get()) {
                return;
            }

            if (!messageCreateEvent.getMessageAuthor().getIdAsString().equals(message.getAuthor().getIdAsString())) {
                return;
            }

            Message m = messageCreateEvent.getMessage();

            // Disregards any message that isn't related to blackjack
            if (!m.getReadableContent().equalsIgnoreCase("hit") && !m.getReadableContent().equalsIgnoreCase("stand") && !m.getReadableContent().equalsIgnoreCase("dd") && !m.getReadableContent().equalsIgnoreCase("double down")) {
                return;
            }

            // Player chooses to hit
            if (m.getReadableContent().equalsIgnoreCase("hit")) {
                playerHand.add(deck.remove(0));
                EmbedBuilder embedBuilder = embedMessageReference.get().getEmbeds().get(0).toBuilder();
                embedBuilder.removeAllFields();
                embedBuilder.addField("Dealer", dealerHand.get(0).getName());
                embedBuilder.addField("Your Hand (" + (isSoft(playerHand) ? "Soft " : "") + getScore(playerHand) + ")", cardsToString(playerHand));
                embedMessageReference.get().delete().join();
                embedMessageReference.set(channel.sendMessage(embedBuilder).join());

                if (getScore(playerHand) >= 21) {
                    playerFinished.set(true);
                }
                return;
            }

            // Player chooses to double down
            if (m.getReadableContent().equalsIgnoreCase("double down") || m.getReadableContent().equalsIgnoreCase("dd")) {
                if (playerPointAmount - (finalBet * 2) < 0) {
                    messageCreateEvent.getChannel().sendMessage("You don't have enough points to double down!");
                    return;
                }
                betIncrease.set(finalBet);
                playerHand.add(deck.remove(0));
            }

            playerFinished.set(true);
        }).removeAfter(90, TimeUnit.SECONDS).addRemoveHandler(() -> {
            playerFinished.set(true);
        });

        // Wait until the player is finished
        while (!playerFinished.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
        bet += betIncrease.get();
        embedMessageReference.get().delete().join();

        // Show dealer's hand
        EmbedBuilder embedBuilder = embedMessageReference.get().getEmbeds().get(0).toBuilder();
        embedBuilder.removeAllFields();
        embedBuilder.addField("Dealer's Hand (" + (isSoft(dealerHand) ? "Soft " : "") + getScore(dealerHand) + ")", cardsToString(dealerHand));
        embedBuilder.addField("Your Hand (" + (isSoft(playerHand) ? "Soft " : "") + getScore(playerHand) + ")", cardsToString(playerHand));
        embedMessage = channel.sendMessage(embedBuilder).join();

        // Player busts
        if (getScore(playerHand) > 21) {
            EmbedBuilder winningEmbed = embedMessage.getEmbeds().get(0).toBuilder();
            winningEmbed.setDescription("**You busted! You lose " + bet + " point" + (bet != 1 ? "s" : "") + "**");
            winningEmbed.setFooter(message.getAuthor().getDisplayName() + " lost!", message.getAuthor().getAvatar());
            embedMessage.edit(winningEmbed).join();
            givePoints(message.getAuthor().getIdAsString(), -bet);
            return;
        }

        // Dealer hits until they get 17
        while (getScore(dealerHand) < 17) {
            dealerHand.add(deck.remove(0));

            embedBuilder = embedMessage.getEmbeds().get(0).toBuilder();
            embedBuilder.removeAllFields();
            embedBuilder.addField("Dealer's Hand (" + (isSoft(dealerHand) ? "Soft " : "") + getScore(dealerHand) + ")", cardsToString(dealerHand));
            embedBuilder.addField("Your Hand (" + (isSoft(playerHand) ? "Soft " : "") + getScore(playerHand) + ")", cardsToString(playerHand));
            embedMessage.edit(embedBuilder).join();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Dealer busts
        if (getScore(dealerHand) > 21) {
            EmbedBuilder winningEmbed = embedMessage.getEmbeds().get(0).toBuilder();
            winningEmbed.setDescription("**Dealer busted! You win " + bet + " point" + (bet != 1 ? "s" : "") + "**");
            winningEmbed.setFooter(message.getAuthor().getDisplayName() + " won!", message.getAuthor().getAvatar());
            embedMessage.edit(winningEmbed).join();
            givePoints(message.getAuthor().getIdAsString(), bet);
            return;
        }

        // Determine winner
        EmbedBuilder winningEmbed = embedMessage.getEmbeds().get(0).toBuilder();
        // Dealer wins
        if (getScore(dealerHand) > getScore(playerHand)) {
            winningEmbed.setDescription("**Dealer beat you! You lose " + bet + " point" + (bet != 1 ? "s" : "") + "**");
            winningEmbed.setFooter(message.getAuthor().getDisplayName() + " lost!", message.getAuthor().getAvatar());
            givePoints(message.getAuthor().getIdAsString(), -bet);
        // Player wins
        } else if (getScore(playerHand) > getScore(dealerHand)) {
            winningEmbed.setDescription("**You beat the dealer! You win " + bet + " point" + (bet != 1 ? "s" : "") + "**");
            winningEmbed.setFooter(message.getAuthor().getDisplayName() + " won!", message.getAuthor().getAvatar());
            givePoints(message.getAuthor().getIdAsString(), bet);
        // Tie
        } else {
            winningEmbed.setDescription("**You and the dealer tied! You don't win or lose any points**");
            winningEmbed.setFooter(message.getAuthor().getDisplayName() + " tied!", message.getAuthor().getAvatar());
        }
        embedMessage.edit(winningEmbed).join();
    }

    private static String cardsToString(Deck d) {
        StringBuilder deckString = new StringBuilder();
        for (Card c : d) {
            deckString.append(c.getName() + "\n");
        }
        return deckString.toString();
    }

    private static int getScore(Deck d) {
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

    private static boolean isSoft(Deck d) {
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

    private static boolean givePoints(String id, long points) {
        try {
            String jsonPath = "bjpoints.json";
            FileReader reader = new FileReader(jsonPath);
            JSONParser parser = new JSONParser();
            JSONObject stats = (JSONObject)parser.parse(reader);
            reader.close();

            if (!stats.containsKey(id)) {
                stats.put(id, new JSONObject());
            }

            JSONObject userstats = (JSONObject) stats.get(id);
            if (!userstats.containsKey("points")) {
                userstats.put("points", 0L);
            }


            userstats.put("points", (long) userstats.get("points") + points);

            stats.put(id, userstats);

            Files.write(Paths.get(jsonPath), stats.toJSONString().getBytes());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
