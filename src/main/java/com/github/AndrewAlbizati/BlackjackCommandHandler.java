package com.github.AndrewAlbizati;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import java.awt.*;
import java.util.ArrayList;

public class BlackjackCommandHandler implements SlashCommandCreateListener {
    final DiscordApi api;

    public BlackjackCommandHandler(DiscordApi api) {
        this.api = api;
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        User user = interaction.getUser();

        // Ignore other slash commands
        if (!interaction.getCommandName().equalsIgnoreCase("blackjack")) {
            return;
        }

        long bet = interaction.getFirstOptionIntValue().get().longValue();
        long playerPointAmount = BlackjackUtils.getPlayerPointAmount(user.getIdAsString());

        // Player tried to bet less than one point
        if (bet < 1) {
            interaction.createImmediateResponder()
                    .setContent("You must bet at least one point.")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
            return;
        }

        // Player's bet is too high
        if (playerPointAmount < bet) {
            interaction.createImmediateResponder()
                    .setContent("Sorry, you need " + (bet - playerPointAmount) + " more points.")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
            return;
        }


        // Create embed with all game information
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Blackjack");
        eb.setDescription("You bet **" + bet + "** point" + (bet != 1 ? "s" : "") +"\n" +
                "You have **" + playerPointAmount + "** point" + (playerPointAmount != 1 ? "s" : "") + "\n\n" +
                "**Rules**\n" +
                "Dealer must hit soft 17\n" +
                "Blackjack pays 3 to 2\n" +
                "Splitting is **not** allowed");
        eb.setColor(new Color(184, 0, 9));
        eb.setFooter("Game with " + user.getDiscriminatedName(), user.getAvatar());
        try {
            eb.setThumbnail("https://the-datascientist.com/wp-content/uploads/2020/05/counting-cards-black-jack.png");
        } catch (Exception e) {
            // Image not found
        }

        ArrayList<String> idsInPlay = new ArrayList<>();
        Deck d = new Deck(0);

        // Generate 4 different random cards
        do {
            Card c = BlackjackUtils.randomCard();
            if (!idsInPlay.contains(c.getId())) {
                idsInPlay.add(c.getId());
                d.add(c);
            }
        } while (idsInPlay.size() < 4);

        // First two cards go to the dealer
        Deck dealerHand = new Deck(0);
        dealerHand.add(d.get(0));
        dealerHand.add(d.get(1));

        // Second two cards go to the player
        Deck playerHand = new Deck(0);
        playerHand.add(d.get(2));
        playerHand.add(d.get(3));

        // Player is dealt a Blackjack
        if (BlackjackUtils.getScore(playerHand) == 21) {
            eb.addField("Dealer's Hand (" + (BlackjackUtils.isSoft(dealerHand) ? "Soft " : "") + BlackjackUtils.getScore(dealerHand) + ")", BlackjackUtils.cardsToString(dealerHand));
            eb.addField("Your Hand (" + (BlackjackUtils.isSoft(playerHand) ? "Soft " : "") + BlackjackUtils.getScore(playerHand) + ")", BlackjackUtils.cardsToString(playerHand));
            eb.setDescription("**You have a blackjack!**");
            eb.setFooter(user.getDiscriminatedName() + " won!", user.getAvatar());

            interaction.createImmediateResponder()
                    .addEmbed(eb)
                    .respond();
            BlackjackUtils.givePoints(user.getIdAsString(), (long) Math.ceil(bet * 1.5));
            return;
        }

        // Dealer is dealt a Blackjack
        if (dealerHand.get(0).getValue() == 1 && BlackjackUtils.getScore(dealerHand) == 21) {
            eb.addField("Dealer's Hand (" + (BlackjackUtils.isSoft(dealerHand) ? "Soft " : "") + BlackjackUtils.getScore(dealerHand) + ")", BlackjackUtils.cardsToString(dealerHand));
            eb.addField("Your Hand (" + (BlackjackUtils.isSoft(playerHand) ? "Soft " : "") + BlackjackUtils.getScore(playerHand) + ")", BlackjackUtils.cardsToString(playerHand));

            eb.setDescription("**Dealer has a blackjack!**");
            eb.setFooter(user.getDiscriminatedName() + " lost!", user.getAvatar());

            interaction.createImmediateResponder()
                    .addEmbed(eb)
                    .respond();
            BlackjackUtils.givePoints(user.getIdAsString(), -bet);
            return;
        }

        // Show the dealer's upcard and the players hand
        eb.addField("Dealer's Hand", dealerHand.get(0).getName());
        eb.addField("Your Hand (" + (BlackjackUtils.isSoft(playerHand) ? "Soft " : "") + BlackjackUtils.getScore(playerHand) + ")", BlackjackUtils.cardsToString(playerHand));

        // Add double down option if the player has enough points
        if (bet * 2 <= playerPointAmount) {
            interaction.createImmediateResponder()
                    .addEmbed(eb)
                    .addComponents(
                            ActionRow.of(Button.primary("hit", "Hit"),
                                    Button.primary("stand", "Stand"),
                                    Button.primary("dd", "Double Down")))
                    .respond();
        } else {
            interaction.createImmediateResponder()
                    .addEmbed(eb)
                    .addComponents(
                            ActionRow.of(Button.primary("hit", "Hit"),
                                    Button.primary("stand", "Stand")))
                    .respond();
        }
    }
}
