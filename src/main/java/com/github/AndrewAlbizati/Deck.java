package com.github.AndrewAlbizati;

import java.util.ArrayList;
import java.util.Collections;

public class Deck extends ArrayList<Card> {
    public Deck(int amountOfDecks) {
        initializeDeck(amountOfDecks);
    }

    /**
     * Clears the deck and initializes it with new cards.
     * @param amountOfDecks Number of decks of cards to be put together.
     */
    public void initializeDeck(int amountOfDecks) {
        clear();
        for (int deck = 0; deck < amountOfDecks; deck++) {
            // Add one deck
            for (int value = 0; value < 13; value++) {
                for (Suit suit: Suit.values()) {
                    add(new Card(value + 1, suit));
                }
            }
        }
    }

    /**
     * Shuffles the entire deck randomly.
     */
    public void shuffleDeck() {
        Collections.shuffle(this);
    }

    /**
     * Sorts the deck based on value, ignoring suits.
     */
    public void sortDeck() {
        boolean sorted = false;
        while(!sorted) {
            sorted = true;
            for (int i = 0; i < this.size() - 1; i++) {
                Card c = this.get(i);
                Card nextC = this.get(i + 1);
                if (compare(c, nextC) == c) {
                    this.set(i, nextC);
                    this.set(i + 1, c);
                    sorted = false;
                }
            }
        }
    }

    private static Card compare(Card c1, Card c2) {
        if (c1.getValue() > c2.getValue()) {
            return c1;
        } else if (c2.getValue() > c1.getValue()) {
            return c2;
        }
        return null;
    }

    /**
     * Reverses the order of the entire deck.
     */
    public void reverseDeck() {
        Collections.reverse(this);
    }

    /**
     * Checks if a deck is soft (contains an ace valued at 11).
     *
     * @return Whether or not the deck is soft.
     */
    public boolean isSoft() {
        int aceCount = 0;
        for (Card c : this) {
            if (c.getValue() == 1) {
                aceCount++;
            }
        }
        // Hands without aces can't be soft
        if (aceCount == 0) {
            return false;
        }

        int scoreWithoutAce = 0;
        for (Card c : this) {
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

    /**
     * Evaluates the score of the deck.
     * Face cards are worth 10 points, aces are worth 1 or 11 points.
     *
     * @return The Blackjack score of the deck.
     */
    public int getScore() {
        int score = 0;

        Deck d2 = new Deck(0);
        d2.addAll(this);

        d2.sortDeck();
        d2.reverseDeck();

        for (Card c : d2) {
            switch (c.getValue()) {
                case 1 -> {
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
                }
                case 11, 12, 13 -> score += 10;
                default -> score += c.getValue();
            }
        }

        return score;
    }

    /**
     * Converts a deck into a user-friendly string.
     *
     * @return A string that lists the name of each card in the deck.
     */
    public String toString() {
        StringBuilder deckString = new StringBuilder();
        for (Card c : this) {
            deckString.append(c.getName());
            deckString.append("\n");
        }
        return deckString.toString();
    }
}
