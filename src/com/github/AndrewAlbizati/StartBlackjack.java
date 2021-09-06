package com.github.AndrewAlbizati;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.awt.*;

public class StartBlackjack {
    final MessageCreateEvent event;
    final DiscordApi api;
    public StartBlackjack(MessageCreateEvent event, DiscordApi api) {
        this.event = event;
        this.api = api;
    }

    public void start() {
        Message message = event.getMessage();
        TextChannel channel = event.getChannel();

        if (message.getContent().split(" ").length != 2)
            return;

        long bet = Long.parseLong(message.getContent().split(" ")[1]);
        long playerPointAmount = BlackjackUtils.getPlayerPointAmount(message.getAuthor().getIdAsString());
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Blackjack");
        eb.setDescription("You bet **" + bet + "** point" + (bet != 1 ? "s" : "") +"\n" +
                "You have **" + playerPointAmount + "** point" + (playerPointAmount != 1 ? "s" : "") + "\n\n" +
                "**Rules**\n" +
                "Dealer must hit soft 17\n" +
                "Blackjack pays 3 to 2\n" +
                "Splitting is **not** allowed");
        eb.setColor(new Color(184, 0, 9));
        eb.setFooter("Game with " + message.getAuthor().getDiscriminatedName(), message.getAuthor().getAvatar());
        try {
            eb.setThumbnail("https://the-datascientist.com/wp-content/uploads/2020/05/counting-cards-black-jack.png");
        } catch (Exception e) {
            // Image not found
        }

        Deck cardsInPlay = new Deck(0);

        // Generate 4 different random cards
        do {
            Card c = BlackjackUtils.randomCard();
            if (!cardsInPlay.contains(c)) {
                cardsInPlay.add(c);
            }
        } while (cardsInPlay.size() < 4);

        Deck dealerHand = new Deck(0);
        dealerHand.add(cardsInPlay.get(0));
        dealerHand.add(cardsInPlay.get(1));

        Deck playerHand = new Deck(0);
        playerHand.add(cardsInPlay.get(2));
        playerHand.add(cardsInPlay.get(3));

        // Player is dealt a Blackjack
        if (BlackjackUtils.getScore(playerHand) == 21) {
            eb.addField("Dealer's Hand (" + (BlackjackUtils.isSoft(dealerHand) ? "Soft " : "") + BlackjackUtils.getScore(dealerHand) + ")", BlackjackUtils.cardsToString(dealerHand));
            eb.addField("Your Hand (" + (BlackjackUtils.isSoft(playerHand) ? "Soft " : "") + BlackjackUtils.getScore(playerHand) + ")", BlackjackUtils.cardsToString(playerHand));
            eb.setDescription("**You have a blackjack!**");
            eb.setFooter(message.getAuthor().getDisplayName() + " won!", message.getAuthor().getAvatar());

            channel.sendMessage(eb);
            BlackjackUtils.givePoints(message.getAuthor().getIdAsString(), (long) Math.ceil(bet * 1.5));
            return;
        }

        // Dealer is dealt a Blackjack
        if (dealerHand.get(0).getValue() == 1 && BlackjackUtils.getScore(dealerHand) == 21) {
            eb.addField("Dealer's Hand (" + (BlackjackUtils.isSoft(dealerHand) ? "Soft " : "") + BlackjackUtils.getScore(dealerHand) + ")", BlackjackUtils.cardsToString(dealerHand));
            eb.addField("Your Hand (" + (BlackjackUtils.isSoft(playerHand) ? "Soft " : "") + BlackjackUtils.getScore(playerHand) + ")", BlackjackUtils.cardsToString(playerHand));

            eb.setDescription("**Dealer has a blackjack!**");
            eb.setFooter(message.getAuthor().getDisplayName() + " lost!", message.getAuthor().getAvatar());

            channel.sendMessage(eb);
            BlackjackUtils.givePoints(message.getAuthor().getIdAsString(), -bet);
            return;
        }

        eb.addField("Dealer's Hand", dealerHand.get(0).getName());
        eb.addField("Your Hand (" + (BlackjackUtils.isSoft(playerHand) ? "Soft " : "") + BlackjackUtils.getScore(playerHand) + ")", BlackjackUtils.cardsToString(playerHand));

        // Add double down option if the player has enough points
        if (bet * 2 <= playerPointAmount) {
            new MessageBuilder()
                    .addEmbed(eb)
                    .addComponents(
                            ActionRow.of(Button.primary("hit", "Hit"),
                                    Button.primary("stand", "Stand"),
                                    Button.primary("dd", "Double Down")))
                    .send(channel).join();
        } else {
            new MessageBuilder()
                    .addEmbed(eb)
                    .addComponents(
                            ActionRow.of(Button.primary("hit", "Hit"),
                                    Button.primary("stand", "Stand")))
                    .send(channel).join();
        }
    }
}
