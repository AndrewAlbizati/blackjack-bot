package com.github.AndrewAlbizati;

public class Card {
    private final int value;
    public int getValue() {
        return value;
    }
    private final Suit suit;
    public Suit getSuit() {
        return suit;
    }


    public Card(int value, Suit suit) {
        this.value = value;
        this.suit = suit;
    }

    /**
     * Converts the card class instance into a human-readable name.
     *
     * @return The name of the card.
     */
    public String getName() {
        String num = switch (value) {
            case 1 -> "Ace";
            case 11 -> "Jack";
            case 12 -> "Queen";
            case 13 -> "King";
            default -> String.valueOf(value);
        };

        return num + " of " + suit.name().substring(0, 1).toUpperCase() + suit.name().substring(1).toLowerCase() + "s";
    }
}
