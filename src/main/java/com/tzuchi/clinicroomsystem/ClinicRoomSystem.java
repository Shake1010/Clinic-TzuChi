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
import javafx.stage.Screen;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

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
import com.tzuchi.clinicroomsystem.AudioAnnouncementService;

public class ClinicRoomSystem extends Application {
    private static final String BASE_URL = "http://localhost:8080/api";
    private Stage primaryStage;
//    private static final String BASE_URL = "http://172.104.124.175:8888/TzuChiQueueingSystem-0.0.1-SNAPSHOT/api";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<String, VBox> queueDisplays = new HashMap<>();
    private final Map<String, Label> latestNumberLabels = new HashMap<>();
    private final Map<String, Label> categoryStatsLabels = new HashMap<>();
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private final AudioAnnouncementService audioService = new AudioAnnouncementService();
    @Override
    public void start(Stage primaryStage) {


        this.primaryStage = primaryStage;
        primaryStage.setTitle("Tzu Chi Clinic Room System");
        showLoginScene();
    }
    private HBox createHeader() {
        HBox header = new HBox();
        header.setStyle("-fx-background-color: rgb(22, 38, 74); -fx-padding: 10;");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(15);

        try {
            // Load and display logo with error handling
            Image logoImage = new Image(getClass().getResourceAsStream("/images/tzuchi-logo.png"));
            ImageView logoView = new ImageView(logoImage);
            logoView.setFitHeight(40);
            logoView.setFitWidth(40);
            logoView.setPreserveRatio(true);
            header.getChildren().add(logoView);
        } catch (Exception e) {
            System.err.println("Could not load logo image: " + e.getMessage());
            // Create a placeholder if image loading fails
            Label logoPlaceholder = new Label("TC");
            logoPlaceholder.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: bold;");
            header.getChildren().add(logoPlaceholder);
        }

        // Create title labels
        VBox titleBox = new VBox(5);
        Label mainTitle = new Label("CLINIC ROOM");
        mainTitle.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subTitle = new Label("SYSTEM");
        subTitle.setStyle("-fx-font-size: 18; -fx-text-fill: white;");
        titleBox.getChildren().addAll(mainTitle, subTitle);

        header.getChildren().add(titleBox);
        return header;
    }

    // Fixed key event handling method
    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case NUMPAD2:
            case DIGIT2:
                callNumber("2");
                break;
            case NUMPAD5:
            case DIGIT5:
                callNumber("5");
                break;
            case NUMPAD8:
            case DIGIT8:
                callNumber("8");
                break;
            case NUMPAD1:
            case DIGIT1:
                returnNumber("1");
                break;
            case NUMPAD4:
            case DIGIT4:
                returnNumber("4");
                break;
            case NUMPAD7:
            case DIGIT7:
                returnNumber("7");
                break;
            default:
                break;
        }
    }

    // Add this method that was missing
    private VBox createEnhancedStatsContainer(String column) {
        VBox statsContainer = new VBox(5);
        statsContainer.setMaxWidth(Double.MAX_VALUE);
        statsContainer.setPadding(new Insets(5));
        statsContainer.setStyle("""
        -fx-background-color: #f5f5f5;
        -fx-border-color: rgb(22, 38, 74);
        -fx-border-width: 1 0 0 0;
    """);

        // Create labels for each statistic type
        Label leftInClinicLabel = new Label(getInitialLeftInClinicText(column));
        Label totalRegisteredLabel = new Label("Total Registered: 0");
        Label totalInQueueLabel = column.equals("2") ? new Label("Total in Queue: 0") : null;

        // Style the labels
        String labelStyle = """
        -fx-font-size: 12px;
        -fx-font-weight: bold;
        -fx-padding: 3;
        -fx-background-color: white;
        -fx-border-color: #cccccc;
        -fx-border-width: 1;
        -fx-background-radius: 3;
        -fx-border-radius: 3;
    """;

        leftInClinicLabel.setStyle(labelStyle);
        totalRegisteredLabel.setStyle(labelStyle);
        if (totalInQueueLabel != null) {
            totalInQueueLabel.setStyle(labelStyle);
        }

        leftInClinicLabel.setMaxWidth(Double.MAX_VALUE);
        totalRegisteredLabel.setMaxWidth(Double.MAX_VALUE);
        if (totalInQueueLabel != null) {
            totalInQueueLabel.setMaxWidth(Double.MAX_VALUE);
        }

        statsContainer.getChildren().add(leftInClinicLabel);
        statsContainer.getChildren().add(totalRegisteredLabel);
        if (totalInQueueLabel != null) {
            statsContainer.getChildren().add(totalInQueueLabel);
        }

        categoryStatsLabels.put(column + "_left", leftInClinicLabel);
        categoryStatsLabels.put(column + "_total_registered", totalRegisteredLabel);
        if (totalInQueueLabel != null) {
            categoryStatsLabels.put(column + "_total_queue", totalInQueueLabel);
        }

        return statsContainer;
    }
    private void showMainScene() {
        // Get screen dimensions
        javafx.geometry.Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // Create main vertical layout
        VBox root = new VBox();
        root.setStyle("-fx-background-color: white;");

        // Create header
        HBox header = createHeader();

        // Main content area
        HBox mainLayout = new HBox();
        mainLayout.setSpacing(screenBounds.getWidth() * 0.01);
        mainLayout.setPadding(new Insets(screenBounds.getHeight() * 0.01));
        mainLayout.setStyle("-fx-background-color: white;");

        // Left section for queues
        VBox queuesSection = new VBox(screenBounds.getHeight() * 0.01);
        queuesSection.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.5));
        queuesSection.setMaxHeight(Double.MAX_VALUE);

        // Queue displays container
        HBox queueDisplaysContainer = new HBox(screenBounds.getWidth() * 0.005);
        queueDisplaysContainer.setAlignment(Pos.TOP_CENTER);

        // Add queue displays
        String[] columns = {"2", "5", "8"};
        for (String column : columns) {
            VBox queueDisplay = createEnhancedQueueDisplay(column);
            queueDisplay.prefWidthProperty().bind(queuesSection.widthProperty().multiply(0.32));
            queueDisplays.put(column, queueDisplay);
            queueDisplaysContainer.getChildren().add(queueDisplay);
            HBox.setHgrow(queueDisplay, Priority.ALWAYS);
        }

        queuesSection.getChildren().add(queueDisplaysContainer);
        VBox.setVgrow(queueDisplaysContainer, Priority.ALWAYS);

        // Right section for video
        VBox videoSection = createVideoSection();
        videoSection.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.5));

        // Add sections to main layout
        mainLayout.getChildren().addAll(queuesSection, videoSection);

        // Add header and main layout to root
        root.getChildren().addAll(header, mainLayout);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // Create scene with fixed key event handling
        Scene scene = new Scene(root);
        scene.setOnKeyPressed(this::handleKeyPress);  // Fixed event handling

        // Configure stage
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start periodic updates
        startPeriodicUpdates();
    }
    private VBox createEnhancedQueueDisplay(String column) {
        VBox display = new VBox(0);
        display.setAlignment(Pos.TOP_CENTER);
        display.setPadding(new Insets(0));
        display.setPrefWidth(190);
        display.setMaxHeight(Double.MAX_VALUE);
        display.setStyle("""
        -fx-border-color: rgb(22, 38, 74);
        -fx-border-width: 1;
        -fx-border-radius: 5;
        -fx-background-color: white;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);
    """);

        // Header
        Label headerLabel = new Label("Column " + column);
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setPadding(new Insets(10));
        headerLabel.setStyle("""
        -fx-background-color: rgb(22, 38, 74);
        -fx-text-fill: white;
        -fx-background-radius: 5 5 0 0;
    """);

        // Queue list with enhanced styling
        VBox queueList = new VBox(0);
        queueList.setAlignment(Pos.TOP_CENTER);
        queueList.setStyle("-fx-background-color: white;");

        // Create queue items with enhanced styling
        for (int i = 0; i < 300; i++) {
            Label lineLabel = new Label("");
            lineLabel.setMaxWidth(Double.MAX_VALUE);
            lineLabel.setPrefHeight(40);

            String backgroundColor = i < 4 ? "rgb(240, 247, 255)" : "white";
            lineLabel.setStyle(String.format("""
            -fx-border-color: #e0e0e0;
            -fx-border-width: 0 0 1 0;
            -fx-padding: 10;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-alignment: center;
            -fx-background-color: %s;
        """, backgroundColor));

            queueList.getChildren().add(lineLabel);
        }

        // Scroll pane with custom styling
        ScrollPane scrollPane = new ScrollPane(queueList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle("""
        -fx-background: white;
        -fx-background-color: white;
        -fx-border-width: 0;
        -fx-padding: 0;
    """);

        // Latest number label with enhanced styling
        Label latestLabel = createEnhancedLatestLabel();
        latestNumberLabels.put(column, latestLabel);

        // Statistics container with enhanced styling
        VBox statsContainer = createEnhancedStatsContainer(column);

        display.getChildren().addAll(headerLabel, scrollPane, latestLabel, statsContainer);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return display;
    }
    private Label createEnhancedLatestLabel() {
        Label label = new Label("Latest: -");
        label.setFont(Font.font("System", FontWeight.BOLD, 14));
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        label.setPadding(new Insets(10));
        label.setStyle("""
        -fx-background-color: #f8f9fa;
        -fx-border-color: #e0e0e0;
        -fx-border-width: 1 0 1 0;
    """);
        return label;
    }

    private VBox createVideoSection() {
        VBox videoSection = new VBox();
        videoSection.setStyle("""
        -fx-border-color: rgb(22, 38, 74);
        -fx-border-width: 2;
        -fx-border-radius: 5;
        -fx-background-color: #f8f9fa;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);
    """);

        mediaView = new MediaView();
        mediaView.fitWidthProperty().bind(videoSection.widthProperty().multiply(0.95));
        mediaView.fitHeightProperty().bind(videoSection.heightProperty().multiply(0.95));
        mediaView.setPreserveRatio(true);

        videoSection.getChildren().add(mediaView);
        videoSection.setAlignment(Pos.CENTER);

        return videoSection;
    }
    private void showLoginScene() {
        VBox loginLayout = new VBox(10);
        loginLayout.setPadding(new Insets(10));
        loginLayout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Login");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Login");
        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red;");

        loginButton.setOnAction(e -> authenticateUser(usernameField.getText(), passwordField.getText(), messageLabel));

        loginLayout.getChildren().addAll(titleLabel, usernameField, passwordField, loginButton, messageLabel);

        Scene loginScene = new Scene(loginLayout, 300, 200);
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }
    private void authenticateUser(String username, String password, Label messageLabel) {
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please fill in both fields.");
            return;
        }

        String endpoint = BASE_URL + "/auth/login";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);

        try {
            String jsonBody = OBJECT_MAPPER.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(responseBody -> {
                        if (responseBody.equalsIgnoreCase("Login Successful")) {
                            Platform.runLater(this::showMainScene); // Show main scene on successful login
                        } else {
                            Platform.runLater(() -> messageLabel.setText("Invalid username or password."));
                        }
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> messageLabel.setText("Error connecting to server."));
                        return null;
                    });
        } catch (Exception e) {
            messageLabel.setText("Error: " + e.getMessage());
        }
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

        // Create statistics container
        VBox statsContainer = createStatsLabel(column);

        display.getChildren().addAll(headerLabel, scrollPane, latestLabel, statsContainer);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return display;
    }
    private VBox createStatsLabel(String column) {
        VBox statsContainer = new VBox(5);
        statsContainer.setMaxWidth(Double.MAX_VALUE);
        statsContainer.setPadding(new Insets(5));
        statsContainer.setStyle("""
        -fx-background-color: #f5f5f5;
        -fx-border-color: #2d5d7b;
        -fx-border-width: 1 0 0 0;
    """);

        // Create labels for each statistic type
        Label leftInClinicLabel = new Label(getInitialLeftInClinicText(column));
        Label totalRegisteredLabel = new Label("Total Registered: 0");
        Label totalInQueueLabel = column.equals("2") ? new Label("Total in Queue: 0") : null;

        // Style the labels
        String labelStyle = """
        -fx-font-size: 12px;
        -fx-font-weight: bold;
        -fx-padding: 3;
        -fx-background-color: white;
        -fx-border-color: #cccccc;
        -fx-border-width: 1;
        -fx-background-radius: 3;
        -fx-border-radius: 3;
    """;

        leftInClinicLabel.setStyle(labelStyle);
        totalRegisteredLabel.setStyle(labelStyle);
        if (totalInQueueLabel != null) {
            totalInQueueLabel.setStyle(labelStyle);
        }

        leftInClinicLabel.setMaxWidth(Double.MAX_VALUE);
        totalRegisteredLabel.setMaxWidth(Double.MAX_VALUE);
        if (totalInQueueLabel != null) {
            totalInQueueLabel.setMaxWidth(Double.MAX_VALUE);
        }

        statsContainer.getChildren().add(leftInClinicLabel);
        statsContainer.getChildren().add(totalRegisteredLabel);
        if (totalInQueueLabel != null) {
            statsContainer.getChildren().add(totalInQueueLabel);
        }

        categoryStatsLabels.put(column + "_left", leftInClinicLabel);
        categoryStatsLabels.put(column + "_total_registered", totalRegisteredLabel);
        if (totalInQueueLabel != null) {
            categoryStatsLabels.put(column + "_total_queue", totalInQueueLabel);
        }

        return statsContainer;
    }
    private String getInitialLeftInClinicText(String column) {
        return switch (column) {
            case "2" -> "Left - E: 0 | A: 0 | W: 0";
            case "5" -> "Left P: 0";
            case "8" -> "Left D: 0";
            default -> "";
        };
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
                            audioService.announceNumber(currentPatient);
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
        String column = switch (button) {
            case "1" -> "2";
            case "4" -> "5";
            case "7" -> "8";
            default -> "";
        };

        if (!column.isEmpty()) {
            String queueEndpoint = BASE_URL + "/row" + column + "/clinic";

            // First get the current queue state
            sendHttpRequest(queueEndpoint, "GET", queueResponse -> {
                try {
                    JsonNode root = OBJECT_MAPPER.readTree(queueResponse);
                    JsonNode patients = root.get("patients");

                    if (patients != null && patients.isArray()) {
                        String withdrawEndpoint = BASE_URL + "/withdraw/row" + column + "/clinic?patientId=";

                        // Send withdraw request - no patientId needed as endpoint will handle it
                        sendHttpRequest(withdrawEndpoint, "PUT", withdrawResponse -> {
                            // Refresh queue display after withdrawal
                            sendHttpRequest(queueEndpoint, "GET", finalResponse -> {
                                Platform.runLater(() -> {
                                    updateQueueDisplay(column, finalResponse);
                                    // Find the new latest called number
                                    try {
                                        JsonNode updatedRoot = OBJECT_MAPPER.readTree(finalResponse);
                                        JsonNode updatedPatients = updatedRoot.get("patients");
                                        if (updatedPatients != null && updatedPatients.isArray()) {
                                            String latestId = "";
                                            int maxNumber = -1;
                                            for (JsonNode p : updatedPatients) {
                                                if (!p.get("inQueueClinic").asBoolean()) {  // Changed to inQueueClinic
                                                    String pid = p.get("patientId").asText();
                                                    int num = Integer.parseInt(pid.substring(1));
                                                    if (num > maxNumber) {
                                                        maxNumber = num;
                                                        latestId = pid;
                                                    }
                                                }
                                            }
                                            updateLatestNumber(column, latestId.isEmpty() ? "-" : latestId);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                            });
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showError("Error",
                            "Failed to process queue data: " + e.getMessage()));
                }
            });
        }
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
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json");

            if (method.equals("PUT")) {
                requestBuilder.PUT(HttpRequest.BodyPublishers.noBody());
            } else if (method.equals("POST")) {
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            } else {
                requestBuilder.GET();
            }

            HttpRequest request = requestBuilder.build();
            System.out.println("Sending " + method + " request to: " + endpoint); // Debug log

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("Response status: " + response.statusCode()); // Debug log
                        System.out.println("Response body: " + response.body()); // Debug log

                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            responseHandler.accept(response.body());
                        } else {
                            Platform.runLater(() -> {
                                try {
                                    JsonNode errorNode = OBJECT_MAPPER.readTree(response.body());
                                    String errorMessage = errorNode.has("message") ?
                                            errorNode.get("message").asText() :
                                            "Request failed with status: " + response.statusCode();
                                    showError("API Error", errorMessage + "\nEndpoint: " + endpoint);
                                } catch (Exception e) {
                                    showError("API Error",
                                            "Request failed with status: " + response.statusCode() +
                                                    "\nEndpoint: " + endpoint);
                                }
                            });
                        }
                    })
                    .exceptionally(e -> {
                        e.printStackTrace();
                        Platform.runLater(() -> showError("Connection Error",
                                "Failed to connect to: " + endpoint + "\nError: " + e.getMessage()));
                        return null;
                    });
        } catch (Exception e) {
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
            VBox queueDisplay = queueDisplays.get(column);
            if (queueDisplay == null) return;

            ScrollPane scrollPane = (ScrollPane) queueDisplay.getChildren().get(1);
            VBox queueList = (VBox) scrollPane.getContent();

            // Reset all labels
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

            // Initialize counters
            Map<Character, Integer> leftInClinic = new HashMap<>();
            Map<Character, Integer> highestNumbers = new HashMap<>();
            int totalInQueue = 0;

            if (patients != null && patients.isArray()) {
                int index = 0;
                for (JsonNode patient : patients) {
                    String patientId = patient.has("patientId") ? patient.get("patientId").asText() : "";
                    boolean inQueue = patient.has("inQueueClinic") ? patient.get("inQueueClinic").asBoolean(true) : true;

                    if (!patientId.isEmpty()) {
                        // Update display for in-queue patients
                        if (inQueue) {
                            if (index < queueList.getChildren().size()) {
                                Label label = (Label) queueList.getChildren().get(index);
                                String backgroundColor = index < 4 ? "#e6f3ff" : "white";
                                label.setText(patientId);
                                index++;
                            }

                            // Count in-queue patients
                            char category = patientId.charAt(0);
                            leftInClinic.merge(category, 1, Integer::sum);
                            totalInQueue++;
                        }

                        // Track highest number for each category
                        char category = patientId.charAt(0);
                        try {
                            int number = Integer.parseInt(patientId.substring(1));
                            highestNumbers.merge(category, number, Integer::max);
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing number from patientId: " + patientId);
                        }
                    }
                }
            }

            // Update statistics labels
            updateStatisticsLabels(column, leftInClinic, totalInQueue, highestNumbers);

        } catch (Exception e) {
            System.err.println("Error updating display for column " + column);
            e.printStackTrace();
        }
    }
    private void updateStatisticsLabels(String column, Map<Character, Integer> leftInClinic,
                                        int totalInQueue, Map<Character, Integer> highestNumbers) {
        // Update left in clinic label
        Label leftLabel = categoryStatsLabels.get(column + "_left");
        if (leftLabel != null) {
            String leftText = switch (column) {
                case "2" -> String.format("Left - E: %d | A: %d | W: %d",
                        leftInClinic.getOrDefault('E', 0),
                        leftInClinic.getOrDefault('A', 0),
                        leftInClinic.getOrDefault('W', 0));
                case "5" -> String.format("Left P: %d", leftInClinic.getOrDefault('P', 0));
                case "8" -> String.format("Left D: %d", leftInClinic.getOrDefault('D', 0));
                default -> "";
            };
            leftLabel.setText(leftText);
        }

        // Update total registered label
        Label totalRegLabel = categoryStatsLabels.get(column + "_total_registered");
        if (totalRegLabel != null) {
            int totalRegistered = switch (column) {
                case "2" -> {
                    int eTotal = highestNumbers.getOrDefault('E', 0);
                    int aTotal = highestNumbers.getOrDefault('A', 0);
                    int wTotal = highestNumbers.getOrDefault('W', 0);
                    yield eTotal + aTotal + wTotal;
                }
                case "5" -> highestNumbers.getOrDefault('P', 0);
                case "8" -> highestNumbers.getOrDefault('D', 0);
                default -> 0;
            };
            totalRegLabel.setText(String.format("Total Registered: %d", totalRegistered));
        }

        // Update total in queue label (only for column 2)
        if (column.equals("2")) {
            Label totalQueueLabel = categoryStatsLabels.get(column + "_total_queue");
            if (totalQueueLabel != null) {
                totalQueueLabel.setText(String.format("Total in Queue: %d", totalInQueue));
            }
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
            mediaPlayer.setMute(true);  // Add this line to mute the video

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
        audioService.stop();
    }
    public static void main(String[] args) {
        launch(args);
    }
}