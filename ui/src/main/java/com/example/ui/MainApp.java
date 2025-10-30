package com.example.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MainApp extends Application {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void start(Stage stage) {
        TextArea prompt = new TextArea("Say hello to Duke!");
        prompt.setPrefRowCount(4);
        Button infer = new Button("Infer (Edge)");
        TextArea output = new TextArea();
        output.setEditable(false);

        infer.setOnAction(e -> {
            String p = prompt.getText();
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/edge/infer"))
                        .POST(HttpRequest.BodyPublishers.ofString(p))
                        .header("Content-Type", "text/plain")
                        .build();

                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                output.setText(resp);
            } catch (Exception ex) {
                output.setText("Error: " + ex.getMessage());
            }
        });

        VBox root = new VBox(8, new Label("Duke42 — Edge"), prompt, infer, new Label("Output"), output);
        root.setPrefSize(600, 400);
        stage.setScene(new Scene(root));
        stage.setTitle("Duke42 — Pilot UI");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
