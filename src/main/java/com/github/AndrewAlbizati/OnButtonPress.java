package com.github.AndrewAlbizati;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.embed.EmbedField;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.listener.interaction.MessageComponentCreateListener;

import java.util.ArrayList;
import java.util.List;

public class OnButtonPress implements MessageComponentCreateListener {
    private final DiscordApi api;

    public OnButtonPress(DiscordApi api) {
        this.api = api;
    }

    @Override
    public void onComponentCreate(MessageComponentCreateEvent messageComponentCreateEvent) {
        MessageComponentInteraction messageComponentInteraction = messageComponentCreateEvent.getMessageComponentInteraction();
        String customId = messageComponentInteraction.getCustomId();

        Message message = messageComponentInteraction.getMessage();
        // Message sent by another bot
        if (!message.getAuthor().getIdAsString().equals(api.getYourself().getIdAsString()))
            return;

        // Message has no embeds (or more than 1 embed)
        if (message.getEmbeds().size() != 1)
            return;

        // Embed doesn't have "Blackjack" as title
        if (!message.getEmbeds().get(0).getTitle().get().equals("Blackjack"))
            return;

        EmbedBuilder eb = message.getEmbeds().get(0).toBuilder();
        TextChannel channel = messageComponentInteraction.getChannel().get();
        User user = messageComponentInteraction.getUser();

        String intendedUser = message.getEmbeds().get(0).getFooter().get().getText().get().substring(10);
        // Return if a different user pressed a button
        if (!user.getDiscriminatedName().equals(intendedUser))
            return;

        // Get player's bet and player's points
        long bet = Long.parseLong(message.getEmbeds().get(0).getDescription().get().split("\n")[0].split(" ")[2].replaceAll("\\*", ""));
        long playerPointAmount = BlackjackUtils.getPlayerPointAmount(user.getIdAsString());

        List<EmbedField> fields = message.getEmbeds().get(0).getFields();

        // Get dealer hand
        Deck dealerHand = new Deck(0);
        dealerHand.add(BlackjackUtils.nameToCard(fields.get(0).getValue()));

        // Get player hand
        Deck playerHand = new Deck(0);
        for (String s : fields.get(1).getValue().split("\n"))
            playerHand.add(BlackjackUtils.nameToCard(s));

        // Create list of card ids that have been played
        ArrayList<String> idsInPlay = new ArrayList<>();
        idsInPlay.add(dealerHand.get(0).getId());

        for (Card c : playerHand) {
            idsInPlay.add(c.getId());
        }

        boolean endGame = false;


        // Handle each of the decisions (dd, hit, stand)
        switch (customId) {
            case "dd":
                bet *= 2;
                endGame = true;

            case "hit":
                Card nextCard; // Generate new card
                do {
                    nextCard = BlackjackUtils.randomCard();
                } while (idsInPlay.contains(nextCard.getId()));
                playerHand.add(nextCard);
                idsInPlay.add(nextCard.getId());

                eb.removeAllFields();
                eb.setDescription("You bet **" + bet + "** point" + (bet != 1 ? "s" : "") + "\n" +
                        "You have **" + playerPointAmount + "** point" + (playerPointAmount != 1 ? "s" : "") + "\n\n" +
                        "**Rules**\n" +
                        "Dealer must hit soft 17\n" +
                        "Blackjack pays 3 to 2\n" +
                        "Splitting is **not** allowed");

                eb.addField("Dealer", dealerHand.get(0).getName());
                eb.addField("Your Hand (" + (BlackjackUtils.isSoft(playerHand) ? "Soft " : "") + BlackjackUtils.getScore(playerHand) + ")", BlackjackUtils.cardsToString(playerHand));

                messageComponentInteraction.getMessage().delete();

                // End game if player busts
                if (BlackjackUtils.getScore(playerHand) >= 21 || endGame) {
                    message = channel.sendMessage(eb).join();
                    endGame = true;
                }

                // Resend the embed if the game is still in play
                if (!endGame) {
                    // Add double down option if the player has enough points
                    if (bet * 2 <= playerPointAmount) {
                        message = new MessageBuilder()
                                .addEmbed(eb)
                                .addComponents(
                                        ActionRow.of(Button.primary("hit", "Hit"),
                                                Button.primary("stand", "Stand"),
                                                Button.primary("dd", "Double Down")))
                                .send(channel).join();
                    } else {
                        message = new MessageBuilder()
                                .addEmbed(eb)
                                .addComponents(
                                        ActionRow.of(Button.primary("hit", "Hit"),
                                                Button.primary("stand", "Stand")))
                                .send(channel).join();
                    }
                }

                break;

            case "stand":
                // Remove message components
                messageComponentInteraction.getMessage().delete();
                message = channel.sendMessage(eb).join();
                endGame = true;
                break;
        }

        // Stop code if the game hasn't ended
        if (!endGame) {
            return;
        }

        // Player can no longer make any moves

        // Player busts
        if (BlackjackUtils.getScore(playerHand) > 21) {
            eb.setDescription("**You busted! You lose " + bet + " point" + (bet != 1 ? "s" : "") + "**");

            eb.removeAllFields();
            eb.addField("Dealer's Hand (" + (BlackjackUtils.isSoft(dealerHand) ? "Soft " : "") + BlackjackUtils.getScore(dealerHand) + ")", BlackjackUtils.cardsToString(dealerHand));
            eb.addField("Your Hand (" + (BlackjackUtils.isSoft(playerHand) ? "Soft " : "") + BlackjackUtils.getScore(playerHand) + ")", BlackjackUtils.cardsToString(playerHand));

            eb.setFooter(user.getDiscriminatedName() + " lost!", user.getAvatar());
            message.edit(eb).join();
            BlackjackUtils.givePoints(user.getIdAsString(), -bet);
            return;
        }

        // Dealer hits until they get 17+
        while (BlackjackUtils.getScore(dealerHand) < 17) {
            Card nextCard; // Generate a new card to add to dealer hand
            do {
                nextCard = BlackjackUtils.randomCard();
            } while (idsInPlay.contains(nextCard.getId()));
            dealerHand.add(nextCard);
            idsInPlay.add(nextCard.getId());

            // Update message with new card
            eb.removeAllFields();
            eb.addField("Dealer's Hand (" + (BlackjackUtils.isSoft(dealerHand) ? "Soft " : "") + BlackjackUtils.getScore(dealerHand) + ")", BlackjackUtils.cardsToString(dealerHand));
            eb.addField("Your Hand (" + (BlackjackUtils.isSoft(playerHand) ? "Soft " : "") + BlackjackUtils.getScore(playerHand) + ")", BlackjackUtils.cardsToString(playerHand));
            message = message.edit(eb).join();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Dealer busts
        if (BlackjackUtils.getScore(dealerHand) > 21) {
            eb.setDescription("**Dealer busted! You win " + bet + " point" + (bet != 1 ? "s" : "") + "**");
            eb.setFooter(user.getDiscriminatedName() + " won!", user.getAvatar());
            message.edit(eb).join();
            BlackjackUtils.givePoints(user.getIdAsString(), bet);
            return;
        }

        // Dealer wins
        if (BlackjackUtils.getScore(dealerHand) > BlackjackUtils.getScore(playerHand)) {
            eb.setDescription("**The dealer beat you! You lose " + bet + " point" + (bet != 1 ? "s" : "") + "**");
            eb.setFooter(user.getDiscriminatedName() + " lost!", user.getAvatar());
            BlackjackUtils.givePoints(user.getIdAsString(), -bet);
        // Player wins
        } else if (BlackjackUtils.getScore(playerHand) > BlackjackUtils.getScore(dealerHand)) {
            eb.setDescription("**You beat the dealer! You win " + bet + " point" + (bet != 1 ? "s" : "") + "**");
            eb.setFooter(user.getDiscriminatedName() + " won!", user.getAvatar());
            BlackjackUtils.givePoints(user.getIdAsString(), bet);
        // Tie
        } else {
            eb.setDescription("**You and the dealer tied! You don't win or lose any points**");
            eb.setFooter(user.getDiscriminatedName() + " tied!", user.getAvatar());
        }
        message.edit(eb).join();
    }
}
