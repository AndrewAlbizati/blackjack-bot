package com.github.AndrewAlbizati;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.awt.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Blackjack extends Thread {

    private MessageCreateEvent event;
    private DiscordApi api;

    public Blackjack(MessageCreateEvent event, DiscordApi api) {
        this.event = event;
        this.api = api;
    }

    public void run() {
        Message message = event.getMessage();
        TextChannel channel = event.getChannel();

        // Blackjack game
        try {
            // Initialize the game
            Deck deck = new Deck(52);
            deck.shuffleDeck();
            Deck[] hands = deck.deal(2, 2);
            Deck dealerHand = hands[0];
            Deck playerHand = hands[1];

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Blackjack");
            eb.setDescription("**Commands**\nType **\"hit\"** to hit\nType **\"stand\"** to stand");
            eb.setColor(new Color(184, 0, 9));
            try {
                eb.setThumbnail("https://the-datascientist.com/wp-content/uploads/2020/05/counting-cards-black-jack.png");
            } catch (Exception e) {
                // Image not found
            }

            eb.addField("Dealer's Hand", dealerHand.get(0).getName());

            String title = "Your Hand (" + getScore(playerHand) + ")";
            eb.addField(title, cardsToString(playerHand));
            Message embedMessage = channel.sendMessage(eb).join();

            // Player is dealt a Blackjack
            if (getScore(playerHand) == 21) {
                EmbedBuilder winningEmbed = embedMessage.getEmbeds().get(0).toBuilder();
                winningEmbed.removeAllFields();
                winningEmbed.addField("Dealer's Hand (" + getScore(dealerHand) + ")", cardsToString(dealerHand));
                winningEmbed.addField("Your Hand (" + getScore(playerHand) + ")", cardsToString(playerHand));
                winningEmbed.setDescription("**You have a blackjack!**");
                winningEmbed.setFooter(message.getAuthor().getDisplayName() + " won!", message.getAuthor().getAvatar());

                embedMessage.edit(winningEmbed);
                return;
            }

            // Dealer is dealt a Blackjack
            if (dealerHand.get(0).getValue() == 1 && getScore(dealerHand) == 21) {
                EmbedBuilder winningEmbed = embedMessage.getEmbeds().get(0).toBuilder();
                winningEmbed.removeAllFields();
                winningEmbed.addField("Dealer's Hand (" + getScore(dealerHand) + ")", cardsToString(dealerHand));
                winningEmbed.addField("Your Hand (" + getScore(playerHand) + ")", cardsToString(playerHand));
                winningEmbed.setDescription("**Dealer has a blackjack!**");
                winningEmbed.setFooter(message.getAuthor().getDisplayName() + " lost!", message.getAuthor().getAvatar());

                embedMessage.edit(winningEmbed).join();
                return;
            }

            // Message listener for hit/stand
            AtomicBoolean roundFinished = new AtomicBoolean(false);
            api.addMessageCreateListener(messageCreateEvent -> {
                if (roundFinished.get()) {
                    return;
                }

                if (!messageCreateEvent.getMessageAuthor().getIdAsString().equals(message.getAuthor().getIdAsString())) {
                    return;
                }

                Message m = messageCreateEvent.getMessage();
                if (!m.getReadableContent().equalsIgnoreCase("hit") && !m.getReadableContent().equalsIgnoreCase("stand")) {
                    return;
                }

                if (m.getReadableContent().equalsIgnoreCase("hit")) {
                    playerHand.add(deck.remove(0));
                    EmbedBuilder embedBuilder = embedMessage.getEmbeds().get(0).toBuilder();
                    embedBuilder.removeAllFields();
                    embedBuilder.addField("Dealer", dealerHand.get(0).getName());
                    embedBuilder.addField("Your Hand (" + getScore(playerHand) + ")", cardsToString(playerHand));
                    embedMessage.edit(embedBuilder).join();

                    if (getScore(playerHand) >= 21) {
                        roundFinished.set(true);
                    }
                    return;
                }

                roundFinished.set(true);
            }).removeAfter(90, TimeUnit.SECONDS).addRemoveHandler(() -> {
                roundFinished.set(true);
            });

            // Wait until the player is finished
            while (!roundFinished.get()) {
                Thread.sleep(100);
            }

            // Show dealer's hand
            EmbedBuilder embedBuilder = embedMessage.getEmbeds().get(0).toBuilder();
            embedBuilder.removeAllFields();
            embedBuilder.addField("Dealer's Hand (" + getScore(dealerHand) + ")", cardsToString(dealerHand));
            embedBuilder.addField("Your Hand (" + getScore(playerHand) + ")", cardsToString(playerHand));
            embedMessage.edit(embedBuilder).join();
            Thread.sleep(100);

            // Player busts
            if (getScore(playerHand) > 21) {
                EmbedBuilder winningEmbed = embedMessage.getEmbeds().get(0).toBuilder();
                winningEmbed.setDescription("**You busted!**");
                winningEmbed.setFooter(message.getAuthor().getDisplayName() + " lost!", message.getAuthor().getAvatar());
                embedMessage.edit(winningEmbed).join();
                return;
            }

            // Dealer hits until they get 17
            while (getScore(dealerHand) < 17) {
                dealerHand.add(deck.remove(0));
                embedBuilder = embedMessage.getEmbeds().get(0).toBuilder();
                embedBuilder.removeAllFields();
                embedBuilder.addField("Dealer's Hand (" + getScore(dealerHand) + ")", cardsToString(dealerHand));
                title = "Your Hand (" + getScore(playerHand) + ")";
                embedBuilder.addField(title, cardsToString(playerHand));
                embedMessage.edit(embedBuilder).join();
                Thread.sleep(1000);
            }

            // Dealer busts
            if (getScore(dealerHand) > 21) {
                EmbedBuilder winningEmbed = embedMessage.getEmbeds().get(0).toBuilder();
                winningEmbed.setDescription("**Dealer busted!**");
                winningEmbed.setFooter(message.getAuthor().getDisplayName() + " won!", message.getAuthor().getAvatar());
                embedMessage.edit(winningEmbed).join();
                return;
            }

            // Determine winner
            EmbedBuilder winningEmbed = embedMessage.getEmbeds().get(0).toBuilder();
            if (getScore(dealerHand) > getScore(playerHand)) {
                winningEmbed.setDescription("**The dealer beat you!**");
                winningEmbed.setFooter(message.getAuthor().getDisplayName() + " lost!", message.getAuthor().getAvatar());
            } else if (getScore(playerHand) > getScore(dealerHand)) {
                winningEmbed.setDescription("**You beat the dealer!**");
                winningEmbed.setFooter(message.getAuthor().getDisplayName() + " won!", message.getAuthor().getAvatar());
            } else {
                winningEmbed.setDescription("**You tied!**");
                winningEmbed.setFooter(message.getAuthor().getDisplayName() + " tied!", message.getAuthor().getAvatar());
            }
            embedMessage.edit(winningEmbed).join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String cardsToString(Deck d) {
        StringBuilder deckString = new StringBuilder();
        for (Card c : d) {
            deckString.append(c.getName() + "\n");
        }
        return deckString.toString();
    }

    private static int getScore(Deck d) {
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
