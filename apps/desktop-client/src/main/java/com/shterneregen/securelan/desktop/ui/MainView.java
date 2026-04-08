package com.shterneregen.securelan.desktop.ui;

import com.shterneregen.securelan.chat.event.ChatConnectedEvent;
import com.shterneregen.securelan.chat.event.ChatCoreEvent;
import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent;
import com.shterneregen.securelan.chat.event.ChatErrorEvent;
import com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent;
import com.shterneregen.securelan.chat.event.ChatMessageSentEvent;
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent;
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent;
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest;
import com.shterneregen.securelan.chat.service.ChatClientService;
import com.shterneregen.securelan.chat.service.ChatEventPublisher;
import com.shterneregen.securelan.chat.service.ChatServerConfig;
import com.shterneregen.securelan.chat.service.ChatServerService;
import com.shterneregen.securelan.chat.service.impl.DefaultChatClientService;
import com.shterneregen.securelan.chat.service.impl.DefaultChatServerService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MainView {
    private final TextField hostField = new TextField("127.0.0.1");
    private final TextField portField = new TextField("5050");
    private final TextField nicknameField = new TextField("alice");
    private final TextField passwordField = new TextField("chatpass");
    private final TextArea logArea = new TextArea();
    private final TextField messageField = new TextField();
    private final Label statusLabel = new Label("Idle");

    private final ChatServerService serverService;
    private final ChatClientService clientService;

    public MainView() {
        ChatEventPublisher publisher = this::handleEvent;
        this.serverService = new DefaultChatServerService(publisher);
        this.clientService = new DefaultChatClientService(publisher);
    }

    public Parent createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setTop(buildTopPanel());
        root.setCenter(buildCenterPanel());
        root.setBottom(buildBottomPanel());
        return root;
    }

    public void shutdown() {
        clientService.disconnect();
        serverService.stop();
    }

    private VBox buildTopPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Host"), hostField, new Label("Port"), portField);
        grid.addRow(1, new Label("Nickname"), nicknameField, new Label("Password"), passwordField);
        GridPane.setHgrow(hostField, Priority.ALWAYS);
        GridPane.setHgrow(nicknameField, Priority.ALWAYS);

        Button startServerButton = new Button("Start Server");
        Button stopServerButton = new Button("Stop Server");
        Button connectButton = new Button("Connect");
        Button disconnectButton = new Button("Disconnect");

        startServerButton.setOnAction(event -> startServer());
        stopServerButton.setOnAction(event -> {
            serverService.stop();
            append("[ui] server stopped");
        });
        connectButton.setOnAction(event -> connectClient());
        disconnectButton.setOnAction(event -> clientService.disconnect());

        HBox buttons = new HBox(10, startServerButton, stopServerButton, connectButton, disconnectButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10,
                new Label("Connection"),
                grid,
                buttons,
                new Separator(),
                statusLabel
        );
        box.setPadding(new Insets(0, 0, 12, 0));
        return box;
    }

    private VBox buildCenterPanel() {
        logArea.setEditable(false);
        logArea.setWrapText(true);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        VBox box = new VBox(8, new Label("Chat Log"), logArea);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox buildBottomPanel() {
        Button sendButton = new Button("Send");
        sendButton.setOnAction(event -> sendMessage());
        messageField.setPromptText("Type a message...");
        messageField.setOnAction(event -> sendMessage());

        HBox row = new HBox(10, messageField, sendButton);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        VBox box = new VBox(8, new Separator(), new Label("Message"), row);
        box.setPadding(new Insets(12, 0, 0, 0));
        return box;
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            serverService.start(new ChatServerConfig(port, passwordField.getText()));
            setStatus("Server running on port " + port);
            append("[ui] server started on port " + port);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void connectClient() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            boolean connected = clientService.connect(new ChatClientConnectRequest(
                    hostField.getText().trim(),
                    port,
                    nicknameField.getText().trim(),
                    passwordField.getText()
            ));
            if (!connected) {
                append("[ui] connection failed");
            }
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        clientService.sendMessage(text);
        messageField.clear();
    }

    private void handleEvent(ChatCoreEvent event) {
        Platform.runLater(() -> {
            if (event instanceof ChatConnectedEvent e) {
                setStatus("Connected as " + e.nickname());
                append("[connected] " + e.nickname() + " -> " + e.remoteAddress());
            } else if (event instanceof ChatDisconnectedEvent e) {
                setStatus("Disconnected");
                append("[disconnected] " + e.nickname() + " - " + e.reason());
            } else if (event instanceof ChatMessageReceivedEvent e) {
                append(e.senderNickname() + ": " + e.text());
            } else if (event instanceof ChatMessageSentEvent e) {
                append("[sent] " + e.senderNickname() + ": " + e.text());
            } else if (event instanceof ChatUserJoinedEvent e) {
                append("[join] " + e.nickname());
            } else if (event instanceof ChatUserLeftEvent e) {
                append("[left] " + e.nickname());
            } else if (event instanceof ChatErrorEvent e) {
                append("[error] " + e.message() + (e.cause() != null ? " -> " + e.cause().getMessage() : ""));
            }
        });
    }

    private void append(String line) {
        logArea.appendText(line + System.lineSeparator());
        logArea.positionCaret(logArea.getLength());
    }

    private void setStatus(String value) {
        statusLabel.setText(value);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
