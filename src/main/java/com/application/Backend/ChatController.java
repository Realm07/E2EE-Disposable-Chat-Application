package com.application.Backend;

import com.application.FrontEnd.ChatRoom;
import com.application.FrontEnd.MainFrame;
import com.application.FrontEnd.components.MessageCellRenderer.ChatMessage;
import com.application.Backend.dto.ClientSignalingMessage; // Your DTO
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// dev.onvoid.webrtc IMPORTS (Actual API usage will be in TODO sections for now)
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCIceServer;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelObserver;
import dev.onvoid.webrtc.RTCDataChannelInit;
import dev.onvoid.webrtc.RTCDataChannelState;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.RTCDataChannelBuffer;
import dev.onvoid.webrtc.RTCIceConnectionState; // For SimplePeerConnectionObserver
import dev.onvoid.webrtc.RTCSignalingState;   // For SimplePeerConnectionObserver
import dev.onvoid.webrtc.RTCIceGatheringState; // For SimplePeerConnectionObserver
import dev.onvoid.webrtc.media.MediaStream;     // For SimplePeerConnectionObserver
import dev.onvoid.webrtc.RTCOfferOptions;
import dev.onvoid.webrtc.RTCAnswerOptions;

// Java standard imports
import javax.swing.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
// No need for Collectors if not used directly in this snippet yet

public class ChatController implements NetworkListener {

    private MainFrame mainFrame;
    private ChatRoom chatRoomUI;
    private final EncryptionService encryptionService;
    private final SignalingService signalingService;
    private final FileUploader fileUploader; // Still here for now, though file sharing will change
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String currentUsername;
    private String activeRoomName;
    private boolean currentRoomE2EEKeyDerived = false;
    private Set<String> joinedRoomNames = new HashSet<>();
    private Map<String, List<ChatMessage>> roomChatHistories = new HashMap<>();
    private Map<String, String> pendingSentPrivateChatProposals = new HashMap<>(); // This will change significantly for WebRTC

    // --- WebRTC specific state using dev.onvoid.webrtc ---
    private PeerConnectionFactory peerConnectionFactory;
    private final Map<String, RTCPeerConnection> peerConnections = new ConcurrentHashMap<>();
    private final Map<String, RTCDataChannel> dataChannels = new ConcurrentHashMap<>();

    private static final List<RTCIceServer> ICE_SERVERS = new ArrayList<>();
    static {
        RTCIceServer stunServer1 = new RTCIceServer();
        stunServer1.urls.add("stun:stun.l.google.com:19302");
        ICE_SERVERS.add(stunServer1);
        RTCIceServer stunServer2 = new RTCIceServer();
        stunServer2.urls.add("stun:stun1.l.google.com:19302");
        ICE_SERVERS.add(stunServer2);
        // TODO: Add your TURN server configurations here when available
    }

    public ChatController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.encryptionService = new EncryptionService();
        this.signalingService = new SignalingService(this); // Pass this controller as the listener
        this.fileUploader = new FileUploader(); // Keep for now, will adapt file sharing
        initializeWebRTCStack();
        System.out.println("[Controller] Initialized with SignalingService for WebRTC (dev.onvoid.webrtc).");
    }

    private void initializeWebRTCStack() {
        System.out.println("[Controller] Initializing WebRTC stack (dev.onvoid.webrtc)...");
        try {
            // Ensure dev.onvoid.webrtc.Loader or similar has run if required by the library
            // For example, some libraries do: Webrtc.Loader.load();
            // From their docs: "The native library is loaded automatically by the static initializer of the dev.onvoid.webrtc. আল্লাহর.Loader class"
            // So, simply instantiating classes from the library might be enough to trigger loading.
            this.peerConnectionFactory = new PeerConnectionFactory();
            if (this.peerConnectionFactory == null) {
                throw new RuntimeException("PeerConnectionFactory could not be created.");
            }
            System.out.println("[Controller] PeerConnectionFactory created successfully (dev.onvoid.webrtc).");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("[Controller] FATAL: UnsatisfiedLinkError creating PeerConnectionFactory. Native libraries not found/loaded: " + ule.getMessage());
            ule.printStackTrace();
            showErrorDialog("FATAL: WebRTC native libraries not found for your OS. Please check application setup.\n" + ule.getMessage());
        } catch (Exception e) {
            System.err.println("[Controller] FATAL: Error creating PeerConnectionFactory (dev.onvoid.webrtc): " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("FATAL: Could not initialize WebRTC components.\n" + e.getMessage());
        }
    }

    // --- Login / Room Entry (Mostly high-level logic remains) ---
    public void joinInitialRoom(String username, String roomName, String password) {
        System.out.println("[Controller] Initial Join attempt: user=" + username + ", room=" + roomName);
        if (username.isEmpty() || roomName.isEmpty() || password.isEmpty()) {
            showErrorDialog("Username, Room Name, and Password cannot be empty."); return;
        }
        this.currentUsername = username;
        List<String> initialUserList = new ArrayList<>();
        if (this.currentUsername != null) initialUserList.add(this.currentUsername);
        mainFrame.switchToChatRoom(this.currentUsername, roomName, initialUserList);
        joinOrSwitchToRoom(roomName, password); // This now uses WebRTC signaling
    }

    public void joinPublicRoom(String username, String roomName) {
        // ... (Similar logic to joinInitialRoom, gets predefinedPassword) ...
        System.out.println("[Controller] PUBLIC Room Join attempt: user=" + username + ", room=" + roomName);
        if (username.isEmpty() || roomName.isEmpty()) {
            showErrorDialog("Username and Room Name cannot be empty."); return;
        }
        // ... (isPublicRoom check, get predefinedPassword) ...
        String predefinedPassword = "TEMP_PUBLIC_PASS"; // Placeholder from your old logic
        if (!isPublicRoom(roomName)) { showErrorDialog("Error: Not a known public room."); return;} // Simplified
        predefinedPassword = "Public" + roomName + "Key_!@#"; // Simplification, use your PUBLIC_ROOM_KEYS
        if (predefinedPassword == null) {showErrorDialog("Error: Key for public room missing."); return;}


        this.currentUsername = username;
        List<String> initialUserList = new ArrayList<>();
        if (this.currentUsername != null) initialUserList.add(this.currentUsername);
        mainFrame.switchToChatRoom(this.currentUsername, roomName, initialUserList);
        joinOrSwitchToRoom(roomName, predefinedPassword);
    }

    private boolean isPublicRoom(String roomName) { // Placeholder from old controller
        // return PUBLIC_ROOM_KEYS.containsKey(roomName); // If you have PUBLIC_ROOM_KEYS map
        return roomName.startsWith("Alpha") || roomName.startsWith("Bravo"); // Simplified
    }

    public void joinOrSwitchToRoom(String roomName, String password) {
        System.out.println("[Controller] Attempting to join/switch to room: " + roomName + " (WebRTC Signaling)");
        if (Objects.equals(this.activeRoomName, roomName) && signalingService.isConnected() && !peerConnections.isEmpty()) {
            System.out.println("[Controller] Already connected to room " + roomName + " via WebRTC. Refreshing UI state.");
            if (chatRoomUI != null) {
                List<String> currentPeersForUI = new ArrayList<>(peerConnections.keySet());
                if (this.currentUsername != null) currentPeersForUI.add(this.currentUsername);
                chatRoomUI.updateUIForRoomSwitch(this.activeRoomName, currentPeersForUI);
            }
            return;
        }

        closeAllP2PConnectionsAndState(); // Closes old P2P and informs old room via signaling if connected

        if (!encryptionService.deriveRoomKey(roomName, password)) {
            showErrorDialog("Failed to derive key for room '" + roomName + "'. Check password.");
            this.activeRoomName = null; this.currentRoomE2EEKeyDerived = false;
            if (chatRoomUI != null) { /* Update UI for failure */ }
            return;
        }
        this.activeRoomName = roomName; this.currentRoomE2EEKeyDerived = true;

        if (chatRoomUI != null) {
            if (!joinedRoomNames.contains(roomName)) { chatRoomUI.addRoomTab(roomName); joinedRoomNames.add(roomName); }
            List<String> selfList = (this.currentUsername != null) ? Collections.singletonList(this.currentUsername) : Collections.emptyList();
            chatRoomUI.updateUIForRoomSwitch(this.activeRoomName, selfList);
            chatRoomUI.displaySystemMessage("Joining room " + this.activeRoomName + "...");
        }

        if (this.currentUsername != null) {
            signalingService.connectAndJoin(this.currentUsername, this.activeRoomName); // This sends "join" to signaling server
        } else {
            System.err.println("[Controller] Cannot join room: currentUsername is not set.");
            showErrorDialog("Cannot join room: User not identified.");
        }
    }

    public void requestRoomSwitch(String targetRoomName) {
        // This logic remains similar to before, but joinOrSwitchToRoom now does WebRTC
        System.out.println("[Controller] UI requested switch to room: " + targetRoomName);
        if (Objects.equals(targetRoomName, this.activeRoomName) && signalingService.isConnected() && !peerConnections.isEmpty()) {
            System.out.println("[Controller] Already in room " + targetRoomName + " via WebRTC. Ensuring UI is current.");
            if (chatRoomUI != null) SwingUtilities.invokeLater(() -> chatRoomUI.updateUIForRoomSwitch(targetRoomName, new ArrayList<>(peerConnections.keySet())));
            return;
        }
        final String previousActiveRoomName = this.activeRoomName;
        // ... (logic for public room password or prompting for private room password - same as before) ...
        String passwordForTargetRoom = "dummyPassword"; // Replace with actual password retrieval logic
        if (isPublicRoom(targetRoomName)) {
            // passwordForTargetRoom = PUBLIC_ROOM_KEYS.get(targetRoomName);
            passwordForTargetRoom = "Public" + targetRoomName + "Key_!@#"; // Simplification
            if(passwordForTargetRoom == null) {showErrorDialog("Key missing for public room " + targetRoomName); return;}
        } else {
            // Prompt for password using JOptionPane
            String pass = JOptionPane.showInputDialog(mainFrame, "Enter password for private room: " + targetRoomName);
            if (pass == null || pass.trim().isEmpty()) {
                if (chatRoomUI != null && previousActiveRoomName != null) {
                    chatRoomUI.updateUIForRoomSwitch(previousActiveRoomName, getOnlineUsersForRoom(previousActiveRoomName));
                }
                return; // User cancelled or entered empty password
            }
            passwordForTargetRoom = pass;
        }
        joinOrSwitchToRoom(targetRoomName, passwordForTargetRoom);
    }


    private void closeAllP2PConnectionsAndState() {
        System.out.println("[Controller] Closing all P2P connections and state (dev.onvoid.webrtc).");
        if (signalingService.isConnected() && this.activeRoomName != null && this.currentUsername != null) {
            // Send a "leave" signal for the current room *before* closing P2P and disconnecting signaling for this room context.
            ClientSignalingMessage leaveMsg = new ClientSignalingMessage("leave", this.activeRoomName, Map.of("user", this.currentUsername, "room", this.activeRoomName));
            leaveMsg.setFromUser(this.currentUsername);
            signalingService.sendSignalingMessage(leaveMsg);
        }
        // Now close actual P2P connections
        peerConnections.forEach((peerId, pc) -> {
            if (pc != null) { System.out.println("Closing RTCPeerConnection to: " + peerId); try { pc.close(); } catch (Exception e) { System.err.println("Err closing PC " + peerId + ":" + e.getMessage());} }
        });
        peerConnections.clear();
        dataChannels.forEach((peerId, dc) -> {
            if (dc != null) { System.out.println("Closing RTCDataChannel to: " + peerId); try { dc.close(); } catch (Exception e) { System.err.println("Err closing DC " + peerId + ":" + e.getMessage());} }
        });
        dataChannels.clear();
        if (chatRoomUI != null) { chatRoomUI.clearUserList(); } // Clears UI list except self
    }

    public void sendMessage(String plainTextMessage) {
        if (!currentRoomE2EEKeyDerived) { if (chatRoomUI != null) chatRoomUI.displaySystemMessage("Cannot send: Room security not established."); return; }
        if (plainTextMessage == null || plainTextMessage.trim().isEmpty()) return;
        if (activeRoomName == null) { if (chatRoomUI != null) chatRoomUI.displaySystemMessage("Not in a room to send message."); return; }
        if (dataChannels.isEmpty()) { if (chatRoomUI != null) chatRoomUI.displaySystemMessage("No active P2P connections to send message."); return; }

        try {
            String encryptedChatPayload = encryptionService.encrypt(plainTextMessage);
            MessageData appMessage = new MessageData(this.currentUsername, encryptedChatPayload, this.activeRoomName);
            String appMessageJson = objectMapper.writeValueAsString(appMessage);
            ByteBuffer buffer = ByteBuffer.wrap(appMessageJson.getBytes(StandardCharsets.UTF_8));
            RTCDataChannelBuffer dataBufferToSend = new RTCDataChannelBuffer(buffer, false);

            System.out.println("[Controller] Sending CHAT over " + dataChannels.size() + " P2P DataChannels.");
            final List<String> failedPeers = new ArrayList<>();
            dataChannels.forEach((peerId, dataChannel) -> {
                if (dataChannel != null && dataChannel.getState() == RTCDataChannelState.OPEN) {
                    try {
                        dataChannel.send(dataBufferToSend); // This call needs try-catch
                    } catch (Exception e) { // Catch the generic Exception declared by send()
                        System.err.println("[Controller] Failed to send CHAT to peer " + peerId + " via DataChannel: " + e.getMessage());
                        e.printStackTrace(); // Log for debugging
                        failedPeers.add(peerId);
                    }
                } else {
                    System.err.println("[Controller] DataChannel to " + peerId + " not open or null for sending CHAT.");
                    failedPeers.add(peerId); // Consider it failed if DC not open
                }
            });

            if (!failedPeers.isEmpty() && chatRoomUI != null) {
                final String failedPeersString = String.join(", ", failedPeers);
                SwingUtilities.invokeLater(() -> chatRoomUI.displaySystemMessage("Note: Message may not have reached all peers: " + failedPeersString));
            }

            // Display self-message immediately
            if (chatRoomUI != null) {
                final String finalPlainTextMessage = plainTextMessage; // For lambda
                SwingUtilities.invokeLater(() -> chatRoomUI.appendMessage(this.currentUsername, finalPlainTextMessage, "STANDARD"));
            }
            synchronized (roomChatHistories) {
                roomChatHistories.computeIfAbsent(activeRoomName, k -> new ArrayList<>())
                        .add(new ChatMessage(this.currentUsername, plainTextMessage, "STANDARD"));
            }

        } catch (Exception e) { // Catches E2EE or JSON serialization errors before P2P send attempt
            System.err.println("[Controller] Error preparing P2P chat message: " + e.getMessage());
            e.printStackTrace();
            if (chatRoomUI != null) {
                SwingUtilities.invokeLater(() -> chatRoomUI.displaySystemMessage("Error sending message due to: " + e.getMessage()));
            }
        }
    }

    // --- NetworkListener Implementation (Signaling & P2P events) ---
    @Override
    public void onSignalingMessage(ClientSignalingMessage sigMessage) {
        SwingUtilities.invokeLater(() -> {
            if (this.activeRoomName == null || (sigMessage.getRoom() != null && !this.activeRoomName.equals(sigMessage.getRoom()))) {
                System.out.println("[Controller] Ignoring signal for room " + sigMessage.getRoom() + " (current: " + this.activeRoomName + ")"); return;
            }
            String fromUser = sigMessage.getFromUser();
            System.out.println("[Controller] Signal: Type='" + sigMessage.getType() + "', From='" + fromUser + "', To='" + sigMessage.getToUser() + "' in room '" + sigMessage.getRoom() + "'");
            try {
                switch (sigMessage.getType().toLowerCase()) {
                    case "peers":
                        if (sigMessage.getPayload() != null) {
                            ClientSignalingMessage.RoomPeersPayload peersPayload = objectMapper.convertValue(sigMessage.getPayload(), ClientSignalingMessage.RoomPeersPayload.class);
                            if (peersPayload != null && peersPayload.getUsers() != null) {
                                peersPayload.getUsers().forEach(peerUserName -> {
                                    if (!Objects.equals(peerUserName, this.currentUsername) &&
                                            !peerConnections.containsKey(peerUserName)) { // Only connect if not already trying
                                        // Rule: Initiator is the one with the lexicographically smaller username.
                                        if (this.currentUsername.compareTo(peerUserName) < 0) {
                                            System.out.println("[P2P Strategy] I ("+this.currentUsername+") will offer to " + peerUserName);
                                            initiateP2PConnectionAndOffer(peerUserName);
                                        } else {
                                            System.out.println("[P2P Strategy] I ("+this.currentUsername+") will wait for offer from " + peerUserName);
                                        }
                                    }
                                });
                            }
                        }
                        break;

                    case "user_joined":
                        if (sigMessage.getPayload() != null) {
                            ClientSignalingMessage.UserEventPayload userEvent = objectMapper.convertValue(sigMessage.getPayload(), ClientSignalingMessage.UserEventPayload.class);
                            String newPeerUserName = userEvent.getUser();
                            if (newPeerUserName != null && !Objects.equals(newPeerUserName, this.currentUsername) &&
                                    !peerConnections.containsKey(newPeerUserName)) { // Only connect if not already trying
                                System.out.println("[Controller] User '" + newPeerUserName + "' joined (signaled).");
                                if (chatRoomUI != null) chatRoomUI.addUserToList(newPeerUserName);
                                // Rule: Initiator is the one with the lexicographically smaller username.
                                if (this.currentUsername.compareTo(newPeerUserName) < 0) {
                                    System.out.println("[P2P Strategy] New user " + newPeerUserName + ". I ("+this.currentUsername+") will offer.");
                                    initiateP2PConnectionAndOffer(newPeerUserName);
                                } else {
                                    System.out.println("[P2P Strategy] New user " + newPeerUserName + ". I ("+this.currentUsername+") will wait for their offer.");
                                }
                            }
                        }
                        break;
                    case "user_left": // A user left the room
                        if (sigMessage.getPayload() != null) {
                            ClientSignalingMessage.UserEventPayload userEvent = objectMapper.convertValue(sigMessage.getPayload(), ClientSignalingMessage.UserEventPayload.class);
                            String leftPeerUserName = userEvent.getUser();
                            if (leftPeerUserName != null) {
                                System.out.println("[Controller] User '" + leftPeerUserName + "' left (signaled).");
                                closeP2PConnectionWithPeer(leftPeerUserName); // This updates UI
                            }
                        }
                        break;
                    case "offer":
                        if (fromUser != null && sigMessage.getPayload() != null) {
                            ClientSignalingMessage.SdpPayload sdpData = objectMapper.convertValue(sigMessage.getPayload(), ClientSignalingMessage.SdpPayload.class);
                            handleReceivedOffer(fromUser, sdpData.getSdp());
                        }
                        break;
                    case "answer":
                        if (fromUser != null && sigMessage.getPayload() != null) {
                            ClientSignalingMessage.SdpPayload sdpData = objectMapper.convertValue(sigMessage.getPayload(), ClientSignalingMessage.SdpPayload.class);
                            handleReceivedAnswer(fromUser, sdpData.getSdp());
                        }
                        break;
                    case "candidate":
                        if (fromUser != null && sigMessage.getPayload() != null) {
                            ClientSignalingMessage.IceCandidatePayload iceData = objectMapper.convertValue(sigMessage.getPayload(), ClientSignalingMessage.IceCandidatePayload.class);
                            handleReceivedIceCandidate(fromUser, iceData);
                        }
                        break;
                    default: System.out.println("[Controller] Unknown signaling message type: " + sigMessage.getType());
                }
            } catch (Exception e) {
                System.err.println("[Controller] Error processing signaling message: " + e.getMessage()); e.printStackTrace();
                onError("Error Processing Signal: " + sigMessage.getType(), e);
            }
        });
    }

    @Override
    public void onSignalingDisconnected() {
        SwingUtilities.invokeLater(() -> {
            System.out.println("[Controller] Disconnected from Signaling Server.");
            if (chatRoomUI != null) {
                chatRoomUI.displaySystemMessage("Warning: Disconnected from signaling service. Cannot establish new P2P connections.");
            }
            // Existing P2P connections might remain active for a while.
            // No need to tear them down immediately unless app policy dictates.
        });
    }

    @Override
    public void onSignalingConnectionError(String errorMessage, Exception e) {
        SwingUtilities.invokeLater(() -> {
            System.err.println("[Controller] Signaling Connection Error: " + errorMessage + (e != null ? " - " + e.getMessage() : ""));
            if (e != null) e.printStackTrace();
            if (chatRoomUI != null) {
                chatRoomUI.displaySystemMessage("Signaling Error: " + errorMessage);
            } else {
                showErrorDialog("Signaling Connection Error: " + errorMessage);
            }
        });
    }

    // --- WebRTC P2P Events (Called by Your Observer Implementations) ---
    @Override
    public void onPeerConnected(String peerUserName) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("[Controller] P2P DataChannel is OPEN with: " + peerUserName);
            if (chatRoomUI != null) {
                chatRoomUI.addUserToList(peerUserName);
                chatRoomUI.displaySystemMessage("Secured P2P connection established with " + peerUserName + ".");

                try {
                    // Optional: Send a P2P heartbeat or presence confirmation
                    MessageData presenceConfirmation = new MessageData(MessageType.HEARTBEAT, this.currentUsername, this.activeRoomName);
                    String msgJson = objectMapper.writeValueAsString(presenceConfirmation);
                    RTCDataChannel dc = dataChannels.get(peerUserName); // Get the specific data channel for this peer

                    if (dc != null && dc.getState() == RTCDataChannelState.OPEN) {
                        try {
                            dc.send(new RTCDataChannelBuffer(ByteBuffer.wrap(msgJson.getBytes(StandardCharsets.UTF_8)), false));
                            System.out.println("[Controller] Sent P2P HEARTBEAT confirmation to " + peerUserName);
                        } catch (Exception e) { // Catch Exception from send()
                            System.err.println("[Controller] Failed to send P2P HEARTBEAT to " + peerUserName + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        System.err.println("[Controller] DataChannel for " + peerUserName + " not open or null for sending P2P HEARTBEAT.");
                    }
                } catch (JsonProcessingException e) {
                    System.err.println("[Controller] Error creating P2P HEARTBEAT message for " + peerUserName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onPeerDisconnected(String peerUserName) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("[Controller] P2P DataChannel is CLOSED/FAILED with: " + peerUserName);
            RTCPeerConnection pc = peerConnections.remove(peerUserName);
            if (pc != null) { try { pc.close(); } catch (Exception e) {} } // Ensure PC is closed
            RTCDataChannel dc = dataChannels.remove(peerUserName);
            if (dc != null) { try { dc.close(); } catch (Exception e) {} } // Ensure DC is closed

            if (chatRoomUI != null) {
                chatRoomUI.removeUserFromList(peerUserName);
                chatRoomUI.displaySystemMessage("P2P connection with " + peerUserName + " lost.");
            }
        });
    }

    @Override
    public void onMessageReceived(MessageData appMessageData) { // Application MessageData received over a P2P DataChannel
        SwingUtilities.invokeLater(() -> {
            // Check if messageData and its critical fields are null - this fixes an earlier error from logs
            if (appMessageData == null || appMessageData.sender == null || appMessageData.type == null || appMessageData.getRoomContext() == null) {
                System.err.println("[Controller P2P] Received invalid AppMessageData (null fields).");
                return;
            }

            final String sender = appMessageData.sender;
            final String roomContext = appMessageData.getRoomContext();

            if (activeRoomName == null || !activeRoomName.equals(roomContext)) {
                System.out.println("[Controller P2P] Ignoring AppMessage for room " + roomContext + " as current is " + activeRoomName);
                return;
            }
            System.out.println("[Controller P2P] AppMessage (" + appMessageData.type + ") from " + sender + " in " + roomContext);

            switch (appMessageData.type) {
                case CHAT:
                    if (!currentRoomE2EEKeyDerived) { /* ... error ... */ return; }
                    if (appMessageData.encryptedData == null) { /* ... error ... */ return; }
                    try {
                        String decryptedText = encryptionService.decrypt(appMessageData.encryptedData);
                        if (chatRoomUI != null) chatRoomUI.appendMessage(sender, decryptedText, "STANDARD");
                        synchronized (roomChatHistories) { roomChatHistories.computeIfAbsent(activeRoomName, k->new ArrayList<>()).add(new ChatMessage(sender, decryptedText,"STANDARD"));}
                    } catch (Exception e) { if (chatRoomUI != null) chatRoomUI.displaySystemMessage("Error decrypting message from " + sender); }
                    break;
                case FILE_SHARE_OFFER:
                    if (chatRoomUI != null) chatRoomUI.displayFileShareOffer(appMessageData);
                    break;
                case HEARTBEAT: // Example: P2P Heartbeat for liveness over data channel
                    System.out.println("[Controller P2P] HEARTBEAT received from " + sender + ". (Liveness confirmed for P2P channel).");
                    break;
                default: System.out.println("[Controller P2P] Unhandled AppMessage type: " + appMessageData.type);
            }
        });
    }

    @Override
    public void onError(String message, Exception e) { // Generic error, also used by some P2P observer failures
        SwingUtilities.invokeLater(() -> {
            // Fix for variable 'message' might not have been initialized.
            // The signature for this method from NetworkListener uses 'genericMessage', let's align.
            // Or, ensure 'message' is always passed to this method call.
            // For now, assuming 'message' IS the parameter name from NetworkListener.
            String errorMessage = (message != null) ? message : "An unspecified error occurred.";
            System.err.println("[Controller] onError: " + errorMessage);
            if (e != null) e.printStackTrace();
            if (chatRoomUI != null) {
                chatRoomUI.displaySystemMessage("[Error] " + errorMessage);
            } else {
                showErrorDialog("Error: " + errorMessage);
            }
        });
    }

    // --- User Action Methods (leaveRoom, etc.) ---
    public void leaveRoom() {
        System.out.println("[Controller] User '" + currentUsername + "' initiating full leave (or switching rooms).");
        String roomBeingLeft = this.activeRoomName; // Capture before nulling

        closeAllP2PConnectionsAndState(); // This sends "leave" signal for activeRoomName

        // If this leave is a precursor to joining another room, connectAndJoin in signalingService
        // will handle WebSocket reconnection if needed. If it's a full app logout, then disconnect.
        // For now, let's assume it could be either, and signalingService.connectAndJoin manages its own WS state.
        // If it's a full logout from the app, you might add:
        // signalingService.disconnect();

        this.activeRoomName = null;
        this.currentRoomE2EEKeyDerived = false;

        if (chatRoomUI != null) {
            final String userWhoLeft = this.currentUsername;
            SwingUtilities.invokeLater(() -> {
                if (chatRoomUI != null) {
                    chatRoomUI.displaySystemMessage("You (" + userWhoLeft + ") have left room " + roomBeingLeft + ".");
                    chatRoomUI.clearUserList();
                    chatRoomUI.setChatScrollPaneTitle("Not in a room");
                    // If this implies going to login: mainFrame.switchToLoginPage();
                }
            });
        }
        // if(mainFrame != null && (chatRoomUI == null || !chatRoomUI.isDisplayable())) {
        //     mainFrame.switchToLoginPage(); // If UI already gone
        // }
    }
    public void handleApplicationShutdown() {
        System.out.println("[Controller] Application shutdown requested.");
        leaveRoom(); // Perform graceful leave from current room/P2P
        signalingService.disconnect(); // Ensure signaling connection is closed
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose(); // Dispose of the WebRTC factory
            System.out.println("[Controller] PeerConnectionFactory disposed.");
        }
        System.out.println("[Controller] Shutdown cleanup complete.");
    }

    class SimpleDataChannelObserver implements dev.onvoid.webrtc.RTCDataChannelObserver { // CORRECT INTERFACE
        private final String peerId;
        private final ChatController controller;
        private final RTCDataChannel dataChannel;

        public SimpleDataChannelObserver(String peerId, ChatController controller, RTCDataChannel dataChannel) {
            this.peerId = peerId;
            this.controller = controller;
            this.dataChannel = dataChannel;
            System.out.println("[DCO][" + peerId + "][" + (dataChannel != null ? dataChannel.getLabel() : "UNKNOWN_DC_LABEL") + "] Observer created.");
        }

        @Override
        public void onStateChange() {
            // It's CRITICAL to get the dataChannel instance somehow,
            // or this observer needs to be specific to one data channel.
            // The constructor now takes the dataChannel it's observing.
            if (this.dataChannel == null) {
                System.err.println("[DCO][" + peerId + "] onStateChange called but dataChannel reference is null!");
                return;
            }
            RTCDataChannelState state = this.dataChannel.getState();
            System.out.println("[DCO][" + peerId + "][" + this.dataChannel.getLabel() + "] State changed to: " + state);
            if (state == RTCDataChannelState.OPEN) {
                SwingUtilities.invokeLater(() -> controller.onPeerConnected(peerId));
            } else if (state == RTCDataChannelState.CLOSED) {
                SwingUtilities.invokeLater(() -> controller.onPeerDisconnected(peerId));
            }
        }

        @Override
        public void onMessage(RTCDataChannelBuffer buffer) { // Parameter type from the interface
            if (this.dataChannel == null) {
                System.err.println("[DCO][" + peerId + "] onMessage called but dataChannel reference is null!");
                return;
            }
            try {
                ByteBuffer byteBuffer = buffer.data;
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                String messageJson = new String(bytes, StandardCharsets.UTF_8);
                System.out.println("[DCO][" + peerId + "][" + this.dataChannel.getLabel() + "] Message received (raw): " + messageJson.substring(0, Math.min(messageJson.length(), 100)) + "...");
                MessageData appMessageData = controller.objectMapper.readValue(messageJson, MessageData.class);
                controller.onMessageReceived(appMessageData); // This method should handle SwingUtilities.invokeLater
            } catch (Exception e) { // Catch broader exceptions during message processing
                System.err.println("[DCO][" + peerId + "][" + this.dataChannel.getLabel() + "] Error processing message: " + e.getMessage());
                e.printStackTrace();
                // Optionally notify controller of an error with this specific peer's data channel
                controller.onError("P2P Data Error from " + peerId, e);
            }
            // TODO: Check dev.onvoid.webrtc documentation if RTCDataChannelBuffer needs explicit release.
            // If buffer.dispose() or buffer.release() exists and is needed:
            // buffer.dispose();
        }

        // Error: SimpleDataChannelObserver is not abstract and does not override abstract method onBufferedAmountChange(long) in dev.onvoid.webrtc.RTCDataChannelObserver
        // This method MUST be present in RTCDataChannelObserver from dev.onvoid.webrtc
        @Override
        public void onBufferedAmountChange(long newAmount) { // Signature from error: onBufferedAmountChange(long)
            // The library might pass only the new amount.
            if (this.dataChannel == null) {
                System.err.println("[DCO][" + peerId + "] onBufferedAmountChange called but dataChannel reference is null!");
                return;
            }
            System.out.println("[DCO][" + peerId + "][" + this.dataChannel.getLabel() +
                    "] Buffered amount changed. New amount: " + newAmount +
                    ". (Actual method to get previous might be different or not available)");
            // You can get current buffered amount via this.dataChannel.getBufferedAmount()
        }
    }

    private void initiateP2PConnectionAndOffer(String peerUserName) {
        if (peerConnections.containsKey(peerUserName) || Objects.equals(peerUserName, this.currentUsername)) {
            System.out.println("[P2P] Connection attempt to " + peerUserName + " skipped: already exists or is self.");
            return;
        }
        System.out.println("[P2P] Initiating OFFER to peer: " + peerUserName);
        if (peerConnectionFactory == null) {
            System.err.println("[P2P] PeerConnectionFactory is NULL! Cannot initiate P2P for " + peerUserName);
            onError("WebRTC Init Error", new IllegalStateException("PeerConnectionFactory not initialized."));
            return;
        }

        RTCConfiguration rtcConfig = new RTCConfiguration();
        if (!ICE_SERVERS.isEmpty()) { rtcConfig.iceServers.addAll(ICE_SERVERS); }

        SimplePeerConnectionObserver pcObserver = new SimplePeerConnectionObserver(peerUserName, this);
        RTCPeerConnection peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, pcObserver);

        if (peerConnection == null) {
            System.err.println("[P2P] Failed to create RTCPeerConnection for " + peerUserName);
            onError("P2P Creation Error", new RuntimeException("Failed to create PeerConnection for " + peerUserName));
            return;
        }
        peerConnections.put(peerUserName, peerConnection);
        pcObserver.setPeerConnection(peerConnection); // Allow observer to have a reference
        System.out.println("[P2P] RTCPeerConnection created for " + peerUserName);

        RTCDataChannelInit dcInit = new RTCDataChannelInit();
        dcInit.ordered = true; // Reliable and ordered for chat messages
        // dcInit.negotiated = false; // Default - Data channel created by one peer and "announced"
        // dcInit.id = -1; // For non-negotiated, ID is assigned by the library

        RTCDataChannel dataChannel = peerConnection.createDataChannel("chat", dcInit);
        if (dataChannel == null) {
            System.err.println("[P2P] Failed to create RTCDataChannel for " + peerUserName);
            peerConnection.close();
            peerConnections.remove(peerUserName);
            onError("P2P DataChannel Error", new RuntimeException("Failed to create DataChannel for " + peerUserName));
            return;
        }
        System.out.println("[P2P] RTCDataChannel 'chat' created for " + peerUserName);
        dataChannels.put(peerUserName, dataChannel);
        setupDataChannelObserver(peerUserName, dataChannel); // Register observer for this new DC

        // Create Offer
        // RTCOfferOptions offerOptions = new RTCOfferOptions(); // If specific options needed
        // offerOptions.offerToReceiveAudio = false;
        // offerOptions.offerToReceiveVideo = false;
        System.out.println("[P2P] Creating SDP Offer for " + peerUserName + "...");
        dev.onvoid.webrtc.RTCOfferOptions offerOptions = new dev.onvoid.webrtc.RTCOfferOptions();
        peerConnection.createOffer(offerOptions, new CreateSessionDescriptionObserver() { // Pass the non-null options
            @Override
            public void onSuccess(RTCSessionDescription sdpOffer) {
                System.out.println("[P2P CreateOffer] SDP Offer created successfully for " + peerUserName);
                System.out.println("[P2P CreateOffer] Offer SDP: " + sdpOffer.sdp.substring(0, Math.min(sdpOffer.sdp.length(),60))+"...");

                peerConnection.setLocalDescription(sdpOffer, new SetSessionDescriptionObserver() {
                    @Override
                    public void onSuccess() {
                        System.out.println("[P2P SetLocal(Offer)] Local SDP Offer set successfully for " + peerUserName);
                        sendSdpToSignaling(peerUserName, "offer", sdpOffer);
                    }
                    @Override
                    public void onFailure(String error) {
                        System.err.println("[P2P SetLocal(Offer)] Failed for " + peerUserName + ": " + error);
                        onError("P2P SetLocalOffer Error for " + peerUserName, new RuntimeException(error));
                        closeP2PConnectionWithPeer(peerUserName); // Clean up failed attempt
                    }
                });
            }
            @Override
            public void onFailure(String error) {
                System.err.println("[P2P CreateOffer] Failed for " + peerUserName + ": " + error);
                onError("P2P CreateOffer Error for " + peerUserName, new RuntimeException(error));
                closeP2PConnectionWithPeer(peerUserName); // Clean up failed attempt
            }
        });
        if (chatRoomUI != null) {
            SwingUtilities.invokeLater(() -> chatRoomUI.addUserToList(peerUserName)); // Add to UI tentatively
        }
    }

    private void handleReceivedOffer(String fromPeerUserName, String sdpOfferString) {
        System.out.println("[P2P] Handling received SDP OFFER from: " + fromPeerUserName);
        if (peerConnectionFactory == null) { /* ... */ return; }

        RTCPeerConnection existingPc = peerConnections.get(fromPeerUserName);
        if (existingPc != null) {
            // We have an existing connection attempt with this peer. This is glare.
            // Rule: Only the peer with the lexicographically smaller username initiates an offer.
            boolean iShouldOffer = this.currentUsername.compareTo(fromPeerUserName) < 0;

            if (iShouldOffer) {
                // I am the designated offerer. If I receive an offer from them, they are "impolite"
                // or a race condition occurred.
                // If my PC is already in HAVE_LOCAL_OFFER, it means I've sent my offer.
                // I should probably ignore their offer and wait for their answer to my offer.
                RTCSignalingState currentState = existingPc.getSignalingState();
                if (currentState == RTCSignalingState.HAVE_LOCAL_OFFER) {
                    System.err.println("[P2P GLARE] Received OFFER from " + fromPeerUserName +
                            ", but I (" + this.currentUsername + ") am the designated offerer and already sent an offer (state: " + currentState + "). Ignoring their offer.");
                    return; // Ignore their offer, wait for their answer to my offer
                } else {
                    // My PC state is not HAVE_LOCAL_OFFER (e.g., STABLE, or something went wrong with my offer).
                    // This is unexpected if I'm the designated offerer.
                    // Let's be "polite" here: close my attempt and process their offer.
                    System.err.println("[P2P GLARE] Received OFFER from " + fromPeerUserName +
                            ", I am designated offerer but my PC state is " + currentState +
                            ". Cleaning up my attempt and processing theirs.");
                    closeP2PConnectionWithPeer(fromPeerUserName); // Clean up my existing attempt fully
                    // Fall through to process their offer by creating a new PC below
                }
            } else {
                // I am the designated answerer (my username is larger). Receiving an offer is expected.
                // However, if an existingPC is present, it implies a previous, possibly failed or duplicate, attempt.
                // Clean it up before processing the new offer to ensure a fresh state.
                System.out.println("[P2P] Received OFFER from " + fromPeerUserName +
                        " (I am answerer). An existing PC was found; cleaning it up before processing new offer.");
                closeP2PConnectionWithPeer(fromPeerUserName);
                // Fall through to process their offer by creating a new PC below
            }
        }

        // Proceed to create a NEW PC to handle this incoming offer (as the designated answerer, or after cleaning up my polite offer)
        RTCConfiguration rtcConfig = new RTCConfiguration();
        rtcConfig.iceServers.addAll(ICE_SERVERS);
        SimplePeerConnectionObserver pcObserver = new SimplePeerConnectionObserver(fromPeerUserName, this);
        RTCPeerConnection newPeerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, pcObserver);
        if (newPeerConnection == null) {
            System.err.println("[P2P] Failed to create RTCPeerConnection for incoming OFFER from " + fromPeerUserName);
            onError("P2P Creation Error", new RuntimeException("Failed to create PeerConnection for " + fromPeerUserName));
            return;
        }
        peerConnections.put(fromPeerUserName, newPeerConnection); // Store before setRemoteDescription
        pcObserver.setPeerConnection(newPeerConnection);
        System.out.println("[P2P] RTCPeerConnection created for incoming OFFER from " + fromPeerUserName);

        RTCSessionDescription remoteSdpOffer = new RTCSessionDescription(RTCSdpType.OFFER, sdpOfferString);
        System.out.println("[P2P] Setting Remote Description (Offer) from " + fromPeerUserName + "...");
        newPeerConnection.setRemoteDescription(remoteSdpOffer, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                System.out.println("[P2P SetRemote(Offer)] Remote SDP Offer set successfully from " + fromPeerUserName);
                System.out.println("[P2P] Creating SDP Answer for " + fromPeerUserName + "...");
                dev.onvoid.webrtc.RTCAnswerOptions answerOptions = new dev.onvoid.webrtc.RTCAnswerOptions();
                newPeerConnection.createAnswer(answerOptions, new CreateSessionDescriptionObserver() { // Pass the non-null options
                    @Override
                    public void onSuccess(RTCSessionDescription sdpAnswer) {
                        System.out.println("[P2P CreateAnswer] SDP Answer created successfully for " + fromPeerUserName);
                        // Log a snippet of the SDP for debugging
                        if (sdpAnswer != null && sdpAnswer.sdp != null) {
                            System.out.println("[P2P CreateAnswer SDP Snippet]: " + sdpAnswer.sdp.substring(0, Math.min(sdpAnswer.sdp.length(),100))+"...");
                        } else {
                            System.err.println("[P2P CreateAnswer] SDP Answer or its description is null!");
                            onError("P2P CreateAnswer SDP Null for " + fromPeerUserName, new NullPointerException("SDP Answer was null"));
                            closeP2PConnectionWithPeer(fromPeerUserName);
                            return;
                        }
                        newPeerConnection.setLocalDescription(sdpAnswer, new SetSessionDescriptionObserver() {
                            @Override
                            public void onSuccess() {
                                System.out.println("[P2P SetLocal(Answer)] Local SDP Answer set successfully for " + fromPeerUserName);
                                sendSdpToSignaling(fromPeerUserName, "answer", sdpAnswer);
                            }
                            @Override
                            public void onFailure(String error) {
                                System.err.println("[P2P SetLocal(Answer)] Failed for " + fromPeerUserName + ": " + error);
                                onError("P2P SetLocalAnswer Error for " + fromPeerUserName, new RuntimeException(error));
                                closeP2PConnectionWithPeer(fromPeerUserName);
                            }
                        });
                    }
                    @Override
                    public void onFailure(String error) {
                        System.err.println("[P2P CreateAnswer] Failed for " + fromPeerUserName + ": " + error);
                        onError("P2P CreateAnswer Error for " + fromPeerUserName, new RuntimeException(error));
                        closeP2PConnectionWithPeer(fromPeerUserName);
                    }
                });
            }
            @Override
            public void onFailure(String error) {
                System.err.println("[P2P SetRemote(Offer)] Failed for " + fromPeerUserName + ": " + error);
                onError("P2P SetRemoteOffer Error", new RuntimeException(error));
                closeP2PConnectionWithPeer(fromPeerUserName);
            }
        });
        if (chatRoomUI != null) { SwingUtilities.invokeLater(() -> chatRoomUI.addUserToList(fromPeerUserName)); }
    }

    private void handleReceivedAnswer(String fromPeerUserName, String sdpAnswerString) {
        System.out.println("[P2P] Handling received SDP ANSWER from: " + fromPeerUserName);
        RTCPeerConnection peerConnection = peerConnections.get(fromPeerUserName);
        if (peerConnection == null) {
            System.err.println("[P2P] No existing RTCPeerConnection found for " + fromPeerUserName + " to set ANSWER.");
            onError("P2P Answer Error", new RuntimeException("Received answer for unknown peer: " + fromPeerUserName));
            return;
        }

        RTCSessionDescription remoteSdpAnswer = new RTCSessionDescription(RTCSdpType.ANSWER, sdpAnswerString);
        System.out.println("[P2P] Setting Remote Description (Answer) from " + fromPeerUserName + "...");
        peerConnection.setRemoteDescription(remoteSdpAnswer, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                System.out.println("[P2P SetRemote(Answer)] Remote SDP Answer set successfully for " + fromPeerUserName);
                // Connection should now proceed to ICE checks and eventually data channel open.
            }
            @Override
            public void onFailure(String error) {
                System.err.println("[P2P SetRemote(Answer)] Failed for " + fromPeerUserName + ": " + error);
                onError("P2P SetRemoteAnswer Error for " + fromPeerUserName, new RuntimeException(error));
                closeP2PConnectionWithPeer(fromPeerUserName);
            }
        });
    }

    private void handleReceivedIceCandidate(String fromPeerUserName, ClientSignalingMessage.IceCandidatePayload candidatePayload) {
        System.out.println("[P2P] Handling received ICE Candidate from: " + fromPeerUserName);
        RTCPeerConnection peerConnection = peerConnections.get(fromPeerUserName);
        if (peerConnection == null) {
            System.err.println("[P2P] No existing RTCPeerConnection found for " + fromPeerUserName + " to add ICE candidate.");
            // It's possible to receive candidates before the PC is fully set up if signaling is very fast.
            // Robust clients might queue early candidates. For now, we'll log an error.
            // TODO: Consider queuing early ICE candidates if PC not yet ready for fromPeerUserName.
            onError("P2P ICE Error", new RuntimeException("Received ICE for unknown/pending peer: " + fromPeerUserName));
            return;
        }

        RTCIceCandidate iceCandidate = new RTCIceCandidate(
                candidatePayload.getSdpMid(),
                candidatePayload.getSdpMLineIndex(),
                candidatePayload.getCandidate()
        );
        System.out.println("[P2P] Adding ICE Candidate from " + fromPeerUserName + ": " + iceCandidate.sdp.substring(0, Math.min(iceCandidate.sdp.length(),30))+"...");
        // For dev.onvoid.webrtc, addIceCandidate might not take an observer and might be synchronous.
        // It could also throw an exception if the candidate is malformed or the PC is in a wrong state.
        try {
            peerConnection.addIceCandidate(iceCandidate);
        } catch (Exception e) {
            System.err.println("[P2P] Error adding received ICE candidate for " + fromPeerUserName + ": " + e.getMessage());
            e.printStackTrace();
            onError("P2P AddICE Error for " + fromPeerUserName, e);
        }
    }

    private void setupDataChannelObserver(String peerUserName, RTCDataChannel dataChannel) {
        System.out.println("[P2P] Setting up DataChannelObserver for peer " + peerUserName + " on DC: " + dataChannel.getLabel());
        if (dataChannel == null) {
            System.err.println("[P2P] Cannot setup observer for null DataChannel with peer " + peerUserName);
            return;
        }
        // Use the SimpleDataChannelObserver we defined.
        // The registerObserver method might be specific to dev.onvoid.webrtc's RTCDataChannel.
        SimpleDataChannelObserver dcObserver = new SimpleDataChannelObserver(peerUserName, this, dataChannel);
        dataChannel.registerObserver(dcObserver); // Assuming this is the correct method
        System.out.println("[P2P] DataChannelObserver registered for " + peerUserName);
    }

    // Called BY your PeerConnectionObserver when an ICE candidate is found by the local PC
    public void onIceCandidateFound(String peerUserName, RTCIceCandidate iceCandidate) {
        // This is CALLED BY SimplePeerConnectionObserver.onIceCandidate
        System.out.println("[Controller CB] ICE Candidate for " + peerUserName + " (forwarding to signaling): " + iceCandidate.sdp.substring(0, Math.min(iceCandidate.sdp.length(), 30))+"...");
        ClientSignalingMessage.IceCandidatePayload payload = new ClientSignalingMessage.IceCandidatePayload();
        payload.setCandidate(iceCandidate.sdp);
        payload.setSdpMid(iceCandidate.sdpMid);
        payload.setSdpMLineIndex(iceCandidate.sdpMLineIndex);
        ClientSignalingMessage iceMsg = new ClientSignalingMessage("candidate", this.currentUsername, peerUserName, this.activeRoomName, payload);
        signalingService.sendSignalingMessage(iceMsg);
    }

    public void onRemoteDataChannel(String peerUserName, RTCDataChannel dataChannel) {
        // This is CALLED BY SimplePeerConnectionObserver.onDataChannel
        System.out.println("[Controller CB] Remote DataChannel '" + dataChannel.getLabel() + "' received from peer " + peerUserName);
        if ("chat".equals(dataChannel.getLabel())) {
            RTCDataChannel existingDC = dataChannels.get(peerUserName);
            if (existingDC != null && existingDC != dataChannel) {
                System.out.println("[Controller CB] Replacing existing data channel for " + peerUserName);
                existingDC.close(); // Close old one if it exists and is different
            }
            dataChannels.put(peerUserName, dataChannel);
            setupDataChannelObserver(peerUserName, dataChannel); // Set up listeners for the new channel
        } else {
            System.err.println("[Controller CB] Received unexpected remote data channel label: '" + dataChannel.getLabel() + "' from " + peerUserName + ". Closing it.");
            dataChannel.close();
        }
    }

    // Helper to send SDP (offer/answer) via signaling service
    private void sendSdpToSignaling(String peerId, String type, RTCSessionDescription sdp) {
        ClientSignalingMessage.SdpPayload payload = new ClientSignalingMessage.SdpPayload();
        payload.setSdpType(type); // "offer" or "answer"
        payload.setSdp(sdp.sdp); // sdp.sdp contains the actual SDP string
        ClientSignalingMessage sdpMsg = new ClientSignalingMessage(type, this.currentUsername, peerId, this.activeRoomName, payload);
        signalingService.sendSignalingMessage(sdpMsg);
    }

    // </editor-fold>


    // --- Utility and Old Logic to Adapt/Remove ---
    private void showErrorDialog(String message) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame, message, "Error", JOptionPane.ERROR_MESSAGE)); }
    public void setActiveChatRoomUI(ChatRoom ui) { this.chatRoomUI = ui; }
    public String getCurrentUsername() { return currentUsername; }
    public String getActiveRoomName() { return activeRoomName; }
    public void setCurrentUsername(String uname) {this.currentUsername = uname; }
    public List<ChatMessage> getChatHistory(String roomName) { synchronized(roomChatHistories) { return new ArrayList<>(roomChatHistories.getOrDefault(roomName, Collections.emptyList())); } }
    private List<String> getOnlineUsersForRoom(String roomName) { // For reverting UI on failed switch
        if (Objects.equals(roomName, this.activeRoomName)) {
            List<String> users = new ArrayList<>(peerConnections.keySet());
            if(this.currentUsername != null) users.add(this.currentUsername); // Add self for UI
            return users;
        }
        return Collections.singletonList(this.currentUsername); // Fallback
    }

    private void closeP2PConnectionWithPeer(String peerUserName) {
        System.out.println("[Controller] Closing P2P connection with peer: " + peerUserName);
        RTCPeerConnection pc = peerConnections.remove(peerUserName);
        if (pc != null) {
            try { pc.close(); } catch (Exception e) { System.err.println("Error closing PC for " + peerUserName + ": " + e.getMessage()); }
        }
        RTCDataChannel dc = dataChannels.remove(peerUserName);
        if (dc != null) {
            try { dc.close(); } catch (Exception e) { System.err.println("Error closing DC for " + peerUserName + ": " + e.getMessage()); }
        }
        // Notify UI via the standard NetworkListener callback if the observer didn't already.
        // However, the observer (SimplePeerConnectionObserver via onIceConnectionChange) should ideally trigger onPeerDisconnected.
        // Calling it here might be redundant if the observers handle it.
        // For now, let's let the observer callbacks primarily handle onPeerDisconnected for UI consistency.
        if (chatRoomUI != null) { // Directly update UI from this explicit close if needed
            final String finalPeerUserName = peerUserName;
            SwingUtilities.invokeLater(() -> {
                if (chatRoomUI != null) { // Re-check for safety
                    chatRoomUI.removeUserFromList(finalPeerUserName);
                    chatRoomUI.displaySystemMessage("P2P Connection with " + finalPeerUserName + " has been closed.");
                }
            });
        }
    }
    // File Sharing: Will need to send MessageData of type FILE_SHARE_OFFER over DataChannels
    public boolean initiateFileShare(String roomNameContext, String senderUsernameContext, File fileToShare) {
        if (!Objects.equals(this.activeRoomName, roomNameContext) || !Objects.equals(this.currentUsername, senderUsernameContext) || !currentRoomE2EEKeyDerived) {
            System.err.println("[Controller] File share precondition fail. ActiveRoom: " + activeRoomName + " vs " + roomNameContext + ", User: " + currentUsername + " vs " + senderUsernameContext + ", KeyDerived: " + currentRoomE2EEKeyDerived);
            if(chatRoomUI != null) SwingUtilities.invokeLater(()->chatRoomUI.fileShareAttemptFinished()); // Let UI know attempt is over
            return false; // Indicate failure to start
        }
        if (dataChannels.isEmpty()) { if (chatRoomUI != null) chatRoomUI.displaySystemMessage("No peers to share file with."); if(chatRoomUI != null) SwingUtilities.invokeLater(()->chatRoomUI.fileShareAttemptFinished()); return false; }

        new Thread(() -> {
            boolean offerSent = false;
            try {
                // This part remains for encrypting file key, etc.
                // For a P2P file transfer, the "downloadUrl" would be replaced by a direct transfer negotiation.
                // For now, we just send the OFFER of a file.
                // SecureRandom random = SecureRandom.getInstanceStrong(); byte[] oneTimeKeyBytes = new byte[32]; random.nextBytes(oneTimeKeyBytes);
                // String encryptedOneTimeFileKeyBase64 = encryptionService.encryptDataWithRoomKey(oneTimeKeyBytes); // If room key set

                MessageData fileOfferAppMessage = new MessageData(
                        this.currentUsername,
                        fileToShare.getName(),
                        fileToShare.length(),
                        null, // No downloadUrl
                        "ENCRYPTED_FILE_KEY_PLACEHOLDER", // Placeholder
                        null, // fileHash,
                        this.activeRoomName
                );
                String offerJson = objectMapper.writeValueAsString(fileOfferAppMessage);
                ByteBuffer buffer = ByteBuffer.wrap(offerJson.getBytes(StandardCharsets.UTF_8));
                RTCDataChannelBuffer dataBufferToSend = new RTCDataChannelBuffer(buffer, false);

                final List<String> failedFileOfferPeers = new ArrayList<>();
                dataChannels.forEach((peerId, dc) -> {
                    if (dc != null && dc.getState() == RTCDataChannelState.OPEN) {
                        try {
                            dc.send(dataBufferToSend);
                            System.out.println("[Controller] File share OFFER sent to peer " + peerId + " for '" + fileToShare.getName() + "'");
                            // offerSentSuccessfullyAtLeastOnce = true; // Uncomment if you want to track this
                        } catch (Exception e) {
                            System.err.println("[Controller] Failed to send file share OFFER to " + peerId + ": " + e.getMessage());
                            failedFileOfferPeers.add(peerId);
                        }
                    } else {
                        System.err.println("[Controller] DataChannel to " + peerId + " not open/null for file share OFFER.");
                        failedFileOfferPeers.add(peerId);
                    }
                });

                if (!failedFileOfferPeers.isEmpty() && chatRoomUI != null) {
                    final String failedPeersStr = String.join(", ", failedFileOfferPeers);
                    SwingUtilities.invokeLater(()-> chatRoomUI.displaySystemMessage("Note: File offer might not have reached peers: " + failedPeersStr));
                } else if (failedFileOfferPeers.size() == dataChannels.size() && !dataChannels.isEmpty()){
                    System.err.println("[Controller] File share OFFER failed to send to ALL peers.");
                    if (chatRoomUI != null) SwingUtilities.invokeLater(()-> chatRoomUI.displaySystemMessage("Failed to send file offer to any peer."));
                } else if (!dataChannels.isEmpty()){
                    System.out.println("[Controller] File share OFFER (P2P metadata) successfully queued for sending for '" + fileToShare.getName() + "'");
                }


            } catch (Exception e) { // Catches errors during MessageData creation or JSON serialization
                System.err.println("[Controller] Error preparing P2P file share OFFER process: " + e.getMessage());
                e.printStackTrace();
                if (chatRoomUI != null) {
                    final String fName = fileToShare.getName();
                    SwingUtilities.invokeLater(() -> chatRoomUI.displaySystemMessage("Failed to prepare file offer: " + fName + ". " + e.getMessage()));
                }
            } finally {
                if (chatRoomUI != null) SwingUtilities.invokeLater(()->chatRoomUI.fileShareAttemptFinished());
            }
        }).start();
        return true; // Offer process initiation attempted
    }

    public void notifyChatDownloaded(String room, String user) { /* No direct P2P equivalent, this was Pusher broadcast */ }
    public void requestPrivateChat(String targetUser) {
        System.out.println("[Controller] TODO: WebRTC Private Chat: Requesting private session with " + targetUser);
        if (chatRoomUI!=null) chatRoomUI.displaySystemMessage("WebRTC Private Chat with " + targetUser + ": Feature under development.");
        // This will involve:
        // 1. Generating a unique private room name + password.
        // 2. Sending a "private_chat_invite" signal to targetUser via Signaling Server, containing proposed room & pass.
        // 3. If targetUser accepts (sends "private_chat_accept" signal), both users then
        //    call joinOrSwitchToRoom(privateRoomName, privatePassword) which sets up WebRTC for that new room.
    }
    public void acceptPrivateChat(String fromUser, String proposedRoom, String pass) {
        System.out.println("[Controller] TODO: WebRTC Private Chat: Accepting invite from " + fromUser + " for room " + proposedRoom);
        // 1. Send "private_chat_accept" signal back to fromUser via Signaling Server.
        // 2. Call joinOrSwitchToRoom(proposedRoom, pass);
    }
    public void declinePrivateChat(String fromUser, String proposedRoom) {
        System.out.println("[Controller] TODO: WebRTC Private Chat: Declining invite from " + fromUser + " for room " + proposedRoom);
        // 1. Send "private_chat_decline" signal back to fromUser via Signaling Server.
    }
}

class SimplePeerConnectionObserver implements PeerConnectionObserver {
    private final String peerId;
    private final ChatController controller;
    private RTCPeerConnection peerConnection; // Keep a reference if observer needs to interact with its PC

    public SimplePeerConnectionObserver(String peerId, ChatController controller) {
        this.peerId = peerId;
        this.controller = controller;
        System.out.println("[PCO][" + peerId + "] Observer created.");
    }

    public void setPeerConnection(RTCPeerConnection pc) {
        this.peerConnection = pc; // Useful if the observer needs to trigger actions on its PC
    }

    @Override
    public void onSignalingChange(RTCSignalingState newState) {
        System.out.println("[PCO][" + peerId + "] SignalingState changed to: " + newState);
    }

    @Override
    public void onIceConnectionChange(RTCIceConnectionState newState) {
        System.out.println("[PCO][" + peerId + "] IceConnectionState changed to: " + newState);
        // Handle disconnections/failures by notifying the controller
        if (newState == RTCIceConnectionState.FAILED ||
                newState == RTCIceConnectionState.DISCONNECTED ||
                newState == RTCIceConnectionState.CLOSED) {
            System.err.println("[PCO][" + peerId + "] ICE Connection State is FAILED/DISCONNECTED/CLOSED.");
            controller.onPeerDisconnected(peerId); // Crucial callback
        } else if (newState == RTCIceConnectionState.CONNECTED || newState == RTCIceConnectionState.COMPLETED) {
            System.out.println("[PCO][" + peerId + "] ICE connection established/completed.");
            // The actual data path readiness is confirmed by DataChannel's onOpen state.
        }
    }

    @Override
    public void onIceGatheringChange(RTCIceGatheringState newState) {
        System.out.println("[PCO][" + peerId + "] IceGatheringState changed to: " + newState);
    }

    @Override
    public void onIceCandidate(RTCIceCandidate candidate) {
        if (candidate != null) {
            System.out.println("[PCO][" + peerId + "] ICE Candidate found: " + candidate.sdp.substring(0, Math.min(candidate.sdp.length(), 30)) + "..."); // Log snippet
            controller.onIceCandidateFound(peerId, candidate); // Forward to controller
        } else {
            System.out.println("[PCO][" + peerId + "] End of ICE candidates signal (null candidate).");
        }
    }

    public void onIceCandidatesRemoved(List<RTCIceCandidate> candidates) {
        System.out.println("[PCO][" + peerId + "] " + (candidates != null ? candidates.size() : 0) + " ICE candidates removed.");
    }

    @Override public void onAddStream(MediaStream stream) {
        System.out.println("[SimplePCObserver] onAddStream for " + peerId + " - Stream: " + (stream != null ? stream.toString() : "null"));
    }


    @Override public void onRemoveStream(MediaStream stream) {
        System.out.println("[SimplePCObserver] onRemoveStream for " + peerId + " - Stream: " + (stream != null ? stream.toString() : "null"));
    }

    @Override
    public void onDataChannel(RTCDataChannel dataChannel) {
        // This is called when the *remote* peer creates a data channel and it's "offered" to us.
        System.out.println("[PCO][" + peerId + "] Remote DataChannel received: '" + dataChannel.getLabel() + "'");
        controller.onRemoteDataChannel(peerId, dataChannel); // Forward to controller to handle it
    }

    @Override
    public void onRenegotiationNeeded() {
        System.out.println("[PCO][" + peerId + "] Renegotiation needed.");
        // For simple mesh chat, renegotiation is less common unless features change.
        // If needed, the "perfect negotiation" pattern often dictates one side is the "polite" peer.
        // TODO: Implement renegotiation logic if your application features require it.
        // This might involve creating a new offer from this side.
        // controller.handleRenegotiationNeeded(peerId);
    }

    // Add any other methods from dev.onvoid.webrtc.PeerConnectionObserver with logging
}