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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

public class MainView {
    private final TextField serverChatPortField = new TextField("5050");
    private final TextField serverFilePortField = new TextField("5051");
    private final TextField downloadsField = new TextField("downloads");

    private final TextField clientHostField = new TextField("127.0.0.1");
    private final TextField clientChatPortField = new TextField("5050");
    private final TextField clientFilePortField = new TextField("5051");
    private final TextField nicknameField = new TextField("alice");
    private final TextField serverPasswordField = new TextField("chatpass");
    private final TextField clientPasswordField = new TextField("chatpass");
    private final TextField fileHostField = new TextField("127.0.0.1");
    private final TextField fileSenderField = new TextField("alice");
    private final TextField recipientField = new TextField("peer");
    private final TextField fileField = new TextField();

    private final TextArea logArea = new TextArea();
    private final TextArea helpArea = new TextArea();
    private final TextField messageField = new TextField();

    private final Label serverStatusValue = new Label("Stopped");
    private final Label connectionStatusValue = new Label("Idle");
    private final Label transferStatusValue = new Label("Idle");

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
        syncPasswords();
        syncSharedClientFields();
        configureUiState();
        publishLocalNetworkInfo();
    }

    private void configureUiState() {
        fileField.setEditable(false);
        messageField.setPromptText("Type a message...");
        logArea.setEditable(false);
        logArea.setWrapText(true);
        helpArea.setEditable(false);
        helpArea.setWrapText(true);
        helpArea.setText(buildHelpText());
        styleStatusValue(serverStatusValue);
        styleStatusValue(connectionStatusValue);
        styleStatusValue(transferStatusValue);
    }

    private void publishLocalNetworkInfo() {
        try {
            List<String> localIps = resolveLocalLanIps();
            if (localIps.isEmpty()) {
                append("[info] local network IP is unavailable right now");
                return;
            }

            if (localIps.size() == 1) {
                append("[info] local network IP: " + localIps.getFirst());
            } else {
                append("[info] local network IPs: " + String.join(", ", localIps));
            }
        } catch (SocketException ex) {
            append("[info] failed to determine local network IP: " + ex.getMessage());
        }
    }

    private List<String> resolveLocalLanIps() throws SocketException {
        List<String> siteLocalAddresses = new ArrayList<>();
        List<String> otherNonLoopbackAddresses = new ArrayList<>();

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                continue;
            }

            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (!(inetAddress instanceof Inet4Address) || inetAddress.isLoopbackAddress()) {
                    continue;
                }

                String hostAddress = inetAddress.getHostAddress();
                if (inetAddress.isSiteLocalAddress()) {
                    siteLocalAddresses.add(hostAddress);
                } else {
                    otherNonLoopbackAddresses.add(hostAddress);
                }
            }
        }

        List<String> result = !siteLocalAddresses.isEmpty() ? siteLocalAddresses : otherNonLoopbackAddresses;
        result.sort(Comparator.naturalOrder());
        return result.stream().distinct().toList();
    }

    private void syncPasswords() {
        serverPasswordField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!clientPasswordField.getText().equals(newValue)) {
                clientPasswordField.setText(newValue);
            }
        });
        clientPasswordField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!serverPasswordField.getText().equals(newValue)) {
                serverPasswordField.setText(newValue);
            }
        });
    }


    private void syncSharedClientFields() {
        fileHostField.textProperty().bindBidirectional(clientHostField.textProperty());
        fileSenderField.textProperty().bindBidirectional(nicknameField.textProperty());
    }

    public Parent createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setTop(buildStatusBar());
        root.setCenter(buildMainTabs());
        return root;
    }

    public void shutdown() {
        clientService.disconnect();
        serverService.stop();
        fileTransferServerService.stop();
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(20,
                createStatusItem("Server", serverStatusValue),
                createStatusItem("Connection", connectionStatusValue),
                createStatusItem("File Transfer", transferStatusValue)
        );
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 12, 0));
        bar.setStyle("-fx-background-color: #f4f4f4; -fx-background-radius: 8; -fx-padding: 10 12 10 12;");
        return bar;
    }

    private HBox createStatusItem(String label, Label valueLabel) {
        Label titleLabel = new Label(label + ":");
        titleLabel.setStyle("-fx-font-weight: bold;");
        HBox box = new HBox(6, titleLabel, valueLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private TabPane buildMainTabs() {
        TabPane tabs = new TabPane();
        tabs.getTabs().add(createChatTab());
        tabs.getTabs().add(createFilesTab());
        tabs.getTabs().add(createServerTab());
        tabs.getTabs().add(createHelpTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabs;
    }

    private Tab createChatTab() {
        Button connectButton = new Button("Connect");
        Button disconnectButton = new Button("Disconnect");
        connectButton.setOnAction(event -> connectClient());
        disconnectButton.setOnAction(event -> clientService.disconnect());

        Button sendButton = new Button("Send");
        sendButton.setOnAction(event -> sendMessage());
        messageField.setOnAction(event -> sendMessage());

        HBox messageRow = new HBox(10, messageField, sendButton);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        VBox content = new VBox(10,
                createSection("Connection", buildClientConnectionPanel(), new HBox(10, connectButton, disconnectButton)),
                createSection("Chat", buildLogPanel(), null),
                createSection("Message", messageRow, null)
        );
        content.setPadding(new Insets(10));
        VBox.setVgrow(content.getChildren().get(1), Priority.ALWAYS);

        Tab tab = new Tab("Chat");
        tab.setContent(content);
        return tab;
    }

    private Tab createFilesTab() {
        Button chooseFileButton = new Button("Choose File");
        Button sendFileButton = new Button("Send File");
        chooseFileButton.setOnAction(event -> chooseFile());
        sendFileButton.setOnAction(event -> sendFile());

        HBox fileRow = new HBox(10, fileField, chooseFileButton, sendFileButton);
        HBox.setHgrow(fileField, Priority.ALWAYS);

        Label note = new Label("Files are sent to the remote host and remote file port shown below.");
        note.setWrapText(true);

        VBox content = new VBox(10,
                createSection("Transfer Target", buildFileTransferPanel(), null),
                createSection("Selected File", fileRow, null),
                createSection("Notes", note, null)
        );
        content.setPadding(new Insets(10));

        Tab tab = new Tab("File Transfer");
        tab.setContent(content);
        return tab;
    }

    private Tab createServerTab() {
        Button startServerButton = new Button("Start Server");
        Button stopServerButton = new Button("Stop Server");
        startServerButton.setOnAction(event -> startServer());
        stopServerButton.setOnAction(event -> stopServer());

        VBox content = new VBox(10,
                createSection("Local Server", buildServerPanel(), new HBox(10, startServerButton, stopServerButton)),
                createSection("Downloads", new Label("Incoming files are stored in the configured downloads folder on this machine."), null)
        );
        content.setPadding(new Insets(10));

        Tab tab = new Tab("Server");
        tab.setContent(content);
        return tab;
    }

    private Tab createHelpTab() {
        VBox content = new VBox(10, helpArea);
        content.setPadding(new Insets(10));
        VBox.setVgrow(helpArea, Priority.ALWAYS);

        Tab tab = new Tab("Help");
        tab.setContent(content);
        return tab;
    }

    private VBox createSection(String title, javafx.scene.Node content, javafx.scene.Node footer) {
        VBox box = new VBox(8);
        box.getChildren().add(new Label(title));
        box.getChildren().add(content);
        if (footer != null) {
            box.getChildren().add(footer);
        }
        return box;
    }

    private GridPane buildServerPanel() {
        GridPane grid = createFormGrid();
        grid.addRow(0, new Label("Chat port"), serverChatPortField, new Label("File port"), serverFilePortField);
        grid.addRow(1, new Label("Password"), serverPasswordField, new Label("Downloads"), downloadsField);
        growFields(serverChatPortField, serverFilePortField, serverPasswordField, downloadsField);
        return grid;
    }

    private GridPane buildClientConnectionPanel() {
        GridPane grid = createFormGrid();
        grid.addRow(0, new Label("Remote host"), clientHostField, new Label("Chat port"), clientChatPortField);
        grid.addRow(1, new Label("Nickname"), nicknameField, new Label("Password"), clientPasswordField);
        growFields(clientHostField, clientChatPortField, nicknameField, clientPasswordField);
        return grid;
    }

    private GridPane buildFileTransferPanel() {
        GridPane grid = createFormGrid();
        grid.addRow(0, new Label("Remote host"), fileHostField, new Label("File port"), clientFilePortField);
        grid.addRow(1, new Label("Sender"), fileSenderField, new Label("Recipient peer"), recipientField);
        growFields(fileHostField, clientFilePortField, fileSenderField, recipientField);
        return grid;
    }

    private VBox buildLogPanel() {
        VBox.setVgrow(logArea, Priority.ALWAYS);
        VBox box = new VBox(logArea);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        return grid;
    }

    private void growFields(Region... fields) {
        for (Region field : fields) {
            GridPane.setHgrow(field, Priority.ALWAYS);
        }
    }

    private void stopServer() {
        serverService.stop();
        fileTransferServerService.stop();
        setServerStatus("Stopped");
        setTransferStatus("Server stopped");
        append("[ui] server stopped");
    }

    private void startServer() {
        try {
            int chatPort = Integer.parseInt(serverChatPortField.getText().trim());
            int filePort = Integer.parseInt(serverFilePortField.getText().trim());
            Path downloadsPath = Path.of(downloadsField.getText().trim()).toAbsolutePath().normalize();
            serverService.start(new ChatServerConfig(chatPort, serverPasswordField.getText()));
            fileTransferServerService.start(new FileTransferServerConfig(filePort, downloadsPath, serverPasswordField.getText()));
            setServerStatus("Running (%d / %d)".formatted(chatPort, filePort));
            setTransferStatus("Waiting for transfers");
            append("[ui] chat server started on port " + chatPort);
            append("[ui] file transfer server started on port " + filePort);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void connectClient() {
        try {
            int port = Integer.parseInt(clientChatPortField.getText().trim());
            boolean connected = clientService.connect(new ChatClientConnectRequest(
                    clientHostField.getText().trim(),
                    port,
                    nicknameField.getText().trim(),
                    clientPasswordField.getText()
            ));
            if (!connected) {
                append("[ui] connection failed");
                setConnectionStatus("Failed");
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
            setTransferStatus("Ready: " + file.getName());
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
            int filePort = Integer.parseInt(clientFilePortField.getText().trim());
            fileTransferClientService.sendFile(new FileTransferClientRequest(
                    fileHostField.getText().trim(),
                    filePort,
                    fileSenderField.getText().trim(),
                    recipientField.getText().trim(),
                    clientPasswordField.getText(),
                    file
            ));
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleChatEvent(ChatCoreEvent event) {
        Platform.runLater(() -> {
            if (event instanceof ChatConnectedEvent e) {
                setConnectionStatus("Connected as " + e.nickname());
                append("[connected] " + e.nickname() + " -> " + e.remoteAddress());
            } else if (event instanceof ChatDisconnectedEvent e) {
                setConnectionStatus("Disconnected");
                append("[disconnected] " + e.nickname() + " - " + e.reason());
            } else if (event instanceof ChatMessageReceivedEvent e) {
                append(e.senderNickname() + ": " + e.text());
            } else if (event instanceof ChatMessageSentEvent ignored) {
                // The message will be shown once when it comes through the regular chat flow.
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
                setTransferStatus((e.outgoing() ? "Sending " : "Receiving ") + e.fileName());
            } else if (event instanceof FileTransferProgressEvent e) {
                append((e.outgoing() ? "[file-send] " : "[file-recv] ") + e.progress().percent() + "% - " + e.progress().transferredBytes() + "/" + e.progress().totalBytes());
                setTransferStatus((e.outgoing() ? "Sending " : "Receiving ") + e.progress().percent() + "%");
            } else if (event instanceof FileTransferCompletedEvent e) {
                append((e.outgoing() ? "[file-send] " : "[file-recv] ") + "completed: " + e.path());
                setTransferStatus("Completed: " + e.fileName());
            } else if (event instanceof FileTransferFailedEvent e) {
                append((e.outgoing() ? "[file-send] " : "[file-recv] ") + "failed: " + e.message());
                setTransferStatus("Transfer failed");
            }
        });
    }

    private void append(String line) {
        logArea.appendText(line + System.lineSeparator());
        logArea.positionCaret(logArea.getLength());
    }

    private void setServerStatus(String value) {
        serverStatusValue.setText(value);
    }

    private void setConnectionStatus(String value) {
        connectionStatusValue.setText(value);
    }

    private void setTransferStatus(String value) {
        transferStatusValue.setText(value);
    }

    private void styleStatusValue(Label label) {
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #1f6feb;");
    }

    private String buildHelpText() {
        return String.join(System.lineSeparator(),
                "Secure LAN Suite overview",
                "",
                "Server tab",
                "- Start Server launches the local chat server and the local file server on this machine.",
                "- Chat port is used for chat messages and handshake.",
                "- File port is used for file uploads.",
                "- Downloads is the folder where incoming files are saved.",
                "",
                "Chat tab",
                "- Remote host is the IP address or host name of the machine running the server.",
                "- Connect joins the remote chat server with the nickname and password shown in the form.",
                "- The event log shows chat activity, joins, leaves, transfer events, and errors.",
                "",
                "File Transfer tab",
                "- Choose File selects the file to send.",
                "- Recipient peer is the logical recipient name included in transfer metadata.",
                "- The file is sent to the remote host and file port from the transfer form.",
                "",
                "Typical LAN scenario",
                "1. Start the server on one computer.",
                "2. On another computer, enter the server IP in Remote host.",
                "3. Use the same password on both sides.",
                "4. Connect in the Chat tab.",
                "5. Send messages in Chat and files in File Transfer.");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
