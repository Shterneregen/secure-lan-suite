package com.shterneregen.securelan.desktop.ui;

import com.shterneregen.securelan.audio.service.AudioCallProfile;
import com.shterneregen.securelan.audio.service.AudioProfileService;
import com.shterneregen.securelan.audio.service.impl.DefaultAudioProfileService;
import com.shterneregen.securelan.chat.event.ChatConnectedEvent;
import com.shterneregen.securelan.chat.event.ChatCoreEvent;
import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent;
import com.shterneregen.securelan.chat.event.ChatErrorEvent;
import com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent;
import com.shterneregen.securelan.chat.event.ChatMessageSentEvent;
import com.shterneregen.securelan.chat.event.ChatSignalReceivedEvent;
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent;
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent;
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest;
import com.shterneregen.securelan.chat.service.ChatClientService;
import com.shterneregen.securelan.chat.service.ChatEventPublisher;
import com.shterneregen.securelan.chat.service.ChatServerConfig;
import com.shterneregen.securelan.chat.service.ChatServerService;
import com.shterneregen.securelan.chat.service.impl.DefaultChatClientService;
import com.shterneregen.securelan.chat.service.impl.DefaultChatServerService;
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode;
import com.shterneregen.securelan.common.model.rtc.RtcSessionState;
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
import com.shterneregen.securelan.webcam.service.VideoCallProfile;
import com.shterneregen.securelan.webcam.service.VideoProfileService;
import com.shterneregen.securelan.webcam.service.impl.DefaultVideoProfileService;
import com.shterneregen.securelan.webrtc.event.RtcAudioLevelEvent;
import com.shterneregen.securelan.webrtc.event.RtcDataMessageEvent;
import com.shterneregen.securelan.webrtc.event.RtcEvent;
import com.shterneregen.securelan.webrtc.event.RtcRuntimeWarningEvent;
import com.shterneregen.securelan.webrtc.event.RtcStateChangedEvent;
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent;
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus;
import com.shterneregen.securelan.webrtc.service.RtcSessionRequest;
import com.shterneregen.securelan.webrtc.service.RtcSessionService;
import com.shterneregen.securelan.webrtc.service.impl.DefaultRtcSessionService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final TextField rtcPeerField = new TextField("peer");
    private final TextField rtcDataChannelField = new TextField("securelan-data");
    private final TextField rtcMessageField = new TextField();

    private final TextArea logArea = new TextArea();
    private final TextArea diagnosticsArea = new TextArea();
    private final TextField messageField = new TextField();

    private final Circle serverIndicator = new Circle(5, Color.web("#9aa4b2"));
    private final Circle connectionIndicator = new Circle(5, Color.web("#9aa4b2"));
    private final Circle peerIndicator = new Circle(5, Color.web("#9aa4b2"));
    private final Circle voiceIndicator = new Circle(5, Color.web("#9aa4b2"));
    private final Circle transferIndicator = new Circle(5, Color.web("#9aa4b2"));

    private final Label serverStatusValue = new Label("Server stopped");
    private final Label connectionStatusValue = new Label("Connection idle");
    private final Label peerStatusValue = new Label("Peer not selected");
    private final Label voiceStatusValue = new Label("Voice idle");
    private final Label voicePanelStatusValue = new Label("Voice idle");
    private final Label transferStatusValue = new Label("Transfers idle");

    private final Label conversationTitleValue = new Label("Shared room activity");
    private final Label conversationSubtitleValue = new Label("Select a peer on the left for voice and file actions.");
    private final Label selectedPeerTitleValue = new Label("No peer selected");
    private final Label selectedPeerMetaValue = new Label("Choose an online peer to send files or start voice.");
    private final Label peersTitleValue = new Label("Contacts / Peers");
    private final Label peersHintValue = new Label("Select an online peer to target voice calls and file transfers.");
    private final Label realtimeRuntimeValue = new Label("Checking runtime");
    private final Label audioProfileValue = new Label();
    private final Label videoProfileValue = new Label();
    private final Label localAudioStatusValue = new Label("Idle");
    private final Label remoteAudioStatusValue = new Label("Idle");
    private final Label transferHintValue = new Label("No transfers yet");

    private final ProgressBar localAudioLevelBar = new ProgressBar(0);
    private final ProgressBar remoteAudioLevelBar = new ProgressBar(0);

    private final ImageView localVideoView = new ImageView();
    private final ImageView remoteVideoView = new ImageView();

    private WritableImage localVideoImage;
    private WritableImage remoteVideoImage;

    private final ObservableList<PeerPresence> peerItems = FXCollections.observableArrayList();
    private final ListView<PeerPresence> peerListView = new ListView<>(peerItems);
    private final ObservableList<TransferEntry> transferItems = FXCollections.observableArrayList();
    private final ListView<TransferEntry> transferListView = new ListView<>(transferItems);
    private final Map<String, TransferEntry> transferEntries = new LinkedHashMap<>();

    private final Button sendQuickActionButton = new Button("Send message");
    private final Button sendFileQuickActionButton = new Button("Send file");
    private final Button startVoiceQuickActionButton = new Button("Start voice");
    private final Button hangUpQuickActionButton = new Button("Hang up");
    private final Button startServerButton = new Button("Start server");
    private final Button stopServerButton = new Button("Stop server");
    private final Button connectButton = new Button("Connect");
    private final Button disconnectButton = new Button("Disconnect");
    private final Button sendMessageButton = new Button("Send");
    private final Button startDataButton = new Button("Start data");
    private final Button sendRtcMessageButton = new Button("Send RTC message");
    private final ToggleButton themeToggleButton = new ToggleButton("Dark theme");

    private BorderPane root;

    private final ChatServerService serverService;
    private final ChatClientService clientService;
    private final FileTransferServerService fileTransferServerService;
    private final FileTransferClientService fileTransferClientService;
    private final RtcSessionService rtcSessionService;
    private final AudioProfileService audioProfileService;
    private final VideoProfileService videoProfileService;

    public MainView() {
        ChatEventPublisher chatPublisher = this::handleChatEvent;
        FileTransferEventPublisher fileTransferPublisher = this::handleFileTransferEvent;
        this.serverService = new DefaultChatServerService(chatPublisher);
        this.clientService = new DefaultChatClientService(chatPublisher);
        this.fileTransferServerService = new DefaultFileTransferServerService(fileTransferPublisher);
        this.fileTransferClientService = new DefaultFileTransferClientService(fileTransferPublisher);
        this.audioProfileService = new DefaultAudioProfileService();
        this.videoProfileService = new DefaultVideoProfileService();
        this.rtcSessionService = new DefaultRtcSessionService(this::handleRtcEvent, clientService::sendSignal);
        syncPasswords();
        syncSharedClientFields();
        configureUiState();
        wireActions();
        publishLocalNetworkInfo();
        publishRealtimeProfiles();
        publishRealtimeRuntimeStatus();
    }

    public Parent createContent() {
        root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(12));
        root.setTop(new VBox(12, buildStatusBar(), buildConnectionWorkspaceHeader()));
        root.setCenter(buildWorkspace());
        applyTheme();
        return root;
    }

    public void shutdown() {
        rtcSessionService.close();
        clientService.disconnect();
        serverService.stop();
        fileTransferServerService.stop();
    }

    private void configureUiState() {
        fileFieldSetup();
        themeToggleButton.setSelected(true);
        themeToggleButton.setFocusTraversable(false);
        themeToggleButton.getStyleClass().addAll("compact-toggle", "theme-toggle");
        messageField.setPromptText("Type a message for the shared chat...");
        rtcMessageField.setPromptText("Optional RTCDataChannel message...");
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.getStyleClass().add("chat-log-area");
        diagnosticsArea.setEditable(false);
        diagnosticsArea.setWrapText(true);
        diagnosticsArea.setPrefRowCount(10);
        diagnosticsArea.getStyleClass().addAll("chat-log-area", "mono-area");
        conversationTitleValue.getStyleClass().add("page-title");
        conversationSubtitleValue.getStyleClass().add("muted-label");
        selectedPeerTitleValue.getStyleClass().add("section-heading");
        selectedPeerMetaValue.setWrapText(true);
        selectedPeerMetaValue.getStyleClass().add("muted-label");
        peersTitleValue.getStyleClass().add("section-heading");
        peersHintValue.setWrapText(true);
        peersHintValue.getStyleClass().add("muted-label");
        styleStatusLabel(serverStatusValue);
        styleStatusLabel(connectionStatusValue);
        styleStatusLabel(peerStatusValue);
        styleStatusLabel(voiceStatusValue);
        voicePanelStatusValue.setWrapText(true);
        voicePanelStatusValue.getStyleClass().addAll("subtle-label", "status-value");
        styleStatusLabel(transferStatusValue);
        realtimeRuntimeValue.setWrapText(true);
        realtimeRuntimeValue.getStyleClass().add("accent-label");
        audioProfileValue.getStyleClass().add("subtle-label");
        videoProfileValue.getStyleClass().add("subtle-label");
        transferHintValue.setWrapText(true);
        transferHintValue.getStyleClass().add("muted-label");
        configureAudioLevelBar(localAudioLevelBar);
        configureAudioLevelBar(remoteAudioLevelBar);
        configureMediaStatusLabel(localAudioStatusValue);
        configureMediaStatusLabel(remoteAudioStatusValue);
        configureVideoView(localVideoView);
        configureVideoView(remoteVideoView);
        styleInteractiveControls();
        peerListView.getStyleClass().add("content-list");
        transferListView.getStyleClass().add("content-list");
        peerListView.setPlaceholder(createMutedLabel("Peers will appear here when they join the chat."));
        peerListView.setCellFactory(list -> new PeerCell());
        transferListView.setPlaceholder(createMutedLabel("Transfers will appear here."));
        transferListView.setCellFactory(list -> new TransferCell());
        peerListView.getSelectionModel().selectedItemProperty().addListener((obs, oldPeer, newPeer) -> updateSelectedPeer(newPeer));
        themeToggleButton.selectedProperty().addListener((obs, oldValue, newValue) -> applyTheme());
        updateSelectedPeer(null);
        updateQuickActionState();
    }

    private void wireActions() {
        startServerButton.setOnAction(event -> startServer());
        stopServerButton.setOnAction(event -> stopServer());
        connectButton.setOnAction(event -> connectClient());
        disconnectButton.setOnAction(event -> clientService.disconnect());
        sendMessageButton.setOnAction(event -> sendMessage());
        messageField.setOnAction(event -> sendMessage());
        sendQuickActionButton.setOnAction(event -> {
            if (messageField.getText().isBlank()) {
                messageField.requestFocus();
            } else {
                sendMessage();
            }
        });
        sendFileQuickActionButton.setOnAction(event -> chooseAndSendFileForSelectedPeer());
        startVoiceQuickActionButton.setOnAction(event -> startRealtimeSession(RtcSessionMode.AUDIO));
        hangUpQuickActionButton.setOnAction(event -> rtcSessionService.closeCurrentSession());
        startDataButton.setOnAction(event -> startRealtimeSession(RtcSessionMode.DATA));
        sendRtcMessageButton.setOnAction(event -> sendRtcMessage());
        rtcMessageField.setOnAction(event -> sendRtcMessage());
    }

    private void fileFieldSetup() {
        fileHostField.setEditable(false);
        fileSenderField.setEditable(false);
        recipientField.setEditable(false);
        rtcPeerField.setEditable(false);
    }

    private void publishRealtimeProfiles() {
        AudioCallProfile audioProfile = audioProfileService.defaultProfile();
        VideoCallProfile videoProfile = videoProfileService.defaultProfile();
        audioProfileValue.setText("Audio: %d Hz, %d ch, echo cancel=%s, noise suppression=%s"
                .formatted(audioProfile.sampleRateHz(), audioProfile.channels(), audioProfile.echoCancellation(), audioProfile.noiseSuppression()));
        videoProfileValue.setText("Video hidden in main UI. Default profile remains %dx%d @ %d FPS."
                .formatted(videoProfile.width(), videoProfile.height(), videoProfile.framesPerSecond()));
    }

    private void publishRealtimeRuntimeStatus() {
        RtcRuntimeStatus status = rtcSessionService.runtimeStatus();
        refreshRealtimeRuntimeValue(status);
        appendDiagnostics("[rtc] runtime: " + status.providerName() + " - " + status.message());
        appendChat("[rtc] runtime: " + status.providerName() + " - " + status.message());
    }

    private void publishLocalNetworkInfo() {
        try {
            List<String> localIps = resolveLocalLanIps();
            if (localIps.isEmpty()) {
                appendChat("[info] local network IP is unavailable right now");
                return;
            }

            if (localIps.size() == 1) {
                appendChat("[info] local network IP: " + localIps.getFirst());
            } else {
                appendChat("[info] local network IPs: " + String.join(", ", localIps));
            }
        } catch (SocketException ex) {
            appendChat("[info] failed to determine local network IP: " + ex.getMessage());
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

    private Node buildStatusBar() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(12,
                createStatusChip(serverIndicator, serverStatusValue),
                createStatusChip(connectionIndicator, connectionStatusValue),
                createStatusChip(peerIndicator, peerStatusValue),
                createStatusChip(voiceIndicator, voiceStatusValue),
                createStatusChip(transferIndicator, transferStatusValue),
                spacer,
                themeToggleButton
        );
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private Node createStatusChip(Circle indicator, Label valueLabel) {
        HBox chip = new HBox(8, indicator, valueLabel);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(6, 10, 6, 10));
        chip.getStyleClass().add("status-chip");
        return chip;
    }

    private Node buildConnectionWorkspaceHeader() {
        HBox row = new HBox(12,
                createCard("Local server", buildServerQuickPanel()),
                createCard("Connection", buildClientConnectionQuickPanel())
        );
        HBox.setHgrow((Node) row.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow((Node) row.getChildren().get(1), Priority.ALWAYS);
        return row;
    }

    private Node buildServerQuickPanel() {
        GridPane grid = createCompactFormGrid();
        grid.addRow(0, new Label("Chat"), serverChatPortField, new Label("File"), serverFilePortField);
        grid.addRow(1, new Label("Password"), serverPasswordField, new Label("Downloads"), downloadsField);
        HBox actions = new HBox(8, startServerButton, stopServerButton);
        VBox box = new VBox(10, grid, actions);
        growFields(serverChatPortField, serverFilePortField, serverPasswordField, downloadsField);
        return box;
    }

    private Node buildClientConnectionQuickPanel() {
        GridPane grid = createCompactFormGrid();
        grid.addRow(0, new Label("Host"), clientHostField, new Label("Port"), clientChatPortField);
        grid.addRow(1, new Label("Nickname"), nicknameField, new Label("Password"), clientPasswordField);
        HBox actions = new HBox(8, connectButton, disconnectButton);
        VBox box = new VBox(10, grid, actions);
        growFields(clientHostField, clientChatPortField, nicknameField, clientPasswordField);
        return box;
    }

    private Node buildWorkspace() {
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(buildPeersColumn(), buildConversationColumn(), buildActionsColumn());
        splitPane.setDividerPositions(0.20, 0.72);
        splitPane.getStyleClass().add("workspace-split-pane");
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        return splitPane;
    }

    private Node buildPeersColumn() {
        VBox.setVgrow(peerListView, Priority.ALWAYS);
        VBox content = new VBox(10, peersTitleValue, peersHintValue, peerListView);
        VBox.setVgrow(content, Priority.ALWAYS);
        return createCard("Peers", content);
    }

    private Node buildConversationColumn() {
        VBox.setVgrow(logArea, Priority.ALWAYS);
        HBox messageRow = new HBox(10, messageField, sendMessageButton);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        VBox content = new VBox(10,
                conversationTitleValue,
                conversationSubtitleValue,
                logArea,
                messageRow
        );
        VBox.setVgrow(logArea, Priority.ALWAYS);
        return createCard("Chat", content);
    }

    private Node buildActionsColumn() {
        sendQuickActionButton.setMaxWidth(Double.MAX_VALUE);
        sendFileQuickActionButton.setMaxWidth(Double.MAX_VALUE);
        startVoiceQuickActionButton.setMaxWidth(Double.MAX_VALUE);
        hangUpQuickActionButton.setMaxWidth(Double.MAX_VALUE);
        startDataButton.setMaxWidth(Double.MAX_VALUE);
        sendRtcMessageButton.setMaxWidth(Double.MAX_VALUE);

        VBox quickActions = new VBox(8,
                sendQuickActionButton,
                sendFileQuickActionButton,
                startVoiceQuickActionButton,
                hangUpQuickActionButton
        );

        VBox voiceBlock = new VBox(8,
                voicePanelStatusValue,
                createMetricBlock("Local microphone", new VBox(6, localAudioLevelBar, localAudioStatusValue)),
                createMetricBlock("Remote audio", new VBox(6, remoteAudioLevelBar, remoteAudioStatusValue))
        );

        VBox transfersBlock = new VBox(8, transferHintValue, transferListView);
        VBox.setVgrow(transferListView, Priority.ALWAYS);

        VBox advancedContent = new VBox(10,
                createSectionHeadingLabel("Runtime"),
                realtimeRuntimeValue,
                audioProfileValue,
                videoProfileValue,
                new Separator(),
                createSectionHeadingLabel("RTCDataChannel"),
                createMutedLabel("Use this for experiments and diagnostics. Video controls are hidden from the main UI until the feature becomes stable."),
                startDataButton,
                rtcMessageField,
                sendRtcMessageButton,
                new Separator(),
                createSectionHeadingLabel("Diagnostics"),
                diagnosticsArea
        );
        VBox.setVgrow(diagnosticsArea, Priority.ALWAYS);

        TitledPane advancedPane = new TitledPane("Advanced / Experimental", advancedContent);
        advancedPane.getStyleClass().add("advanced-pane");
        advancedPane.setExpanded(false);
        advancedPane.setAnimated(false);

        Node quickActionsCard = createSectionCard("Quick actions", quickActions);
        Node voiceCard = createSectionCard("Voice status", voiceBlock);
        Node transfersCard = createSectionCard("Transfers", transfersBlock);

        VBox content = new VBox(12,
                selectedPeerTitleValue,
                selectedPeerMetaValue,
                quickActionsCard,
                voiceCard,
                transfersCard,
                advancedPane
        );
        VBox.setVgrow(transfersCard, Priority.ALWAYS);
        VBox.setVgrow(transferListView, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("actions-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return createCard("Actions", scrollPane);
    }

    private Node createMetricBlock(String title, Node content) {
        VBox box = new VBox(6, createMetricTitleLabel(title), content);
        return box;
    }

    private Node createSectionCard(String title, Node content) {
        VBox box = new VBox(8, sectionTitle(title), content);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("section-card");
        return box;
    }

    private Label sectionTitle(String title) {
        return createSectionHeadingLabel(title);
    }

    private Node createCard(String title, Node content) {
        Label header = new Label(title);
        header.getStyleClass().add("card-title");
        VBox box = new VBox(10, header, content);
        box.setPadding(new Insets(14));
        box.getStyleClass().add("panel-card");
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }


    private Label createSectionHeadingLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Label createMetricTitleLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("metric-title");
        return label;
    }

    private Label createMutedLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("muted-label");
        return label;
    }

    private void styleInteractiveControls() {
        applyButtonVariant(startServerButton, "primary-button");
        applyButtonVariant(stopServerButton, "danger-button");
        applyButtonVariant(connectButton, "primary-button");
        applyButtonVariant(disconnectButton, "secondary-button");
        applyButtonVariant(sendMessageButton, "primary-button");
        applyButtonVariant(sendQuickActionButton, "primary-button");
        applyButtonVariant(sendFileQuickActionButton, "secondary-button");
        applyButtonVariant(startVoiceQuickActionButton, "primary-button");
        applyButtonVariant(hangUpQuickActionButton, "danger-button");
        applyButtonVariant(startDataButton, "secondary-button");
        applyButtonVariant(sendRtcMessageButton, "secondary-button");
    }

    private void applyButtonVariant(Button button, String variantClass) {
        button.getStyleClass().addAll("app-button", variantClass);
        button.setMaxWidth(Double.MAX_VALUE);
    }

    private void applyTheme() {
        if (root == null) {
            return;
        }
        root.getStylesheets().setAll(
                stylesheet("/styles/base.css"),
                stylesheet(themeToggleButton.isSelected() ? "/styles/dark-theme.css" : "/styles/light-theme.css")
        );
        peerListView.refresh();
        transferListView.refresh();
    }

    private String stylesheet(String path) {
        return Objects.requireNonNull(getClass().getResource(path), "Missing stylesheet: " + path).toExternalForm();
    }

    private GridPane createCompactFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
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
        setServerStatus("Server stopped", Color.web("#9aa4b2"));
        setTransferStatus("Transfers idle", Color.web("#9aa4b2"));
        appendChat("[ui] server stopped");
    }

    private void startServer() {
        try {
            int chatPort = Integer.parseInt(serverChatPortField.getText().trim());
            int filePort = Integer.parseInt(serverFilePortField.getText().trim());
            Path downloadsPath = Path.of(downloadsField.getText().trim()).toAbsolutePath().normalize();
            serverService.start(new ChatServerConfig(chatPort, serverPasswordField.getText()));
            fileTransferServerService.start(new FileTransferServerConfig(filePort, downloadsPath, serverPasswordField.getText()));
            setServerStatus("Server running", Color.web("#1f9d55"));
            setTransferStatus("Transfers idle", Color.web("#9aa4b2"));
            appendChat("[ui] chat server started on port " + chatPort);
            appendChat("[ui] file transfer server started on port " + filePort);
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
                appendChat("[ui] connection failed");
                setConnectionStatus("Connection failed", Color.web("#dc2626"));
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

    private void chooseAndSendFileForSelectedPeer() {
        PeerPresence peer = peerListView.getSelectionModel().getSelectedItem();
        if (peer == null || !peer.online()) {
            showError("Select an online peer first");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose file to send to " + peer.nickname());
        File file = chooser.showOpenDialog(null);
        if (file == null) {
            return;
        }

        recipientField.setText(peer.nickname());
        rtcPeerField.setText(peer.nickname());

        try {
            int filePort = Integer.parseInt(clientFilePortField.getText().trim());
            fileTransferClientService.sendFile(new FileTransferClientRequest(
                    fileHostField.getText().trim(),
                    filePort,
                    fileSenderField.getText().trim(),
                    peer.nickname(),
                    clientPasswordField.getText(),
                    file.toPath()
            ));
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void startRealtimeSession(RtcSessionMode mode) {
        PeerPresence peer = peerListView.getSelectionModel().getSelectedItem();
        if (peer == null || !peer.online()) {
            showError("Select an online peer first");
            return;
        }

        try {
            recipientField.setText(peer.nickname());
            rtcPeerField.setText(peer.nickname());
            clearRealtimeMediaUi();
            rtcSessionService.startSession(new RtcSessionRequest(
                    nicknameField.getText().trim(),
                    peer.nickname(),
                    mode,
                    rtcDataChannelField.getText().trim()
            ));
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void sendRtcMessage() {
        String payload = rtcMessageField.getText().trim();
        if (payload.isEmpty()) {
            return;
        }
        rtcSessionService.sendDataMessage(payload);
        rtcMessageField.clear();
    }

    private void handleChatEvent(ChatCoreEvent event) {
        Platform.runLater(() -> {
            if (event instanceof ChatConnectedEvent e) {
                setConnectionStatus("Connected as " + e.nickname(), Color.web("#1f9d55"));
                appendChat("[connected] " + e.nickname() + " -> " + e.remoteAddress());
                peerItems.clear();
                updateSelectedPeer(null);
            } else if (event instanceof ChatDisconnectedEvent e) {
                setConnectionStatus("Connection idle", Color.web("#9aa4b2"));
                setPeerStatus("Peer not selected", Color.web("#9aa4b2"));
                appendChat("[disconnected] " + e.nickname() + " - " + e.reason());
                peerItems.clear();
                updateSelectedPeer(null);
            } else if (event instanceof ChatMessageReceivedEvent e) {
                upsertPeer(e.senderNickname(), true);
                appendChat(e.senderNickname() + ": " + e.text());
            } else if (event instanceof ChatMessageSentEvent ignored) {
                // message appears once via normal chat flow
            } else if (event instanceof ChatUserJoinedEvent e) {
                if (!e.nickname().equalsIgnoreCase(nicknameField.getText().trim())) {
                    PeerPresence peer = upsertPeer(e.nickname(), true);
                    appendChat("[join] " + e.nickname());
                    if (peerListView.getSelectionModel().getSelectedItem() == null) {
                        peerListView.getSelectionModel().select(peer);
                    }
                }
            } else if (event instanceof ChatUserLeftEvent e) {
                markPeerOffline(e.nickname());
                appendChat("[left] " + e.nickname());
            } else if (event instanceof ChatSignalReceivedEvent e) {
                upsertPeer(e.signal().fromPeer(), true);
                appendDiagnostics("[rtc-signal] " + e.signal().type() + " from " + e.signal().fromPeer() + " to " + e.signal().toPeer());
                rtcSessionService.acceptInboundSignal(nicknameField.getText().trim(), e.signal());
            } else if (event instanceof ChatErrorEvent e) {
                appendChat("[error] " + e.message() + (e.cause() != null ? " -> " + e.cause().getMessage() : ""));
                appendDiagnostics("[error] " + e.message() + (e.cause() != null ? " -> " + e.cause().getMessage() : ""));
            }
        });
    }

    private void handleFileTransferEvent(FileTransferEvent event) {
        Platform.runLater(() -> {
            if (event instanceof FileTransferStartedEvent e) {
                TransferEntry entry = new TransferEntry(e.transferId(), e.fileName(), e.outgoing() ? "Sending" : "Receiving", 0, e.totalBytes());
                transferEntries.put(e.transferId(), entry);
                refreshTransferEntries();
                appendChat((e.outgoing() ? "[file-send] " : "[file-recv] ") + "started: " + e.fileName());
                setTransferStatus(activeTransferSummary(), Color.web("#f59e0b"));
            } else if (event instanceof FileTransferProgressEvent e) {
                TransferEntry existing = transferEntries.get(e.transferId());
                if (existing != null) {
                    existing.status = e.outgoing() ? "Sending" : "Receiving";
                    existing.percent = e.progress().percent();
                    existing.totalBytes = e.progress().totalBytes();
                    refreshTransferEntries();
                }
                setTransferStatus(activeTransferSummary(), Color.web("#f59e0b"));
            } else if (event instanceof FileTransferCompletedEvent e) {
                TransferEntry entry = transferEntries.computeIfAbsent(e.transferId(), id -> new TransferEntry(id, e.fileName(), "Completed", 100, e.totalBytes()));
                entry.status = "Completed";
                entry.percent = 100;
                entry.totalBytes = e.totalBytes();
                refreshTransferEntries();
                appendChat((e.outgoing() ? "[file-send] " : "[file-recv] ") + "completed: " + e.path());
                setTransferStatus(activeTransferSummary(), transferEntries.values().stream().anyMatch(TransferEntry::active) ? Color.web("#f59e0b") : Color.web("#1f9d55"));
            } else if (event instanceof FileTransferFailedEvent e) {
                TransferEntry entry = transferEntries.computeIfAbsent(e.transferId(), id -> new TransferEntry(id, e.fileName(), "Failed", 0, 0));
                entry.status = "Failed";
                refreshTransferEntries();
                appendChat((e.outgoing() ? "[file-send] " : "[file-recv] ") + "failed: " + e.message());
                setTransferStatus(activeTransferSummary(), transferEntries.values().stream().anyMatch(TransferEntry::active) ? Color.web("#f59e0b") : Color.web("#dc2626"));
            }
        });
    }

    private void handleRtcEvent(RtcEvent event) {
        Platform.runLater(() -> {
            if (event instanceof RtcStateChangedEvent e) {
                appendChat("[rtc] " + e.mode() + " session " + e.state() + " with " + e.remotePeer() + " - " + e.message());
                appendDiagnostics("[rtc] " + e.mode() + " session " + e.state() + " with " + e.remotePeer() + " - " + e.message());
                refreshRealtimeRuntimeValue(rtcSessionService.runtimeStatus());
                upsertPeer(e.remotePeer(), true);
                updateVoiceStatusFromRtc(e);
                if (e.state() == RtcSessionState.CLOSED
                        || e.state() == RtcSessionState.FAILED
                        || e.state() == RtcSessionState.UNAVAILABLE) {
                    clearRealtimeMediaUi();
                }
            } else if (event instanceof RtcRuntimeWarningEvent e) {
                appendDiagnostics("[rtc-warning] " + e.message());
                refreshRealtimeRuntimeValue(rtcSessionService.runtimeStatus());
            } else if (event instanceof RtcDataMessageEvent e) {
                appendChat((e.outgoing() ? "[rtc-send] " : "[rtc-recv] ") + e.payload());
                appendDiagnostics((e.outgoing() ? "[rtc-send] " : "[rtc-recv] ") + e.payload());
            } else if (event instanceof RtcVideoFrameEvent e) {
                updateVideoPreview(e);
            } else if (event instanceof RtcAudioLevelEvent e) {
                updateAudioLevel(e);
            }
        });
    }

    private void appendChat(String line) {
        logArea.appendText(line + System.lineSeparator());
        logArea.positionCaret(logArea.getLength());
    }

    private void appendDiagnostics(String line) {
        diagnosticsArea.appendText(line + System.lineSeparator());
        diagnosticsArea.positionCaret(diagnosticsArea.getLength());
    }

    private void refreshRealtimeRuntimeValue(RtcRuntimeStatus status) {
        if (status == null) {
            realtimeRuntimeValue.setText("Unavailable");
            return;
        }
        realtimeRuntimeValue.setText(status.available()
                ? status.providerName() + " ready"
                : "Unavailable — " + status.message());
    }

    private void updateVideoPreview(RtcVideoFrameEvent event) {
        if (event.local()) {
            localVideoImage = applyVideoFrame(localVideoView, localVideoImage, event);
        } else {
            remoteVideoImage = applyVideoFrame(remoteVideoView, remoteVideoImage, event);
        }
    }

    private WritableImage applyVideoFrame(ImageView view, WritableImage target, RtcVideoFrameEvent event) {
        if (target == null || (int) target.getWidth() != event.width() || (int) target.getHeight() != event.height()) {
            target = new WritableImage(event.width(), event.height());
        }
        target.getPixelWriter().setPixels(
                0,
                0,
                event.width(),
                event.height(),
                PixelFormat.getIntArgbInstance(),
                event.argbPixels(),
                0,
                event.width()
        );
        view.setImage(target);
        view.setRotate(event.rotation());
        return target;
    }

    private void updateAudioLevel(RtcAudioLevelEvent event) {
        ProgressBar bar = event.local() ? localAudioLevelBar : remoteAudioLevelBar;
        Label label = event.local() ? localAudioStatusValue : remoteAudioStatusValue;
        bar.setProgress(Math.max(0d, Math.min(1d, event.level())));
        String peerInfo = event.local() ? "local microphone" : event.peer();
        label.setText((event.active() ? "Active" : "Quiet") + " — " + peerInfo + " — " + Math.round(event.level() * 100) + "%");
    }

    private void clearRealtimeMediaUi() {
        localVideoView.setImage(null);
        remoteVideoView.setImage(null);
        localVideoView.setRotate(0);
        remoteVideoView.setRotate(0);
        localVideoImage = null;
        remoteVideoImage = null;
        localAudioLevelBar.setProgress(0);
        remoteAudioLevelBar.setProgress(0);
        localAudioStatusValue.setText("Idle");
        remoteAudioStatusValue.setText("Idle");
    }

    private void updateVoiceStatusFromRtc(RtcStateChangedEvent event) {
        if (!event.mode().audioEnabled()) {
            return;
        }

        String peer = event.remotePeer() == null || event.remotePeer().isBlank() ? "peer" : event.remotePeer();
        if (event.state() == RtcSessionState.CONNECTED) {
            setVoiceStatus("In call with " + peer, Color.web("#1f9d55"));
        } else if (event.state() == RtcSessionState.CONNECTING || event.state() == RtcSessionState.NEGOTIATING) {
            setVoiceStatus("Voice connecting", Color.web("#f59e0b"));
        } else if (event.state() == RtcSessionState.CLOSING) {
            setVoiceStatus("Voice closing", Color.web("#f59e0b"));
        } else if (event.state() == RtcSessionState.CLOSED) {
            setVoiceStatus("Voice idle", Color.web("#9aa4b2"));
        } else if (event.state() == RtcSessionState.FAILED || event.state() == RtcSessionState.UNAVAILABLE) {
            setVoiceStatus("Voice unavailable", Color.web("#dc2626"));
        }
    }

    private void updateQuickActionState() {
        PeerPresence selectedPeer = peerListView.getSelectionModel().getSelectedItem();
        boolean hasOnlinePeer = selectedPeer != null && selectedPeer.online();
        sendFileQuickActionButton.setDisable(!hasOnlinePeer);
        startVoiceQuickActionButton.setDisable(!hasOnlinePeer);
        sendQuickActionButton.setDisable(connectionStatusValue.getText().startsWith("Connection idle"));
        startDataButton.setDisable(!hasOnlinePeer);
        sendRtcMessageButton.setDisable(false);
    }

    private PeerPresence upsertPeer(String nickname, boolean online) {
        if (nickname == null || nickname.isBlank() || nickname.equalsIgnoreCase(nicknameField.getText().trim())) {
            return null;
        }

        for (PeerPresence item : peerItems) {
            if (item.nickname().equalsIgnoreCase(nickname)) {
                item.online = online;
                peerListView.refresh();
                sortPeers();
                refreshSelectedPeerStatus();
                return item;
            }
        }

        PeerPresence created = new PeerPresence(nickname, online);
        peerItems.add(created);
        sortPeers();
        refreshSelectedPeerStatus();
        return created;
    }

    private void markPeerOffline(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return;
        }
        for (PeerPresence item : peerItems) {
            if (item.nickname().equalsIgnoreCase(nickname)) {
                item.online = false;
                peerListView.refresh();
                sortPeers();
                refreshSelectedPeerStatus();
                return;
            }
        }
    }

    private void sortPeers() {
        FXCollections.sort(peerItems, Comparator
                .comparing(PeerPresence::online).reversed()
                .thenComparing(PeerPresence::nickname, String.CASE_INSENSITIVE_ORDER));
    }

    private void updateSelectedPeer(PeerPresence peer) {
        if (peer == null) {
            recipientField.setText("");
            rtcPeerField.setText("");
            conversationTitleValue.setText("Shared room activity");
            conversationSubtitleValue.setText("Select a peer on the left for voice and file actions.");
            selectedPeerTitleValue.setText("No peer selected");
            selectedPeerMetaValue.setText("Choose an online peer to send files or start voice.");
            setPeerStatus("Peer not selected", Color.web("#9aa4b2"));
        } else {
            recipientField.setText(peer.nickname());
            rtcPeerField.setText(peer.nickname());
            conversationTitleValue.setText("Shared room activity");
            conversationSubtitleValue.setText("Actions on the right will target “" + peer.nickname() + "”. Text chat remains shared for now.");
            selectedPeerTitleValue.setText(peer.nickname());
            selectedPeerMetaValue.setText(peer.online()
                    ? "Online — chat, file transfer, and voice are available."
                    : "Offline — wait until this peer rejoins the chat.");
            setPeerStatus(peer.online() ? "Peer " + peer.nickname() : "Peer offline", peer.online() ? Color.web("#1f9d55") : Color.web("#9aa4b2"));
        }
        updateQuickActionState();
    }

    private void refreshSelectedPeerStatus() {
        updateSelectedPeer(peerListView.getSelectionModel().getSelectedItem());
    }

    private void refreshTransferEntries() {
        transferItems.setAll(transferEntries.values());
        long activeCount = transferEntries.values().stream().filter(TransferEntry::active).count();
        if (activeCount == 0 && transferEntries.isEmpty()) {
            transferHintValue.setText("No transfers yet");
        } else if (activeCount == 0) {
            transferHintValue.setText("No active transfers. Recent results remain visible below.");
        } else {
            transferHintValue.setText(activeCount + " active transfer" + (activeCount == 1 ? "" : "s"));
        }
    }

    private String activeTransferSummary() {
        long activeCount = transferEntries.values().stream().filter(TransferEntry::active).count();
        if (activeCount == 0) {
            return "Transfers idle";
        }
        return activeCount + " transfer" + (activeCount == 1 ? " active" : "s active");
    }

    private void setServerStatus(String value, Color color) {
        serverStatusValue.setText(value);
        serverIndicator.setFill(color);
    }

    private void setConnectionStatus(String value, Color color) {
        connectionStatusValue.setText(value);
        connectionIndicator.setFill(color);
        updateQuickActionState();
    }

    private void setPeerStatus(String value, Color color) {
        peerStatusValue.setText(value);
        peerIndicator.setFill(color);
    }

    private void setVoiceStatus(String value, Color color) {
        voiceStatusValue.setText(value);
        voicePanelStatusValue.setText(value);
        voiceIndicator.setFill(color);
    }

    private void setTransferStatus(String value, Color color) {
        transferStatusValue.setText(value);
        transferIndicator.setFill(color);
    }

    private void styleStatusLabel(Label label) {
        label.getStyleClass().add("status-value");
    }

    private void configureAudioLevelBar(ProgressBar bar) {
        bar.setPrefWidth(260);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setProgress(0);
    }

    private void configureMediaStatusLabel(Label label) {
        label.setWrapText(true);
        label.getStyleClass().add("subtle-label");
    }

    private void configureVideoView(ImageView view) {
        view.setPreserveRatio(true);
        view.setFitWidth(320);
        view.setFitHeight(200);
        view.setSmooth(true);
        view.getStyleClass().add("video-preview");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation failed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static final class PeerPresence {
        private final String nickname;
        private boolean online;

        private PeerPresence(String nickname, boolean online) {
            this.nickname = nickname;
            this.online = online;
        }

        public String nickname() {
            return nickname;
        }

        public boolean online() {
            return online;
        }
    }

    private static final class TransferEntry {
        private final String transferId;
        private final String fileName;
        private String status;
        private int percent;
        private long totalBytes;

        private TransferEntry(String transferId, String fileName, String status, int percent, long totalBytes) {
            this.transferId = transferId;
            this.fileName = fileName;
            this.status = status;
            this.percent = percent;
            this.totalBytes = totalBytes;
        }

        private boolean active() {
            return "Sending".equals(status) || "Receiving".equals(status);
        }
    }

    private static final class PeerCell extends ListCell<PeerPresence> {
        @Override
        protected void updateItem(PeerPresence item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            Circle dot = new Circle(5, item.online() ? Color.web("#1f9d55") : Color.web("#9aa4b2"));
            Label name = new Label(item.nickname());
            name.getStyleClass().add("list-primary");
            Label meta = new Label(item.online() ? "chat • voice • file" : "offline");
            meta.getStyleClass().add("list-secondary");
            VBox textBox = new VBox(2, name, meta);
            HBox row = new HBox(8, dot, textBox);
            row.getStyleClass().add("list-row");
            row.setAlignment(Pos.CENTER_LEFT);
            if (!getStyleClass().contains("content-list-cell")) {
                getStyleClass().add("content-list-cell");
            }
            setGraphic(row);
        }
    }

    private static final class TransferCell extends ListCell<TransferEntry> {
        @Override
        protected void updateItem(TransferEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            Label name = new Label(item.fileName);
            name.getStyleClass().add("list-primary");
            String metaText = item.status;
            if (item.percent > 0 && item.percent < 100 && item.active()) {
                metaText += " — " + item.percent + "%";
            } else if (item.percent == 100 && "Completed".equals(item.status)) {
                metaText += " — 100%";
            }
            if (item.totalBytes > 0) {
                metaText += " — " + item.totalBytes + " bytes";
            }
            Label meta = new Label(metaText);
            meta.getStyleClass().add("list-secondary");
            VBox box = new VBox(2, name, meta);
            box.getStyleClass().add("list-row");
            if (!getStyleClass().contains("content-list-cell")) {
                getStyleClass().add("content-list-cell");
            }
            setGraphic(box);
        }
    }
}
