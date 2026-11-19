/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.casino;


import java.util.ArrayList;

public class Build {
    
    
    private int targetValue;
    private boolean ownedByPlayer;
    private ArrayList<Card> cards = new ArrayList<>();

    public Build(int targetValue, boolean ownedByPlayer,
                 ArrayList<Card> cards) {
        this.targetValue = targetValue;
        this.ownedByPlayer = ownedByPlayer;
        this.cards.addAll(cards);
    }

    public int getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public boolean isOwnedByPlayer() {
        return ownedByPlayer;
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public void addCard(Card card) {
        cards.add(card);
    }

    public void addCards(ArrayList<Card> newCards) {
        cards.addAll(newCards);
    }

    @Override
    public String toString() {
        return "Build of " + targetValue
                + " (" + cards.size() + " cards)";
    }
}
