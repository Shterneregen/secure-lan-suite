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
import com.shterneregen.securelan.filetransfer.event.FileTransferCompletedEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferStartedEvent;
import com.shterneregen.securelan.filetransfer.service.FileTransferClientRequest;
import com.shterneregen.securelan.filetransfer.service.FileTransferClientService;
import com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher;
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig;
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService;
import com.shterneregen.securelan.filetransfer.service.impl.DefaultFileTransferClientService;
import com.shterneregen.securelan.filetransfer.service.impl.DefaultFileTransferServerService;
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
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;

public class MainView {
    private final TextField hostField = new TextField("127.0.0.1");
    private final TextField portField = new TextField("5050");
    private final TextField nicknameField = new TextField("alice");
    private final TextField passwordField = new TextField("chatpass");
    private final TextField recipientField = new TextField("peer");
    private final TextField fileField = new TextField();
    private final TextField downloadsField = new TextField("downloads");
    private final TextArea logArea = new TextArea();
    private final TextField messageField = new TextField();
    private final Label statusLabel = new Label("Idle");
    private final Label fileStatusLabel = new Label("No file selected");

    private final ChatServerService serverService;
    private final ChatClientService clientService;
    private final FileTransferServerService fileTransferServerService;
    private final FileTransferClientService fileTransferClientService;

    public MainView() {
        ChatEventPublisher chatPublisher = this::handleChatEvent;
        FileTransferEventPublisher fileTransferPublisher = this::handleFileTransferEvent;
        this.serverService = new DefaultChatServerService(chatPublisher);
        this.clientService = new DefaultChatClientService(chatPublisher);
        this.fileTransferServerService = new DefaultFileTransferServerService(fileTransferPublisher);
        this.fileTransferClientService = new DefaultFileTransferClientService(fileTransferPublisher);
        fileField.setEditable(false);
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
        fileTransferServerService.stop();
    }

    private VBox buildTopPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Host"), hostField, new Label("Port"), portField);
        grid.addRow(1, new Label("Nickname"), nicknameField, new Label("Password"), passwordField);
        grid.addRow(2, new Label("Recipient"), recipientField, new Label("Downloads"), downloadsField);
        GridPane.setHgrow(hostField, Priority.ALWAYS);
        GridPane.setHgrow(nicknameField, Priority.ALWAYS);
        GridPane.setHgrow(recipientField, Priority.ALWAYS);
        GridPane.setHgrow(downloadsField, Priority.ALWAYS);

        Button startServerButton = new Button("Start Server");
        Button stopServerButton = new Button("Stop Server");
        Button connectButton = new Button("Connect");
        Button disconnectButton = new Button("Disconnect");

        startServerButton.setOnAction(event -> startServer());
        stopServerButton.setOnAction(event -> {
            serverService.stop();
            fileTransferServerService.stop();
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
                statusLabel,
                fileStatusLabel
        );
        box.setPadding(new Insets(0, 0, 12, 0));
        return box;
    }

    private VBox buildCenterPanel() {
        logArea.setEditable(false);
        logArea.setWrapText(true);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        VBox box = new VBox(8, new Label("Event Log"), logArea);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox buildBottomPanel() {
        Button sendButton = new Button("Send");
        sendButton.setOnAction(event -> sendMessage());
        messageField.setPromptText("Type a message...");
        messageField.setOnAction(event -> sendMessage());

        Button chooseFileButton = new Button("Choose File");
        Button sendFileButton = new Button("Send File");
        chooseFileButton.setOnAction(event -> chooseFile());
        sendFileButton.setOnAction(event -> sendFile());

        HBox messageRow = new HBox(10, messageField, sendButton);
        HBox.setHgrow(messageField, Priority.ALWAYS);
        HBox fileRow = new HBox(10, fileField, chooseFileButton, sendFileButton);
        HBox.setHgrow(fileField, Priority.ALWAYS);

        VBox box = new VBox(8,
                new Separator(),
                new Label("Message"),
                messageRow,
                new Label("File Transfer"),
                fileRow
        );
        box.setPadding(new Insets(12, 0, 0, 0));
        return box;
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            Path downloadsPath = Path.of(downloadsField.getText().trim()).toAbsolutePath().normalize();
            serverService.start(new ChatServerConfig(port, passwordField.getText()));
            fileTransferServerService.start(new FileTransferServerConfig(fileTransferPort(port), downloadsPath, passwordField.getText()));
            setStatus("Server running on chat port %d and file port %d".formatted(port, fileTransferPort(port)));
            setFileStatus("Incoming files -> " + downloadsPath);
            append("[ui] server started on port " + port);
            append("[ui] file transfer server started on port " + fileTransferPort(port));
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

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose file to send");
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            fileField.setText(file.getAbsolutePath());
            setFileStatus("Ready to send: " + file.getName());
        }
    }

    private void sendFile() {
        try {
            String filePath = fileField.getText().trim();
            if (filePath.isEmpty()) {
                showError("Choose a file first");
                return;
            }
            Path file = Path.of(filePath);
            int port = Integer.parseInt(portField.getText().trim());
            fileTransferClientService.sendFile(new FileTransferClientRequest(
                    hostField.getText().trim(),
                    fileTransferPort(port),
                    nicknameField.getText().trim(),
                    recipientField.getText().trim(),
                    passwordField.getText(),
                    file
            ));
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private int fileTransferPort(int basePort) {
        return basePort + 1;
    }

    private void handleChatEvent(ChatCoreEvent event) {
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

    private void handleFileTransferEvent(FileTransferEvent event) {
        Platform.runLater(() -> {
            if (event instanceof FileTransferStartedEvent e) {
                append((e.outgoing() ? "[file-send] " : "[file-recv] ") + "started: " + e.fileName() + " (" + e.totalBytes() + " bytes)");
                setFileStatus((e.outgoing() ? "Sending " : "Receiving ") + e.fileName());
            } else if (event instanceof FileTransferProgressEvent e) {
                append((e.outgoing() ? "[file-send] " : "[file-recv] ") + e.progress().percent() + "% - " + e.progress().transferredBytes() + "/" + e.progress().totalBytes());
                setFileStatus((e.outgoing() ? "Sending " : "Receiving ") + e.progress().percent() + "%");
            } else if (event instanceof FileTransferCompletedEvent e) {
                append((e.outgoing() ? "[file-send] " : "[file-recv] ") + "completed: " + e.path());
                setFileStatus("Completed: " + e.fileName());
            } else if (event instanceof FileTransferFailedEvent e) {
                append((e.outgoing() ? "[file-send] " : "[file-recv] ") + "failed: " + e.message());
                setFileStatus("Transfer failed");
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

    private void setFileStatus(String value) {
        fileStatusLabel.setText(value);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
