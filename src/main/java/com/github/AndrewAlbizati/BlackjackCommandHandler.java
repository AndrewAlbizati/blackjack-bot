package com.github.AndrewAlbizati;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import java.awt.*;
import java.util.HashMap;

public class BlackjackCommandHandler implements SlashCommandCreateListener {
    public static final HashMap<Long, Game> blackjackGames = new HashMap<>();

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        User user = interaction.getUser();

        // Ignore other slash commands
        if (!interaction.getCommandName().equalsIgnoreCase("blackjack")) {
            return;
        }

        if (blackjackGames.containsKey(user.getId())) {
            interaction.createImmediateResponder()
                    .setContent("Please finish your previous Blackjack game before starting a new one.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }

        if (interaction.getOptionLongValueByIndex(0).isEmpty()) {
            interaction.createImmediateResponder()
                    .setContent("Please provide a valid bet.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }


        Game game = new Game(user, interaction.getOptionLongValueByIndex(0).get());

        // Player tried to bet less than one point
        if (game.getBet() < 1) {
            interaction.createImmediateResponder()
                    .setContent("You must bet at least one point.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }

        // Player's bet is too high
        if (game.getPlayerPointAmount() < game.getBet()) {
            interaction.createImmediateResponder()
                    .setContent("Sorry, you need " + (game.getBet() - game.getPlayerPointAmount()) + " more points.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }


        // Create embed with all game information
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Blackjack");
        eb.setDescription("You bet **" + game.getBet() + "** point" + (game.getBet() != 1 ? "s" : "") +"\n" +
                "You have **" + game.getPlayerPointAmount() + "** point" + (game.getPlayerPointAmount() != 1 ? "s" : "") + "\n\n" +
                "**Rules**\n" +
                "Dealer must hit soft 17\n" +
                "Blackjack pays 3 to 2\n" +
                "Splitting is **not** allowed");
        eb.setColor(new Color(184, 0, 9));
        eb.setFooter("Game with " + user.getDiscriminatedName(), user.getAvatar());
        eb.setThumbnail("https://the-datascientist.com/wp-content/uploads/2020/05/counting-cards-black-jack.png");

        // Player is dealt a Blackjack
        if (game.getPlayerHand().getScore() == 21) {
            eb.addField("Dealer's Hand (" + (game.getDealerHand().isSoft() ? "Soft " : "") + game.getDealerHand().getScore() + ")", game.getDealerHand().toString());
            eb.addField("Your Hand (" + (game.getPlayerHand().isSoft() ? "Soft " : "") + game.getPlayerHand().getScore() + ")", game.getPlayerHand().toString());
            eb.setDescription("**You have a blackjack! You win " + (long) Math.ceil(game.getBet() * 1.5) + " points!**");
            eb.setFooter(user.getDiscriminatedName() + " won!", user.getAvatar());

            interaction.createImmediateResponder()
                    .addEmbed(eb)
                    .respond();
            game.givePoints((long) Math.ceil(game.getBet() * 1.5));
            return;
        }

        // Dealer is dealt a Blackjack
        if (game.getDealerHand().get(0).getValue() == 1 && game.getDealerHand().getScore() == 21) {
            eb.addField("Dealer's Hand (" + (game.getDealerHand().isSoft() ? "Soft " : "") + game.getDealerHand().getScore() + ")", game.getDealerHand().toString());
            eb.addField("Your Hand (" + (game.getPlayerHand().isSoft() ? "Soft " : "") + game.getPlayerHand().getScore() + ")", game.getPlayerHand().toString());
            eb.setDescription("**Dealer has a blackjack! You lose " + game.getBet() + " point" + (game.getBet() == 1 ? "" : "s") + "**");
            eb.setFooter(user.getDiscriminatedName() + " lost!", user.getAvatar());

            interaction.createImmediateResponder()
                    .addEmbed(eb)
                    .respond();
            game.givePoints(-game.getBet());
            return;
        }

        // Show the dealer's up card and the players hand
        eb.addField("Dealer's Hand", game.getDealerHand().get(0).getName());
        eb.addField("Your Hand (" + (game.getPlayerHand().isSoft() ? "Soft " : "") + game.getPlayerHand().getScore() + ")", game.getPlayerHand().toString());

        Message message;
        // Add double down option if the player has enough points
        if (game.getBet() * 2 <= game.getPlayerPointAmount()) {
            message = interaction.createImmediateResponder()
                    .addEmbed(eb)
                    .addComponents(
                            ActionRow.of(Button.primary("hit", "Hit"),
                                    Button.primary("stand", "Stand"),
                                    Button.primary("dd", "Double Down")))
                    .respond().join().update().join();
        } else {
            message = interaction.createImmediateResponder()
                    .addEmbed(eb)
                    .addComponents(
                            ActionRow.of(Button.primary("hit", "Hit"),
                                    Button.primary("stand", "Stand")))
                    .respond().join().update().join();
        }

        game.setMessage(message);
        blackjackGames.put(user.getId(), game);
    }
}
