/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.mycompany.casino;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public class GameLogic {
    private ArrayList<Card> deck = new ArrayList<>();
    private ArrayList<Card> playerHand = new ArrayList<>();
    private ArrayList<Card> computerHand = new ArrayList<>();
    private ArrayList<Card> tableCards = new ArrayList<>();
    private ArrayList<Build> builds = new ArrayList<>();
    private ArrayList<Card> playerCaptured = new ArrayList<>();
    private ArrayList<Card> computerCaptured = new ArrayList<>();
    // The target card for a build stays in its owner's hand and cannot be placed
    // on the table while that build exists.
    private Map<Build, Card> reservedTargetCards = new IdentityHashMap<>();
    // Once any cards have been added to an existing build, its target cannot be raised.
    private Map<Build, Boolean> buildHasBeenExtended = new IdentityHashMap<>();

    private int round = 1;
    private boolean playerTurn = true;
    private boolean gameOver = false;
    private Boolean lastCaptureByPlayer = null;

    public GameLogic() {
        createDeck();
        Collections.shuffle(deck);
        dealCards(10);
    }

    private void createDeck() {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        for (int i = 0; i < suits.length; i++) {
            for (int value = 1; value <= 10; value++) {
                deck.add(new Card(value, suits[i]));
            }
        }
    }

    private void dealCards(int number) {
        for (int i = 0; i < number && !deck.isEmpty(); i++) {
            playerHand.add(deck.remove(0));
            computerHand.add(deck.remove(0));
        }
    }

    private ArrayList<Card> getHand(boolean isPlayer) {
        return isPlayer ? playerHand : computerHand;
    }

    private ArrayList<Card> getCaptured(boolean isPlayer) {
        return isPlayer ? playerCaptured : computerCaptured;
    }

    private boolean validTurn(boolean isPlayer) {
        return !gameOver && playerTurn == isPlayer;
    }

    private boolean hasBuild(boolean isPlayer) {
        for (Build build : builds) {
            if (build.isOwnedByPlayer() == isPlayer) {
                return true;
            }
        }
        return false;
    }

    private int findOwnBuild(boolean isPlayer) {
        for (int i = 0; i < builds.size(); i++) {
            if (builds.get(i).isOwnedByPlayer() == isPlayer) {
                return i;
            }
        }
        return -1;
    }

    // In round 1, a player with their own build cannot place
    // a card freely on the table.
    public boolean placeCard(int handIndex, boolean isPlayer) {
        if (!validTurn(isPlayer)) return false;
        if (round == 1 && hasBuild(isPlayer)) return false;

        ArrayList<Card> hand = getHand(isPlayer);
        if (handIndex < 0 || handIndex >= hand.size()) return false;

        Card cardToPlace = hand.get(handIndex);
        for (Build build : builds) {
            if (build.isOwnedByPlayer() == isPlayer
                    && reservedTargetCards.get(build) == cardToPlace) {
                return false;
            }
        }

        tableCards.add(hand.remove(handIndex));
        endTurn();
        return true;
    }

    // Timeout-only move: automatically place a non-reserved hand card on the table.
    // This is used only when the player's 30-second timer expires. It intentionally
    // bypasses the round-one own-build restriction so the timed turn cannot stall.
    public boolean placeCardAfterTimeout(int handIndex, boolean isPlayer) {
        if (!validTurn(isPlayer)) return false;

        ArrayList<Card> hand = getHand(isPlayer);
        if (handIndex < 0 || handIndex >= hand.size()) return false;

        Card cardToPlace = hand.get(handIndex);
        for (Build build : builds) {
            if (build.isOwnedByPlayer() == isPlayer
                    && reservedTargetCards.get(build) == cardToPlace) {
                return false;
            }
        }

        tableCards.add(hand.remove(handIndex));
        endTurn();
        return true;
    }

    // Capture a non-empty combination of table cards matching
    // the value of the card played from the hand.
    public boolean captureTableCombination(
            int handIndex, ArrayList<Integer> tableIndexes,
            boolean isPlayer) {

        if (!validTurn(isPlayer) || tableIndexes == null) return false;

        ArrayList<Card> hand = getHand(isPlayer);
        if (handIndex < 0 || handIndex >= hand.size()) return false;

        ArrayList<Integer> indexes = new ArrayList<>(tableIndexes);
        Collections.sort(indexes);
        int sum = 0;

        for (int i = 0; i < indexes.size(); i++) {
            int index = indexes.get(i);
            if (index < 0 || index >= tableCards.size()) return false;
            if (i > 0 && indexes.get(i - 1).equals(index)) return false;
            sum += tableCards.get(index).getValue();
        }

        Card playedCard = hand.get(handIndex);
        if (sum != playedCard.getValue()) return false;

        hand.remove(handIndex);
        ArrayList<Card> captured = getCaptured(isPlayer);

        // Put the captured table cards in the pile first, then the hand card.
        // The hand card was the last card played, so it appears on top.
        for (int i = indexes.size() - 1; i >= 0; i--) {
            int tableIndex = indexes.get(i);
            Card card = tableCards.remove(tableIndex);
            captured.add(card);
        }
        captured.add(playedCard);

        lastCaptureByPlayer = isPlayer;
        endTurn();
        return true;
    }

    // Create a build using only table cards whose total equals a target card
    // held in the player's hand. The target stays in hand, so this does not end
    // the turn because no card has been played from the hand.
    public boolean makeBuildFromTable(
            int targetHandIndex, ArrayList<Integer> tableIndexes, boolean isPlayer) {

        if (!validTurn(isPlayer) || tableIndexes == null || tableIndexes.isEmpty()
                || hasBuild(isPlayer)) return false;

        ArrayList<Card> hand = getHand(isPlayer);
        if (targetHandIndex < 0 || targetHandIndex >= hand.size()) return false;

        Card targetCard = hand.get(targetHandIndex);
        int targetValue = targetCard.getValue();
        ArrayList<Integer> indexes = new ArrayList<>(tableIndexes);
        Collections.sort(indexes);
        int sum = 0;

        for (int i = 0; i < indexes.size(); i++) {
            int index = indexes.get(i);
            if (index < 0 || index >= tableCards.size()) return false;
            if (i > 0 && indexes.get(i - 1).equals(index)) return false;
            sum += tableCards.get(index).getValue();
        }
        if (sum != targetValue) return false;

        ArrayList<Card> buildCards = new ArrayList<>();
        for (int i = indexes.size() - 1; i >= 0; i--) {
            buildCards.add(tableCards.remove((int) indexes.get(i)));
        }

        Build newBuild = new Build(targetValue, isPlayer, buildCards);
        builds.add(newBuild);
        reservedTargetCards.put(newBuild, targetCard);
        buildHasBeenExtended.put(newBuild, false);
        return true;
    }

    // A target-value card must stay in the player's hand.
    // A separate hand card and table cards form that target.
    public boolean makeBuild(
            int targetHandIndex, int addHandIndex,
            ArrayList<Integer> tableIndexes, boolean isPlayer) {

        if (!validTurn(isPlayer) || tableIndexes == null) return false;

        ArrayList<Card> hand = getHand(isPlayer);
        if (targetHandIndex < 0 || targetHandIndex >= hand.size()
                || addHandIndex < 0 || addHandIndex >= hand.size()
                || targetHandIndex == addHandIndex) return false;

        Card targetCard = hand.get(targetHandIndex);
        Card addCard = hand.get(addHandIndex);
        int targetValue = targetCard.getValue();
        int sum = addCard.getValue();

        ArrayList<Integer> indexes = new ArrayList<>(tableIndexes);
        Collections.sort(indexes);

        // A new build must include at least one card already on the table.
        if (indexes.isEmpty()) return false;

        for (int i = 0; i < indexes.size(); i++) {
            int index = indexes.get(i);
            if (index < 0 || index >= tableCards.size()) return false;
            if (i > 0 && indexes.get(i - 1).equals(index)) return false;
            sum += tableCards.get(index).getValue();
        }

        if (sum != targetValue || hand.size() < 2) return false;

        ArrayList<Card> buildCards = new ArrayList<>();
        buildCards.add(addCard);

        for (int i = indexes.size() - 1; i >= 0; i--) {
            int tableIndex = indexes.get(i);
            Card card = tableCards.remove(tableIndex);
            buildCards.add(card);
        }

        // Remove the added card only. Keep the target card.
        hand.remove(addHandIndex);
        Build newBuild = new Build(targetValue, isPlayer, buildCards);
        builds.add(newBuild);
        reservedTargetCards.put(newBuild, targetCard);
        buildHasBeenExtended.put(newBuild, false);

        // The target card remains in hand and cannot be placed on the table.
        // Using the separate added card from the hand ends the turn.
        endTurn();
        return true;
    }

    // Add another group to your own build. The target value stays the same.
    // Keep a matching target card in hand for capturing the build later.
    public boolean extendBuild(
            int targetHandIndex, int addHandIndex,
            ArrayList<Integer> tableIndexes, boolean isPlayer) {

        if (!validTurn(isPlayer) || tableIndexes == null) return false;

        int buildIndex = findOwnBuild(isPlayer);
        if (buildIndex == -1) return false;

        ArrayList<Card> hand = getHand(isPlayer);
        if (targetHandIndex < 0 || targetHandIndex >= hand.size()
                || addHandIndex < 0 || addHandIndex >= hand.size()
                || targetHandIndex == addHandIndex) return false;

        Build build = builds.get(buildIndex);
        Card targetCard = hand.get(targetHandIndex);
        Card addCard = hand.get(addHandIndex);

        if (targetCard.getValue() != build.getTargetValue()) return false;

        int sum = addCard.getValue();
        ArrayList<Integer> indexes = new ArrayList<>(tableIndexes);
        Collections.sort(indexes);

        for (int i = 0; i < indexes.size(); i++) {
            int index = indexes.get(i);
            if (index < 0 || index >= tableCards.size()) return false;
            if (i > 0 && indexes.get(i - 1).equals(index)) return false;
            sum += tableCards.get(index).getValue();
        }

        if (sum != build.getTargetValue()) return false;

        Card addedCard = hand.remove(addHandIndex);
        build.addCard(addedCard);

        for (int i = indexes.size() - 1; i >= 0; i--) {
            int tableIndex = indexes.get(i);
            Card card = tableCards.remove(tableIndex);
            build.addCard(card);
        }

        // The build remains at the same value and can be extended repeatedly.
        // Once extended, it can no longer be raised to a different target.
        buildHasBeenExtended.put(build, true);
        endTurn();
        return true;
    }

    // Add ordinary table cards and optionally the opponent's top captured card
    // to an existing build. This action does not use a hand card, so the turn continues.
    public boolean addTableCardsToBuild(
            int buildIndex, ArrayList<Integer> tableIndexes,
            boolean useOpponentCapturedTop, boolean isPlayer) {

        if (!validTurn(isPlayer) || tableIndexes == null
                || buildIndex < 0 || buildIndex >= builds.size()) return false;

        Build build = builds.get(buildIndex);
        if (build.isOwnedByPlayer() != isPlayer) return false;

        ArrayList<Card> opponentCaptured = getCaptured(!isPlayer);
        if (useOpponentCapturedTop && opponentCaptured.isEmpty()) return false;

        ArrayList<Integer> indexes = new ArrayList<>(tableIndexes);
        Collections.sort(indexes);
        int sum = useOpponentCapturedTop
                ? opponentCaptured.get(opponentCaptured.size() - 1).getValue() : 0;

        for (int i = 0; i < indexes.size(); i++) {
            int index = indexes.get(i);
            if (index < 0 || index >= tableCards.size()) return false;
            if (i > 0 && indexes.get(i - 1).equals(index)) return false;
            sum += tableCards.get(index).getValue();
        }

        if (indexes.isEmpty() && !useOpponentCapturedTop) return false;
        if (sum != build.getTargetValue()) return false;

        if (useOpponentCapturedTop) {
            Card stolenCard = opponentCaptured.remove(opponentCaptured.size() - 1);
            build.addCard(stolenCard);
        }

        for (int i = indexes.size() - 1; i >= 0; i--) {
            Card card = tableCards.remove((int) indexes.get(i));
            build.addCard(card);
        }
        buildHasBeenExtended.put(build, true);

        // No hand card was played, so the same player keeps their turn.
        return true;
    }

    // Add an optional card from the player's hand, table cards, and optionally
    // the opponent's top captured card to the player's existing build. Their
    // values must add up to the build target. Using a hand card ends the turn.
    public boolean addCardsToBuildWithHandCard(
            int buildIndex, int handIndex, ArrayList<Integer> tableIndexes,
            boolean useOpponentCapturedTop, boolean isPlayer) {

        if (!validTurn(isPlayer) || tableIndexes == null
                || buildIndex < 0 || buildIndex >= builds.size()) return false;
        Build build = builds.get(buildIndex);
        if (build.isOwnedByPlayer() != isPlayer) return false;

        ArrayList<Card> hand = getHand(isPlayer);
        if (handIndex < 0 || handIndex >= hand.size()) return false;
        Card handCard = hand.get(handIndex);
        if (reservedTargetCards.get(build) == handCard) return false;

        ArrayList<Card> opponentCaptured = getCaptured(!isPlayer);
        if (useOpponentCapturedTop && opponentCaptured.isEmpty()) return false;

        ArrayList<Integer> indexes = new ArrayList<>(tableIndexes);
        Collections.sort(indexes);
        int sum = handCard.getValue();
        if (useOpponentCapturedTop) {
            sum += opponentCaptured.get(opponentCaptured.size() - 1).getValue();
        }
        for (int i = 0; i < indexes.size(); i++) {
            int index = indexes.get(i);
            if (index < 0 || index >= tableCards.size()) return false;
            if (i > 0 && indexes.get(i - 1).equals(index)) return false;
            sum += tableCards.get(index).getValue();
        }
        if (sum != build.getTargetValue()) return false;

        // Remove the hand card and include it in the build.
        hand.remove(handIndex);
        build.addCard(handCard);
        if (useOpponentCapturedTop) {
            build.addCard(opponentCaptured.remove(opponentCaptured.size() - 1));
        }
        for (int i = indexes.size() - 1; i >= 0; i--) {
            build.addCard(tableCards.remove((int) indexes.get(i)));
        }
        buildHasBeenExtended.put(build, true);
        endTurn();
        return true;
    }

    // Take over an opponent's build by raising its target to a higher card in hand.
    // The player may use table cards, the opponent's captured top card (if allowed),
    // and optionally a separate card from their hand. The target card stays in hand.
    // If a separate hand card is used, the turn ends; otherwise it continues.
    public boolean takeOverBuildWithTableCards(
            int buildIndex, int targetHandIndex, ArrayList<Integer> tableIndexes,
            boolean useOpponentCapturedTop, boolean isPlayer) {
        return takeOverBuildWithCards(buildIndex, targetHandIndex, -1,
                tableIndexes, useOpponentCapturedTop, isPlayer);
    }

    public boolean takeOverBuildWithCards(
            int buildIndex, int targetHandIndex, int addHandIndex,
            ArrayList<Integer> tableIndexes, boolean useOpponentCapturedTop,
            boolean isPlayer) {

        if (!validTurn(isPlayer) || tableIndexes == null
                || buildIndex < 0 || buildIndex >= builds.size()) return false;

        ArrayList<Card> hand = getHand(isPlayer);
        if (targetHandIndex < 0 || targetHandIndex >= hand.size()) return false;
        if (addHandIndex >= hand.size() || addHandIndex < -1
                || addHandIndex == targetHandIndex) return false;

        Build oldBuild = builds.get(buildIndex);
        if (oldBuild.isOwnedByPlayer() == isPlayer) return false;
        // A build may only be raised to a higher target before anyone extends it.
        if (Boolean.TRUE.equals(buildHasBeenExtended.get(oldBuild))) return false;

        Card targetCard = hand.get(targetHandIndex);
        Card addedHandCard = addHandIndex == -1 ? null : hand.get(addHandIndex);
        int newTarget = targetCard.getValue();
        int oldTarget = oldBuild.getTargetValue();
        if (newTarget <= oldTarget) return false;

        ArrayList<Card> opponentCaptured = getCaptured(!isPlayer);
        if (useOpponentCapturedTop
                && (!hasBuild(isPlayer) || opponentCaptured.isEmpty())) return false;

        ArrayList<Integer> indexes = new ArrayList<>(tableIndexes);
        Collections.sort(indexes);
        int addedValue = addedHandCard == null ? 0 : addedHandCard.getValue();
        if (useOpponentCapturedTop) {
            addedValue += opponentCaptured.get(opponentCaptured.size() - 1).getValue();
        }

        for (int i = 0; i < indexes.size(); i++) {
            int index = indexes.get(i);
            if (index < 0 || index >= tableCards.size()) return false;
            if (i > 0 && indexes.get(i - 1).equals(index)) return false;
            addedValue += tableCards.get(index).getValue();
        }

        if (indexes.isEmpty() && !useOpponentCapturedTop && addedHandCard == null) return false;
        if (oldTarget + addedValue != newTarget) return false;

        ArrayList<Card> newBuildCards = new ArrayList<>(oldBuild.getCards());
        if (addedHandCard != null) newBuildCards.add(addedHandCard);
        if (useOpponentCapturedTop) {
            newBuildCards.add(opponentCaptured.remove(opponentCaptured.size() - 1));
        }
        for (int i = indexes.size() - 1; i >= 0; i--) {
            newBuildCards.add(tableCards.remove((int) indexes.get(i)));
        }

        // Remove only the additional hand card; the higher target stays in hand.
        if (addedHandCard != null) hand.remove(addedHandCard);

        Build newBuild = new Build(newTarget, isPlayer, newBuildCards);
        builds.set(buildIndex, newBuild);
        reservedTargetCards.remove(oldBuild);
        buildHasBeenExtended.remove(oldBuild);
        reservedTargetCards.put(newBuild, targetCard);
        buildHasBeenExtended.put(newBuild, false);

        // If the takeover creates a second build with the same target value
        // for this player, combine both sets of cards into one build.
        // This works for every target value (Ace through 10), not only 10.
        mergeSameValueBuilds(newBuild);

        if (addedHandCard != null) {
            endTurn();
        }
        return true;
    }

    // Combines builds owned by the same player when their target values match.
    // The existing build is kept; cards from the other build are added to it.
    private void mergeSameValueBuilds(Build changedBuild) {
        int changedIndex = builds.indexOf(changedBuild);
        if (changedIndex < 0) {
            return;
        }

        for (int i = 0; i < builds.size(); i++) {
            if (i == changedIndex) {
                continue;
            }

            Build otherBuild = builds.get(i);

            if (otherBuild.isOwnedByPlayer() == changedBuild.isOwnedByPlayer()
                    && otherBuild.getTargetValue() == changedBuild.getTargetValue()) {

                // Keep the other build and move all cards from the changed build
                // into it. Its existing target card remains reserved.
                for (Card card : changedBuild.getCards()) {
                    otherBuild.addCard(card);
                }

                boolean wasExtended =
                        Boolean.TRUE.equals(buildHasBeenExtended.get(otherBuild))
                        || Boolean.TRUE.equals(buildHasBeenExtended.get(changedBuild));

                reservedTargetCards.remove(changedBuild);
                buildHasBeenExtended.remove(changedBuild);
                builds.remove(changedBuild);

                buildHasBeenExtended.put(otherBuild, wasExtended);
                return;
            }
        }
    }

    // Use the opponent's top captured card to capture this player's own build.
    // This is allowed only when the player already has a build.
    public boolean captureOwnBuildWithOpponentTop(int buildIndex, boolean isPlayer) {
        if (!validTurn(isPlayer) || !hasBuild(isPlayer)
                || buildIndex < 0 || buildIndex >= builds.size()) return false;

        ArrayList<Card> opponentCaptured = getCaptured(!isPlayer);
        if (opponentCaptured.isEmpty()) return false;

        Build build = builds.get(buildIndex);
        if (build.isOwnedByPlayer() != isPlayer) return false;

        Card topCard = opponentCaptured.get(opponentCaptured.size() - 1);
        if (topCard.getValue() != build.getTargetValue()) return false;

        opponentCaptured.remove(opponentCaptured.size() - 1);
        ArrayList<Card> captured = getCaptured(isPlayer);
        captured.addAll(build.getCards());
        // The opponent's captured-pile card was the last card used.
        captured.add(topCard);
        reservedTargetCards.remove(build);
        buildHasBeenExtended.remove(build);
        builds.remove(buildIndex);

        // No card was played from hand; keep the current turn.
        lastCaptureByPlayer = isPlayer;
        return true;
    }

    // Capture the whole build with a card matching its target.
    public boolean captureBuild(
            int handIndex, int buildIndex, boolean isPlayer) {

        if (!validTurn(isPlayer)) return false;

        ArrayList<Card> hand = getHand(isPlayer);
        if (handIndex < 0 || handIndex >= hand.size()
                || buildIndex < 0 || buildIndex >= builds.size()) {
            return false;
        }

        Card playedCard = hand.get(handIndex);
        Build build = builds.get(buildIndex);
        if (playedCard.getValue() != build.getTargetValue()) return false;

        hand.remove(handIndex);
        ArrayList<Card> captured = getCaptured(isPlayer);
        captured.addAll(build.getCards());
        // The card played from the hand was used last, so it is the pile's top card.
        captured.add(playedCard);
        reservedTargetCards.remove(build);
        buildHasBeenExtended.remove(build);
        builds.remove(buildIndex);

        lastCaptureByPlayer = isPlayer;
        endTurn();
        return true;
    }

    private void endTurn() {
        playerTurn = !playerTurn;
        checkRoundEnd();
    }

    private void checkRoundEnd() {
        if (!playerHand.isEmpty() || !computerHand.isEmpty()) return;

        if (round == 1) {
            round = 2;
            dealCards(10);
        } else {
            finishGame();
        }
    }

    private void finishGame() {
        if (lastCaptureByPlayer != null) {
            ArrayList<Card> captured = getCaptured(lastCaptureByPlayer);
            captured.addAll(tableCards);
            tableCards.clear();

            for (Build build : builds) {
                captured.addAll(build.getCards());
            }
            builds.clear();
            reservedTargetCards.clear();
        }
        gameOver = true;
    }

    // Every successful action that uses a card from the hand ends the turn,
    // including creating or extending a build.
    public void computerPlay() {
        if (gameOver || playerTurn || computerHand.isEmpty()) return;

        while (!gameOver && !playerTurn && !computerHand.isEmpty()) {
            boolean madeAction = false;

            // 1. Capture a build when a card in the computer's hand matches it.
            for (int i = 0; i < computerHand.size(); i++) {
                boolean capturedBuild = false;
                for (int j = 0; j < builds.size(); j++) {
                    if (computerHand.get(i).getValue()
                            == builds.get(j).getTargetValue()) {
                        if (captureBuild(i, j, false)) {
                            return; // Capturing ends the turn.
                        }
                    }
                }
                if (capturedBuild) break;
            }

            // 2. Capture a combination of ordinary table cards.
            for (int i = 0; i < computerHand.size(); i++) {
                ArrayList<Integer> combination = new ArrayList<>();
                if (findCombination(0, computerHand.get(i).getValue(),
                        combination, new ArrayList<Integer>())
                        && !combination.isEmpty()) {
                    if (captureTableCombination(i, combination, false)) {
                        return; // Capturing ends the turn.
                    }
                }
            }

            // 3. Take over the player's build by adding table cards that raise
            // its target to a card held by the computer. The target stays in hand.
            // This action does not use a hand card, so the computer continues its turn.
            for (int targetIndex = 0; targetIndex < computerHand.size() && !madeAction; targetIndex++) {
                int newTarget = computerHand.get(targetIndex).getValue();
                for (int buildIndex = 0; buildIndex < builds.size() && !madeAction; buildIndex++) {
                    Build opponentBuild = builds.get(buildIndex);
                    if (opponentBuild.isOwnedByPlayer() == false
                            || newTarget <= opponentBuild.getTargetValue()) {
                        continue;
                    }
                    int needed = newTarget - opponentBuild.getTargetValue();
                    ArrayList<Integer> combination = new ArrayList<>();
                    if (findCombination(0, needed, combination, new ArrayList<Integer>())
                            && !combination.isEmpty()) {
                        if (takeOverBuildWithTableCards(buildIndex, targetIndex,
                                combination, false, false)) {
                            madeAction = true;
                        }
                    }

                    // The computer may also use a separate card from its hand
                    // to make up the difference, optionally with table cards.
                    for (int addIndex = 0;
                            addIndex < computerHand.size() && !madeAction;
                            addIndex++) {
                        if (addIndex == targetIndex) continue;
                        int remaining = needed - computerHand.get(addIndex).getValue();
                        if (remaining < 0) continue;

                        ArrayList<Integer> addCombination = new ArrayList<>();
                        if (remaining == 0
                                || findCombination(0, remaining, addCombination,
                                        new ArrayList<Integer>())) {
                            if (takeOverBuildWithCards(buildIndex, targetIndex,
                                    addIndex, addCombination, false, false)) {
                                madeAction = true;
                            }
                        }
                    }
                }
            }
            if (madeAction) {
                // Do not end the turn here: no card was played from the hand.
                // Loop again so the computer can continue acting this turn.
                continue;
            }

            // 4. Add cards from the table and/or the player's captured-pile top
            // to the computer's existing build. No hand card is used, so the
            // computer keeps its turn and can make another move.
            int ownBuildIndex = findOwnBuild(false);
            if (ownBuildIndex != -1) {
                int targetValue = builds.get(ownBuildIndex).getTargetValue();
                ArrayList<Integer> tableAddition = new ArrayList<>();
                if (findCombination(0, targetValue, tableAddition, new ArrayList<Integer>())
                        && addTableCardsToBuild(ownBuildIndex, tableAddition, false, false)) {
                    continue;
                }

                ArrayList<Card> playerCapturedPile = getCaptured(true);
                if (!playerCapturedPile.isEmpty()) {
                    int topValue = playerCapturedPile.get(playerCapturedPile.size() - 1).getValue();
                    int neededFromTable = targetValue - topValue;
                    if (neededFromTable == 0) {
                        if (addTableCardsToBuild(ownBuildIndex,
                                new ArrayList<Integer>(), true, false)) {
                            continue;
                        }
                    } else if (neededFromTable > 0) {
                        ArrayList<Integer> topAndTableAddition = new ArrayList<>();
                        if (findCombination(0, neededFromTable, topAndTableAddition,
                                new ArrayList<Integer>())
                                && addTableCardsToBuild(ownBuildIndex,
                                        topAndTableAddition, true, false)) {
                            continue;
                        }
                    }
                }

                // Each hand-card extension group must still total the same target.
                for (int targetIndex = 0;
                        targetIndex < computerHand.size() && !madeAction;
                        targetIndex++) {
                    if (computerHand.get(targetIndex).getValue() != targetValue) {
                        continue;
                    }

                    for (int addIndex = 0;
                            addIndex < computerHand.size() && !madeAction;
                            addIndex++) {
                        if (targetIndex == addIndex) continue;

                        int needed = targetValue - computerHand.get(addIndex).getValue();
                        if (needed < 0) continue;

                        ArrayList<Integer> combination = new ArrayList<>();
                        if (needed == 0) {
                            if (extendBuild(targetIndex, addIndex, combination, false)) {
                                madeAction = true;
                            }
                        } else if (findCombination(0, needed, combination,
                                new ArrayList<Integer>())) {
                            if (extendBuild(targetIndex, addIndex, combination, false)) {
                                madeAction = true;
                            }
                        }
                    }
                }

                if (madeAction) return;

                // During round 1, a player with their own build cannot
                // place a card freely. If no legal action exists, stop.
                if (round == 1) return;
            }

            // 5. Create a build from table cards alone when they add up to a
            // target card in the computer's hand. The target remains in hand,
            // so this action does not end the computer's turn.
            if (!hasBuild(false)) {
                for (int targetIndex = 0; targetIndex < computerHand.size() && !madeAction; targetIndex++) {
                    ArrayList<Integer> tableCombination = new ArrayList<>();
                    if (findCombination(0, computerHand.get(targetIndex).getValue(),
                            tableCombination, new ArrayList<Integer>())) {
                        if (makeBuildFromTable(targetIndex, tableCombination, false)) {
                            madeAction = true;
                        }
                    }
                }
            }
            if (madeAction) continue;

            // 6. Try to create a build. The target card stays in the hand.
            if (computerHand.size() > 1 && !hasBuild(false)) {
                for (int targetIndex = 0;
                        targetIndex < computerHand.size() && !madeAction;
                        targetIndex++) {
                    for (int addIndex = 0;
                            addIndex < computerHand.size() && !madeAction;
                            addIndex++) {
                        if (targetIndex == addIndex) continue;

                        int needed = computerHand.get(targetIndex).getValue()
                                - computerHand.get(addIndex).getValue();

                        if (needed < 0) continue;

                        ArrayList<Integer> combination = new ArrayList<>();
                        if (needed == 0) {
                            if (makeBuild(targetIndex, addIndex, combination, false)) {
                                madeAction = true;
                            }
                        } else if (findCombination(0, needed, combination,
                                new ArrayList<Integer>())) {
                            if (makeBuild(targetIndex, addIndex,
                                    combination, false)) {
                                madeAction = true;
                            }
                        }
                    }
                }
            }

            // Creating a build uses a hand card and ends the computer's turn.
            if (madeAction) return;

            // 6. No capture/build action was found. Place a card to end the turn
            // if the rules allow it.
            if (!(round == 1 && hasBuild(false))) {
                if (placeCard(0, false)) return;
            }

            // No legal move was found. Avoid an endless loop.
            return;
        }
    }

    // Finds a non-empty set of table indexes with the requested total.
    private boolean findCombination(
            int start, int target, ArrayList<Integer> result,
            ArrayList<Integer> chosen) {

        if (target == 0) {
            if (chosen.isEmpty()) return false;
            result.clear();
            result.addAll(chosen);
            return true;
        }

        if (target < 0) return false;

        for (int i = start; i < tableCards.size(); i++) {
            chosen.add(i);
            if (findCombination(i + 1,
                    target - tableCards.get(i).getValue(),
                    result, chosen)) {
                return true;
            }
            chosen.remove(chosen.size() - 1);
        }

        return false;
    }

    public int calculateScore(boolean isPlayer) {
        ArrayList<Card> captured = getCaptured(isPlayer);
        int score = 0;

        for (Card card : captured) {
            // Every Ace is worth 1 point.
            if (card.getValue() == 1) score++;

            // The 10 of Diamonds is worth 2 points.
            if (card.getValue() == 10
                    && card.getSuit().equals("Diamonds")) score += 2;

            // The 2 of Spades is worth 1 additional point on its own.
            if (card.getValue() == 2
                    && card.getSuit().equals("Spades")) score++;
        }

        int playerCards = playerCaptured.size();
        int computerCards = computerCaptured.size();

        if (playerCards > computerCards && isPlayer) {
            score += 2;
        } else if (computerCards > playerCards && !isPlayer) {
            score += 2;
        } else if (playerCards == computerCards) {
            score++;
        }

        int playerSpades = countSpades(playerCaptured);
        int computerSpades = countSpades(computerCaptured);

        if (playerSpades > computerSpades && isPlayer) {
            score += 2;
        } else if (computerSpades > playerSpades && !isPlayer) {
            score += 2;
        } else if (playerSpades == computerSpades) {
            score++;
        }

        return score;
    }

    private int countSpades(ArrayList<Card> cards) {
        int count = 0;
        for (Card card : cards) {
            if (card.getSuit().equals("Spades")) count++;
        }
        return count;
    }

    public int getPlayerScore() {
        return calculateScore(true);
    }

    public int getComputerScore() {
        return calculateScore(false);
    }

    public String getWinner() {
        if (getPlayerScore() > getComputerScore()) return "You win!";
        if (getComputerScore() > getPlayerScore()) return "Computer wins!";
        return "The game is a tie!";
    }

    public boolean hasBuildBeenExtended(int buildIndex) {
        if (buildIndex < 0 || buildIndex >= builds.size()) return false;
        return Boolean.TRUE.equals(buildHasBeenExtended.get(builds.get(buildIndex)));
    }

    public ArrayList<Card> getPlayerHand() {
        return playerHand;
    }

    public ArrayList<Card> getComputerHand() {
        return computerHand;
    }

    public ArrayList<Card> getTableCards() {
        return tableCards;
    }

    public ArrayList<Build> getBuilds() {
        return builds;
    }

    public ArrayList<Card> getPlayerCaptured() {
        return playerCaptured;
    }

    public ArrayList<Card> getComputerCaptured() {
        return computerCaptured;
    }

    public int getComputerHandSize() {
        return computerHand.size();
    }

    public int getRound() {
        return round;
    }

    public boolean isPlayerTurn() {
        return playerTurn;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}