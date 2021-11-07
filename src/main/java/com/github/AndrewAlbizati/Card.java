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
        String num;

        switch(value) {
            case 1:
                num = "Ace";
                break;
            case 11:
                num = "Jack";
                break;
            case 12:
                num = "Queen";
                break;
            case 13:
                num = "King";
                break;
            default:
                num = String.valueOf(value);
        }


        return num + " of " + capitalize(suit.name()) + "s";
    }

    /**
     * Converts the card class instance into a unique id.
     * 5 of Spades = 5s
     * King of Clubs = 13c
     *
     * @return The id of the card.
     */
    public String getId() {
        return value + suit.name().substring(0, 1).toLowerCase();
    }

    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
