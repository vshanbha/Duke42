package com.example.ui;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.kordamp.ikonli.javafx.FontIcon;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    private final String chatId = UUID.randomUUID().toString();

    @Override
    public void start(Stage stage) {
        // 2. Apply the theme before you create any scenes
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        HttpClient httpClient = HttpClient.newHttpClient();
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        Tab inferTab = new Tab("Infer");
        TextArea prompt = new TextArea("Say hello to Duke!");
        prompt.setPrefRowCount(4);
        Button infer = new Button();
        TextArea output = new TextArea();
        output.setEditable(false);
    
        infer.setOnAction(e -> {
            String p = prompt.getText(); // Grab the prompt text from the UI thread
            output.setText("Thinking...");
            infer.setDisable(true);

            Task<String> inferTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/edge/infer"))
                            .POST(HttpRequest.BodyPublishers.ofString(p))
                            .header("Content-Type", "text/plain")
                            .build();
                    try {
                        return httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                    } catch (IOException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };

            inferTask.setOnSucceeded(event -> {
                output.setText(inferTask.getValue());
                prompt.clear();
                infer.setDisable(false);
            });
            inferTask.setOnFailed(event -> {
                output.setText("Error: " + inferTask.getException().getMessage());
                infer.setDisable(false);
            });

            new Thread(inferTask).start();
        });

        HBox inferInputRow = new HBox(10, prompt, infer);
        HBox.setHgrow(prompt, Priority.ALWAYS);
        inferInputRow.setAlignment(Pos.CENTER);

        infer.setGraphic(new FontIcon("far-paper-plane"));
        VBox inferLayout = new VBox(20, new Label("Output"), output, inferInputRow);
        VBox.setVgrow(output, Priority.ALWAYS);
        inferTab.setContent(inferLayout);

        Tab chatTab = new Tab("Chat");
        TextArea chatInput = new TextArea("Hello, Duke!");
        chatInput.setPrefRowCount(1);
        VBox chatOutput = new VBox();
        VBox.setVgrow(chatOutput, Priority.ALWAYS);
        Button chatSend = new Button();
        ScrollPane scrollPane = new ScrollPane(chatOutput);
            scrollPane.setFitToWidth(true);
        scrollPane.setVvalue(1.0);
        scrollPane.setPrefHeight(400);


        chatSend.setOnAction(e -> {
            String message = chatInput.getText();
            Label thinkingLabel = new Label("Thinking...");
            chatOutput.getChildren().add(thinkingLabel);
            scrollPane.setVvalue(1.0);
            chatSend.setDisable(true);

            Task<String> chatTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(
                                    "http://localhost:8080/edge/chat/" + chatId + "?message=" + encodedMessage))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .header("Content-Type", "text/plain")
                            .build();
                    try {
                        return httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                    } catch (IOException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };

            chatTask.setOnSucceeded(event -> {
                chatOutput.getChildren().remove(thinkingLabel);
                Label humanMessage = new Label("You: " + message);
                Label aiMessage = new Label("AI: " + chatTask.getValue());
                humanMessage.setWrapText(true);
                aiMessage.setWrapText(true);

                chatOutput.getChildren().addAll(humanMessage, aiMessage);
                chatInput.clear();
                chatSend.setDisable(false);
                scrollPane.setVvalue(1.0); // Scroll to the bottom after adding a new message
            });

            chatTask.setOnFailed(event -> {
                Label errorMessage = new Label("Error: " + chatTask.getException().getMessage());
                chatOutput.getChildren().add(errorMessage);
                errorMessage.setWrapText(true);
                chatSend.setDisable(false);
            });
            new Thread(chatTask).start();
        });

        chatTab.setContent(new VBox());
        HBox chatInputRow = new HBox(10, chatInput, chatSend);
        chatInputRow.setAlignment(Pos.CENTER);
        chatSend.setGraphic(new FontIcon("far-paper-plane"));
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        VBox chatLayout = new VBox(20, scrollPane, chatInputRow);
        VBox.setVgrow(chatOutput, Priority.ALWAYS);

        // Implement Shift+Enter for new line and Enter for submission
        chatInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    chatInput.insertText(chatInput.getCaretPosition(), System.lineSeparator());
                } else {
                    chatSend.fire();
                }
                event.consume();
            }
        });

        prompt.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    prompt.insertText(prompt.getCaretPosition(), System.lineSeparator());
                } else {
                    infer.fire();
                }
                event.consume();
            }
        });

        chatTab.setContent(chatLayout);

        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            double availableHeight = newVal.doubleValue() - 200; // Adjust this value based on your layout
            scrollPane.setPrefHeight(Math.max(400, availableHeight)); // Ensure a minimum height of 400
            scrollPane.setVvalue(1.0);
        });

        // Set initial scrollPane height
        double initialAvailableHeight = 600 - 200; // Initial stage height - top padding and other elements height
        scrollPane.setPrefHeight(Math.max(400, initialAvailableHeight));


        tabPane.getTabs().addAll(inferTab, chatTab);

        VBox root = new VBox(20, tabPane);
        root.setPrefSize(800, 600);

        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root));
        stage.setTitle("Duke42 — Pilot UI");
        stage.show();
    }


    public static void main(String[] args) {
        launch();
    }
}
