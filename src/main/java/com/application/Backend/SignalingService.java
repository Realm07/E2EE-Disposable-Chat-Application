package com.application.Backend;

import com.application.Backend.dto.ClientSignalingMessage; // Your client-side DTO
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class SignalingService {
    private WebSocketClient wsClient;
    private final NetworkListener networkListener; // Your existing listener interface
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String currentUserNameForSignaling; // To send in join messages
    private String currentRoomForSignaling;

    private static final String SIGNALING_SERVER_URL = "wss://anochat-webrtc-signaling.onrender.com/signaling";

    public SignalingService(NetworkListener networkListener) {
        this.networkListener = networkListener;
    }

    public void connectAndJoin(String userName, String roomName) {
        this.currentUserNameForSignaling = userName;
        this.currentRoomForSignaling = roomName;

        if (wsClient != null && wsClient.isOpen()) { // Or a custom flag
            System.out.println("[SignalingService] Already connected. Closing existing first to ensure fresh join.");
            wsClient.close();
            // wsClient = null; // Important to allow re-creation
        }

        try {
            System.out.println("[SignalingService] Creating new WebSocketClient for " + SIGNALING_SERVER_URL);
            wsClient = new WebSocketClient(new URI(SIGNALING_SERVER_URL)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("[SignalingService] Connection opened to signaling server.");
                    // After connection is open, send the join message
                    ClientSignalingMessage joinMsg = new ClientSignalingMessage(
                            "join",
                            currentRoomForSignaling,
                            Map.of("user", currentUserNameForSignaling, "room", currentRoomForSignaling)
                    );
                    // The server-side handler expects 'fromUser' from the client, or extracts from payload.
                    // To be explicit, also set the top-level fromUser if your DTO allows and server uses it:
                    joinMsg.setFromUser(currentUserNameForSignaling);
                    sendSignalingMessage(joinMsg);
                    // Note: networkListener.onConnected() is now for P2P connections, not this signaling WS connection
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("[SignalingService] Received raw signal: " + message);
                    try {
                        ClientSignalingMessage signalingMessage = objectMapper.readValue(message, ClientSignalingMessage.class);
                        networkListener.onSignalingMessage(signalingMessage); // Delegate to ChatController
                    } catch (JsonProcessingException e) {
                        System.err.println("[SignalingService] Error deserializing signaling message: " + e.getMessage());
                        networkListener.onError("Signaling JSON Error: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[SignalingService] Signaling connection closed. Code: " + code + ", Reason: '" + reason + "', Remote: " + remote);
                    networkListener.onSignalingDisconnected(); // Notify controller
                    wsClient = null; // Nullify to allow fresh connection
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[SignalingService] Signaling WebSocket error: " + ex.getMessage());
                    ex.printStackTrace();
                    networkListener.onError("Signaling Connection Error: " + ex.getMessage(), ex);
                    wsClient = null; // Nullify on error to allow fresh connection attempt
                }
            };
            System.out.println("[SignalingService] Attempting to connect to signaling server: " + SIGNALING_SERVER_URL);
            wsClient.connect(); // Asynchronous connection attempt
        } catch (URISyntaxException e) {
            System.err.println("[SignalingService] Invalid signaling server URI: " + e.getMessage());
            networkListener.onError("Invalid Signaling URI", e);
        }
    }

    public void sendSignalingMessage(ClientSignalingMessage message) {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                System.out.println("[SignalingService] Sending signal: " + jsonMessage);
                wsClient.send(jsonMessage);
            } catch (JsonProcessingException e) {
                System.err.println("[SignalingService] Error serializing signaling message for sending: " + e.getMessage());
            }
        } else {
            System.err.println("[SignalingService] Cannot send signal, WebSocket client is not open or is null.");
            // Optionally: queue messages or notify user of connection issue
        }
    }

    public void disconnect() {
        if (wsClient != null) {
            System.out.println("[SignalingService] Disconnecting signaling client...");
            if (currentUserNameForSignaling != null && currentRoomForSignaling != null && wsClient.isOpen()) {
                // Send a leave message if connected
                ClientSignalingMessage leaveMsg = new ClientSignalingMessage(
                        "leave",
                        currentRoomForSignaling,
                        Map.of("user", currentUserNameForSignaling, "room", currentRoomForSignaling)
                );
                leaveMsg.setFromUser(currentUserNameForSignaling);
                sendSignalingMessage(leaveMsg);
            }
            wsClient.close(); // This will trigger onClose event
            wsClient = null;
        }
    }

    public boolean isConnected() {
        return wsClient != null && wsClient.isOpen();
    }
}