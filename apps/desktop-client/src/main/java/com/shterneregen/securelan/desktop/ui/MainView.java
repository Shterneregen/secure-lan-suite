package com.shterneregen.securelan.desktop.ui;

import com.shterneregen.securelan.audio.service.AudioCallProfile;
import com.shterneregen.securelan.audio.service.AudioProfileService;
import com.shterneregen.securelan.audio.service.impl.DefaultAudioProfileService;
import com.shterneregen.securelan.chat.discovery.DiscoveredPeer;
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig;
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryListener;
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryService;
import com.shterneregen.securelan.chat.discovery.impl.UdpBroadcastPeerDiscoveryService;
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
import com.shterneregen.securelan.common.net.NetworkConstants;
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode;
import com.shterneregen.securelan.common.model.rtc.RtcSessionState;
import com.shterneregen.securelan.desktop.service.DefaultRandomNicknameService;
import com.shterneregen.securelan.desktop.service.RandomNicknameService;
import com.shterneregen.securelan.filetransfer.event.FileTransferCompletedEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferStartedEvent;
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata;
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
import com.shterneregen.securelan.webrtc.service.RtcMediaDevice;
import com.shterneregen.securelan.webrtc.service.RtcMediaDeviceService;
import com.shterneregen.securelan.webrtc.service.RtcSessionRequest;
import com.shterneregen.securelan.webrtc.service.RtcSessionService;
import com.shterneregen.securelan.webrtc.service.impl.DefaultRtcMediaDeviceService;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
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
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class MainView {
    private static final boolean LOCAL_VIDEO_PREVIEW_ENABLED = Boolean.parseBoolean(System.getProperty("securelan.rtc.videoPreview.local.enabled", "true"));
    private static final boolean REMOTE_VIDEO_PREVIEW_ENABLED = Boolean.parseBoolean(System.getProperty("securelan.rtc.videoPreview.remote.enabled", "true"));
    private static final double TRANSFER_LIST_VISIBLE_ROWS = 3;
    private static final double TRANSFER_LIST_ROW_HEIGHT = 48;
    private static final Path DEFAULT_DOWNLOADS_PATH = Path.of("downloads").toAbsolutePath().normalize();
    private static final String LOCAL_PEER_ID = UUID.randomUUID().toString();
    private static final int CLIENT_FILE_PORT_OFFSET = 1000;
    private final RandomNicknameService randomNicknameService = new DefaultRandomNicknameService();

    private final TextField serverChatPortField = new TextField(Integer.toString(NetworkConstants.DEFAULT_CHAT_PORT));
    private final TextField serverFilePortField = new TextField(Integer.toString(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT));

    private final TextField clientHostField = new TextField("127.0.0.1");
    private final TextField clientChatPortField = new TextField(Integer.toString(NetworkConstants.DEFAULT_CHAT_PORT));
    private final TextField clientFilePortField = new TextField(Integer.toString(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT));
    private final TextField nicknameField = new TextField(randomNicknameService.generate());
    private final TextField clientPasswordField = new TextField("chatpass");
    private final TextField fileHostField = new TextField("127.0.0.1");
    private final TextField fileSenderField = new TextField(nicknameField.getText());
    private final TextField recipientField = new TextField("peer");
    private final TextField rtcPeerField = new TextField("peer");
    private final TextField rtcDataChannelField = new TextField("securelan-data");
    private final TextField rtcMessageField = new TextField();

    private final TextArea logArea = new TextArea();
    private final TextArea diagnosticsArea = new TextArea();
    private final TextField messageField = new TextField();
    private final ComboBox<MediaDeviceChoice> microphoneChoiceBox = new ComboBox<>();
    private final ComboBox<MediaDeviceChoice> cameraChoiceBox = new ComboBox<>();

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
    private final Label conversationSubtitleValue = new Label("Select a peer on the left for voice, video, and file actions.");
    private final Label selectedPeerTitleValue = new Label("No peer selected");
    private final Label selectedPeerMetaValue = new Label("Choose an online peer to send files or start a voice/video session.");
    private final Label peersTitleValue = new Label("Contacts / Peers");
    private final Label peersHintValue = new Label("Select an online peer to target voice/video calls and file transfers.");
    private final Label realtimeRuntimeValue = new Label("Checking runtime");
    private final Label audioProfileValue = new Label();
    private final Label videoProfileValue = new Label();
    private final Label localAudioStatusValue = new Label("Idle");
    private final Label remoteAudioStatusValue = new Label("Idle");
    private final Label microphoneTestStatusValue = new Label("Not tested");
    private final Label cameraTestStatusValue = new Label("Not tested");
    private final Label transferHintValue = new Label("No transfers yet");
    private final Label videoStageTitleValue = new Label("Video stage");
    private final Label videoStageSubtitleValue = new Label("Start a video call to open the inline video stage.");
    private final Label videoStageBadgeValue = new Label("Idle");
    private final Label videoParticipantsValue = new Label("Waiting for participants");
    private final Label videoMediaValue = new Label("Camera starts only when a video call begins");
    private final Label videoPreviewValue = new Label();
    private final Label remoteVideoCaptionValue = new Label("Remote stream");
    private final Label localVideoCaptionValue = new Label("Self preview");
    private final Label remoteVideoPlaceholderValue = new Label("Remote video will appear here when the call connects.");
    private final Label localVideoPlaceholderValue = new Label();

    private final ProgressBar localAudioLevelBar = new ProgressBar(0);
    private final ProgressBar remoteAudioLevelBar = new ProgressBar(0);

    private final ImageView localVideoView = new ImageView();
    private final ImageView remoteVideoView = new ImageView();
    private final VBox videoStageBox = new VBox(12);

    private WritableImage localVideoImage;
    private WritableImage remoteVideoImage;
    private RtcMediaDeviceService.CameraPreviewSession cameraPreviewSession;
    private Stage cameraPreviewStage;
    private ImageView cameraPreviewView;
    private Label cameraPreviewPlaceholderValue;
    private Label cameraPreviewStatusValue;
    private WritableImage cameraPreviewImage;
    private final AtomicLong latestCameraPreviewGeneration = new AtomicLong(0);
    private final AtomicLong fileTransferThreadSequence = new AtomicLong(0);
    private final ExecutorService fileTransferExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "securelan-file-transfer-client-" + fileTransferThreadSequence.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });
    private final Object transferProgressLock = new Object();
    private final Map<String, FileTransferProgressEvent> pendingTransferProgressEvents = new LinkedHashMap<>();
    private final AtomicBoolean transferProgressUiUpdateScheduled = new AtomicBoolean(false);

    private final ObservableList<PeerPresence> peerItems = FXCollections.observableArrayList();
    private final ListView<PeerPresence> peerListView = new ListView<>(peerItems);
    private final ObservableList<TransferEntry> transferItems = FXCollections.observableArrayList();
    private final ListView<TransferEntry> transferListView = new ListView<>(transferItems);
    private final Map<String, TransferEntry> transferEntries = new LinkedHashMap<>();

    private final Button sendFileQuickActionButton = new Button("Attach");
    private final Button startVoiceQuickActionButton = new Button("Voice call");
    private final Button startVideoQuickActionButton = new Button("Video call");
    private final Button hangUpQuickActionButton = new Button("End call");
    private final Button startServerButton = new Button("Open room");
    private final Button stopServerButton = new Button("Stop hosting");
    private final CheckBox discoverableCheckBox = new CheckBox("Discoverable");
    private final Button connectButton = new Button("Connect");
    private final Button disconnectButton = new Button("Disconnect");
    private final CheckBox autoAcceptFilesCheckBox = new CheckBox("Accept files without confirmation");
    private final Button sendMessageButton = new Button("Send");
    private final Button startDataButton = new Button("Start data");
    private final Button sendRtcMessageButton = new Button("Send RTC message");
    private final Button testMicrophoneButton = new Button("Test mic");
    private final Button testCameraButton = new Button("Test camera");
    private final ToggleButton themeToggleButton = new ToggleButton("Dark theme");

    private BorderPane root;

    private final ChatServerService serverService;
    private final ChatClientService clientService;
    private final FileTransferServerService fileTransferServerService;
    private final FileTransferClientService fileTransferClientService;
    private final RtcSessionService rtcSessionService;
    private final RtcMediaDeviceService rtcMediaDeviceService;
    private final PeerDiscoveryService peerDiscoveryService;
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
        this.rtcMediaDeviceService = new DefaultRtcMediaDeviceService();
        this.peerDiscoveryService = new UdpBroadcastPeerDiscoveryService();
        this.rtcSessionService = new DefaultRtcSessionService(this::handleRtcEvent, clientService::sendSignal);
        syncSharedClientFields();
        configureUiState();
        wireActions();
        publishLocalNetworkInfo();
        publishRealtimeProfiles();
        refreshMediaDeviceChoices();
        publishRealtimeRuntimeStatus();
        startPeerDiscoveryListener();
    }

    public Parent createContent() {
        root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(10));
        root.setTop(new VBox(8, buildStatusBar(), buildConnectionWorkspaceHeader()));
        root.setCenter(buildWorkspace());
        applyTheme();
        return root;
    }

    public void shutdown() {
        fileTransferExecutor.shutdownNow();
        closeCameraPreview();
        rtcMediaDeviceService.close();
        rtcSessionService.close();
        peerDiscoveryService.stop();
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
        peersHintValue.setText("Discovered LAN peers appear here. Select one to connect before sending files or starting a call.");
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
        videoStageTitleValue.getStyleClass().add("section-heading");
        videoStageSubtitleValue.setWrapText(true);
        videoStageSubtitleValue.getStyleClass().add("muted-label");
        videoStageBadgeValue.getStyleClass().addAll("status-value", "video-stage-badge");
        videoParticipantsValue.getStyleClass().add("status-value");
        videoMediaValue.getStyleClass().add("status-value");
        videoPreviewValue.getStyleClass().add("status-value");
        remoteVideoCaptionValue.getStyleClass().add("metric-title");
        localVideoCaptionValue.getStyleClass().add("metric-title");
        remoteVideoPlaceholderValue.setWrapText(true);
        remoteVideoPlaceholderValue.getStyleClass().addAll("muted-label", "video-stage-placeholder");
        localVideoPlaceholderValue.setWrapText(true);
        localVideoPlaceholderValue.getStyleClass().addAll("muted-label", "video-stage-placeholder");
        videoPreviewValue.setText(LOCAL_VIDEO_PREVIEW_ENABLED
                ? "Self preview on • remote preview " + (REMOTE_VIDEO_PREVIEW_ENABLED ? "on" : "off")
                : "Self preview disabled • remote preview " + (REMOTE_VIDEO_PREVIEW_ENABLED ? "on" : "off"));
        localVideoPlaceholderValue.setText(LOCAL_VIDEO_PREVIEW_ENABLED
                ? "Self preview will appear here when your camera starts."
                : "Self preview is disabled by configuration. Your camera still sends video to the peer.");
        configureAudioLevelBar(localAudioLevelBar);
        configureAudioLevelBar(remoteAudioLevelBar);
        configureMediaStatusLabel(localAudioStatusValue);
        configureMediaStatusLabel(remoteAudioStatusValue);
        configureMediaStatusLabel(microphoneTestStatusValue);
        configureMediaStatusLabel(cameraTestStatusValue);
        configureVideoView(localVideoView);
        configureVideoView(remoteVideoView);
        configureVideoStage();
        autoAcceptFilesCheckBox.setSelected(false);
        discoverableCheckBox.setSelected(true);
        styleInteractiveControls();
        configureMediaDeviceSelectors();
        peerListView.getStyleClass().add("content-list");
        transferListView.getStyleClass().add("content-list");
        transferListView.setFixedCellSize(TRANSFER_LIST_ROW_HEIGHT);
        transferListView.setPrefHeight(TRANSFER_LIST_ROW_HEIGHT * TRANSFER_LIST_VISIBLE_ROWS + 2);
        transferListView.setMinHeight(Region.USE_PREF_SIZE);
        transferListView.setMaxHeight(Region.USE_PREF_SIZE);
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
        discoverableCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> updateDiscoverableState(newValue));
        connectButton.setOnAction(event -> connectClient());
        disconnectButton.setOnAction(event -> disconnectClient());
        sendMessageButton.setOnAction(event -> sendMessage());
        messageField.setOnAction(event -> sendMessage());
        sendFileQuickActionButton.setOnAction(event -> chooseAndSendFileForSelectedPeer());
        startVoiceQuickActionButton.setOnAction(event -> startRealtimeSession(RtcSessionMode.AUDIO));
        startVideoQuickActionButton.setOnAction(event -> startRealtimeSession(RtcSessionMode.AUDIO_VIDEO));
        hangUpQuickActionButton.setOnAction(event -> rtcSessionService.closeCurrentSession());
        startDataButton.setOnAction(event -> startRealtimeSession(RtcSessionMode.DATA));
        sendRtcMessageButton.setOnAction(event -> sendRtcMessage());
        rtcMessageField.setOnAction(event -> sendRtcMessage());
        testMicrophoneButton.setOnAction(event -> testSelectedMicrophone());
        testCameraButton.setOnAction(event -> testSelectedCamera());
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
        videoProfileValue.setText((LOCAL_VIDEO_PREVIEW_ENABLED
                ? "Video stage enabled inline with self preview. "
                : "Video stage enabled inline with self preview disabled by configuration. ")
                + "Default profile %dx%d @ %d FPS."
                .formatted(videoProfile.width(), videoProfile.height(), videoProfile.framesPerSecond()));
    }

    private void configureMediaDeviceSelectors() {
        configureMediaDeviceChoiceBox(microphoneChoiceBox, "Default microphone");
        configureMediaDeviceChoiceBox(cameraChoiceBox, "Default camera");
    }

    private void configureMediaDeviceChoiceBox(ComboBox<MediaDeviceChoice> choiceBox, String prompt) {
        choiceBox.setMaxWidth(Double.MAX_VALUE);
        choiceBox.setPromptText(prompt);
        choiceBox.setButtonCell(new MediaDeviceChoiceCell());
        choiceBox.setCellFactory(list -> new MediaDeviceChoiceCell());
    }

    private void refreshMediaDeviceChoices() {
        refreshMediaDeviceChoiceBox(microphoneChoiceBox, rtcMediaDeviceService.audioCaptureDevices(), "System default microphone", "No microphones detected");
        refreshMediaDeviceChoiceBox(cameraChoiceBox, rtcMediaDeviceService.videoCaptureDevices(), "System default camera", "No cameras detected");
    }

    private void refreshMediaDeviceChoiceBox(ComboBox<MediaDeviceChoice> choiceBox, List<RtcMediaDevice> devices, String defaultLabel, String emptyLabel) {
        String selectedId = selectedDeviceId(choiceBox);
        ObservableList<MediaDeviceChoice> choices = FXCollections.observableArrayList();
        choices.add(MediaDeviceChoice.systemDefault(defaultLabel));
        for (RtcMediaDevice device : devices) {
            choices.add(MediaDeviceChoice.of(device));
        }

        choiceBox.setItems(choices);
        MediaDeviceChoice selectedChoice = choices.stream()
                .filter(choice -> choice.matches(selectedId))
                .findFirst()
                .orElseGet(() -> choices.isEmpty() ? null : choices.getFirst());
        choiceBox.getSelectionModel().select(selectedChoice);
        if (devices.isEmpty()) {
            appendDiagnostics("[rtc] " + emptyLabel);
        }
    }

    private String selectedDeviceId(ComboBox<MediaDeviceChoice> choiceBox) {
        MediaDeviceChoice selected = choiceBox.getSelectionModel().getSelectedItem();
        return selected == null ? "" : selected.deviceId();
    }

    private void testSelectedMicrophone() {
        testMicrophoneButton.setDisable(true);
        String deviceId = selectedDeviceId(microphoneChoiceBox);
        appendDiagnostics("[rtc-test] microphone test started");
        Thread thread = new Thread(() -> {
            String result = rtcMediaDeviceService.testAudioCaptureDevice(deviceId);
            Platform.runLater(() -> {
                appendDiagnostics("[rtc-test] " + result);
                microphoneTestStatusValue.setText(result);
                testMicrophoneButton.setDisable(false);
            });
        }, "securelan-rtc-microphone-test");
        thread.setDaemon(true);
        thread.start();
    }

    private void testSelectedCamera() {
        testCameraButton.setDisable(true);
        String deviceId = selectedDeviceId(cameraChoiceBox);
        closeCameraPreview();
        long previewGeneration = latestCameraPreviewGeneration.incrementAndGet();
        openCameraPreviewWindow();
        appendDiagnostics("[rtc-test] camera preview started");
        Thread thread = new Thread(() -> {
            RtcMediaDeviceService.CameraPreviewSession previewSession;
            try {
                previewSession = rtcMediaDeviceService.startVideoPreview(
                        deviceId,
                        frame -> Platform.runLater(() -> updateCameraPreview(previewGeneration, frame))
                );
            } catch (Throwable error) {
                String result = "Camera preview failed: " + error.getClass().getSimpleName() + ": " + error.getMessage();
                Platform.runLater(() -> {
                    appendDiagnostics("[rtc-test] " + result);
                    cameraTestStatusValue.setText(result);
                    if (cameraPreviewStatusValue != null) {
                        cameraPreviewStatusValue.setText(result);
                    }
                    testCameraButton.setDisable(false);
                });
                return;
            }
            Platform.runLater(() -> {
                try {
                    if (previewGeneration != latestCameraPreviewGeneration.get()) {
                        closeCameraPreviewSessionQuietly(previewSession);
                        return;
                    }
                    cameraPreviewSession = previewSession;
                    String result = previewSession.statusMessage();
                    appendDiagnostics("[rtc-test] " + result);
                    cameraTestStatusValue.setText(result);
                    if (cameraPreviewStatusValue != null) {
                        cameraPreviewStatusValue.setText(result);
                    }
                } finally {
                    testCameraButton.setDisable(false);
                }
            });
        }, "securelan-rtc-camera-test");
        thread.setDaemon(true);
        thread.start();
    }

    private void openCameraPreviewWindow() {
        cameraPreviewImage = null;
        cameraPreviewView = new ImageView();
        configureVideoView(cameraPreviewView);
        cameraPreviewView.setFitWidth(640);
        cameraPreviewView.setFitHeight(480);
        cameraPreviewPlaceholderValue = new Label("Starting camera preview…");
        cameraPreviewPlaceholderValue.setWrapText(true);
        cameraPreviewPlaceholderValue.getStyleClass().addAll("muted-label", "video-stage-placeholder");
        cameraPreviewStatusValue = new Label("Starting camera preview…");
        cameraPreviewStatusValue.setWrapText(true);
        cameraPreviewStatusValue.getStyleClass().add("muted-label");
        StackPane surface = createVideoSurface(cameraPreviewView, cameraPreviewPlaceholderValue, "video-stage-surface", 640, 480);
        VBox content = new VBox(12, new Label("Camera preview"), cameraPreviewStatusValue, surface);
        content.setPadding(new Insets(14));
        content.getStyleClass().add("video-stage-card");

        cameraPreviewStage = new Stage();
        cameraPreviewStage.setTitle("SecureLanSuite — Camera preview");
        Scene scene = new Scene(content, 700, 580);
        scene.getStylesheets().setAll(
                stylesheet("/styles/base.css"),
                stylesheet(themeToggleButton.isSelected() ? "/styles/dark-theme.css" : "/styles/light-theme.css")
        );
        cameraPreviewStage.setScene(scene);
        cameraPreviewStage.setOnCloseRequest(event -> closeCameraPreview());
        cameraPreviewStage.show();
    }

    private void updateCameraPreview(long previewGeneration, RtcVideoFrameEvent event) {
        if (previewGeneration != latestCameraPreviewGeneration.get() || cameraPreviewView == null) {
            return;
        }
        cameraPreviewImage = applyVideoFrame(cameraPreviewView, cameraPreviewImage, event);
        if (cameraPreviewPlaceholderValue != null) {
            cameraPreviewPlaceholderValue.setVisible(false);
            cameraPreviewPlaceholderValue.setManaged(false);
        }
        if (cameraPreviewStatusValue != null) {
            cameraPreviewStatusValue.setText("Camera preview live • " + event.width() + "x" + event.height());
        }
    }

    private void closeCameraPreview() {
        latestCameraPreviewGeneration.incrementAndGet();
        RtcMediaDeviceService.CameraPreviewSession session = cameraPreviewSession;
        cameraPreviewSession = null;
        if (session != null) {
            closeCameraPreviewSessionQuietly(session);
        }
        Stage stage = cameraPreviewStage;
        cameraPreviewStage = null;
        if (stage != null) {
            stage.close();
        }
        cameraPreviewView = null;
        cameraPreviewPlaceholderValue = null;
        cameraPreviewStatusValue = null;
        cameraPreviewImage = null;
    }

    private void closeCameraPreviewSessionQuietly(RtcMediaDeviceService.CameraPreviewSession session) {
        try {
            session.close();
        } catch (Throwable ignored) {
            // Camera preview cleanup must not break window shutdown or test button recovery.
        }
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

    private void syncSharedClientFields() {
        fileHostField.textProperty().bindBidirectional(clientHostField.textProperty());
        fileSenderField.textProperty().bindBidirectional(nicknameField.textProperty());
    }

    private Node buildStatusBar() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(8,
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
        HBox chip = new HBox(6, indicator, valueLabel);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(4, 8, 4, 8));
        chip.getStyleClass().add("status-chip");
        return chip;
    }

    private Node buildConnectionWorkspaceHeader() {
        HBox row = new HBox(8,
                createCompactCard("My profile", buildServerQuickPanel()),
                createCompactCard("Manual connection", buildClientConnectionQuickPanel())
        );
        HBox.setHgrow((Node) row.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow((Node) row.getChildren().get(1), Priority.ALWAYS);
        return row;
    }

    private Node buildServerQuickPanel() {
        Label summary = createMutedLabel("Set your name and shared room password. Then open a room. Keep Discoverable enabled if peers should find it automatically on the LAN.");

        GridPane mainGrid = createCompactFormGrid();
        mainGrid.addRow(0, new Label("Your name"), nicknameField, new Label("Room password"), clientPasswordField);

        GridPane advancedGrid = createCompactFormGrid();
        advancedGrid.addRow(0, new Label("Chat port"), serverChatPortField, new Label("File port"), serverFilePortField);
        VBox advancedContent = new VBox(6,
                createMutedLabel("Change these only if another app already uses the default ports."),
                advancedGrid
        );
        TitledPane advancedPane = createCollapsedAdvancedPane("Advanced network settings", advancedContent);

        HBox actions = new HBox(8, startServerButton, stopServerButton, discoverableCheckBox);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(8, summary, mainGrid, actions, advancedPane);
        growFields(nicknameField, clientPasswordField, serverChatPortField, serverFilePortField);
        return box;
    }

    private Node buildClientConnectionQuickPanel() {
        Label summary = createMutedLabel("Fallback for rooms that were not discovered automatically. Usually you will select a discovered peer from the list.");

        GridPane mainGrid = createCompactFormGrid();
        mainGrid.addRow(0, new Label("Host address"), clientHostField);

        GridPane advancedGrid = createCompactFormGrid();
        advancedGrid.addRow(0, new Label("Chat port"), clientChatPortField, new Label("File port"), clientFilePortField);
        VBox advancedContent = new VBox(6,
                createMutedLabel("Use custom ports only when the host changed them in advanced settings."),
                advancedGrid
        );
        TitledPane advancedPane = createCollapsedAdvancedPane("Advanced network settings", advancedContent);

        HBox actions = new HBox(8, connectButton, disconnectButton);
        VBox box = new VBox(8, summary, mainGrid, actions, advancedPane);
        growFields(clientHostField, nicknameField, clientPasswordField, clientChatPortField, clientFilePortField);
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
        HBox callActions = new HBox(8, startVoiceQuickActionButton, startVideoQuickActionButton, hangUpQuickActionButton);
        callActions.setAlignment(Pos.CENTER_RIGHT);
        VBox conversationHeading = new VBox(4, conversationTitleValue, conversationSubtitleValue);
        HBox.setHgrow(conversationHeading, Priority.ALWAYS);
        HBox conversationHeader = new HBox(10, conversationHeading, callActions);
        conversationHeader.setAlignment(Pos.CENTER_LEFT);

        HBox messageRow = new HBox(10, sendFileQuickActionButton, messageField, sendMessageButton);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        VBox content = new VBox(12,
                videoStageBox,
                conversationHeader,
                logArea,
                messageRow
        );
        VBox.setVgrow(logArea, Priority.ALWAYS);
        return createCard("Chat", content);
    }

    private void configureVideoStage() {
        videoStageBox.getStyleClass().add("video-stage-card");
        videoStageBox.setPadding(new Insets(14));
        videoStageBox.setVisible(false);
        videoStageBox.setManaged(false);
        remoteVideoView.setFitWidth(640);
        remoteVideoView.setFitHeight(300);
        localVideoView.setFitWidth(220);
        localVideoView.setFitHeight(150);
        videoStageBox.getChildren().setAll(buildVideoStageContent());
    }

    private Node buildVideoStageContent() {
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(10,
                new VBox(4, videoStageTitleValue, videoStageSubtitleValue),
                headerSpacer,
                videoStageBadgeValue
        );
        header.setAlignment(Pos.CENTER_LEFT);

        HBox metrics = new HBox(8,
                createVideoMetricChip("Participants", videoParticipantsValue),
                createVideoMetricChip("Media", videoMediaValue),
                createVideoMetricChip("Preview", videoPreviewValue)
        );

        VBox remoteBox = new VBox(8,
                remoteVideoCaptionValue,
                createVideoSurface(remoteVideoView, remoteVideoPlaceholderValue, "video-stage-surface", 640, 300)
        );
        HBox.setHgrow(remoteBox, Priority.ALWAYS);
        VBox.setVgrow((Node) remoteBox.getChildren().get(1), Priority.ALWAYS);

        VBox localBox = new VBox(8,
                localVideoCaptionValue,
                createVideoSurface(localVideoView, localVideoPlaceholderValue, "video-stage-surface-small", 220, 150)
        );
        localBox.setAlignment(Pos.TOP_LEFT);

        HBox stageBody = new HBox(12, remoteBox, localBox);
        HBox.setHgrow(remoteBox, Priority.ALWAYS);

        return new VBox(12, header, metrics, stageBody);
    }

    private Node createVideoMetricChip(String title, Label valueLabel) {
        Label titleLabel = createMetricTitleLabel(title);
        VBox box = new VBox(2, titleLabel, valueLabel);
        box.getStyleClass().add("video-metric-chip");
        box.setPadding(new Insets(8, 10, 8, 10));
        return box;
    }

    private StackPane createVideoSurface(ImageView view, Label placeholder, String styleClass, double width, double height) {
        StackPane stack = new StackPane(view, placeholder);
        stack.setAlignment(Pos.CENTER);
        stack.setMinHeight(height);
        stack.setPrefHeight(height);
        stack.setPrefWidth(width);
        stack.setMaxWidth(Double.MAX_VALUE);
        stack.getStyleClass().add(styleClass);
        return stack;
    }

    private Node buildActionsColumn() {
        startDataButton.setMaxWidth(Double.MAX_VALUE);
        sendRtcMessageButton.setMaxWidth(Double.MAX_VALUE);
        testMicrophoneButton.setMaxWidth(Double.MAX_VALUE);
        testCameraButton.setMaxWidth(Double.MAX_VALUE);

        VBox voiceBlock = new VBox(8,
                voicePanelStatusValue,
                createMetricBlock("Microphone", new VBox(6, microphoneChoiceBox, testMicrophoneButton, microphoneTestStatusValue)),
                createMetricBlock("Local microphone", new VBox(6, localAudioLevelBar, localAudioStatusValue)),
                createMetricBlock("Remote audio", new VBox(6, remoteAudioLevelBar, remoteAudioStatusValue)),
                createMetricBlock("Camera", new VBox(6, cameraChoiceBox, testCameraButton, cameraTestStatusValue))
        );

        VBox transfersBlock = new VBox(8,
                transferHintValue,
                autoAcceptFilesCheckBox,
                createMutedLabel("Unchecked by default: incoming files ask for confirmation and are accepted only from online chat peers."),
                transferListView);

        VBox advancedContent = new VBox(10,
                createSectionHeadingLabel("Runtime"),
                realtimeRuntimeValue,
                audioProfileValue,
                videoProfileValue,
                new Separator(),
                createSectionHeadingLabel("RTCDataChannel"),
                createMutedLabel("Use this for diagnostics and RTCDataChannel experiments. Video now opens as an inline stage in the chat column with self preview enabled by default."),
                startDataButton,
                rtcMessageField,
                sendRtcMessageButton,
                new Separator(),
                createSectionHeadingLabel("Diagnostics"),
                diagnosticsArea
        );
        VBox.setVgrow(diagnosticsArea, Priority.ALWAYS);

        TitledPane mediaPane = new TitledPane("Audio / Video devices", voiceBlock);
        mediaPane.getStyleClass().add("advanced-pane");
        mediaPane.setExpanded(false);
        mediaPane.setAnimated(false);

        TitledPane advancedPane = new TitledPane("Advanced / Experimental", advancedContent);
        advancedPane.getStyleClass().add("advanced-pane");
        advancedPane.setExpanded(false);
        advancedPane.setAnimated(false);

        Node transfersCard = createSectionCard("Transfers", transfersBlock);

        VBox content = new VBox(12,
                selectedPeerTitleValue,
                selectedPeerMetaValue,
                transfersCard,
                mediaPane,
                advancedPane
        );

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

    private Node createCompactCard(String title, Node content) {
        Label header = new Label(title);
        header.getStyleClass().add("card-title");
        VBox box = new VBox(6, header, content);
        box.setPadding(new Insets(10, 12, 10, 12));
        box.getStyleClass().addAll("panel-card", "compact-panel-card");
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

    private TitledPane createCollapsedAdvancedPane(String title, Node content) {
        TitledPane pane = new TitledPane(title, content);
        pane.getStyleClass().add("advanced-pane");
        pane.setExpanded(false);
        pane.setAnimated(false);
        return pane;
    }

    private void styleInteractiveControls() {
        applyButtonVariant(startServerButton, "primary-button");
        applyButtonVariant(stopServerButton, "danger-button");
        applyButtonVariant(connectButton, "primary-button");
        applyButtonVariant(disconnectButton, "danger-button");
        applyButtonVariant(sendMessageButton, "primary-button");
        applyButtonVariant(sendFileQuickActionButton, "secondary-button");
        applyButtonVariant(startVoiceQuickActionButton, "secondary-button");
        applyButtonVariant(startVideoQuickActionButton, "secondary-button");
        applyButtonVariant(hangUpQuickActionButton, "danger-button");
        applyInlineButtonWidth(sendFileQuickActionButton);
        applyInlineButtonWidth(startVoiceQuickActionButton);
        applyInlineButtonWidth(startVideoQuickActionButton);
        applyInlineButtonWidth(hangUpQuickActionButton);
        applyButtonVariant(startDataButton, "secondary-button");
        applyButtonVariant(sendRtcMessageButton, "secondary-button");
        applyButtonVariant(testMicrophoneButton, "secondary-button");
        applyButtonVariant(testCameraButton, "secondary-button");
    }

    private void applyButtonVariant(Button button, String variantClass) {
        button.getStyleClass().addAll("app-button", variantClass);
        button.setMaxWidth(Double.MAX_VALUE);
    }

    private void applyInlineButtonWidth(Button button) {
        button.setMaxWidth(Region.USE_PREF_SIZE);
        button.setMinWidth(Region.USE_PREF_SIZE);
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
        grid.setVgap(5);
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
        startPeerDiscoveryListener();
        setServerStatus("Server stopped", Color.web("#9aa4b2"));
        setTransferStatus("Transfers idle", Color.web("#9aa4b2"));
        appendChat("[ui] server stopped");
        updateQuickActionState();
    }

    private void startPeerDiscoveryListener() {
        if (serverService.isRunning()) {
            startPeerDiscovery(localChatPort(), localFilePort(), discoverableCheckBox.isSelected());
        } else {
            startPeerDiscovery(PeerDiscoveryConfig.listenOnly(LOCAL_PEER_ID, nicknameField.getText().trim()), false);
        }
    }

    private void startPeerDiscovery(int chatPort, int filePort) {
        startPeerDiscovery(chatPort, filePort, discoverableCheckBox.isSelected());
    }

    private void startPeerDiscovery(int chatPort, int filePort, boolean announceEnabled) {
        startPeerDiscovery(PeerDiscoveryConfig.defaults(
                LOCAL_PEER_ID,
                nicknameField.getText().trim(),
                chatPort,
                filePort,
                announceEnabled
        ), announceEnabled || serverService.isRunning());
    }

    private void startPeerDiscovery(PeerDiscoveryConfig discoveryConfig, boolean hosting) {
        peerDiscoveryService.start(discoveryConfig, new PeerDiscoveryListener() {
            @Override
            public void onPeerDiscovered(DiscoveredPeer peer) {
                Platform.runLater(() -> handlePeerDiscovered(peer));
            }

            @Override
            public void onPeerExpired(DiscoveredPeer peer) {
                Platform.runLater(() -> handlePeerExpired(peer));
            }

            @Override
            public void onDiscoveryError(String message, Throwable cause) {
                Platform.runLater(() -> {
                    appendDiagnostics("[discovery-error] " + message + (cause != null ? " -> " + cause.getMessage() : ""));
                    if (!peerDiscoveryService.isRunning()) {
                        appendChat("[discovery] " + message);
                    }
                });
            }
        });
        if (peerDiscoveryService.isRunning()) {
            appendChat(hosting
                    ? discoveryStartedMessage(discoveryConfig)
                    : "[discovery] listening on UDP " + discoveryConfig.discoveryPort());
            peersHintValue.setText("Looking for SecureLanSuite peers on this LAN. Select a discovered peer and connect before sending files or starting a call.");
        }
    }

    private String discoveryStartedMessage(PeerDiscoveryConfig discoveryConfig) {
        if (discoveryConfig.announceEnabled()) {
            return "[discovery] broadcasting as " + discoveryConfig.nickname() + " on UDP " + discoveryConfig.discoveryPort();
        }
        return "[discovery] room is hidden; listening on UDP " + discoveryConfig.discoveryPort() + " without broadcasting";
    }

    private void updateDiscoverableState(boolean discoverable) {
        if (!serverService.isRunning()) {
            return;
        }
        peerDiscoveryService.setAnnounceEnabled(discoverable);
        appendChat(discoverable
                ? "[discovery] room is now discoverable"
                : "[discovery] room is now hidden from automatic discovery");
    }

    private void startServer() {
        try {
            int chatPort = Integer.parseInt(serverChatPortField.getText().trim());
            int filePort = Integer.parseInt(serverFilePortField.getText().trim());
            serverService.start(new ChatServerConfig(chatPort, clientPasswordField.getText()));
            startLocalFileTransferListener(filePort);
            startPeerDiscovery(chatPort, filePort);
            connectToLocalHostedChat(chatPort, filePort);
            setServerStatus("Server running", Color.web("#1f9d55"));
            setTransferStatus("Transfers idle", Color.web("#9aa4b2"));
            appendChat("[ui] chat server started on port " + chatPort);
            appendChat("[ui] file transfer server started on port " + filePort);
        } catch (Exception ex) {
            showError(ex.getMessage());
        } finally {
            updateQuickActionState();
        }
    }

    private void connectToLocalHostedChat(int chatPort, int filePort) {
        clientHostField.setText("127.0.0.1");
        fileHostField.setText("127.0.0.1");
        clientChatPortField.setText(Integer.toString(chatPort));
        clientFilePortField.setText(Integer.toString(filePort));
        if (clientService.isConnected()) {
            return;
        }
        boolean connected = clientService.connect(new ChatClientConnectRequest(
                "127.0.0.1",
                chatPort,
                nicknameField.getText().trim(),
                clientPasswordField.getText()
        ));
        if (!connected) {
            appendChat("[ui] local hosting connection failed");
            setConnectionStatus("Local connection failed", Color.web("#dc2626"));
        } else {
            if (!fileTransferServerService.isRunning()) {
                startLocalFileTransferListener(filePort);
            }
            appendChat("[ui] joined local hosted room");
        }
    }

    private void connectClient() {
        try {
            PeerPresence selectedPeer = peerListView.getSelectionModel().getSelectedItem();
            if (selectedPeer != null && selectedPeer.discovered()) {
                clientHostField.setText(selectedPeer.host());
                clientChatPortField.setText(Integer.toString(selectedPeer.chatPort()));
                clientFilePortField.setText(Integer.toString(selectedPeer.filePort()));
            }
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
            } else {
                startLocalFileTransferListener(localFilePort());
                startPeerDiscoveryListener();
            }
        } catch (Exception ex) {
            showError(ex.getMessage());
        } finally {
            updateQuickActionState();
        }
    }

    private void disconnectClient() {
        clientService.disconnect();
        if (!serverService.isRunning()) {
            fileTransferServerService.stop();
        }
        startPeerDiscoveryListener();
        updateQuickActionState();
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
        if (!clientService.isConnected()) {
            showError("Connect to chat before sending files");
            return;
        }
        if (!peer.discovered()) {
            showError("Selected peer does not advertise a file transfer endpoint yet");
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
        if (peer.discovered()) {
            fileHostField.setText(peer.host());
            clientFilePortField.setText(Integer.toString(peer.filePort()));
        }

        try {
            int filePort = Integer.parseInt(clientFilePortField.getText().trim());
            FileTransferClientRequest request = new FileTransferClientRequest(
                    fileHostField.getText().trim(),
                    filePort,
                    fileSenderField.getText().trim(),
                    peer.nickname(),
                    clientPasswordField.getText(),
                    file.toPath()
            );
            sendFileAsync(request);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void sendFileAsync(FileTransferClientRequest request) {
        setTransferStatus("Preparing file transfer", Color.web("#f59e0b"));
        try {
            fileTransferExecutor.execute(() -> {
                try {
                    fileTransferClientService.sendFile(request);
                } catch (Exception ex) {
                    Platform.runLater(() -> showError(fileTransferErrorMessage(ex)));
                }
            });
        } catch (RuntimeException ex) {
            showError(fileTransferErrorMessage(ex));
        }
    }

    private void startLocalFileTransferListener(int filePort) {
        if (fileTransferServerService.isRunning()) {
            return;
        }
        fileTransferServerService.start(new FileTransferServerConfig(
                filePort,
                DEFAULT_DOWNLOADS_PATH,
                clientPasswordField.getText(),
                this::acceptIncomingFileTransfer
        ));
        appendChat("[ui] file transfer listener started on port " + filePort);
    }

    private int localFilePort() {
        if (serverService.isRunning()) {
            return Integer.parseInt(serverFilePortField.getText().trim());
        }
        int remoteFilePort = Integer.parseInt(clientFilePortField.getText().trim());
        int candidate = remoteFilePort + CLIENT_FILE_PORT_OFFSET;
        if (candidate > 65535) {
            candidate = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT + CLIENT_FILE_PORT_OFFSET;
        }
        return candidate;
    }

    private int localChatPort() {
        if (serverService.isRunning()) {
            return Integer.parseInt(serverChatPortField.getText().trim());
        }
        return Integer.parseInt(clientChatPortField.getText().trim());
    }

    private boolean acceptIncomingFileTransfer(FileTransferMetadata metadata, String remoteAddress) {
        if (!clientService.isConnected()) {
            Platform.runLater(() -> appendChat("[file-recv] rejected " + metadata.fileName() + " from " + metadata.senderId() + ": chat is not connected"));
            return false;
        }
        PeerPresence peer = findOnlinePeer(metadata.senderId());
        if (peer == null) {
            Platform.runLater(() -> appendChat("[file-recv] rejected " + metadata.fileName() + " from unknown/offline peer " + metadata.senderId()));
            return false;
        }
        if (autoAcceptFilesCheckBox.isSelected()) {
            Platform.runLater(() -> appendChat("[file-recv] auto-accepted " + metadata.fileName() + " from " + metadata.senderId()));
            return true;
        }

        FutureTask<Boolean> promptTask = new FutureTask<>(() -> showIncomingFileConfirmation(metadata, remoteAddress));
        Platform.runLater(promptTask);
        try {
            return promptTask.get();
        } catch (Exception ex) {
            Platform.runLater(() -> appendDiagnostics("[file-recv] confirmation failed: " + ex.getMessage()));
            return false;
        }
    }

    private boolean showIncomingFileConfirmation(FileTransferMetadata metadata, String remoteAddress) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Incoming file");
        alert.setHeaderText("Accept file from " + metadata.senderId() + "?");
        alert.setContentText("File: " + metadata.fileName()
                + System.lineSeparator()
                + "Size: " + formatMegabytes(metadata.fileSize())
                + System.lineSeparator()
                + "Remote: " + remoteAddress);
        Optional<ButtonType> result = alert.showAndWait();
        boolean accepted = result.isPresent() && result.get() == ButtonType.OK;
        appendChat((accepted ? "[file-recv] accepted " : "[file-recv] rejected ") + metadata.fileName() + " from " + metadata.senderId());
        return accepted;
    }

    private String fileTransferErrorMessage(Throwable error) {
        Throwable candidate = error.getCause() != null ? error.getCause() : error;
        String message = candidate.getMessage();
        if (message == null || message.isBlank()) {
            return candidate.getClass().getSimpleName();
        }
        return message;
    }

    private void startRealtimeSession(RtcSessionMode mode) {
        PeerPresence peer = peerListView.getSelectionModel().getSelectedItem();
        if (peer == null || !peer.online()) {
            showError("Select an online peer first");
            return;
        }
        if (!clientService.isConnected()) {
            showError("Connect to chat before starting voice or video calls");
            return;
        }

        try {
            recipientField.setText(peer.nickname());
            rtcPeerField.setText(peer.nickname());
            clearRealtimeMediaUi();
            if (mode.videoEnabled()) {
                showVideoStage(true);
                videoStageTitleValue.setText(mode == RtcSessionMode.AUDIO_VIDEO
                        ? "Video call with " + peer.nickname()
                        : "Video stream with " + peer.nickname());
                videoStageSubtitleValue.setText("Preparing camera and signaling for " + peer.nickname() + "…");
                videoParticipantsValue.setText(nicknameField.getText().trim() + " • " + peer.nickname());
                videoStageBadgeValue.setText("Preparing");
                videoMediaValue.setText(mode == RtcSessionMode.AUDIO_VIDEO ? "Audio + camera" : "Camera only");
            }
            rtcSessionService.startSession(new RtcSessionRequest(
                    nicknameField.getText().trim(),
                    peer.nickname(),
                    mode,
                    rtcDataChannelField.getText().trim(),
                    selectedDeviceId(microphoneChoiceBox),
                    selectedDeviceId(cameraChoiceBox)
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
                clearRealtimeMediaUi();
                updateSelectedPeer(null);
            } else if (event instanceof ChatDisconnectedEvent e) {
                setConnectionStatus("Connection idle", Color.web("#9aa4b2"));
                setPeerStatus("Peer not selected", Color.web("#9aa4b2"));
                appendChat("[disconnected] " + e.nickname() + " - " + e.reason());
                peerItems.clear();
                clearRealtimeMediaUi();
                updateSelectedPeer(null);
            } else if (event instanceof ChatMessageReceivedEvent e) {
                if (!isSystemSender(e.senderNickname())) {
                    upsertPeer(e.senderNickname(), true);
                }
                appendChat(e.senderNickname() + ": " + e.text());
            } else if (event instanceof ChatMessageSentEvent ignored) {
                // message appears once via normal chat flow
            } else if (event instanceof ChatUserJoinedEvent e) {
                if (!e.nickname().equalsIgnoreCase(nicknameField.getText().trim())) {
                    PeerPresence peer = upsertPeer(e.nickname(), true);
                    if (peer != null && peer.online()) {
                        appendChat("[join] " + e.nickname());
                        if (peerListView.getSelectionModel().getSelectedItem() == null) {
                            peerListView.getSelectionModel().select(peer);
                        }
                    }
                }
            } else if (event instanceof ChatUserLeftEvent e) {
                if (markPeerOffline(e.nickname())) {
                    appendChat("[left] " + e.nickname());
                }
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
        if (event instanceof FileTransferProgressEvent progressEvent) {
            queueTransferProgress(progressEvent);
            return;
        }
        if (event instanceof FileTransferCompletedEvent || event instanceof FileTransferFailedEvent) {
            clearPendingTransferProgress(event.transferId());
        }
        Platform.runLater(() -> applyFileTransferEvent(event));
    }

    private void queueTransferProgress(FileTransferProgressEvent event) {
        synchronized (transferProgressLock) {
            pendingTransferProgressEvents.put(event.transferId(), event);
        }
        scheduleTransferProgressUiDrain();
    }

    private void clearPendingTransferProgress(String transferId) {
        synchronized (transferProgressLock) {
            pendingTransferProgressEvents.remove(transferId);
        }
    }

    private void scheduleTransferProgressUiDrain() {
        if (transferProgressUiUpdateScheduled.compareAndSet(false, true)) {
            Platform.runLater(this::drainTransferProgressEvents);
        }
    }

    private void drainTransferProgressEvents() {
        Map<String, FileTransferProgressEvent> snapshot;
        synchronized (transferProgressLock) {
            snapshot = new LinkedHashMap<>(pendingTransferProgressEvents);
            pendingTransferProgressEvents.clear();
        }
        try {
            snapshot.values().forEach(this::applyFileTransferEvent);
        } finally {
            transferProgressUiUpdateScheduled.set(false);
            boolean hasMoreProgress;
            synchronized (transferProgressLock) {
                hasMoreProgress = !pendingTransferProgressEvents.isEmpty();
            }
            if (hasMoreProgress) {
                scheduleTransferProgressUiDrain();
            }
        }
    }

    private void applyFileTransferEvent(FileTransferEvent event) {
        if (event instanceof FileTransferStartedEvent e) {
            TransferEntry entry = new TransferEntry(e.transferId(), e.fileName(), e.outgoing() ? "Sending" : "Receiving", 0, e.totalBytes());
            transferEntries.put(e.transferId(), entry);
            refreshTransferEntries();
            appendChat((e.outgoing() ? "[file-send] " : "[file-recv] ") + "started: " + e.fileName());
            setTransferStatus(activeTransferSummary(), Color.web("#f59e0b"));
        } else if (event instanceof FileTransferProgressEvent e) {
            TransferEntry existing = transferEntries.get(e.transferId());
            if (existing != null && existing.active()) {
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
    }

    private void handleRtcEvent(RtcEvent event) {
        Platform.runLater(() -> {
            if (event instanceof RtcStateChangedEvent e) {
                appendDiagnostics("[rtc] " + e.mode() + " session " + e.state() + " with " + e.remotePeer() + " - " + e.message());
                refreshRealtimeRuntimeValue(rtcSessionService.runtimeStatus());
                upsertPeer(e.remotePeer(), true);
                updateVoiceStatusFromRtc(e);
                updateVideoStageFromRtc(e);
                updateQuickActionState();
                if (e.state() == RtcSessionState.CLOSED
                        || e.state() == RtcSessionState.FAILED
                        || e.state() == RtcSessionState.UNAVAILABLE) {
                    clearRealtimeMediaUi();
                }
            } else if (event instanceof RtcRuntimeWarningEvent e) {
                appendDiagnostics("[rtc-warning] " + e.message());
                refreshRealtimeRuntimeValue(rtcSessionService.runtimeStatus());
                if (videoStageBox.isVisible() && (e.message().toLowerCase().contains("video") || e.message().toLowerCase().contains("camera"))) {
                    videoStageSubtitleValue.setText(e.message());
                }
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

    private void updateVideoStageFromRtc(RtcStateChangedEvent event) {
        if (!event.mode().videoEnabled()) {
            return;
        }

        showVideoStage(true);
        String remotePeer = safePeerName(event.remotePeer());
        videoStageTitleValue.setText(event.mode() == RtcSessionMode.AUDIO_VIDEO
                ? "Video call with " + remotePeer
                : "Video stream with " + remotePeer);
        videoParticipantsValue.setText(nicknameField.getText().trim() + " • " + remotePeer);
        videoMediaValue.setText(event.mode() == RtcSessionMode.AUDIO_VIDEO ? "Audio + camera" : "Camera only");
        videoStageSubtitleValue.setText(event.message());

        switch (event.state()) {
            case NEGOTIATING -> videoStageBadgeValue.setText("Negotiating");
            case CONNECTING -> videoStageBadgeValue.setText("Connecting");
            case CONNECTED -> videoStageBadgeValue.setText("Live");
            case CLOSING -> videoStageBadgeValue.setText("Closing");
            case CLOSED -> {
                videoStageBadgeValue.setText("Ended");
                showVideoStage(false);
            }
            case FAILED, UNAVAILABLE -> {
                videoStageBadgeValue.setText("Unavailable");
                showVideoStage(false);
            }
        }

        updateVideoCaptions();
    }

    private void updateVideoPreview(RtcVideoFrameEvent event) {
        if (!isActiveVideoFrame(event)) {
            return;
        }
        if (event.local()) {
            localVideoImage = applyVideoFrame(localVideoView, localVideoImage, event);
            localVideoPlaceholderValue.setVisible(false);
            localVideoPlaceholderValue.setManaged(false);
            localVideoCaptionValue.setText("Self preview • " + event.width() + "x" + event.height());
        } else {
            remoteVideoImage = applyVideoFrame(remoteVideoView, remoteVideoImage, event);
            remoteVideoPlaceholderValue.setVisible(false);
            remoteVideoPlaceholderValue.setManaged(false);
            remoteVideoCaptionValue.setText(safePeerName(event.peer()) + " • " + event.width() + "x" + event.height());
            showVideoStage(true);
            if (!"Live".equals(videoStageBadgeValue.getText())) {
                videoStageBadgeValue.setText("Live");
            }
        }
    }

    private boolean isActiveVideoFrame(RtcVideoFrameEvent event) {
        if (event == null) {
            return false;
        }
        return rtcSessionService.currentSession()
                .filter(snapshot -> snapshot.sessionId().equals(event.sessionId()))
                .filter(snapshot -> snapshot.mode().videoEnabled())
                .filter(snapshot -> snapshot.state() != RtcSessionState.CLOSED
                        && snapshot.state() != RtcSessionState.FAILED
                        && snapshot.state() != RtcSessionState.UNAVAILABLE)
                .isPresent();
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
                PixelFormat.getByteBgraInstance(),
                event.bgraPixels(),
                0,
                event.width() * 4
        );
        view.setImage(target);
        view.setRotate(event.rotation());
        return target;
    }

    private void updateVideoCaptions() {
        if (remoteVideoView.getImage() == null) {
            remoteVideoCaptionValue.setText("Remote stream");
            remoteVideoPlaceholderValue.setVisible(true);
            remoteVideoPlaceholderValue.setManaged(true);
        }
        if (localVideoView.getImage() == null) {
            localVideoCaptionValue.setText(LOCAL_VIDEO_PREVIEW_ENABLED ? "Self preview" : "Self preview (disabled)");
            localVideoPlaceholderValue.setVisible(true);
            localVideoPlaceholderValue.setManaged(true);
        }
    }

    private void showVideoStage(boolean visible) {
        videoStageBox.setVisible(visible);
        videoStageBox.setManaged(visible);
    }

    private String safePeerName(String peer) {
        return peer == null || peer.isBlank() ? "peer" : peer;
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
        videoStageBadgeValue.setText("Idle");
        videoStageTitleValue.setText("Video stage");
        videoStageSubtitleValue.setText("Start a video call to open the inline video stage.");
        videoParticipantsValue.setText("Waiting for participants");
        videoMediaValue.setText("Camera starts only when a video call begins");
        remoteVideoCaptionValue.setText("Remote stream");
        localVideoCaptionValue.setText(LOCAL_VIDEO_PREVIEW_ENABLED ? "Self preview" : "Self preview (disabled)");
        remoteVideoPlaceholderValue.setText("Remote video will appear here when the call connects.");
        localVideoPlaceholderValue.setText(LOCAL_VIDEO_PREVIEW_ENABLED
                ? "Self preview will appear here when your camera starts."
                : "Self preview is disabled by configuration. Your camera still sends video to the peer.");
        remoteVideoPlaceholderValue.setVisible(true);
        remoteVideoPlaceholderValue.setManaged(true);
        localVideoPlaceholderValue.setVisible(true);
        localVideoPlaceholderValue.setManaged(true);
        showVideoStage(false);
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
        boolean hasFileCapableOnlinePeer = selectedPeer != null && selectedPeer.online() && selectedPeer.discovered();
        boolean hasCallableOnlinePeer = selectedPeer != null && selectedPeer.online();
        boolean localServerRunning = isLocalServerRunning();
        boolean clientConnected = clientService.isConnected();

        startServerButton.setDisable(localServerRunning);
        stopServerButton.setDisable(!localServerRunning);
        connectButton.setDisable(clientConnected);
        disconnectButton.setDisable(!clientConnected);
        sendFileQuickActionButton.setDisable(!clientConnected || !hasFileCapableOnlinePeer);
        startVoiceQuickActionButton.setDisable(!clientConnected || !hasCallableOnlinePeer);
        startVideoQuickActionButton.setDisable(!clientConnected || !hasCallableOnlinePeer);
        startDataButton.setDisable(!clientConnected || !hasCallableOnlinePeer);
        sendRtcMessageButton.setDisable(false);
        boolean canHangUp = rtcSessionService.currentSession()
                .map(snapshot -> snapshot.state() != RtcSessionState.CLOSED && snapshot.state() != RtcSessionState.FAILED && snapshot.state() != RtcSessionState.UNAVAILABLE)
                .orElse(false);
        hangUpQuickActionButton.setDisable(!canHangUp);
    }

    private boolean isLocalServerRunning() {
        return serverService.isRunning();
    }

    private void handlePeerDiscovered(DiscoveredPeer discoveredPeer) {
        PeerPresence peer = upsertDiscoveredPeer(discoveredPeer);
        if (peer == null) {
            return;
        }
        appendDiagnostics("[discovery] " + discoveredPeer.nickname() + " at " + discoveredPeer.host() + ":" + discoveredPeer.chatPort());
        if (peerListView.getSelectionModel().getSelectedItem() == null) {
            peerListView.getSelectionModel().select(peer);
        }
    }

    private void handlePeerExpired(DiscoveredPeer discoveredPeer) {
        if (markPeerOffline(discoveredPeer.nickname())) {
            appendDiagnostics("[discovery] expired " + discoveredPeer.nickname() + " at " + discoveredPeer.host());
        }
    }

    private PeerPresence upsertDiscoveredPeer(DiscoveredPeer discoveredPeer) {
        if (discoveredPeer == null) {
            return null;
        }
        return upsertPeer(
                discoveredPeer.nickname(),
                true,
                discoveredPeer.peerId(),
                discoveredPeer.host(),
                discoveredPeer.chatPort(),
                discoveredPeer.filePort(),
                discoveredPeer.lastSeen()
        );
    }

    private PeerPresence upsertPeer(String nickname, boolean online) {
        return upsertPeer(nickname, online, null, null, 0, 0, null);
    }

    private PeerPresence upsertPeer(String nickname, boolean online, String peerId, String host, int chatPort, int filePort, Instant lastSeen) {
        if (nickname == null || nickname.isBlank() || isSystemSender(nickname) || nickname.equalsIgnoreCase(nicknameField.getText().trim())) {
            return null;
        }

        for (PeerPresence item : peerItems) {
            if (samePeer(item, nickname, peerId)) {
                boolean changed = item.apply(online, peerId, host, chatPort, filePort, lastSeen);
                if (changed) {
                    peerListView.refresh();
                    sortPeers();
                    refreshSelectedPeerStatus();
                    return item;
                }
                return null;
            }
        }

        PeerPresence created = new PeerPresence(nickname, online, peerId, host, chatPort, filePort, lastSeen);
        peerItems.add(created);
        sortPeers();
        refreshSelectedPeerStatus();
        return created;
    }

    private PeerPresence findOnlinePeer(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return null;
        }
        return peerItems.stream()
                .filter(PeerPresence::online)
                .filter(peer -> peer.nickname().equalsIgnoreCase(nickname))
                .findFirst()
                .orElse(null);
    }

    private boolean samePeer(PeerPresence peer, String nickname, String peerId) {
        if (peerId != null && !peerId.isBlank() && peer.peerId() != null && !peer.peerId().isBlank()) {
            return peer.peerId().equals(peerId);
        }
        return peer.nickname().equalsIgnoreCase(nickname);
    }

    private boolean markPeerOffline(String nickname) {
        if (nickname == null || nickname.isBlank() || isSystemSender(nickname)) {
            return false;
        }
        for (PeerPresence item : peerItems) {
            if (item.nickname().equalsIgnoreCase(nickname)) {
                boolean changed = item.online;
                item.online = false;
                if (changed) {
                    peerListView.refresh();
                    sortPeers();
                    refreshSelectedPeerStatus();
                }
                return changed;
            }
        }
        return false;
    }

    private boolean isSystemSender(String nickname) {
        return nickname != null && nickname.equalsIgnoreCase("system");
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
            conversationSubtitleValue.setText("Connect to chat, then select a peer on the left for voice, video, and file actions.");
            selectedPeerTitleValue.setText("No peer selected");
            selectedPeerMetaValue.setText("Choose an online chat peer to send files or start a voice/video session.");
            setPeerStatus("Peer not selected", Color.web("#9aa4b2"));
        } else {
            recipientField.setText(peer.nickname());
            rtcPeerField.setText(peer.nickname());
            if (peer.discovered()) {
                clientHostField.setText(peer.host());
                fileHostField.setText(peer.host());
                clientChatPortField.setText(Integer.toString(peer.chatPort()));
                clientFilePortField.setText(Integer.toString(peer.filePort()));
            }
            conversationTitleValue.setText("Shared room activity");
            conversationSubtitleValue.setText("Actions on the right will target “" + peer.nickname() + "”. Text chat remains shared for now.");
            selectedPeerTitleValue.setText(peer.nickname());
            selectedPeerMetaValue.setText(peer.online()
                    ? clientService.isConnected()
                    ? peer.discovered()
                    ? "Online via chat and LAN discovery — " + peer.host() + ":" + peer.chatPort() + " chat, " + peer.filePort() + " file."
                    : "Online in chat — voice and video are available."
                    : peer.discovered()
                    ? "Discovered via LAN — connect to chat before sending files or starting calls."
                    : "Online candidate — connect to chat before starting voice or video."
                    : "Offline — wait until this peer rejoins the chat or discovery refreshes.");
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
        updateQuickActionState();
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

    private static String formatMegabytes(long bytes) {
        double megabytes = bytes / (1024.0 * 1024.0);
        return String.format(Locale.ROOT, "%.2f MB", megabytes);
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
        private String peerId;
        private String host;
        private int chatPort;
        private int filePort;
        private Instant lastSeen;

        private PeerPresence(String nickname, boolean online, String peerId, String host, int chatPort, int filePort, Instant lastSeen) {
            this.nickname = nickname;
            this.online = online;
            this.peerId = peerId;
            this.host = host;
            this.chatPort = chatPort;
            this.filePort = filePort;
            this.lastSeen = lastSeen;
        }

        private boolean apply(boolean online, String peerId, String host, int chatPort, int filePort, Instant lastSeen) {
            boolean changed = this.online != online;
            this.online = online;
            if (peerId != null && !peerId.isBlank() && !Objects.equals(this.peerId, peerId)) {
                this.peerId = peerId;
                changed = true;
            }
            if (host != null && !host.isBlank() && !Objects.equals(this.host, host)) {
                this.host = host;
                changed = true;
            }
            if (chatPort > 0 && this.chatPort != chatPort) {
                this.chatPort = chatPort;
                changed = true;
            }
            if (filePort > 0 && this.filePort != filePort) {
                this.filePort = filePort;
                changed = true;
            }
            if (lastSeen != null && !Objects.equals(this.lastSeen, lastSeen)) {
                this.lastSeen = lastSeen;
                changed = true;
            }
            return changed;
        }

        public String nickname() {
            return nickname;
        }

        public boolean online() {
            return online;
        }

        public String peerId() {
            return peerId;
        }

        public String host() {
            return host;
        }

        public int chatPort() {
            return chatPort;
        }

        public int filePort() {
            return filePort;
        }

        public Instant lastSeen() {
            return lastSeen;
        }

        public boolean discovered() {
            return host != null && !host.isBlank() && chatPort > 0 && filePort > 0;
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

    private record MediaDeviceChoice(String deviceId, String label, boolean systemDefault, boolean defaultDevice) {
        private static MediaDeviceChoice systemDefault(String label) {
            return new MediaDeviceChoice("", label, true, true);
        }

        private static MediaDeviceChoice of(RtcMediaDevice device) {
            return new MediaDeviceChoice(device.id(), device.name(), false, device.defaultDevice());
        }

        private boolean matches(String selectedDeviceId) {
            String normalized = selectedDeviceId == null ? "" : selectedDeviceId.trim();
            return deviceId.equals(normalized);
        }

        @Override
        public String toString() {
            if (systemDefault) {
                return label;
            }
            return defaultDevice ? label + " (default)" : label;
        }
    }

    private static final class MediaDeviceChoiceCell extends ListCell<MediaDeviceChoice> {
        @Override
        protected void updateItem(MediaDeviceChoice item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.toString());
            setGraphic(null);
        }
    }

    private static String peerMeta(PeerPresence item) {
        if (!item.online()) {
            return item.discovered() ? "offline • " + item.host() : "offline";
        }
        if (item.discovered()) {
            return "discovered • " + item.host() + ":" + item.chatPort() + " • file " + item.filePort();
        }
        return "chat • voice • video • file";
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
            Label meta = new Label(peerMeta(item));
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
                metaText += " — " + formatMegabytes(item.totalBytes);
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
