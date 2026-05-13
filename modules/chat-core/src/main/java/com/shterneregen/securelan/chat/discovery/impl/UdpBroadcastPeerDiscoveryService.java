package com.shterneregen.securelan.chat.discovery.impl;

import com.shterneregen.securelan.chat.discovery.DiscoveredPeer;
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig;
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryListener;
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpBroadcastPeerDiscoveryService implements PeerDiscoveryService {
    private static final int RECEIVE_BUFFER_SIZE = 1024;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean announceEnabled = new AtomicBoolean(false);
    private final Map<String, DiscoveredPeer> peers = new ConcurrentHashMap<>();

    private DatagramSocket socket;
    private Thread receiverThread;
    private Thread announcerThread;
    private PeerDiscoveryConfig config;
    private PeerDiscoveryListener listener;

    @Override
    public void start(PeerDiscoveryConfig config, PeerDiscoveryListener listener) {
        if (running.get()) {
            stop();
        }
        this.config = config;
        this.listener = listener;
        announceEnabled.set(config.announceEnabled());
        peers.clear();
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.setBroadcast(true);
            socket.bind(new InetSocketAddress(config.discoveryPort()));
            running.set(true);
            receiverThread = new Thread(this::receiveLoop, "securelan-peer-discovery-receiver");
            receiverThread.setDaemon(true);
            receiverThread.start();
            announcerThread = new Thread(this::announceLoop, "securelan-peer-discovery-announcer");
            announcerThread.setDaemon(true);
            announcerThread.start();
        } catch (IOException e) {
            closeSocket();
            publishError("Unable to start peer discovery", e);
        }
    }

    @Override
    public void stop() {
        running.set(false);
        announceEnabled.set(false);
        closeSocket();
        peers.clear();
    }

    @Override
    public void setAnnounceEnabled(boolean announceEnabled) {
        this.announceEnabled.set(announceEnabled);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public List<DiscoveredPeer> snapshot() {
        return peers.values().stream()
                .sorted(Comparator.comparing(DiscoveredPeer::nickname, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void announceLoop() {
        while (running.get()) {
            try {
                if (announceEnabled.get()) {
                    announceOnce();
                }
                expireStalePeers();
                Thread.sleep(config.announceInterval().toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                publishError("Unable to announce peer discovery message", e);
            }
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
        while (running.get()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                handlePacket(packet);
            } catch (SocketException e) {
                if (running.get()) {
                    publishError("Peer discovery socket error", e);
                }
            } catch (Exception e) {
                publishError("Unable to process peer discovery message", e);
            }
        }
    }

    private void handlePacket(DatagramPacket packet) {
        DiscoveryMessage message = DiscoveryMessageCodec.fromBytes(packet.getData(), packet.getLength());
        if (config.peerId().equals(message.peerId())) {
            return;
        }
        DiscoveredPeer peer = new DiscoveredPeer(
                message.peerId(),
                message.nickname(),
                packet.getAddress().getHostAddress(),
                message.chatPort(),
                message.filePort(),
                Instant.now()
        );
        peers.put(peer.peerId(), peer);
        listener.onPeerDiscovered(peer);
    }

    private void announceOnce() throws IOException {
        byte[] data = DiscoveryMessageCodec.toBytes(new DiscoveryMessage(
                config.peerId(),
                config.nickname(),
                config.chatPort(),
                config.filePort()
        ));
        for (InetAddress address : broadcastAddresses()) {
            DatagramPacket packet = new DatagramPacket(data, data.length, address, config.discoveryPort());
            socket.send(packet);
        }
    }

    private void expireStalePeers() {
        Instant cutoff = Instant.now().minus(config.staleTimeout());
        for (DiscoveredPeer peer : new ArrayList<>(peers.values())) {
            if (peer.lastSeen().isBefore(cutoff) && peers.remove(peer.peerId(), peer)) {
                listener.onPeerExpired(peer);
            }
        }
    }

    private static List<InetAddress> broadcastAddresses() throws SocketException, UnknownHostException {
        List<InetAddress> result = new ArrayList<>();
        result.add(InetAddress.getLoopbackAddress());
        result.add(InetAddress.getByName("255.255.255.255"));
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                continue;
            }
            networkInterface.getInterfaceAddresses().stream()
                    .filter(address -> address.getAddress() instanceof Inet4Address)
                    .map(address -> address.getBroadcast())
                    .filter(address -> address != null)
                    .forEach(result::add);
        }
        return result.stream().distinct().toList();
    }

    private void closeSocket() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    private void publishError(String message, Throwable cause) {
        if (listener != null) {
            listener.onDiscoveryError(message, cause);
        }
    }
}
