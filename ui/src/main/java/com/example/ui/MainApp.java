package com.example.ui;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    private HttpClient httpClient;

    @Override
    public void start(Stage stage) {
        // 2. Apply the theme before you create any scenes
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        TextArea prompt = new TextArea("Say hello to Duke!");
        prompt.setPrefRowCount(4);
        Button infer = new Button("Infer (Edge)");
        TextArea output = new TextArea();
        output.setEditable(false);

        infer.setOnAction(e -> {
            String p = prompt.getText(); // Grab the prompt text from the UI thread
            output.setText("Thinking...");
            infer.setDisable(true);

            // Run the network call on a background thread to keep the UI responsive
            Task<String> inferTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    if (httpClient == null) { httpClient = HttpClient.newHttpClient(); }

                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/edge/infer"))
                            .POST(HttpRequest.BodyPublishers.ofString(p))
                            .header("Content-Type", "text/plain")
                            .build();

                    return httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                }
            };

            // When the task is done, update the UI on the JavaFX Application Thread
            inferTask.setOnSucceeded(event -> {
                output.setText(inferTask.getValue());
                infer.setDisable(false);
            });
            inferTask.setOnFailed(event -> {
                output.setText("Error: " + inferTask.getException().getMessage());
                infer.setDisable(false);
            });

            new Thread(inferTask).start();
        });

        VBox root = new VBox(20, new Label("\uD83D\uDE80 Duke42 — Edge"), prompt, infer, new Label("Output"), output);
        root.setPrefSize(600, 400);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root));
        stage.setTitle("\uD83D\uDE80 Duke42 — Pilot UI");
        stage.show();
    }

    @Override
    public void stop() {
        // Gracefully shut down the HttpClient's executor
        if (httpClient != null) {
            httpClient.close();
            System.out.println("[INFO] HttpClient closed.");
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
