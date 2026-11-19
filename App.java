package com.mycompany.casino;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.animation.TranslateTransition;
import javafx.animation.RotateTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.stage.Modality;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.geometry.HPos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.input.KeyCombination;

public class App extends Application {
    private GameLogic game;
    private Label roundLabel, computerLabel, capturedLabel, messageLabel;
    private Label playerCapturedTop, computerCapturedTop;
    private StackPane playerCapturedCard, computerCapturedCard;
    private FlowPane computerPane, tablePane, buildPane, playerPane;
    private boolean gameOverShown = false;
    private boolean computerAnimating = false;
    private StackPane tableLayer;
    private VBox gameRoot;
    private VBox gameOverPopup;
    private Label gameOverWinnerLabel, gameOverScoreLabel;
    private Label turnTimerLabel;
    private Timeline playerTurnTimer;
    private int secondsRemaining = 30;
    private boolean playerTimerRunning = false;
    private Stage mainStage;
    private Pane dealAnimationLayer;
    private int lastDisplayedRound = 0;
    private boolean dealingCards = false;

    // Click-based controls: select a hand card, then another hand card to build.
    private int selectedHandIndex = -1;
    private int buildTargetHandIndex = -1;
    private int buildAddHandIndex = -1;
    private int selectedBuildIndexForAdd = -1;
    private int takeoverTargetHandIndex = -1;
    private int takeoverAddHandIndex = -1;
    private int ownBuildAddHandIndex = -1;
    private boolean useOpponentCapturedTop = false;
    private final ArrayList<Integer> selectedTableIndexes = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        mainStage = stage;
        // Keep the application full-screen on every screen and prevent Escape
        // from switching it back to a window.
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // Re-apply full-screen whenever any screen/scene is replaced, including
        // the animated shuffle screen and the transition into the game.
        stage.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> keepFullScreen(stage));
            }
        });

        // If JavaFX or the window manager drops full-screen during a transition,
        // request it again while the application is showing.
        stage.fullScreenProperty().addListener((observable, wasFullScreen, isFullScreen) -> {
            if (!isFullScreen && stage.isShowing()) {
                Platform.runLater(() -> {
                    if (stage.isShowing() && !stage.isFullScreen()) {
                        keepFullScreen(stage);
                    }
                });
            }
        });

        showEntrance(stage);
    }

    private void keepFullScreen(Stage stage) {
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        if (!stage.isShowing()) {
            stage.show();
        }
        stage.setFullScreen(true);
    }

    // First screen shown when the application opens.
    private void showEntrance(Stage stage) {
        stopPlayerTimer();
        StackPane background = new StackPane();
        background.setStyle("-fx-background-color: #e8c58e;"
                + "-fx-border-color: #e8c58e; -fx-border-width: 22;");

        // Decorative face-up cards on the felt so both hearts and spades are visible.
        HBox deck = new HBox(-18);
        deck.setAlignment(Pos.CENTER);
        deck.setMinSize(190, 150);
        deck.setPrefSize(190, 150);
        deck.setMaxSize(190, 150);

        StackPane heartCard = entranceCard("10", "♥", "#c62828");
        heartCard.setRotate(-9);
        heartCard.setTranslateY(5);

        StackPane spadeCard = entranceCard("A", "♠", "#171717");
        spadeCard.setRotate(9);
        spadeCard.setTranslateY(-5);

        // A few blue-backed cards behind the two visible face cards suggest a deck.
        StackPane deckBacks = new StackPane();
        deckBacks.setMinSize(104, 146);
        deckBacks.setPrefSize(104, 146);
        deckBacks.setMaxSize(104, 146);
        for (int i = 2; i >= 0; i--) {
            Label back = new Label("♠  ♥\n♥  ♠");
            back.setAlignment(Pos.CENTER);
            back.setMinSize(94, 136);
            back.setPrefSize(94, 136);
            back.setMaxSize(94, 136);
            back.setStyle("-fx-background-color: #173b78; -fx-border-color: #fff4d6;"
                    + "-fx-border-width: 3; -fx-background-radius: 9; -fx-border-radius: 9;"
                    + "-fx-text-fill: #fff4d6; -fx-font-size: 21px; -fx-font-weight: bold;");
            back.setTranslateX(i * 3);
            back.setTranslateY(i * -3);
            deckBacks.getChildren().add(back);
        }
        deck.getChildren().addAll(deckBacks, heartCard, spadeCard);

        Label title = new Label("CASINO  ♠ ♥");
        title.setStyle("-fx-text-fill: #fff2cf; -fx-font-size: 48px;"
                + "-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, black, 7, 0.4, 2, 3);");

        Label subtitle = new Label("A classic card game");
        subtitle.setStyle("-fx-text-fill: white; -fx-font-size: 17px;");

        Button playButton = new Button("PLAY");
        playButton.setPrefWidth(220);
        playButton.setPrefHeight(48);
        playButton.setStyle("-fx-background-color: #d8b56d; -fx-text-fill: #2c1b10;"
                + "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 8;");

        Button detailsButton = new Button("DETAILS");
        detailsButton.setPrefWidth(220);
        detailsButton.setPrefHeight(44);
        detailsButton.setStyle("-fx-background-color: #d8b56d; -fx-text-fill: #2c1b10;"
                + "-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8;"
                + "-fx-border-color: #d8b56d; -fx-border-radius: 8;");
        detailsButton.setOnAction(event -> showGameRules(stage));

        Button quitButton = new Button("QUIT");
        quitButton.setPrefWidth(220);
        quitButton.setPrefHeight(44);
        quitButton.setStyle("-fx-background-color: #d8b56d; -fx-text-fill: #2c1b10;"
                + "-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8;"
                + "-fx-border-color: #d8b56d; -fx-border-radius: 8;");
        quitButton.setOnAction(event -> Platform.exit());

        VBox menu = new VBox(16, deck, title, subtitle, playButton, detailsButton, quitButton);
        menu.setAlignment(Pos.CENTER);
        menu.setMaxWidth(340);
        background.getChildren().add(menu);

        playButton.setOnAction(event -> {
            // Show actual moving cards during the two-second loading period.
            StackPane loadingRoot = new StackPane();
            loadingRoot.setStyle("-fx-background-color: #e8c58e;"
                    + "-fx-border-color: #e8c58e; -fx-border-width: 22;");

            Label loadingText = new Label("SHUFFLING THE CARDS...");
            loadingText.setStyle("-fx-text-fill: #fff2cf; -fx-font-size: 25px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-effect: dropshadow(gaussian, black, 5, 0.3, 1, 2);");
            StackPane.setAlignment(loadingText, Pos.TOP_CENTER);
            StackPane.setMargin(loadingText, new Insets(190, 0, 0, 0));

            Pane movingCards = new Pane();
            movingCards.setMinSize(0, 0);
            movingCards.setPrefSize(1200, 800);
            movingCards.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            ArrayList<TranslateTransition> cardMoves = new ArrayList<>();
            ArrayList<RotateTransition> cardSpins = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                StackPane back = shuffleCard();
                double startX = 600 - 45 + (i - 3) * 12;
                double startY = 320 + (i % 2) * 8;
                back.setLayoutX(startX);
                back.setLayoutY(startY);
                back.setRotate((i - 3) * 3);
                movingCards.getChildren().add(back);

                TranslateTransition move = new TranslateTransition(
                        Duration.millis(260 + (i % 3) * 35), back);
                move.setByX((i % 2 == 0 ? 1 : -1) * (115 + i * 13));
                move.setByY((i % 2 == 0 ? -1 : 1) * (45 + (i % 3) * 24));
                move.setAutoReverse(true);
                move.setCycleCount(TranslateTransition.INDEFINITE);
                move.play();
                cardMoves.add(move);

                RotateTransition spin = new RotateTransition(
                        Duration.millis(420 + (i % 3) * 80), back);
                spin.setByAngle((i % 2 == 0 ? 1 : -1) * (20 + i * 5));
                spin.setAutoReverse(true);
                spin.setCycleCount(RotateTransition.INDEFINITE);
                spin.play();
                cardSpins.add(spin);
            }

            loadingRoot.getChildren().addAll(movingCards, loadingText);
            stage.setScene(new Scene(loadingRoot, 1200, 800));
            // Keep the shuffle/loading screen full-screen too, after changing scenes.
            keepFullScreen(stage);

            PauseTransition wait = new PauseTransition(Duration.seconds(2));
            wait.setOnFinished(done -> {
                for (TranslateTransition move : cardMoves) move.stop();
                for (RotateTransition spin : cardSpins) spin.stop();
                showGameScreen(stage);
            });
            wait.play();
        });

        stage.setTitle("Casino");
        stage.setScene(new Scene(background, 1200, 800));
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        keepFullScreen(stage);
    }

    // Card back used in the animated shuffle screen.
    private StackPane shuffleCard() {
        StackPane card = new StackPane();
        card.setMinSize(90, 130);
        card.setPrefSize(90, 130);
        card.setMaxSize(90, 130);
        card.setStyle("-fx-background-color: #173b78; -fx-border-color: #fff2cf;"
                + "-fx-border-width: 3; -fx-background-radius: 9; -fx-border-radius: 9;"
                + "-fx-effect: dropshadow(gaussian, black, 8, 0.3, 2, 3);");

        Label pattern = new Label("♠  ♥\n♥  ♠\n♠  ♥");
        pattern.setAlignment(Pos.CENTER);
        pattern.setStyle("-fx-text-fill: #fff2cf; -fx-font-size: 23px;"
                + "-fx-font-weight: bold;");
        card.getChildren().add(pattern);
        return card;
    }

    // Entrance-screen face-up playing card. The spade and heart are deliberately visible.
    private StackPane entranceCard(String value, String suit, String colour) {
        StackPane card = new StackPane();
        card.setMinSize(88, 128);
        card.setPrefSize(88, 128);
        card.setMaxSize(88, 128);
        card.setPadding(new Insets(7));
        card.setStyle("-fx-background-color: #fffdf8; -fx-border-color: #242424;"
                + "-fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 8, 0.2, 2, 3);");

        Label corner = new Label(value + "\n" + suit);
        corner.setAlignment(Pos.TOP_LEFT);
        corner.setStyle("-fx-text-fill: " + colour + "; -fx-font-size: 16px;"
                + "-fx-font-weight: bold;");

        Label center = new Label(suit);
        center.setStyle("-fx-text-fill: " + colour + "; -fx-font-size: 54px;"
                + "-fx-font-weight: bold;");

        Label bottom = new Label(value + "\n" + suit);
        bottom.setRotate(180);
        bottom.setAlignment(Pos.BOTTOM_RIGHT);
        bottom.setStyle("-fx-text-fill: " + colour + "; -fx-font-size: 16px;"
                + "-fx-font-weight: bold;");

        StackPane.setAlignment(corner, Pos.TOP_LEFT);
        StackPane.setAlignment(center, Pos.CENTER);
        StackPane.setAlignment(bottom, Pos.BOTTOM_RIGHT);
        card.getChildren().addAll(center, corner, bottom);
        return card;
    }

    // Changes the main window to the details screen; no popup window is opened.
    private void showGameRules(Stage owner) {

        Label title = new Label("♠  CASINO RULES  ♥");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setStyle("-fx-font-family: Georgia; -fx-font-size: 25px;"
                + "-fx-font-weight: bold; -fx-text-fill: #4a2a16;");

        Label subtitle = new Label("Learn how to play the game");
        subtitle.setAlignment(Pos.CENTER);
        subtitle.setMaxWidth(Double.MAX_VALUE);
        subtitle.setStyle("-fx-font-family: Georgia; -fx-font-size: 14px;"
                + "-fx-font-style: italic; -fx-text-fill: #704624;");

        VBox rulesText = new VBox(12);
        rulesText.setPadding(new Insets(12, 18, 12, 18));
        rulesText.setStyle("-fx-background-color: #f2d6a2;"
                + "-fx-border-color: #b8874d; -fx-border-width: 1.5;"
                + "-fx-background-radius: 3; -fx-border-radius: 3;");

        rulesText.getChildren().addAll(
                ruleSection("1. THE DECK", "The game uses 40 cards: Ace through 10 in each of the four suits. An Ace has a value of 1."),
                ruleSection("2. THE DEAL AND ROUNDS", "There are two rounds. At the start of each round, you and your opponent receive 10 cards each. Cards are dealt again for Round 2."),
                ruleSection("3. YOUR TURN", "Click one of your cards and choose an available action. You have 30 seconds to make your move. If time runs out, the game automatically plays a card onto the table."),
                ruleSection("4. CAPTURING CARDS", "You can capture a table card, or a group of table cards, when their value or combined total matches the value of the card you play. Captured cards are added to your captured pile."),
                ruleSection("5. BUILDS", "A build combines cards on the table under a target value. You can create or extend a build when the game allows it. To capture a build, play a card matching its target value. Follow the action prompts shown by the game."),
                ruleSection("6. WHEN YOU CANNOT CAPTURE", "If you do not make a capture or build, play a card onto the table. Cards left on the table may be captured later."),
                ruleSection("7. WHO STARTS ROUND 2?", "The player who did not make the last move in Round 1 starts Round 2. After Round 2, the last player to capture takes any cards and builds remaining on the table."),
                ruleSection("8. SCORING", "• Each captured Ace: 1 point\n• 10 of Diamonds: 2 points\n• 2 of Spades: 1 point\n• Most cards captured: 2 points (1 point each if tied)\n• Most Spades captured: 2 points (1 point each if tied)\nThe point for the 2 of Spades is separate from the bonus for capturing the most Spades."),
                ruleSection("9. HOW TO WIN", "When the game ends, the scores are added up. The player with the highest total score wins.")
        );

        ScrollPane scrollPane = new ScrollPane(rulesText);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportHeight(455);
        scrollPane.setStyle("-fx-background: #e8c58e; -fx-background-color: #e8c58e;"
                + "-fx-border-color: transparent;");

        // The dark brown rounded bars at the top and bottom make the parchment
        // look like an open scroll rather than a plain dialog box.
        Label topRoll = new Label("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        topRoll.setMaxWidth(Double.MAX_VALUE);
        topRoll.setAlignment(Pos.CENTER);
        topRoll.setStyle("-fx-text-fill: #80502b; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label bottomRoll = new Label("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        bottomRoll.setMaxWidth(Double.MAX_VALUE);
        bottomRoll.setAlignment(Pos.CENTER);
        bottomRoll.setStyle("-fx-text-fill: #80502b; -fx-font-size: 15px; -fx-font-weight: bold;");

        Button closeButton = new Button("BACK TO MENU");
        closeButton.setStyle("-fx-background-color: #704624; -fx-text-fill: #fff2cf;"
                + "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 6;"
                + "-fx-padding: 9 22 9 22;");
        closeButton.setOnAction(event -> showEntrance(owner));

        VBox scroll = new VBox(9, topRoll, title, subtitle, scrollPane, bottomRoll, closeButton);
        scroll.setAlignment(Pos.CENTER);
        scroll.setPadding(new Insets(18, 25, 18, 25));
        scroll.setStyle("-fx-background-color: #e8c58e;"
                + "-fx-border-color: #704624; -fx-border-width: 10;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 14, 0.25, 2, 4);");

        owner.setTitle("Casino - Details and Rules");
        owner.setScene(new Scene(scroll, 1200, 800));
        keepFullScreen(owner);
    }

    private VBox ruleSection(String headingText, String bodyText) {
        Label heading = new Label(headingText);
        heading.setStyle("-fx-font-family: Georgia; -fx-font-size: 15px;"
                + "-fx-font-weight: bold; -fx-text-fill: #4a2a16;");
        heading.setWrapText(true);

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.setMaxWidth(Double.MAX_VALUE);
        body.setStyle("-fx-font-family: Georgia; -fx-font-size: 13px;"
                + "-fx-text-fill: #3f2b1c; -fx-line-spacing: 2px;");

        VBox section = new VBox(4, heading, body);
        section.setPadding(new Insets(0, 0, 7, 0));
        return section;
    }

    // Builds and displays the actual game after the two-second loading screen.
    private void showGameScreen(Stage stage) {
        game = new GameLogic();

        roundLabel = new Label();
        computerLabel = new Label();
        capturedLabel = new Label();
        messageLabel = new Label();
        playerCapturedTop = new Label();
        computerCapturedTop = new Label();
        playerCapturedCard = new StackPane();
        computerCapturedCard = new StackPane();

        computerPane = new FlowPane(4, 4);
        tablePane = new FlowPane(4, 4);
        buildPane = new FlowPane(4, 4);
        playerPane = new FlowPane(4, 4);

        computerPane.setAlignment(Pos.CENTER);
        tablePane.setAlignment(Pos.CENTER);
        buildPane.setAlignment(Pos.CENTER);
        playerPane.setAlignment(Pos.CENTER);
        // Keep all ten of the player's cards in one clear, visible row.
        computerPane.setHgap(2);
        computerPane.setVgap(2);
        computerPane.setPrefWrapLength(510);
        playerPane.setHgap(2);
        playerPane.setVgap(2);
        playerPane.setPrefWrapLength(510);
        playerPane.setMinWidth(510);
        playerPane.setPrefWidth(510);
        playerPane.setMaxWidth(510);

        // Reserve the same vertical space for both hands and the table areas.
        // This prevents the playing table from jumping when a new game starts
        // and the number of cards/builds changes.
        computerPane.setMinHeight(92);
        computerPane.setPrefHeight(92);
        computerPane.setMaxHeight(92);
        playerPane.setMinHeight(92);
        playerPane.setPrefHeight(92);
        playerPane.setMaxHeight(92);
        tablePane.setMinHeight(175);
        tablePane.setPrefHeight(175);
        tablePane.setMaxHeight(175);
        buildPane.setMinHeight(105);
        buildPane.setPrefHeight(105);
        buildPane.setMaxHeight(105);

        for (Label label : new Label[]{roundLabel, computerLabel, capturedLabel,
                messageLabel, playerCapturedTop, computerCapturedTop}) {
            label.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");
        }
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

        // Rectangular pool-table style frame. No pockets or holes are drawn;
        // the green felt in the middle is the card-playing area.
        VBox root = new VBox(5);
        gameRoot = root;
        root.setPadding(new Insets(7));
        root.setFillWidth(true);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #e8c58e;");

        VBox tableArea = new VBox(5, tablePane, buildPane);
        tableArea.setAlignment(Pos.CENTER);
        tableArea.setMinWidth(0);
        // Portrait-oriented table: tall and narrower, while the cards themselves
        // remain upright. Keep its dimensions stable when the game restarts.
        tableArea.setMinWidth(520);
        tableArea.setPrefWidth(520);
        tableArea.setMaxWidth(520);
        tableArea.setMinHeight(340);
        tableArea.setPrefHeight(340);
        tableArea.setMaxHeight(340);
        tableArea.setPadding(new Insets(6));
        // Plain rectangular green felt playing surface with a solid wooden rim.
        // There are deliberately no pocket/ hole graphics on the table.
        tableArea.setStyle("-fx-background-color: #90EE90;"
                + "-fx-border-color: #e8c58e; -fx-border-width: 12;"
                + "-fx-border-radius: 0; -fx-background-radius: 0;");

        // Turn timer and a door button are displayed at the top-right during play.
        turnTimerLabel = new Label("Your time: 30s");
        turnTimerLabel.setStyle("-fx-text-fill: #fff2cf; -fx-font-size: 15px; -fx-font-weight: bold;");

        Button doorButton = new Button("➡ 🚪");
        doorButton.setTooltip(new javafx.scene.control.Tooltip("Return to the Casino entrance"));
        doorButton.setFocusTraversable(false);
        doorButton.setStyle("-fx-background-color: #d8b56d; -fx-text-fill: #2c1b10;"
                + "-fx-font-size: 17px; -fx-font-weight: bold; -fx-background-radius: 6;"
                + "-fx-padding: 4 10 4 10;");
        doorButton.setOnAction(event -> {
            stopPlayerTimer();
            showEntrance(mainStage);
        });

        HBox topBar = new HBox(12, roundLabel, computerLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // The table layer lets the final-score panel sit directly over the table.
        tableLayer = new StackPane(tableArea);
        tableLayer.setMinWidth(0);
        tableLayer.setMinHeight(360);
        tableLayer.setPrefHeight(360);
        tableLayer.setMaxHeight(360);
        tableLayer.setMaxWidth(620);
        tableLayer.setAlignment(Pos.CENTER);
        // Keep the smaller table slightly to the right while preserving room for both hands.
        tableLayer.setTranslateX(24);
        VBox.setVgrow(tableLayer, javafx.scene.layout.Priority.NEVER);
        createGameOverPopup();
        tableLayer.getChildren().add(gameOverPopup);
        StackPane.setAlignment(gameOverPopup, Pos.CENTER);

        VBox playerCapturedArea = new VBox(7,
                whiteHeading("YOUR CAPTURED CARDS"), playerCapturedCard, playerCapturedTop);
        playerCapturedArea.setAlignment(Pos.CENTER);
        playerCapturedArea.setMinWidth(90);
        playerCapturedArea.setPrefWidth(90);
        playerCapturedArea.setMaxWidth(90);

        VBox computerCapturedArea = new VBox(7,
                whiteHeading("OPPONENT'S CAPTURED CARDS"), computerCapturedCard, computerCapturedTop);
        computerCapturedArea.setAlignment(Pos.CENTER);
        computerCapturedArea.setMinWidth(90);
        computerCapturedArea.setPrefWidth(90);
        computerCapturedArea.setMaxWidth(90);

        // Opponent's hand and captured pile stay together at the top.
        HBox opponentRow = new HBox(8, computerCapturedArea, computerPane);
        opponentRow.setAlignment(Pos.CENTER);
        opponentRow.setPadding(new Insets(4));
        opponentRow.setTranslateX(-20);
        opponentRow.setStyle("-fx-background-color: #e8c58e; -fx-background-radius: 10;");

        // Your captured pile sits beside your hand at the bottom.
        HBox playerRow = new HBox(8, playerCapturedArea, playerPane);
        playerRow.setAlignment(Pos.CENTER);
        playerRow.setPadding(new Insets(4));
        playerRow.setTranslateX(-20);
        playerRow.setStyle("-fx-background-color: #e8c58e; -fx-background-radius: 10;");

        root.getChildren().addAll(
                topBar,
                whiteHeading("OPPONENT'S HAND (cards hidden)"), opponentRow,
                tableLayer, capturedLabel,
                whiteHeading("YOUR HAND — click a card to choose an action"),
                playerRow, messageLabel
        );

        stage.setTitle("Casino Card Game");

        // A transparent full-window layer lets cards visibly travel from the deck
        // in the middle of the table to both players' hands.
        dealAnimationLayer = new Pane();
        dealAnimationLayer.setPickOnBounds(false);
        HBox doorOverlay = new HBox(doorButton);
        doorOverlay.setAlignment(Pos.TOP_RIGHT);
        doorOverlay.setPadding(new Insets(8, 10, 0, 0));
        doorOverlay.setPickOnBounds(false);

        // Keep the countdown fixed at the bottom-left of the game window,
        // independently of the table layout and full-screen sizing.
        HBox timerOverlay = new HBox(turnTimerLabel);
        timerOverlay.setAlignment(Pos.BOTTOM_LEFT);
        timerOverlay.setPadding(new Insets(0, 0, 12, 14));
        timerOverlay.setPickOnBounds(false);
        turnTimerLabel.setStyle("-fx-background-color: #4b2e18; -fx-background-radius: 6;"
                + "-fx-padding: 7 11 7 11; -fx-text-fill: #fff2cf;"
                + "-fx-font-size: 15px; -fx-font-weight: bold;");

        StackPane screenRoot = new StackPane(root, dealAnimationLayer, doorOverlay, timerOverlay);
        StackPane.setAlignment(doorOverlay, Pos.TOP_RIGHT);
        StackPane.setAlignment(timerOverlay, Pos.BOTTOM_LEFT);
        // Portrait game window; only the table's shape/layout changes.
        // Card faces remain upright and game logic is untouched.
        stage.setScene(new Scene(screenRoot, 1000, 900));
        stage.setMinWidth(850);
        stage.setMinHeight(750);
        lastDisplayedRound = game.getRound();
        refreshScreen();
        keepFullScreen(stage);
        startDealAnimation(game.getRound());
    }

    // Visually deal ten cards to each side, one card at a time. The actual
    // cards are already in GameLogic; this animation shows the dealing action.
    private void startDealAnimation(int roundBeingDealt) {
        if (game == null || dealAnimationLayer == null || dealingCards || game.isGameOver()) {
            return;
        }

        dealingCards = true;
        stopPlayerTimer();
        computerPane.setOpacity(0);
        playerPane.setOpacity(0);
        dealAnimationLayer.getChildren().clear();

        Label dealingLabel = new Label("DEALING CARDS — ROUND " + roundBeingDealt);
        dealingLabel.setStyle("-fx-text-fill: #fff2cf; -fx-font-size: 20px;"
                + "-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, black, 5, 0.3, 1, 2);");
        dealingLabel.setLayoutX(420);
        dealingLabel.setLayoutY(45);
        dealAnimationLayer.getChildren().add(dealingLabel);

        SequentialTransition dealingSequence = new SequentialTransition();
        double centerX = 555;
        double centerY = 335;
        for (int i = 0; i < 20; i++) {
            boolean toComputer = i % 2 == 0;
            int cardNumber = i / 2;
            StackPane cardBack = shuffleCard();
            cardBack.setMinSize(43, 62);
            cardBack.setPrefSize(43, 62);
            cardBack.setMaxSize(43, 62);
            cardBack.setLayoutX(centerX);
            cardBack.setLayoutY(centerY);
            cardBack.setScaleX(0.78);
            cardBack.setScaleY(0.78);
            dealAnimationLayer.getChildren().add(cardBack);

            double targetX = 300 + cardNumber * 58;
            double targetY = toComputer ? 112 : 665;
            TranslateTransition dealMove = new TranslateTransition(Duration.millis(95), cardBack);
            dealMove.setFromX(0);
            dealMove.setFromY(0);
            dealMove.setToX(targetX - centerX);
            dealMove.setToY(targetY - centerY);
            dealMove.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
            dealingSequence.getChildren().add(dealMove);
        }

        dealingSequence.setOnFinished(event -> {
            dealAnimationLayer.getChildren().clear();
            computerPane.setOpacity(1);
            playerPane.setOpacity(1);
            dealingCards = false;
            refreshScreen();
            if (game != null && !game.isGameOver() && !game.isPlayerTurn()) {
                animateComputerMove();
            }
        });
        dealingSequence.play();
    }

    private Label whiteHeading(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
        return label;
    }

    private void refreshScreen() {
        if (game == null) return;
        boolean roundChanged = lastDisplayedRound != 0 && game.getRound() != lastDisplayedRound;
        if (roundChanged) {
            lastDisplayedRound = game.getRound();
        }
        roundLabel.setText("Round: " + game.getRound());
        computerLabel.setText("Bot cards remaining: " + game.getComputerHandSize());

        computerPane.getChildren().clear();
        tablePane.getChildren().clear();
        buildPane.getChildren().clear();
        playerPane.getChildren().clear();

        // Bot cards use the same rectangle size, but only their backs are shown.
        for (int i = 0; i < game.getComputerHandSize(); i++) {
            Button back = new Button("◆\n◆");
            back.setMinSize(48, 68);
            back.setPrefSize(48, 68);
            back.setMaxSize(48, 68);
            back.setFocusTraversable(false);
            back.setMouseTransparent(true);
            back.setStyle(
                    "-fx-background-color: #183a70; -fx-border-color: white;" +
                    "-fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8;" +
                    "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;"
            );
            computerPane.getChildren().add(back);
        }

        for (int i = 0; i < game.getTableCards().size(); i++) {
            Card card = game.getTableCards().get(i);
            StackPane face = cardFace(card);
            final int tableIndex = i;
            face.setOnMouseClicked(event -> {
                if (!game.isPlayerTurn() || game.isGameOver() || computerAnimating) return;
                if (event.getClickCount() >= 2) {
                    handleTableDoubleClick(tableIndex);
                } else {
                    handleTableSingleClick(tableIndex);
                }
            });
            tablePane.getChildren().add(face);
        }

        for (int i = 0; i < game.getBuilds().size(); i++) {
            Build build = game.getBuilds().get(i);

            // Only show the cards, with the target value underneath them.
            Label buildLabel = new Label("Target: " + build.getTargetValue());
            buildLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

            // Show the build as one compact overlapping stack. The newest card
            // added to Build.getCards() is the last child, so it appears on top.
            ArrayList<Card> orderedCards = new ArrayList<>(build.getCards());
            StackPane stackedCards = new StackPane();
            stackedCards.setAlignment(Pos.TOP_LEFT);
            stackedCards.setMinSize(62, 84);
            stackedCards.setPrefSize(62, 84);
            stackedCards.setMaxSize(62, 84);
            for (int cardIndex = 0; cardIndex < orderedCards.size(); cardIndex++) {
                Card buildCard = orderedCards.get(cardIndex);
                StackPane shownCard = cardFace(buildCard);
                shownCard.setMinSize(54, 76);
                shownCard.setPrefSize(54, 76);
                shownCard.setMaxSize(54, 76);
                // Older cards peek out behind the newest card.
                shownCard.setTranslateX(Math.min(cardIndex, 3) * 3);
                shownCard.setTranslateY(Math.min(cardIndex, 3) * 3);
                stackedCards.getChildren().add(shownCard);
            }

            VBox buildBox = new VBox(2, stackedCards, buildLabel);
            buildBox.setAlignment(Pos.TOP_CENTER);
            buildBox.setPadding(new Insets(7));
            buildBox.setStyle("-fx-background-color: rgba(0,0,0,0.12);"
                    + "-fx-border-color: #d7c27a; -fx-border-width: 1.5;"
                    + "-fx-background-radius: 8; -fx-border-radius: 8;");
            final int buildIndex = i;
            buildBox.setOnMouseClicked(event -> {
                if (!game.isPlayerTurn() || game.isGameOver() || computerAnimating) return;
                if (event.getClickCount() >= 2) {
                    if ((selectedHandIndex != -1 || ownBuildAddHandIndex != -1)
                            && buildTargetHandIndex == -1) {
                        handleBuildDoubleClick(buildIndex);
                    } else if (hasOwnBuild() && build.isOwnedByPlayer()
                            && useOpponentCapturedTop) {
                        boolean success = game.captureOwnBuildWithOpponentTop(buildIndex, true);
                        useOpponentCapturedTop = false;
                        selectedBuildIndexForAdd = -1;
                        selectedTableIndexes.clear();
                        if (success) {
                            messageLabel.setText("Captured your build using the opponent's top captured card. Your turn continues.");
                        } else {
                            messageLabel.setText("That captured card does not match your build's target.");
                        }
                        refreshScreen();
                    }
                } else if (!build.isOwnedByPlayer() && selectedHandIndex != -1
                        && buildTargetHandIndex == -1) {
                    int targetValue = game.getPlayerHand().get(selectedHandIndex).getValue();
                    if (targetValue <= build.getTargetValue()) {
                        messageLabel.setText("To take over this build, select a hand card higher than its current target.");
                    } else {
                        selectedBuildIndexForAdd = buildIndex;
                        takeoverTargetHandIndex = selectedHandIndex;
                        takeoverAddHandIndex = -1;
                        ownBuildAddHandIndex = -1;
                        selectedTableIndexes.clear();
                        messageLabel.setText("Takeover selected. You may click another card in your hand to add it, then click table cards to make up the remaining value. Your target card stays in your hand.");
                    }
                    refreshScreen();
                } else if (build.isOwnedByPlayer() && hasOwnBuild()
                        && ((selectedHandIndex != -1
                                && selectedHandIndex < game.getPlayerHand().size()
                                && game.getPlayerHand().get(selectedHandIndex).getValue()
                                        == build.getTargetValue())
                            || (ownBuildAddHandIndex != -1
                                && ownBuildAddHandIndex < game.getPlayerHand().size()
                                && game.getPlayerHand().get(ownBuildAddHandIndex).getValue()
                                        == build.getTargetValue()))) {
                    // Keep a matching hand card selected across the first click
                    // of a double-click, so an extended build can still be captured.
                    selectedBuildIndexForAdd = -1;
                    messageLabel.setText("Matching card selected. Double-click this build to capture it.");
                } else if (build.isOwnedByPlayer() && hasOwnBuild()) {
                    selectedBuildIndexForAdd = buildIndex;
                    selectedHandIndex = -1;
                    buildTargetHandIndex = -1;
                    buildAddHandIndex = -1;
                    takeoverTargetHandIndex = -1;
                    takeoverAddHandIndex = -1;
                    ownBuildAddHandIndex = -1;
                    selectedTableIndexes.clear();
                    messageLabel.setText("Your build selected. Click table cards and/or the opponent's top captured card to add a group, or click a hand card to include it.");
                    tryCompleteTableOnlyBuildAddition();
                    refreshScreen();
                }
            });
            buildPane.getChildren().add(buildBox);
        }

        for (int i = 0; i < game.getPlayerHand().size(); i++) {
            final int cardIndex = i;
            Card card = game.getPlayerHand().get(i);

            Button face = new Button();
            face.setGraphic(cardFace(card));
            face.setMinSize(48, 68);
            face.setPrefSize(48, 68);
            face.setMaxSize(48, 68);
            if (cardIndex == selectedHandIndex || cardIndex == buildTargetHandIndex
                    || cardIndex == buildAddHandIndex || cardIndex == takeoverTargetHandIndex
                    || cardIndex == takeoverAddHandIndex || cardIndex == ownBuildAddHandIndex) {
                face.setStyle("-fx-background-color: #f7d34a; -fx-padding: 2; -fx-border-color: #f7d34a; -fx-border-width: 3;");
            } else {
                face.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            }
            face.setOnMouseClicked(event -> {
                if (!game.isPlayerTurn() || game.isGameOver() || computerAnimating) return;
                if (event.getClickCount() >= 2) {
                    handleHandDoubleClick(cardIndex);
                } else {
                    handleHandSingleClick(cardIndex);
                }
            });
            playerPane.getChildren().add(face);
        }

        capturedLabel.setText("Captured cards — You: " + game.getPlayerCaptured().size()
                + " | Bot: " + game.getComputerCaptured().size());
        playerCapturedTop.setText(game.getPlayerCaptured().isEmpty()
                ? "0 cards" : game.getPlayerCaptured().size() + " cards");
        computerCapturedTop.setText(game.getComputerCaptured().isEmpty()
                ? "0 cards" : game.getComputerCaptured().size() + " cards");

        playerCapturedCard.getChildren().clear();
        computerCapturedCard.getChildren().clear();

        // The opponent's top captured card can only be selected while the player
        // already owns a build, and only to add to or capture that build.
        computerCapturedCard.setOnMouseClicked(event -> {
            if (event.getClickCount() != 1) return;
            if (!game.isPlayerTurn() || game.isGameOver() || computerAnimating) return;
            if (!hasOwnBuild() || game.getComputerCaptured().isEmpty()) {
                messageLabel.setText("You can use the opponent's top captured card only when you already have a build.");
                return;
            }
            useOpponentCapturedTop = !useOpponentCapturedTop;
            if (useOpponentCapturedTop) {
                if (selectedBuildIndexForAdd != -1 && takeoverTargetHandIndex != -1) {
                    messageLabel.setText("Opponent's top captured card selected for the takeover. Select table cards to reach the new target.");
                    tryCompleteTableOnlyBuildAddition();
                } else if (selectedBuildIndexForAdd != -1 && ownBuildAddHandIndex != -1) {
                    messageLabel.setText("Opponent's top captured card selected. Now select table cards so the hand card, captured card, and table cards total the build target.");
                    tryCompleteTableOnlyBuildAddition();
                } else {
                    messageLabel.setText("Opponent's top captured card selected. Select your build, then table cards to add it.");
                }
            } else {
                messageLabel.setText("Opponent's top captured card unselected.");
            }
            refreshScreen();
        });
        if (game.getPlayerCaptured().isEmpty()) {
            playerCapturedCard.getChildren().add(emptyPileCard());
        } else {
            playerCapturedCard.getChildren().add(cardFace(
                    game.getPlayerCaptured().get(game.getPlayerCaptured().size() - 1)));
        }
        if (game.getComputerCaptured().isEmpty()) {
            computerCapturedCard.getChildren().add(emptyPileCard());
        } else {
            computerCapturedCard.getChildren().add(cardFace(
                    game.getComputerCaptured().get(game.getComputerCaptured().size() - 1)));
        }

        if (game.isGameOver()) {
            messageLabel.setText("Game finished! " + game.getWinner());
            showFinalScore();
        } else if (!game.isPlayerTurn()) {
            messageLabel.setText("Bot's turn!");
        } else if (buildTargetHandIndex != -1 && buildAddHandIndex != -1) {
            messageLabel.setText("Building: click the table card(s) that complete the target value.");
        } else if (selectedHandIndex != -1) {
            messageLabel.setText("Selected " + game.getPlayerHand().get(selectedHandIndex)
                    + ". Double-click a table card/build to capture, or double-click this hand card to place it.");
        } else {
            messageLabel.setText("Your turn: click a hand card. Double-click it to place it on the table.");
        }
        if (roundChanged) {
            startDealAnimation(game.getRound());
        } else {
            syncPlayerTurnTimer();
        }
    }

    private void syncPlayerTurnTimer() {
        if (dealingCards || game == null || game.isGameOver() || !game.isPlayerTurn() || computerAnimating) {
            stopPlayerTimer();
            if (turnTimerLabel != null) {
                turnTimerLabel.setText(game != null && game.isGameOver() ? "Time: --" : "Opponent's turn");
            }
            return;
        }

        // Do not restart the countdown just because the player selected a card.
        if (playerTimerRunning) {
            updateTimerLabel();
            return;
        }

        secondsRemaining = 30;
        playerTimerRunning = true;
        updateTimerLabel();
        playerTurnTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (game == null || game.isGameOver() || !game.isPlayerTurn() || computerAnimating) {
                stopPlayerTimer();
                return;
            }
            secondsRemaining--;
            updateTimerLabel();
            if (secondsRemaining <= 0) {
                stopPlayerTimer();
                automaticallyPlayForPlayer();
            }
        }));
        playerTurnTimer.setCycleCount(Timeline.INDEFINITE);
        playerTurnTimer.play();
    }

    private void updateTimerLabel() {
        if (turnTimerLabel == null) return;
        turnTimerLabel.setText("Your time: " + secondsRemaining + "s");
        if (secondsRemaining <= 5) {
            turnTimerLabel.setStyle("-fx-text-fill: #ff5555; -fx-font-size: 15px; -fx-font-weight: bold;");
        } else {
            turnTimerLabel.setStyle("-fx-text-fill: #fff2cf; -fx-font-size: 15px; -fx-font-weight: bold;");
        }
    }

    private void stopPlayerTimer() {
        if (playerTurnTimer != null) {
            playerTurnTimer.stop();
            playerTurnTimer = null;
        }
        playerTimerRunning = false;
    }

    private void automaticallyPlayForPlayer() {
        if (game == null || game.isGameOver() || !game.isPlayerTurn() || computerAnimating) return;
        resetClickSelection();

        // The timeout plays a hand card onto the table automatically. The timeout
        // move has a dedicated method so it can still progress if a build normally
        // prevents freely placing a card during round one.
        for (int i = 0; i < game.getPlayerHand().size(); i++) {
            if (game.placeCardAfterTimeout(i, true)) {
                messageLabel.setText("Time is up! A card was automatically played for you.");
                finishMove(true);
                return;
            }
        }
        // If every remaining card is reserved as a build target, use a matching
        // card to capture its own build so the timed turn still advances legally.
        for (int handIndex = 0; handIndex < game.getPlayerHand().size(); handIndex++) {
            for (int buildIndex = 0; buildIndex < game.getBuilds().size(); buildIndex++) {
                if (game.getPlayerHand().get(handIndex).getValue()
                        == game.getBuilds().get(buildIndex).getTargetValue()
                        && game.getBuilds().get(buildIndex).isOwnedByPlayer()) {
                    if (game.captureBuild(handIndex, buildIndex, true)) {
                        messageLabel.setText("Time is up! Your build was automatically captured for you.");
                        finishMove(true);
                        return;
                    }
                }
            }
        }
        messageLabel.setText("Time is up, but no playable card was available.");
        refreshScreen();
    }

    private StackPane emptyPileCard() {
        StackPane empty = new StackPane(new Label("Empty"));
        empty.setMinSize(54, 76);
        empty.setPrefSize(54, 76);
        empty.setMaxSize(54, 76);
        empty.setStyle("-fx-background-color: rgba(255,255,255,0.15);"
                + "-fx-border-color: white; -fx-border-style: dashed;"
                + "-fx-border-radius: 7; -fx-background-radius: 7;");
        ((Label) empty.getChildren().get(0)).setStyle("-fx-text-fill: white; -fx-font-size: 11px;");
        return empty;
    }

    private String topCard(ArrayList<Card> cards) {
        if (cards.isEmpty()) return "empty";
        return cardText(cards.get(cards.size() - 1)).replace("\n", " ");
    }

    private String cardText(Card card) {
        String value = card.getValue() == 1 ? "A" : String.valueOf(card.getValue());
        return value + "\n" + suitSymbol(card.getSuit());
    }

    private String suitSymbol(String suit) {
        switch (suit) {
            case "Hearts": return "♥";
            case "Diamonds": return "♦";
            case "Clubs": return "♣";
            default: return "♠";
        }
    }

    // Draws a card with the correct number of suit symbols (pips).
    // For example, the 10 of Hearts shows ten heart symbols, not one large heart.
    private StackPane cardFace(Card card) {
        String value = card.getValue() == 1 ? "A" : String.valueOf(card.getValue());
        String suit = suitSymbol(card.getSuit());
        String colour = (card.getSuit().equals("Hearts")
                || card.getSuit().equals("Diamonds")) ? "#c62828" : "#111111";

        Label topLeft = new Label(value + "\n" + suit);
        topLeft.setStyle("-fx-text-fill: " + colour
                + "; -fx-font-size: 8px; -fx-font-weight: bold;");
        topLeft.setAlignment(Pos.TOP_LEFT);

        Label bottomRight = new Label(value + "\n" + suit);
        bottomRight.setStyle("-fx-text-fill: " + colour
                + "; -fx-font-size: 8px; -fx-font-weight: bold;");
        bottomRight.setRotate(180);

        GridPane pips = new GridPane();
        pips.setAlignment(Pos.CENTER);
        pips.setHgap(1);
        pips.setVgap(0);
        pips.setMinSize(29, 40);
        pips.setPrefSize(29, 40);
        pips.setMaxSize(29, 40);

        int number = card.getValue();
        int pairCount = number / 2;
        int[] rows;
        if (number % 2 == 1) {
            switch (pairCount) {
                case 0: rows = new int[]{}; break;
                case 1: rows = new int[]{0}; break;
                case 2: rows = new int[]{0, 4}; break;
                case 3: rows = new int[]{0, 1, 4}; break;
                case 4: rows = new int[]{0, 1, 3, 4}; break;
                default: rows = new int[]{0, 1, 2, 3, 4}; break;
            }
        } else {
            switch (pairCount) {
                case 1: rows = new int[]{0}; break;
                case 2: rows = new int[]{0, 4}; break;
                case 3: rows = new int[]{0, 2, 4}; break;
                case 4: rows = new int[]{0, 1, 3, 4}; break;
                default: rows = new int[]{0, 1, 2, 3, 4}; break;
            }
        }

        for (int row : rows) {
            addPip(pips, suit, colour, 0, row, false);
            addPip(pips, suit, colour, 1, row, false);
        }
        if (number % 2 == 1) {
            addPip(pips, suit, colour, 0, 2, true);
        }

        StackPane face = new StackPane();
        face.setMinSize(48, 68);
        face.setPrefSize(48, 68);
        face.setMaxSize(48, 68);
        face.setPadding(new Insets(4));
        face.setStyle("-fx-background-color: #fffdf8;"
                + "-fx-border-color: #252525; -fx-border-width: 1.5;"
                + "-fx-background-radius: 7; -fx-border-radius: 7;");

        StackPane.setAlignment(topLeft, Pos.TOP_LEFT);
        StackPane.setAlignment(bottomRight, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(pips, Pos.CENTER);
        face.getChildren().addAll(pips, topLeft, bottomRight);
        return face;
    }

    private void addPip(GridPane pips, String suit, String colour,
                        int column, int row, boolean centered) {
        Label pip = new Label(suit);
        pip.setStyle("-fx-text-fill: " + colour
                + "; -fx-font-size: 8px; -fx-font-weight: bold;");
        pip.setAlignment(Pos.CENTER);
        pip.setMinSize(14, 9);
        pip.setPrefSize(14, 9);
        pip.setMaxSize(14, 9);
        if (centered) {
            GridPane.setColumnSpan(pip, 2);
            GridPane.setHalignment(pip, HPos.CENTER);
        }
        pips.add(pip, column, row);
    }

    // Animate the card first. When it reaches the destination area, perform
    // the move and refresh the display.
    private void animateSelectedCard(int handIndex, Node destination,
                                     Runnable moveAction) {
        if (handIndex < 0 || handIndex >= playerPane.getChildren().size()) {
            moveAction.run();
            refreshScreen();
            return;
        }

        Node cardNode = playerPane.getChildren().get(handIndex);
        Bounds sourceBounds = cardNode.localToScene(cardNode.getBoundsInLocal());
        Node landingNode = destination;
        if (destination instanceof FlowPane && !((FlowPane) destination).getChildren().isEmpty()) {
            // Land on the first visible card/build rather than the middle of the whole area.
            landingNode = ((FlowPane) destination).getChildren().get(0);
        }
        Bounds destinationBounds = landingNode.localToScene(landingNode.getBoundsInLocal());

        double moveX = destinationBounds.getMinX() + destinationBounds.getWidth() / 2
                - (sourceBounds.getMinX() + sourceBounds.getWidth() / 2);
        double moveY = destinationBounds.getMinY() + destinationBounds.getHeight() / 2
                - (sourceBounds.getMinY() + sourceBounds.getHeight() / 2);

        TranslateTransition transition = new TranslateTransition(Duration.millis(450), cardNode);
        transition.setByX(moveX);
        transition.setByY(moveY);
        transition.setOnFinished(event -> {
            cardNode.setTranslateX(0);
            cardNode.setTranslateY(0);
            moveAction.run();
        });
        transition.play();
    }

    private void finishMove(boolean success) {
        if (!success) {
            // The logic only removes cards after validating a move. Refreshing
            // therefore returns any animated card to the hand/table's real state.
            messageLabel.setText("Move not valid — your cards have been returned. Try another move.");
            resetClickSelection();
            refreshScreen();
            return;
        } else if (!game.isGameOver() && !game.isPlayerTurn()) {
            refreshScreen();
            animateComputerMove();
            return;
        }
        refreshScreen();
    }

    // Animate one hidden Bot card moving toward the play area before
    // the Bot's game action is applied.
    private void animateComputerMove() {
        if (dealingCards || computerAnimating || game.isGameOver() || game.isPlayerTurn()) return;
        stopPlayerTimer();
        computerAnimating = true;

        if (computerPane.getChildren().isEmpty()) {
            game.computerPlay();
            computerAnimating = false;
            refreshScreen();
            return;
        }

        Node movingCard = computerPane.getChildren().get(0);
        Bounds sourceBounds = movingCard.localToScene(movingCard.getBoundsInLocal());
        Bounds destinationBounds = tablePane.localToScene(tablePane.getBoundsInLocal());

        double moveX = destinationBounds.getMinX() + destinationBounds.getWidth() / 2
                - (sourceBounds.getMinX() + sourceBounds.getWidth() / 2);
        double moveY = destinationBounds.getMinY() + destinationBounds.getHeight() / 2
                - (sourceBounds.getMinY() + sourceBounds.getHeight() / 2);

        TranslateTransition transition = new TranslateTransition(Duration.millis(500), movingCard);
        transition.setByX(moveX);
        transition.setByY(moveY);
        transition.setOnFinished(event -> {
            movingCard.setTranslateX(0);
            movingCard.setTranslateY(0);
            game.computerPlay();
            computerAnimating = false;
            refreshScreen();
        });
        transition.play();
    }

    private boolean hasOwnBuild() {
        for (Build build : game.getBuilds()) {
            if (build.isOwnedByPlayer()) return true;
        }
        return false;
    }

    // When extending, aim for the visual stack belonging to your existing build.
    // When creating a new build, aim for the build area itself.
    private Node buildDestination() {
        for (int i = 0; i < game.getBuilds().size(); i++) {
            if (game.getBuilds().get(i).isOwnedByPlayer()
                    && i < buildPane.getChildren().size()) {
                return buildPane.getChildren().get(i);
            }
        }
        return buildPane;
    }

    private void handleHandSingleClick(int cardIndex) {
        // When an own build is selected, a hand card may be added to the group.
        // The card is consumed only if the complete group matches the build target.
        if (selectedBuildIndexForAdd != -1 && takeoverTargetHandIndex == -1
                && selectedBuildIndexForAdd < game.getBuilds().size()
                && game.getBuilds().get(selectedBuildIndexForAdd).isOwnedByPlayer()) {
            ownBuildAddHandIndex = cardIndex;
            selectedHandIndex = -1;
            selectedTableIndexes.clear();
            messageLabel.setText("Hand card selected for your build. Add the opponent's top captured card and/or table cards so their total equals the build target.");
            tryCompleteTableOnlyBuildAddition();
            refreshScreen();
            return;
        }

        // During a takeover, the first selected hand card is the new target.
        // Clicking a different hand card selects the optional card to add to the build.
        if (selectedBuildIndexForAdd != -1 && takeoverTargetHandIndex != -1
                && takeoverAddHandIndex == -1 && cardIndex != takeoverTargetHandIndex) {
            takeoverAddHandIndex = cardIndex;
            selectedHandIndex = takeoverTargetHandIndex;
            selectedTableIndexes.clear();
            messageLabel.setText("Added hand card selected. Click table cards to make up the remaining value.");
            tryCompleteTableOnlyBuildAddition();
            refreshScreen();
            return;
        }

        if (selectedHandIndex == -1) {
            selectedHandIndex = cardIndex;
            buildTargetHandIndex = -1;
            buildAddHandIndex = -1;
            selectedBuildIndexForAdd = -1;
            takeoverTargetHandIndex = -1;
            takeoverAddHandIndex = -1;
            selectedTableIndexes.clear();
            refreshScreen();
            return;
        }

        if (buildTargetHandIndex != -1 && buildAddHandIndex != -1) {
            // A different hand card cancels the current build selection and selects a new card.
            selectedHandIndex = cardIndex;
            buildTargetHandIndex = -1;
            buildAddHandIndex = -1;
            selectedBuildIndexForAdd = -1;
            takeoverTargetHandIndex = -1;
            takeoverAddHandIndex = -1;
            selectedTableIndexes.clear();
            refreshScreen();
            return;
        }

        if (selectedHandIndex == cardIndex) {
            // Click the selected target card again to confirm a build made only
            // from table cards. The target remains in hand, so the turn continues.
            if (buildTargetHandIndex == -1 && buildAddHandIndex == -1
                    && selectedBuildIndexForAdd == -1 && !selectedTableIndexes.isEmpty()) {
                int sum = 0;
                for (int index : selectedTableIndexes) {
                    if (index >= 0 && index < game.getTableCards().size()) {
                        sum += game.getTableCards().get(index).getValue();
                    }
                }
                int targetValue = game.getPlayerHand().get(selectedHandIndex).getValue();
                if (sum == targetValue) {
                    boolean success = game.makeBuildFromTable(
                            selectedHandIndex, new ArrayList<>(selectedTableIndexes), true);
                    selectedTableIndexes.clear();
                    selectedHandIndex = -1;
                    messageLabel.setText(success
                            ? "Build created from table cards. Your target card stays in your hand, so your turn continues."
                            : "Could not create that build. Check the total and build rules.");
                    refreshScreen();
                    return;
                }
            }
            // Otherwise keep the card selected; double-click places it on the table.
            refreshScreen();
            return;
        }

        // First card is the build target and the second is the card consumed to make the build.
        buildTargetHandIndex = selectedHandIndex;
        buildAddHandIndex = cardIndex;
        selectedTableIndexes.clear();
        int targetValue = game.getPlayerHand().get(buildTargetHandIndex).getValue();
        int addValue = game.getPlayerHand().get(buildAddHandIndex).getValue();
        int existingBuildValue = getOwnedBuildValue();
        if (hasOwnBuild() && targetValue != existingBuildValue) {
            showMessage("Cannot extend build",
                    "Select a hand card matching your existing build's target value ("
                    + existingBuildValue + ") first.");
            buildTargetHandIndex = -1;
            buildAddHandIndex = -1;
            selectedHandIndex = cardIndex;
            refreshScreen();
            return;
        }

        int neededFromTable = targetValue - addValue;
        if (neededFromTable < 0) {
            showMessage("Cannot build",
                    "The second hand card is higher than the target. Select a valid added card.");
            buildTargetHandIndex = -1;
            buildAddHandIndex = -1;
            selectedHandIndex = cardIndex;
        } else if (neededFromTable == 0) {
            completeBuild();
            return;
        }
        refreshScreen();
    }

    private void handleHandDoubleClick(int cardIndex) {
        // If table cards have been selected and add up to this target, a double-click
        // on the target confirms a build from those table cards. Otherwise it places
        // the selected hand card on the table.
        if (selectedHandIndex == cardIndex && buildTargetHandIndex == -1) {
            int sum = 0;
            for (int index : selectedTableIndexes) {
                if (index >= 0 && index < game.getTableCards().size()) {
                    sum += game.getTableCards().get(index).getValue();
                }
            }
            int targetValue = game.getPlayerHand().get(cardIndex).getValue();
            if (!selectedTableIndexes.isEmpty() && sum == targetValue
                    && selectedBuildIndexForAdd == -1) {
                boolean success = game.makeBuildFromTable(
                        cardIndex, new ArrayList<>(selectedTableIndexes), true);
                resetClickSelection();
                messageLabel.setText(success
                        ? "Build created from table cards. Your target card stays in your hand, so your turn continues."
                        : "Could not create that build. Check the total and build rules.");
                refreshScreen();
                return;
            }

            final int movingIndex = cardIndex;
            animateSelectedCard(movingIndex, tablePane, () -> {
                boolean success = game.placeCard(movingIndex, true);
                resetClickSelection();
                finishMove(success);
            });
        } else {
            handleHandSingleClick(cardIndex);
        }
    }

    private void handleTableSingleClick(int tableIndex) {
        if (selectedBuildIndexForAdd != -1) {
            if (selectedTableIndexes.contains(tableIndex)) return;
            selectedTableIndexes.add(tableIndex);
            tryCompleteTableOnlyBuildAddition();
            refreshScreen();
            return;
        }

        if (buildTargetHandIndex != -1 && buildAddHandIndex != -1) {
            if (selectedTableIndexes.contains(tableIndex)) return;
            selectedTableIndexes.add(tableIndex);
            int sum = 0;
            for (int index : selectedTableIndexes) {
                sum += game.getTableCards().get(index).getValue();
            }
            int targetValue = game.getPlayerHand().get(buildTargetHandIndex).getValue();
            int addValue = game.getPlayerHand().get(buildAddHandIndex).getValue();
            int needed = targetValue - addValue;
            if (sum == needed) {
                completeBuild();
                return;
            }
            if (sum > needed) {
                selectedTableIndexes.clear();
                showMessage("Build total too high", "Those table cards exceed the value needed. Click the correct table card or cards.");
            } else {
                messageLabel.setText("Selected table total: " + sum + " of " + needed + ". Click more table cards.");
            }
            refreshScreen();
            return;
        }

        if (selectedHandIndex == -1) {
            messageLabel.setText("First click a card in your hand.");
            return;
        }

        // With only a target card selected, table cards can form a build whose
        // total equals that target. Click the hand target again to confirm.
        if (buildTargetHandIndex == -1 && buildAddHandIndex == -1) {
            if (!selectedTableIndexes.contains(tableIndex)) {
                selectedTableIndexes.add(tableIndex);
            }
            int sum = 0;
            for (int index : selectedTableIndexes) {
                sum += game.getTableCards().get(index).getValue();
            }
            int wanted = game.getPlayerHand().get(selectedHandIndex).getValue();
            if (sum > wanted) {
                selectedTableIndexes.clear();
                messageLabel.setText("Selected cards exceed the target. Try again.");
            } else if (sum == wanted) {
                messageLabel.setText("Table cards total " + wanted + ". Click your selected hand card again to create a build, or double-click the last table card to capture the combination.");
            } else {
                messageLabel.setText("Selected table total: " + sum + " of " + wanted + ". Click more cards.");
            }
            refreshScreen();
            return;
        }

        // A single matching card is captured on double-click. For combinations, click cards
        // one by one; the combination is captured when their total matches the hand card.
        if (selectedTableIndexes.contains(tableIndex)) return;
        selectedTableIndexes.add(tableIndex);
        int sum = 0;
        for (int index : selectedTableIndexes) {
            sum += game.getTableCards().get(index).getValue();
        }
        int wanted = game.getPlayerHand().get(selectedHandIndex).getValue();
        if (sum > wanted) {
            selectedTableIndexes.clear();
            messageLabel.setText("Those cards are worth more than your selected card. Try again.");
            refreshScreen();
        } else if (sum == wanted) {
            messageLabel.setText("Selected table total matches your card. Double-click the last selected table card to capture, or click your selected hand card again to build.");
            refreshScreen();
        } else {
            messageLabel.setText("Selected table total: " + sum + " of " + wanted
                    + ". Click more cards, then double-click the last one to capture the combination.");
            refreshScreen();
        }
    }

    private void tryCompleteTableOnlyBuildAddition() {
        if (selectedBuildIndexForAdd < 0
                || selectedBuildIndexForAdd >= game.getBuilds().size()) return;

        int sum = useOpponentCapturedTop && !game.getComputerCaptured().isEmpty()
                ? game.getComputerCaptured().get(game.getComputerCaptured().size() - 1).getValue()
                : 0;
        for (int index : selectedTableIndexes) {
            if (index >= 0 && index < game.getTableCards().size()) {
                sum += game.getTableCards().get(index).getValue();
            }
        }

        Build build = game.getBuilds().get(selectedBuildIndexForAdd);
        boolean takingOver = takeoverTargetHandIndex != -1;
        boolean addingHandCardToOwnBuild = !takingOver && ownBuildAddHandIndex != -1;
        int requiredValue = takingOver
                ? game.getPlayerHand().get(takeoverTargetHandIndex).getValue() - build.getTargetValue()
                : build.getTargetValue();
        if (takingOver && takeoverAddHandIndex != -1) {
            requiredValue -= game.getPlayerHand().get(takeoverAddHandIndex).getValue();
        }
        if (addingHandCardToOwnBuild) {
            requiredValue -= game.getPlayerHand().get(ownBuildAddHandIndex).getValue();
        }

        if (sum == requiredValue
                && (!selectedTableIndexes.isEmpty() || useOpponentCapturedTop
                    || (takingOver && takeoverAddHandIndex != -1))) {
            final int buildIndex = selectedBuildIndexForAdd;
            final ArrayList<Integer> indexes = new ArrayList<>(selectedTableIndexes);
            final boolean useTop = useOpponentCapturedTop;
            boolean success;
            if (takingOver) {
                success = game.takeOverBuildWithCards(
                        buildIndex, takeoverTargetHandIndex, takeoverAddHandIndex,
                        indexes, useTop, true);
            } else if (addingHandCardToOwnBuild) {
                success = game.addCardsToBuildWithHandCard(
                        buildIndex, ownBuildAddHandIndex, indexes, useTop, true);
            } else {
                success = game.addTableCardsToBuild(buildIndex, indexes, useTop, true);
            }
            boolean usedHandCard = (takingOver && takeoverAddHandIndex != -1)
                    || addingHandCardToOwnBuild;
            selectedBuildIndexForAdd = -1;
            takeoverTargetHandIndex = -1;
            takeoverAddHandIndex = -1;
            ownBuildAddHandIndex = -1;
            selectedTableIndexes.clear();
            useOpponentCapturedTop = false;
            if (success) {
                messageLabel.setText(takingOver
                        ? (usedHandCard
                            ? "Build taken over using a card from your hand. Your turn ends."
                            : "Build taken over using table/captured-pile cards. Your turn continues.")
                        : (usedHandCard
                            ? "Cards added to your build using a hand card. Your turn ends."
                            : "Cards added to the build. Your turn continues because you did not play from your hand."));
            } else if (takingOver && game.hasBuildBeenExtended(buildIndex)) {
                messageLabel.setText("You cannot raise this build's target because it has already been extended.");
            } else {
                messageLabel.setText("Those cards do not add up to the required value, or this move is not allowed.");
            }
            if (success && usedHandCard) {
                finishMove(true);
                return;
            }
            refreshScreen();
        } else if (sum > requiredValue) {
            selectedTableIndexes.clear();
            messageLabel.setText("Selected cards exceed the required value. Try selecting the cards again.");
        } else {
            messageLabel.setText("Selected total: " + sum + " of " + requiredValue
                    + ". Click more table cards or select the opponent's top captured card.");
        }
    }

    private void handleTableDoubleClick(int tableIndex) {
        if (selectedHandIndex == -1 || buildTargetHandIndex != -1
                || selectedBuildIndexForAdd != -1) return;
        ArrayList<Integer> indexes = new ArrayList<>();
        if (selectedTableIndexes.contains(tableIndex)) {
            indexes.addAll(selectedTableIndexes);
        } else {
            indexes.addAll(selectedTableIndexes);
            indexes.add(tableIndex);
        }
        int total = 0;
        for (int index : indexes) {
            if (index < 0 || index >= game.getTableCards().size()) return;
            total += game.getTableCards().get(index).getValue();
        }
        final int handIndex = selectedHandIndex;
        if (total != game.getPlayerHand().get(handIndex).getValue()) {
            // A double-click on one card can still capture that card if it matches.
            indexes.clear();
            indexes.add(tableIndex);
            if (game.getTableCards().get(tableIndex).getValue()
                    != game.getPlayerHand().get(handIndex).getValue()) return;
        }
        final ArrayList<Integer> captureIndexes = new ArrayList<>(indexes);
        animateSelectedCard(handIndex, playerCapturedCard, () -> {
            boolean success = game.captureTableCombination(handIndex, captureIndexes, true);
            resetClickSelection();
            finishMove(success);
        });
    }

    private void handleBuildDoubleClick(int buildIndex) {
        if (buildTargetHandIndex != -1) return;
        if (buildIndex < 0 || buildIndex >= game.getBuilds().size()) return;

        // Normally the player selects a hand card first. Also support selecting
        // their build first and then choosing the matching card, which previously
        // left the card in the build-add selection instead of capturing the build.
        final int handIndex;
        if (selectedHandIndex != -1) {
            handIndex = selectedHandIndex;
        } else if (ownBuildAddHandIndex != -1) {
            handIndex = ownBuildAddHandIndex;
        } else {
            messageLabel.setText("Select a hand card matching the build target, then double-click the build.");
            return;
        }

        if (handIndex < 0 || handIndex >= game.getPlayerHand().size()) return;
        final int targetValue = game.getBuilds().get(buildIndex).getTargetValue();
        if (game.getPlayerHand().get(handIndex).getValue() != targetValue) {
            messageLabel.setText("To capture this build, select a hand card matching its target value ("
                    + targetValue + "). The build may be extended; its target still determines capture.");
            return;
        }

        animateSelectedCard(handIndex, playerCapturedCard, () -> {
            boolean success = game.captureBuild(handIndex, buildIndex, true);
            resetClickSelection();
            if (success) {
                finishMove(true);
            } else {
                messageLabel.setText("Could not capture the build. Check that it is your turn and the selected card matches the target.");
                refreshScreen();
            }
        });
    }

    private void completeTableCapture() {
        final int handIndex = selectedHandIndex;
        final ArrayList<Integer> indexes = new ArrayList<>(selectedTableIndexes);
        animateSelectedCard(handIndex, playerCapturedCard, () -> {
            boolean success = game.captureTableCombination(handIndex, indexes, true);
            resetClickSelection();
            finishMove(success);
        });
    }

    private void completeBuild() {
        final int targetIndex = buildTargetHandIndex;
        final int addIndex = buildAddHandIndex;
        final ArrayList<Integer> indexes = new ArrayList<>(selectedTableIndexes);
        animateSelectedCard(addIndex, buildDestination(), () -> {
            boolean success;
            if (hasOwnBuild()) {
                success = game.extendBuild(targetIndex, addIndex, indexes, true);
            } else {
                success = game.makeBuild(targetIndex, addIndex, indexes, true);
            }
            resetClickSelection();
            finishMove(success);
        });
    }

    private int getOwnedBuildValue() {
        for (Build build : game.getBuilds()) {
            if (build.isOwnedByPlayer()) return build.getTargetValue();
        }
        return 0;
    }

    private void resetClickSelection() {
        selectedHandIndex = -1;
        buildTargetHandIndex = -1;
        buildAddHandIndex = -1;
        selectedBuildIndexForAdd = -1;
        takeoverTargetHandIndex = -1;
        takeoverAddHandIndex = -1;
        ownBuildAddHandIndex = -1;
        useOpponentCapturedTop = false;
        selectedTableIndexes.clear();
    }

    private ArrayList<Integer> parseIndexes(String text) {
        String[] parts = text.split(",");
        ArrayList<Integer> indexes = new ArrayList<>();
        for (String part : parts) {
            if (!part.trim().isEmpty()) indexes.add(Integer.parseInt(part.trim()));
        }
        return indexes;
    }

    // Invalid moves are reported in the message line, not in pop-up windows.
    private void showMessage(String title, String message) {
        messageLabel.setText(message);
    }

    private void createGameOverPopup() {
        gameOverWinnerLabel = new Label();
        gameOverWinnerLabel.setStyle("-fx-font-size: 25px; -fx-font-weight: bold;"
                + "-fx-text-fill: #ffffff;");

        gameOverScoreLabel = new Label();
        gameOverScoreLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #fff4e8;"
                + "-fx-line-spacing: 4px;");
        gameOverScoreLabel.setAlignment(Pos.CENTER);
        gameOverScoreLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button playAgainButton = new Button("Play Again");
        playAgainButton.setStyle("-fx-background-color: #287a3b; -fx-text-fill: white;"
                + "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 6;"
                + "-fx-padding: 8 18 8 18;");
        playAgainButton.setOnAction(event -> restartGame());

        Button exitButton = new Button("Exit");
        exitButton.setStyle("-fx-background-color: #6d2424; -fx-text-fill: white;"
                + "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 6;"
                + "-fx-padding: 8 18 8 18;");
        exitButton.setOnAction(event -> {
            stopPlayerTimer();
            showEntrance(mainStage);
        });

        HBox buttons = new HBox(12, playAgainButton, exitButton);
        buttons.setAlignment(Pos.CENTER);

        gameOverPopup = new VBox(14, gameOverWinnerLabel, gameOverScoreLabel, buttons);
        gameOverPopup.setAlignment(Pos.CENTER);
        gameOverPopup.setPadding(new Insets(22, 28, 22, 28));
        gameOverPopup.setMaxWidth(390);
        gameOverPopup.setStyle("-fx-background-color: #e8c58e;"
                + "-fx-border-color: #b58a5b; -fx-border-width: 3;"
                + "-fx-background-radius: 10; -fx-border-radius: 10;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 18, 0.35, 0, 5);");
        gameOverPopup.setVisible(false);
        gameOverPopup.setManaged(false);
    }

    private void showFinalScore() {
        // Keep the end-of-game popup visible whenever the finished game is refreshed.
        // This prevents it from being skipped if the screen refreshes more than once.
        gameOverShown = true;

        Platform.runLater(() -> {
            if (game == null || !game.isGameOver() || gameOverPopup == null) {
                return;
            }
            String winner = game.getWinner();
            gameOverWinnerLabel.setText(winner.equals("Computer wins!") ? "Bot wins!" : winner);

            if (winner.equals("You win!")) {
                gameOverWinnerLabel.setStyle("-fx-font-size: 25px; -fx-font-weight: bold;"
                        + "-fx-text-fill: #39d353;");
            } else if (winner.equals("Computer wins!")) {
                gameOverWinnerLabel.setStyle("-fx-font-size: 25px; -fx-font-weight: bold;"
                        + "-fx-text-fill: #ff4545;");
            } else {
                gameOverWinnerLabel.setStyle("-fx-font-size: 25px; -fx-font-weight: bold;"
                        + "-fx-text-fill: #ffd166;");
            }

            gameOverScoreLabel.setText("FINAL SCORE\n"
                    + "You: " + game.getPlayerScore() + " points\n"
                    + "Bot: " + game.getComputerScore() + " points\n\n"
                    + "Your captured cards: " + game.getPlayerCaptured().size() + "\n"
                    + "Bot captured cards: " + game.getComputerCaptured().size());
            gameOverPopup.setManaged(true);
            gameOverPopup.setVisible(true);
            // Bring only the popup forward inside the table layer. Do not
            // raise tableLayer above the hand rows, or the table will cover them.
            gameOverPopup.toFront();
        });
    }

    private void restartGame() {
        stopPlayerTimer();

        // Keep the table in its original slot between the opponent and player areas.
        // If it was previously brought to the front, it can cover the player hand.
        if (gameRoot != null && tableLayer != null) {
            gameRoot.getChildren().remove(tableLayer);
            int tablePosition = Math.min(3, gameRoot.getChildren().size());
            gameRoot.getChildren().add(tablePosition, tableLayer);
        }
        game = new GameLogic();
        lastDisplayedRound = 0;
        gameOverShown = false;
        if (gameOverPopup != null) {
            gameOverPopup.setVisible(false);
            gameOverPopup.setManaged(false);
        }
        computerAnimating = false;
        resetClickSelection();
        refreshScreen();
        startDealAnimation(game.getRound());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
