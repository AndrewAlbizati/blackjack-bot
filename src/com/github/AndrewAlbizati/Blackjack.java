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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Blackjack implements CommandExecutor {
    private HashMap<String, KnownCustomEmoji> emojiHashMap = getEmojisMap();
    public Blackjack() {

    }

    @Command(aliases = {"!blackjack", "!bj"}, async = true, description = "Plays a game of Blackjack.")
    public void onCommand(Server server, TextChannel channel, Message message) throws IOException {
        Deck deck = new Deck(52);
        deck.shuffleDeck();
        Deck[] hands = deck.deal(2, 2);
        Deck dealerHand = hands[0];
        Deck player1Hand = hands[1];
        EmbedBuilder eb = new EmbedBuilder().setTitle("Blackjack");
        eb.addField("Dealer", emojiHashMap.get(dealerHand.get(0).getId()).getMentionTag());

        StringBuilder handEmojis = new StringBuilder();
        for (Card c : player1Hand) {
            handEmojis.append(emojiHashMap.get(c.getId()).getMentionTag());
        }

        String title = "Player 1 (" + calculateScore(player1Hand).toString().replaceAll("\\[", "").replaceAll("\\]", "");
        eb.addField(title, handEmojis.toString());
        channel.sendMessage(eb);
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
        d.sortDeckWithSuits();
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
            }
        }
        if (score.isEmpty()) {
            score.add(scoreWithoutAce);
        }

        return score;
    }
}
