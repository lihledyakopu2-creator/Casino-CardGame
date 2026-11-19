/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.mycompany.casino;

/**
 *
 * @author temba
 */
public class CardTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
       Card card1 = new Card(1, "Spades");
        Card card2 = new Card(10, "Diamonds");
        Card card3 = new Card(5, "Hearts");

        System.out.println(card1);
        System.out.println(card2);
        System.out.println(card3);
    }
    
}
