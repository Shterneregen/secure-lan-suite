package com.shterneregen.securelan.webrtc.runtime;

import com.shterneregen.securelan.common.model.rtc.*;
import com.shterneregen.securelan.webrtc.event.*;
import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.video.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Real JNI regression probe; synthetic video avoids camera/microphone permissions and hardware. */
public final class NativeVideoSmoke {
    public static void main(String[] args) throws Exception {
        var watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            var thread = new Thread(r, "native-video-watchdog");
            thread.setDaemon(true);
            return thread;
        });
        watchdog.schedule(() -> {
            System.err.println("Native video negotiation/cleanup timed out; thread stacks:");
            Thread.getAllStackTraces().forEach((thread, stack) -> {
                System.err.println(thread.getName() + " " + thread.getState());
                for (var frame : stack) System.err.println("  " + frame);
            });
            Runtime.getRuntime().halt(1);
        }, 45, TimeUnit.SECONDS);
        try {
            try (var engine = createEngine(false)) {
                exchange(engine, false, false);
                exchange(engine, true, false);
            }
            if (args.length > 0 && Boolean.parseBoolean(args[0])) {
                try (var engine = createEngine(true)) {
                    exchange(engine, true, true);
                    exchange(engine, true, true); // Reuse the engine and reopen the camera after teardown.
                }
            }
            System.out.println("PASS: native negotiation, remote frames, data channel and cleanup");
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        } finally {
            watchdog.shutdownNow();
        }
    }

    private static WebRtcJavaEngine createEngine(boolean camera) {
        // Explicitly missing devices prevent fallback to physical capture unless requested.
        return new WebRtcJavaEngine("native-smoke-no-microphone", camera ? "" : "native-smoke-no-camera", "native-smoke-no-speaker");
    }

    private static void exchange(WebRtcJavaEngine engine, boolean outgoingCall, boolean camera) throws Exception {
        var signals = new LinkedBlockingQueue<RtcSignalEnvelope>();
        var ice = new LinkedBlockingQueue<RTCIceCandidate>();
        var connected = new CountDownLatch(1);
        var receivedFrame = new CountDownLatch(1);
        var localFrame = new CountDownLatch(1);
        var receivedData = new CountDownLatch(1);
        var channelOpen = new CountDownLatch(1);
        var channels = new ConcurrentLinkedQueue<RTCDataChannel>();
        boolean video = !outgoingCall || camera;
        var mode = video ? RtcSessionMode.VIDEO : RtcSessionMode.DATA;
        Consumer<RtcEvent> events = event -> {
            if (event instanceof RtcStateChangedEvent state && state.state() == RtcSessionState.CONNECTED) connected.countDown();
            if (event instanceof RtcVideoFrameEvent frame && !frame.local()) receivedFrame.countDown();
            if (event instanceof RtcVideoFrameEvent frame && frame.local()) localFrame.countDown();
            if (event instanceof RtcDataMessageEvent message && "native-smoke".equals(message.payload())) receivedData.countDown();
            if (event instanceof RtcStateChangedEvent state && "RTCDataChannel is open".equals(state.message())) channelOpen.countDown();
        };
        var factory = new PeerConnectionFactory();
        var source = new CustomVideoSource();
        var track = factory.createVideoTrack("synthetic-video", source);
        var peer = factory.createPeerConnection(new RTCConfiguration(), new PeerConnectionObserver() {
            @Override public void onIceCandidate(RTCIceCandidate candidate) { ice.add(candidate); }
            @Override public void onDataChannel(RTCDataChannel channel) { channels.add(channel); }
        });
        String id = java.util.UUID.randomUUID().toString();
        try {
            if (video) peer.addTrack(track, List.of("synthetic-stream"));
            if (outgoingCall) {
                engine.startSession(id, "desktop", "synthetic-peer", mode, "securelan-data", "", "", signals::add, events);
            } else {
                var configuration = new RTCDataChannelInit();
                configuration.ordered = true;
                configuration.id = -1;
                configuration.maxPacketLifeTime = -1;
                configuration.maxRetransmits = -1;
                channels.add(peer.createDataChannel("securelan-data", configuration));
                var sdp = createDescription(peer, true);
                setDescription(peer, sdp, true);
                var signal = RtcSignalEnvelope.offer("synthetic-peer", "desktop", mode, "securelan-data", sdp.sdp);
                id = signal.sessionId();
                engine.handleRemoteSignal(signal, signals::add, events);
            }
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            boolean answered = false;
            var pending = new java.util.ArrayList<RTCIceCandidate>();
            while ((connected.getCount() != 0 || receivedData.getCount() != 0 || channelOpen.getCount() != 0 || (video && receivedFrame.getCount() != 0) ||
                    (camera && localFrame.getCount() != 0)) && System.nanoTime() < deadline) {
                RTCIceCandidate candidate;
                while ((candidate = ice.poll()) != null) {
                    engine.handleRemoteSignal(RtcSignalEnvelope.iceCandidate(id, "synthetic-peer", "desktop",
                            mode, "securelan-data", candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex), signals::add, events);
                }
                RtcSignalEnvelope outgoing;
                while ((outgoing = signals.poll()) != null) {
                    if (outgoing.type() == RtcSignalType.OFFER) {
                        setDescription(peer, new RTCSessionDescription(RTCSdpType.OFFER, outgoing.sdp()), false);
                        var answer = createDescription(peer, false);
                        setDescription(peer, answer, true);
                        engine.handleRemoteSignal(RtcSignalEnvelope.answer(id, "synthetic-peer", "desktop", mode,
                                "securelan-data", answer.sdp), signals::add, events);
                        answered = true;
                        pending.forEach(peer::addIceCandidate);
                        pending.clear();
                    } else if (outgoing.type() == RtcSignalType.ANSWER) {
                        setDescription(peer, new RTCSessionDescription(RTCSdpType.ANSWER, outgoing.sdp()), false);
                        answered = true;
                        pending.forEach(peer::addIceCandidate);
                        pending.clear();
                    } else if (outgoing.type() == RtcSignalType.ICE_CANDIDATE) {
                        var remoteIce = new RTCIceCandidate(outgoing.sdpMid(), outgoing.sdpMLineIndex(), outgoing.iceCandidate());
                        if (answered) peer.addIceCandidate(remoteIce); else pending.add(remoteIce);
                    } else if (outgoing.type() == RtcSignalType.ERROR) {
                        throw new AssertionError(outgoing.message());
                    }
                }
                var buffer = NativeI420Buffer.allocate(64, 48);
                var frame = new VideoFrame(buffer, System.nanoTime());
                try { source.pushFrame(frame); } finally { frame.release(); }
                if (receivedData.getCount() != 0) {
                    for (var channel : channels) {
                        if (channel.getState() == RTCDataChannelState.OPEN) {
                            channel.send(new RTCDataChannelBuffer(java.nio.ByteBuffer.wrap("native-smoke".getBytes(java.nio.charset.StandardCharsets.UTF_8)), false));
                        }
                    }
                }
                TimeUnit.MILLISECONDS.sleep(50);
            }
            if (!answered || connected.getCount() != 0 || receivedData.getCount() != 0 || channelOpen.getCount() != 0 || (video && receivedFrame.getCount() != 0) ||
                    (camera && localFrame.getCount() != 0)) {
                throw new AssertionError("Missing native result: answer=" + answered + " connected=" + connected.getCount()
                        + " data=" + receivedData.getCount() + " channel=" + channelOpen.getCount()
                        + " remoteFrame=" + receivedFrame.getCount() + " localFrame=" + localFrame.getCount());
            }
            engine.closeSession(id, events);
            System.out.println("PASS: " + (outgoingCall ? "outgoing " : "incoming ") + mode + " camera=" + camera);
        } finally {
            engine.closeSession(id, events);
            for (var sender : peer.getSenders()) peer.removeTrack(sender);
            for (var channel : channels) channel.close();
            peer.close();
            for (var channel : channels) channel.dispose();
            track.dispose();
            source.dispose();
            factory.dispose();
        }
    }

    private static RTCSessionDescription createDescription(RTCPeerConnection peer, boolean offer) throws Exception {
        var complete = new CompletableFuture<RTCSessionDescription>();
        var observer = new CreateSessionDescriptionObserver() {
            @Override public void onSuccess(RTCSessionDescription description) { complete.complete(description); }
            @Override public void onFailure(String error) { complete.completeExceptionally(new AssertionError(error)); }
        };
        if (offer) peer.createOffer(new RTCOfferOptions(), observer); else peer.createAnswer(new RTCAnswerOptions(), observer);
        return complete.get(5, TimeUnit.SECONDS);
    }

    private static void setDescription(RTCPeerConnection peer, RTCSessionDescription description, boolean local) throws Exception {
        var complete = new CompletableFuture<Void>();
        var observer = new SetSessionDescriptionObserver() {
            @Override public void onSuccess() { complete.complete(null); }
            @Override public void onFailure(String error) { complete.completeExceptionally(new AssertionError(error)); }
        };
        if (local) peer.setLocalDescription(description, observer); else peer.setRemoteDescription(description, observer);
        complete.get(5, TimeUnit.SECONDS);
    }
}
