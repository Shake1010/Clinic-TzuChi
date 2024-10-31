package com.tzuchi.clinicroomsystem;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import java.io.File;
import javafx.stage.FileChooser;

public class ClinicRoomSystem extends Application {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<String, VBox> queueDisplays = new HashMap<>();
    private final Map<String, Label> latestNumberLabels = new HashMap<>();
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tzu Chi Clinic Room System");

        // Main horizontal layout
        HBox mainLayout = new HBox(10);
        mainLayout.setPadding(new Insets(10));
        mainLayout.setStyle("-fx-background-color: white;");

        // Left section for queues
        VBox queuesSection = new VBox(10);
        queuesSection.setPrefWidth(800);
        queuesSection.setMaxHeight(Double.MAX_VALUE);

        // Queue displays container
        HBox queueDisplaysContainer = new HBox(5);
        queueDisplaysContainer.setAlignment(Pos.TOP_CENTER);

        // Add queue displays
        String[] columns = {"2", "5", "8"};
        for (String column : columns) {
            VBox queueDisplay = createQueueDisplay(column);
            queueDisplays.put(column, queueDisplay);
            queueDisplaysContainer.getChildren().add(queueDisplay);
            HBox.setHgrow(queueDisplay, Priority.ALWAYS);
        }

        queuesSection.getChildren().add(queueDisplaysContainer);

        // Right section for video
        VBox videoSection = new VBox();
        videoSection.setPrefWidth(800);
        videoSection.setStyle("""
    -fx-border-color: #2d5d7b;
    -fx-border-width: 2;
    -fx-background-color: #f0f0f0;
    """);

        // Setup video components
        mediaView = new MediaView();
        mediaView.setFitWidth(780);
        mediaView.setFitHeight(870);
        mediaView.setPreserveRatio(true);

        // Load video directly with path
        loadAndPlayVideo("C:\\Users\\tina_\\Desktop\\TzuChiVideo\\【名人蔬食】甘佳鑫 茹素的力量.mp4");  // Replace this with your actual video path

        // Add components to video section
        videoSection.getChildren().add(mediaView);
        videoSection.setAlignment(Pos.CENTER);

        mainLayout.getChildren().addAll(queuesSection, videoSection);
        HBox.setHgrow(videoSection, Priority.ALWAYS);

        Scene scene = new Scene(mainLayout, 1600, 900);

        // Add keyboard event handling
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case NUMPAD2 -> callNumber("2");
                case NUMPAD5 -> callNumber("5");
                case NUMPAD8 -> callNumber("8");
                case NUMPAD1 -> returnNumber("1");
                case NUMPAD4 -> returnNumber("4");
                case NUMPAD7 -> returnNumber("7");
                default -> {}
            }
        });

        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();

        startPeriodicUpdates();
    }

    private VBox createQueueDisplay(String column) {
        VBox display = new VBox(0);
        display.setAlignment(Pos.TOP_CENTER);
        display.setPadding(new Insets(0));
        display.setPrefWidth(190);
        display.setMaxHeight(Double.MAX_VALUE);
        display.setStyle("""
        -fx-border-color: #2d5d7b;
        -fx-border-width: 1;
        -fx-background-color: white;
        """);

        Label headerLabel = new Label("Column " + column);
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setPadding(new Insets(10));
        headerLabel.setStyle("-fx-background-color: #e8e8e8;");

        VBox queueList = new VBox(0);
        queueList.setAlignment(Pos.TOP_CENTER);
        queueList.setStyle("-fx-background-color: white;");

        // Pre-create empty line labels
        for (int i = 0; i < 300; i++) {
            Label lineLabel = new Label("");
            lineLabel.setMaxWidth(Double.MAX_VALUE);
            lineLabel.setPrefHeight(40);

            String backgroundColor = i < 4 ? "#e6f3ff" : "white";
            lineLabel.setStyle(String.format("""
            -fx-border-color: #cccccc;
            -fx-border-width: 0 0 1 0;
            -fx-padding: 10;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-alignment: center;
            -fx-background-color: %s;
            """, backgroundColor));

            queueList.getChildren().add(lineLabel);
        }

        ScrollPane scrollPane = new ScrollPane(queueList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle("""
        -fx-background: white;
        -fx-background-color: white;
        -fx-border-width: 0;
        """);

        Label latestLabel = new Label("Latest: -");
        latestLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        latestLabel.setMaxWidth(Double.MAX_VALUE);
        latestLabel.setAlignment(Pos.CENTER);
        latestLabel.setPadding(new Insets(10));
        latestLabel.setStyle("-fx-background-color: #e8e8e8;");
        latestNumberLabels.put(column, latestLabel);

        display.getChildren().addAll(headerLabel, scrollPane, latestLabel);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return display;
    }

    private void callNumber(String column) {
        String endpoint = switch (column) {
            case "2" -> BASE_URL + "/call/clinic/highest";
            case "5" -> BASE_URL + "/call/clinic/nextP";
            case "8" -> BASE_URL + "/call/clinic/nextD";
            default -> "";
        };

        if (!endpoint.isEmpty()) {
            System.out.println("Calling next number for column " + column + " at endpoint: " + endpoint);

            sendHttpRequest(endpoint, "POST", response -> {
                System.out.println("Call response received: " + response);

                try {
                    JsonNode responseNode = OBJECT_MAPPER.readTree(response);
                    String currentPatient = responseNode.has("currentPatient") ?
                            responseNode.get("currentPatient").asText() : null;

                    if (currentPatient != null) {
                        Platform.runLater(() -> {
                            updateLatestNumber(column, currentPatient);

                            // Then fetch updated queue
                            String queueEndpoint = BASE_URL + "/row" + column + "/clinic";
                            sendHttpRequest(queueEndpoint, "GET", queueResponse -> {
                                Platform.runLater(() -> updateQueueDisplay(column, queueResponse));
                            });
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Error processing response: " + e.getMessage());
                    Platform.runLater(() -> showError("Processing Error",
                            "Error processing response for column " + column + ": " + e.getMessage()));
                }
            });
        }
    }

    private void returnNumber(String button) {
        System.out.println("Return button pressed: " + button);
    }

    private void startPeriodicUpdates() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Starting periodic update...");
                updateAllQueues();
            }
        }, 0, 5000);
    }

    private void updateAllQueues() {
        String[] columns = {"2", "5", "8"};
        for (String column : columns) {
            String endpoint = BASE_URL + "/row" + column + "/clinic";
            System.out.println("Updating queue for column " + column);
            sendHttpRequest(endpoint, "GET", response -> {
                Platform.runLater(() -> updateQueueDisplay(column, response));
            });
        }
    }

    private void sendHttpRequest(String endpoint, String method, java.util.function.Consumer<String> responseHandler) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10));

            if (method.equals("POST")) {
                requestBuilder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.noBody());
            } else {
                requestBuilder.GET();
            }

            requestBuilder.header("Accept", "application/json");

            HttpRequest request = requestBuilder.build();
            System.out.println("Sending " + method + " request to: " + endpoint);

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println(method + " response from " + endpoint);
                        System.out.println("Status code: " + response.statusCode());
                        System.out.println("Response body: " + response.body());

                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            if (response.body() == null || response.body().isEmpty()) {
                                Platform.runLater(() -> showError("Empty Response",
                                        "Server returned empty response for: " + endpoint));
                                return;
                            }
                            responseHandler.accept(response.body());
                        } else {
                            Platform.runLater(() -> showError("API Error",
                                    "Request failed with status: " + response.statusCode() +
                                            "\nEndpoint: " + endpoint +
                                            "\nResponse: " + response.body()));
                        }
                    })
                    .exceptionally(e -> {
                        System.err.println("Request failed for " + endpoint);
                        e.printStackTrace();
                        Platform.runLater(() -> showError("Connection Error",
                                "Failed to connect to: " + endpoint + "\nError: " + e.getMessage()));
                        return null;
                    });
        } catch (Exception e) {
            System.err.println("Error creating request for " + endpoint);
            e.printStackTrace();
            Platform.runLater(() -> showError("Request Error",
                    "Error creating request for: " + endpoint + "\nError: " + e.getMessage()));
        }
    }

    private void updateLatestNumber(String column, String patientId) {
        try {
            Label label = latestNumberLabels.get(column);
            if (label != null && patientId != null) {
                String displayId = patientId.trim().replaceAll("^\"|\"$", "");
                if (!displayId.isEmpty()) {
                    label.setText("Latest: " + displayId);
                    System.out.println("Updated latest number for column " + column + " to: " + displayId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating latest number for column " + column + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateQueueDisplay(String column, String responseBody) {
        try {
            System.out.println("Updating display for column " + column + " with response: " + responseBody);

            VBox queueDisplay = queueDisplays.get(column);
            if (queueDisplay == null) return;

            ScrollPane scrollPane = (ScrollPane) queueDisplay.getChildren().get(1);
            VBox queueList = (VBox) scrollPane.getContent();

            // Clear existing text but maintain highlight colors
            for (int i = 0; i < queueList.getChildren().size(); i++) {
                if (queueList.getChildren().get(i) instanceof Label) {
                    Label label = (Label) queueList.getChildren().get(i);
                    String backgroundColor = i < 4 ? "#e6f3ff" : "white";
                    label.setText("");
                    label.setStyle(String.format("""
                    -fx-border-color: #cccccc;
                    -fx-border-width: 0 0 1 0;
                    -fx-padding: 10;
                    -fx-font-size: 16px;
                    -fx-font-weight: bold;
                    -fx-alignment: center;
                    -fx-background-color: %s;
                    """, backgroundColor));
                }
            }

            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode patients = root.get("patients");

            if (patients != null && patients.isArray()) {
                int index = 0;
                for (JsonNode patient : patients) {
                    if (index >= queueList.getChildren().size()) break;

                    String patientId = patient.has("patientId") ? patient.get("patientId").asText() : "";
                    boolean inQueue = patient.has("inQueueClinic") ? patient.get("inQueueClinic").asBoolean(true) : true;

                    if (!patientId.isEmpty() && inQueue) {
                        Label label = (Label) queueList.getChildren().get(index);
                        String backgroundColor = index < 4 ? "#e6f3ff" : "white";
                        label.setText(patientId);
                        index++;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error updating display for column " + column);
            System.err.println("Response body: " + responseBody);
            e.printStackTrace();
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void selectAndPlayVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Video File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mov")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            loadAndPlayVideo(selectedFile.getAbsolutePath());
        }
    }

    private void loadAndPlayVideo(String videoPath) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            File videoFile = new File(videoPath);
            Media media = new Media(videoFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop the video

            mediaPlayer.setOnError(() -> {
                String errorMessage = mediaPlayer.getError().getMessage();
                showError("Video Error", "Error playing video: " + errorMessage);
            });

            mediaPlayer.play();

        } catch (Exception e) {
            showError("Video Error", "Error loading video: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add this method to clean up resources when the application closes
    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}