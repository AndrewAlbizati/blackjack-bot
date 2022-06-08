package com.github.AndrewAlbizati;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.listener.interaction.MessageComponentCreateListener;

public class OnButtonPress implements MessageComponentCreateListener {
    @Override
    public void onComponentCreate(MessageComponentCreateEvent messageComponentCreateEvent) {
        MessageComponentInteraction messageComponentInteraction = messageComponentCreateEvent.getMessageComponentInteraction();
        String customId = messageComponentInteraction.getCustomId();

        Message message = messageComponentInteraction.getMessage();
        Game game = null;

        for (Long userId : BlackjackCommandHandler.blackjackGames.keySet()) {
            if (BlackjackCommandHandler.blackjackGames.get(userId).getMessage().getId() == message.getId()) {
                game = BlackjackCommandHandler.blackjackGames.get(userId);
            }
        }

        if (game == null || game.getUser().getId() != messageComponentInteraction.getUser().getId()) {
            return;
        }

        EmbedBuilder eb = message.getEmbeds().get(0).toBuilder();

        boolean endGame = false;

        // Handle each of the decisions (dd, hit, stand)
        switch (customId) {
            case "dd":
                game.setBet(game.getBet() * 2);
                endGame = true;

            case "hit":
                game.getPlayerHand().add(game.getDeck().deal());

                eb.removeAllFields();
                eb.setDescription("You bet **" + game.getBet() + "** point" + (game.getBet() != 1 ? "s" : "") + "\n" +
                        "You have **" + game.getPlayerPointAmount() + "** point" + (game.getPlayerPointAmount() != 1 ? "s" : "") + "\n\n" +
                        "**Rules**\n" +
                        "Dealer must hit soft 17\n" +
                        "Blackjack pays 3 to 2\n" +
                        "Splitting is **not** allowed");

                eb.addField("Dealer", game.getDealerHand().get(0).toString());
                eb.addField("Your Hand (" + (game.getPlayerHand().isSoft() ? "Soft " : "") + game.getPlayerHand().getScore() + ")", game.getPlayerHand().toString());

                // End game if player busts
                if (game.getPlayerHand().getScore() >= 21) {
                    endGame = true;
                }

                // Resend the embed if the game is still in play
                if (!endGame) {
                    message.createUpdater()
                            .setEmbed(eb)
                            .removeAllComponents()
                            .addComponents(
                                    ActionRow.of(Button.primary("hit", "Hit"),
                                            Button.primary("stand", "Stand")))
                            .applyChanges();
                }

                break;

            case "stand":
                // Remove message components
                message.createUpdater()
                        .setEmbed(eb)
                        .removeAllComponents()
                        .applyChanges();
                endGame = true;
                break;
        }

        messageComponentInteraction.acknowledge();

        // Stop code if the game hasn't ended
        if (!endGame) {
            return;
        }

        // Player can no longer make any moves

        // Player busts
        if (game.getPlayerHand().getScore() > 21) {
            eb.setDescription("**You busted! You lose " + game.getBet() + " point" + (game.getBet() != 1 ? "s" : "") + "**");

            eb.removeAllFields();
            eb.addField("Dealer's Hand (" + (game.getDealerHand().isSoft() ? "Soft " : "") + game.getDealerHand().getScore() + ")", game.getDealerHand().toString());
            eb.addField("Your Hand (" + (game.getPlayerHand().isSoft() ? "Soft " : "") + game.getPlayerHand().getScore() + ")", game.getPlayerHand().toString());

            eb.setFooter(game.getUser().getDiscriminatedName() + " lost!", game.getUser().getAvatar());
            message.createUpdater()
                    .setEmbed(eb)
                    .removeAllComponents()
                    .applyChanges();
            game.givePoints(-game.getBet());
            BlackjackCommandHandler.blackjackGames.remove(game.getUser().getId());
            return;
        }

        // Dealer hits until they get 17+
        while (game.getDealerHand().getScore() < 17) {
            game.getDealerHand().add(game.getDeck().deal());
        }

        // Add final score
        eb.removeAllFields();
        eb.addField("Dealer's Hand (" + (game.getDealerHand().isSoft() ? "Soft " : "") + game.getDealerHand().getScore() + ")", game.getDealerHand().toString());
        eb.addField("Your Hand (" + (game.getPlayerHand().isSoft() ? "Soft " : "") + game.getPlayerHand().getScore() + ")", game.getPlayerHand().toString());


        // Dealer busts
        if (game.getDealerHand().getScore() > 21) {
            eb.setDescription("**Dealer busted! You win " + game.getBet() + " point" + (game.getBet() != 1 ? "s" : "") + "**");
            eb.setFooter(game.getUser().getDiscriminatedName() + " won!", game.getUser().getAvatar());

            message.createUpdater()
                    .setEmbed(eb)
                    .removeAllComponents()
                    .applyChanges();
            game.givePoints(game.getBet());
            BlackjackCommandHandler.blackjackGames.remove(game.getUser().getId());
            return;
        }

        // Dealer wins
        if (game.getDealerHand().getScore() > game.getPlayerHand().getScore()) {
            eb.setDescription("**The dealer beat you! You lose " + game.getBet() + " point" + (game.getBet() != 1 ? "s" : "") + "**");
            eb.setFooter(game.getUser().getDiscriminatedName() + " lost!", game.getUser().getAvatar());
            game.givePoints(-game.getBet());
        // Player wins
        } else if (game.getPlayerHand().getScore() > game.getDealerHand().getScore()) {
            eb.setDescription("**You beat the dealer! You win " + game.getBet() + " point" + (game.getBet() != 1 ? "s" : "") + "**");
            eb.setFooter(game.getUser().getDiscriminatedName() + " won!", game.getUser().getAvatar());
            game.givePoints(game.getBet());
        // Tie
        } else {
            eb.setDescription("**You and the dealer tied! You don't win or lose any points**");
            eb.setFooter(game.getUser().getDiscriminatedName() + " tied!", game.getUser().getAvatar());
        }
        message.createUpdater()
                .setEmbed(eb)
                .removeAllComponents()
                .applyChanges();
        BlackjackCommandHandler.blackjackGames.remove(game.getUser().getId());
    }
}
