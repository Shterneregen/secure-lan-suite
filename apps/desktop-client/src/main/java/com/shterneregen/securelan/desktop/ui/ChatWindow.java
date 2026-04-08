package com.shterneregen.securelan.desktop.ui;

import com.shterneregen.securelan.chat.event.ChatConnectedEvent;
import com.shterneregen.securelan.chat.event.ChatCoreEvent;
import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent;
import com.shterneregen.securelan.chat.event.ChatErrorEvent;
import com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent;
import com.shterneregen.securelan.chat.event.ChatMessageSentEvent;
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent;
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent;
import com.shterneregen.securelan.chat.model.ChatClientConnectRequest;
import com.shterneregen.securelan.chat.model.ChatServerConfig;
import com.shterneregen.securelan.chat.service.ChatClientService;
import com.shterneregen.securelan.chat.service.ChatEventPublisher;
import com.shterneregen.securelan.chat.service.ChatServerService;
import com.shterneregen.securelan.chat.service.impl.DefaultChatClientService;
import com.shterneregen.securelan.chat.service.impl.DefaultChatServerService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

public class ChatWindow extends JFrame {
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("5050");
    private final JTextField nicknameField = new JTextField("alice");
    private final JTextField passwordField = new JTextField("chatpass");
    private final JTextArea logArea = new JTextArea();
    private final JTextField messageField = new JTextField();

    private final ChatServerService serverService;
    private final ChatClientService clientService;

    public ChatWindow() {
        super("SecureLanSuite Chat MVP");
        ChatEventPublisher publisher = this::handleEvent;
        this.serverService = new DefaultChatServerService(publisher);
        this.clientService = new DefaultChatClientService(publisher);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        add(createConnectionPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createMessagePanel(), BorderLayout.SOUTH);
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 4, 8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Connection"));

        JButton startServerButton = new JButton("Start Server");
        JButton stopServerButton = new JButton("Stop Server");
        JButton connectButton = new JButton("Connect");
        JButton disconnectButton = new JButton("Disconnect");

        startServerButton.addActionListener(e -> startServer());
        stopServerButton.addActionListener(e -> serverService.stop());
        connectButton.addActionListener(e -> connectClient());
        disconnectButton.addActionListener(e -> clientService.disconnect());

        panel.add(new JLabel("Host"));
        panel.add(hostField);
        panel.add(new JLabel("Port"));
        panel.add(portField);

        panel.add(new JLabel("Nickname"));
        panel.add(nicknameField);
        panel.add(new JLabel("Password"));
        panel.add(passwordField);

        panel.add(startServerButton);
        panel.add(stopServerButton);
        panel.add(connectButton);
        panel.add(disconnectButton);

        return panel;
    }

    private JScrollPane createCenterPanel() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        return new JScrollPane(logArea);
    }

    private JPanel createMessagePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Message"));
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        return panel;
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            serverService.start(new ChatServerConfig(port, passwordField.getText()));
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
        messageField.setText("");
    }

    private void handleEvent(ChatCoreEvent event) {
        SwingUtilities.invokeLater(() -> {
            if (event instanceof ChatConnectedEvent e) {
                append("[connected] " + e.nickname() + " -> " + e.remoteAddress());
            } else if (event instanceof ChatDisconnectedEvent e) {
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
        logArea.append(line + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
