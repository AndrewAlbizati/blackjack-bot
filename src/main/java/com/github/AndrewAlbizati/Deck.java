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
     * Prints out each of the cards in the deck to the console.
     */
    public void printDeck() {
        for (int i = 0; i < super.size(); i++) {
            Card c = super.get(i);
            System.out.println(c.getName());
        }
    }

    /**
     * Sorts the deck based on value, ignoring suits.
     */
    public void sortDeck() {
        Object[] cards = this.toArray();
        boolean sorted = false;
        Card temp;

        while(!sorted) {
            sorted = true;
            for (int i = 0; i < cards.length - 1; i++) {
                Card c = (Card) cards[i];
                Card nextC = (Card) cards[i + 1];
                if (compare(c, nextC) == cards[i]) {
                    temp = c;
                    cards[i] = cards[i+1];
                    cards[i+1] = temp;
                    sorted = false;
                }
            }
        }
        clear();
        for (int i = 0; i < cards.length; i++) {
            add((Card) cards[i]);
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
}
