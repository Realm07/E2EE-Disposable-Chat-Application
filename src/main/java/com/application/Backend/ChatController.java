// src/main/java/com/application/Backend/ChatController.java
package com.application.Backend;

import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

// Frontend imports
import com.application.FrontEnd.ChatRoom;
import com.application.FrontEnd.MainFrame;
// Assuming MessageCellRenderer and its inner ChatMessage class are correctly located
import com.application.FrontEnd.components.MessageCellRenderer.ChatMessage;

public class ChatController implements NetworkListener {

    private MainFrame mainFrame;
    private ChatRoom chatRoomUI;

    private final EncryptionService encryptionService;
    private final PusherService networkService;
    private final FileUploader fileUploader;

    private String currentUsername;
    private String activeRoomName;
    private Set<String> joinedRoomNames = new HashSet<>();
    private Map<String, List<ChatMessage>> roomChatHistories = new HashMap<>();
    private Map<String, String> pendingSentPrivateChatProposals = new HashMap<>();

    private Timer heartbeatSendTimer;
    private Map<String, Long> userLastHeartbeat = new HashMap<>();
    private Timer userTimeoutCheckTimer;
    private static final long HEARTBEAT_INTERVAL_MS = 20 * 1000;
    private static final long USER_TIMEOUT_THRESHOLD_MS = HEARTBEAT_INTERVAL_MS * 3 + 5000; // Slightly more than 3 intervals

    private static final Map<String, String> PUBLIC_ROOM_KEYS = new HashMap<>();
    static {
        PUBLIC_ROOM_KEYS.put("Alpha",   "PublicAlphaKey_!@#");
        PUBLIC_ROOM_KEYS.put("Bravo",   "PublicBravoKey_$%^");
        PUBLIC_ROOM_KEYS.put("Charlie", "PublicCharlieKey_*()");
        PUBLIC_ROOM_KEYS.put("Delta",   "PublicDeltaKey_-+=");
        PUBLIC_ROOM_KEYS.put("Echo",    "PublicEchoKey_{}|");
    }

    public ChatController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.encryptionService = new EncryptionService(); // Ensure this class is fully implemented
        this.networkService = new PusherService(this);   // Ensure this class is fully implemented
        this.fileUploader = new FileUploader();           // Ensure this class is fully implemented
        System.out.println("[Controller] Initialized.");
    }

    private boolean isPublicRoom(String roomName) {
        return PUBLIC_ROOM_KEYS.containsKey(roomName);
    }

    public void joinInitialRoom(String username, String roomName, String password) {
        System.out.println("[Controller] Initial Join attempt: user=" + username + ", room=" + roomName);
        if (username.isEmpty() || roomName.isEmpty() || password.isEmpty()) {
            showErrorDialog("Username, Room Name, and Password cannot be empty.");
            return;
        }
        this.currentUsername = username;
        java.util.List<String> initialUserList = new java.util.ArrayList<>(); // Explicit java.util
        if (this.currentUsername != null) initialUserList.add(this.currentUsername);
        mainFrame.switchToChatRoom(this.currentUsername, roomName, initialUserList); // PASS AS java.util.List<String>
        joinOrSwitchToRoom(roomName, password);
    }

    public void joinPublicRoom(String username, String roomName) {
        System.out.println("[Controller] PUBLIC Room Join attempt: user=" + username + ", room=" + roomName);
        if (username.isEmpty() || roomName.isEmpty()) {
            showErrorDialog("Username and Room Name cannot be empty.");
            return;
        }
        if (!isPublicRoom(roomName)) {
            showErrorDialog("Internal Error: Unknown public room '" + roomName + "'.");
            return;
        }
        this.currentUsername = username;
        String predefinedPassword = PUBLIC_ROOM_KEYS.get(roomName);
        if (predefinedPassword == null) {
            showErrorDialog("Internal Error: Missing key for public room '" + roomName + "'.");
            return;
        }
        java.util.List<String> initialUserList = new java.util.ArrayList<>(); // Explicit java.util
        if (this.currentUsername != null) initialUserList.add(this.currentUsername);
        mainFrame.switchToChatRoom(this.currentUsername, roomName, initialUserList); // PASS AS java.util.List<String>
        joinOrSwitchToRoom(roomName, predefinedPassword);
    }

    public void sendMessage(String plainTextMessage) {
        if (plainTextMessage == null || plainTextMessage.trim().isEmpty()) return;
        if (!networkService.isConnectedExplicit() || activeRoomName == null) {
            String errorMsg = activeRoomName == null ? "Not connected to any room." : "Not connected to room '" + activeRoomName + "'.";
            showErrorDialog(errorMsg + " Cannot send message.");
            if (chatRoomUI != null) chatRoomUI.displaySystemMessage("Failed to send: Not connected.");
            return;
        }
        if (this.currentUsername == null) {
            showErrorDialog("Internal error: Username not set.");
            return;
        }
        try {
            String encryptedBase64 = encryptionService.encrypt(plainTextMessage);
            MessageData messageData = new MessageData(this.currentUsername, encryptedBase64, this.activeRoomName);
            boolean sendInitiated = networkService.sendChatMessage(messageData);
            if (!sendInitiated) {
                showErrorDialog("Failed to initiate sending message (Network error). Please try again.");
                if (chatRoomUI != null) chatRoomUI.displaySystemMessage("Failed to send: Network issue.");
            }
        } catch (Exception e) {
            System.err.println("[Controller] Error encrypting/sending message: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Error sending message: " + e.getMessage());
            if (chatRoomUI != null) chatRoomUI.displaySystemMessage("Failed to send: Encryption error.");
        }
    }

    public void leaveRoom() {
        System.out.println("[Controller] User '" + (currentUsername != null ? currentUsername : "Unknown") + "' initiated leaving via button.");
        stopHeartbeatTimers();

        if (currentUsername != null && networkService.isConnectedExplicit()) {
            sendSystemMessage(MessageType.LEAVE);
        }
        networkService.disconnect(); // Request disconnect

        // UI actions after network actions
        if (chatRoomUI != null && this.currentUsername != null) {
            final String userWhoLeft = this.currentUsername;
            SwingUtilities.invokeLater(() -> {
                if(chatRoomUI != null) {
                    chatRoomUI.displaySystemMessage("You (" + userWhoLeft + ") have left the application.");
                }
            });
        }
        clearLocalStateAndSwitchToLogin();
    }

    private void clearLocalStateAndSwitchToLogin() {
        this.currentUsername = null;
        this.activeRoomName = null;
        // this.chatRoomUI = null; // Do not nullify UI if MainFrame manages its lifecycle.
        // MainFrame will call setActiveChatRoomUI(null) if needed or reinitialize.
        joinedRoomNames.clear();
        roomChatHistories.clear();
        pendingSentPrivateChatProposals.clear();
        userLastHeartbeat.clear(); // Cleared by stopHeartbeatTimers

        System.out.println("[Controller] State cleared. Switching to login page.");
        if (mainFrame != null) {
            SwingUtilities.invokeLater(() -> mainFrame.switchToLoginPage());
        }
    }

    public void handleApplicationShutdown() {
        System.out.println("[Controller] Application shutdown requested.");
        stopHeartbeatTimers();
        if (currentUsername != null && networkService != null && networkService.isConnectedExplicit()) {
            sendSystemMessage(MessageType.LEAVE); // Try to send leave message
            try { Thread.sleep(200); } catch (InterruptedException ignored) {} // Short delay for message to go
        }
        if (networkService != null) networkService.disconnect();
        System.out.println("[Controller] Shutdown cleanup complete.");
    }

    public void setActiveChatRoomUI(ChatRoom chatRoomUI) {
        this.chatRoomUI = chatRoomUI;
        if (this.chatRoomUI != null) {
            if (networkService.isConnectedExplicit() && activeRoomName != null &&
                    networkService.getChannelName() != null &&
                    networkService.getChannelName().contains(activeRoomName)) {
                // If UI is set and we are already connected, ensure UI reflects this.
                // Typically onConnected handles this, but this is a safeguard.
                System.out.println("[Controller] ChatRoom UI set and already connected to: " + activeRoomName + ". Updating UI.");
                this.chatRoomUI.updateUIForRoomSwitch(activeRoomName, getOnlineUsersForCurrentRoom());
                this.chatRoomUI.displaySystemMessage("[System] Reconnected to room: " + activeRoomName);
            } else if (activeRoomName != null) {
                // If UI is set but not connected, but an activeRoomName is known (e.g. mid-reconnect)
                // ensure the UI is at least pointing to the right room name and has a minimal user list
                System.out.println("[Controller] ChatRoom UI set for room: " + activeRoomName + ", but not yet connected. Pre-setting UI.");
                List<String> selfList = new ArrayList<>();
                if(this.currentUsername != null) selfList.add(this.currentUsername);
                this.chatRoomUI.updateUIForRoomSwitch(activeRoomName, selfList);
            }
        }
    }

    public void joinOrSwitchToRoom(String roomName, String password) {
        System.out.println("[Controller] Attempting to join/switch backend to room: " + roomName);

        if (networkService.isConnectedExplicit()) {
            // If connected to a different room, send LEAVE for the old room
            if(this.currentUsername != null && this.activeRoomName != null && !this.activeRoomName.equals(roomName)) {
                System.out.println("[Controller] Sending LEAVE for old room: " + this.activeRoomName);
                sendSystemMessage(MessageType.LEAVE);
            }
            System.out.println("[Controller] Disconnecting from previous channel: " + (networkService.getChannelName() != null ? networkService.getChannelName() : "N/A"));
            networkService.disconnect(); // Disconnect before joining new one
            try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }

        stopHeartbeatTimers(); // Stop and clear heartbeats for the old room AFTER sending LEAVE

        System.out.println("[Controller] Deriving key for room: " + roomName);
        if (!encryptionService.deriveRoomKey(roomName, password)) { // Ensure deriveRoomKey sets internal state in encryptionService
            showErrorDialog("Failed to derive key for room '" + roomName + "'. Check password.");
            this.activeRoomName = null;
            if (chatRoomUI != null) { // Revert UI to a state that indicates failure or no room
                chatRoomUI.displaySystemMessage("Failed to join " + roomName + ": Invalid credentials.");
                chatRoomUI.setChatScrollPaneTitle("Not Connected");
                chatRoomUI.clearUserList();
            }
            return;
        }

        this.activeRoomName = roomName; // Set active room state
        networkService.setChannelName(roomName); // Inform network service

        if (chatRoomUI != null) { // Add tab immediately
            if (!joinedRoomNames.contains(roomName)) {
                chatRoomUI.addRoomTab(roomName);
                joinedRoomNames.add(roomName);
            }
            // Preemptively update UI for the switch attempt. onConnected will refine it.
            List<String> selfList = new ArrayList<>();
            if (this.currentUsername != null) selfList.add(this.currentUsername);
            chatRoomUI.updateUIForRoomSwitch(this.activeRoomName, selfList);
            chatRoomUI.displaySystemMessage("Attempting to connect to " + this.activeRoomName + "...");
        }

        System.out.println("[Controller] Initiating connection to room: " + this.activeRoomName + " on channel: " + networkService.getChannelName());
        networkService.connect();
    }

    public void requestRoomSwitch(String targetRoomName) {
        System.out.println("[Controller] UI requested switch to room: " + targetRoomName);
        if (Objects.equals(targetRoomName, this.activeRoomName) && networkService.isConnectedExplicit()) {
            System.out.println("[Controller] Already connected to room: " + targetRoomName + ". Ensuring UI is current.");
            if(chatRoomUI != null) SwingUtilities.invokeLater(() -> chatRoomUI.updateUIForRoomSwitch(targetRoomName, getOnlineUsersForCurrentRoom()));
            return;
        }

        final String previousActiveRoomName = this.activeRoomName; // Store for potential revert

        if (isPublicRoom(targetRoomName)) {
            String predefinedPassword = PUBLIC_ROOM_KEYS.get(targetRoomName);
            if (predefinedPassword == null) {
                showErrorDialog("Internal Error: Missing key for public room '" + targetRoomName + "'.");
                return;
            }
            joinOrSwitchToRoom(targetRoomName, predefinedPassword);
        } else {
            JPasswordField passwordField = new JPasswordField(15);
            JPanel panel = new JPanel(new GridLayout(0,1));
            panel.add(new JLabel("Enter password for room: " + targetRoomName));
            panel.add(passwordField);
            SwingUtilities.invokeLater(() -> {
                passwordField.requestFocusInWindow();
                int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Enter Room Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    String password = new String(passwordField.getPassword());
                    java.util.Arrays.fill(passwordField.getPassword(), ' ');
                    if (!password.isEmpty()) {
                        joinOrSwitchToRoom(targetRoomName, password);
                    } else {
                        showErrorDialog("Password cannot be empty.");
                        if (chatRoomUI != null && previousActiveRoomName != null) {
                            chatRoomUI.updateUIForRoomSwitch(previousActiveRoomName, getOnlineUsersForRoom(previousActiveRoomName)); // Revert UI
                        }
                    }
                } else {
                    System.out.println("[Controller] Room switch cancelled by user for room: " + targetRoomName);
                    if (chatRoomUI != null && previousActiveRoomName != null) {
                        chatRoomUI.updateUIForRoomSwitch(previousActiveRoomName, getOnlineUsersForRoom(previousActiveRoomName)); // Revert UI
                    }
                }
            });
        }
    }

    private List<String> getOnlineUsersForCurrentRoom() {
        // Heartbeats are cleared on room switch, so this set is for the current room.
        return new ArrayList<>(userLastHeartbeat.keySet());
    }

    private List<String> getOnlineUsersForRoom(String roomName) {
        if (Objects.equals(roomName, this.activeRoomName)) {
            return getOnlineUsersForCurrentRoom();
        }
        // If asking for a room we're not currently active in,
        // we don't have its live user list. Return empty or just self if it's a known joined room.
        List<String> users = new ArrayList<>();
        if (this.currentUsername != null) users.add(this.currentUsername); // Default to self
        return users;
    }


    @Override
    public void onConnected() {
        final String connectedRoomName = this.activeRoomName; // Use the state set by joinOrSwitchToRoom
        System.out.println("[Controller] Network Listener: Connected. Expected Room: " + connectedRoomName + ", Actual Channel: " + networkService.getChannelName());

        if (connectedRoomName == null || !networkService.getChannelName().contains(connectedRoomName)) {
            System.err.println("[Controller] Connected event, but room mismatch or null state. Expected: " + connectedRoomName + ", Actual: " + networkService.getChannelName());
            if (chatRoomUI != null) chatRoomUI.displaySystemMessage("Connection error: Room mismatch.");
            return;
        }

        // Add self to heartbeat list for this new room
        userLastHeartbeat.clear(); // Ensure it's clean for the new room
        if (this.currentUsername != null) {
            userLastHeartbeat.put(this.currentUsername, System.currentTimeMillis());
        }

        SwingUtilities.invokeLater(() -> {
            if (chatRoomUI != null) {
                System.out.println("[Controller] Confirmed connection to active room: " + connectedRoomName);
                // Update UI with current user list (which is initially just self after clearing heartbeats)
                chatRoomUI.updateUIForRoomSwitch(connectedRoomName, getOnlineUsersForCurrentRoom());
                chatRoomUI.displaySystemMessage("Welcome to " + connectedRoomName + "! You are connected.");

                sendSystemMessage(MessageType.JOIN); // Announce join to others
                startHeartbeatTimers();              // Start heartbeats for this room
            }
        });
    }

    @Override
    public void onDisconnected() {
        System.out.println("[Controller] Network Listener: Disconnected!");
        stopHeartbeatTimers(); // Stop heartbeats when disconnected
        final String disconnectedRoom = this.activeRoomName; // Capture before it might be nulled
        SwingUtilities.invokeLater(() -> {
            if (chatRoomUI != null) {
                chatRoomUI.displaySystemMessage("[System] Disconnected from " + (disconnectedRoom != null ? disconnectedRoom : "chat service") + ".");
                // Optionally clear user list on disconnect or show "offline" state
                // chatRoomUI.clearUserList(); // Clears list except self
                // chatRoomUI.setChatScrollPaneTitle( (disconnectedRoom != null ? disconnectedRoom : "Chat") + " (Offline)");
            }
        });
        // Do not automatically switch to login here, as disconnects can be temporary.
        // LeaveRoom handles explicit leaves. Reconnect logic might be elsewhere.
    }

    @Override
    public void onMessageReceived(MessageData messageData) {
        if (messageData == null || messageData.sender == null || messageData.type == null) {
            System.err.println("[Controller] Received invalid MessageData (null sender or type).");
            return;
        }

        final String sender = messageData.sender;
        final String currentRoom = this.activeRoomName; // Message is for current active room

        if (currentRoom == null) {
            System.err.println("[Controller] Message received for room '" + messageData.getRoomContext() + "' but no active room locally. Ignoring.");
            return;
        }

        // Crucial: only process messages intended for the CURRENT active room
        // Pusher usually handles this by channel subscription, but good to double check if MessageData has room context
        String messageRoomContext = messageData.getRoomContext(); // Assuming MessageData has this
        if (messageRoomContext != null && !messageRoomContext.equals(currentRoom)) {
            System.out.println("[Controller] Ignoring message for room '" + messageRoomContext + "' as current room is '" + currentRoom + "'");
            return;
        }


        if (sender.equals(this.currentUsername) &&
                (messageData.type == MessageType.JOIN ||
                        messageData.type == MessageType.LEAVE ||
                        messageData.type == MessageType.DOWNLOAD)) {
            // System.out.println("[Controller] Ignoring self-sent system message: " + messageData.type);
            return; // Own JOIN/LEAVE/DOWNLOAD usually handled or not needed to be re-processed
        }

        // System.out.println("[Controller] Processing " + messageData.type + " from " + sender + " for active room " + currentRoom);
        switch (messageData.type) {
            case CHAT:
                handleChatMessage(messageData, sender, currentRoom);
                break;
            case JOIN:
                handleJoinMessage(sender, currentRoom);
                break;
            case LEAVE:
                handleLeaveMessage(sender, currentRoom);
                break;
            case HEARTBEAT:
                handleHeartbeatMessage(sender, currentRoom);
                break;
            case DOWNLOAD: // User downloaded chat history
                SwingUtilities.invokeLater(() -> {
                    if (chatRoomUI != null && Objects.equals(currentRoom, this.activeRoomName)) {
                        if (!sender.equals(this.currentUsername)) { // Only display if it's not self
                            chatRoomUI.displaySystemMessage(sender + " has downloaded the chat history.");
                        }
                    }
                });
                break;
            case FILE_SHARE_OFFER:
                handleFileShareOffer(messageData, sender, currentRoom);
                break;
            case PRIVATE_CHAT_REQUEST:
                handlePrivateChatRequest(messageData, sender, currentRoom);
                break;
            case PRIVATE_CHAT_ACCEPTED:
                handlePrivateChatAccepted(messageData, sender, currentRoom);
                break;
            case PRIVATE_CHAT_DECLINED:
                handlePrivateChatDeclined(messageData, sender, currentRoom);
                break;
            default:
                System.err.println("[Controller] Received message with unknown type: " + messageData.type);
        }
    }

    private void handleChatMessage(MessageData messageData, String sender, String roomForMessage) {
        if (messageData.encryptedData == null) {
            System.err.println("[Controller] Received CHAT message with null encryptedData from " + sender);
            return;
        }
        try {
            final String decryptedText = encryptionService.decrypt(messageData.encryptedData);
            final ChatMessage chatMessage = new ChatMessage(sender, decryptedText, "STANDARD"); // Assuming "STANDARD" type for ChatRoom
            synchronized(roomChatHistories) {
                roomChatHistories.computeIfAbsent(roomForMessage, k -> new ArrayList<>()).add(chatMessage);
            }
            SwingUtilities.invokeLater(() -> {
                if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                    chatRoomUI.appendMessage(sender, decryptedText, "STANDARD");
                    if (!sender.equals(this.currentUsername)) { // Infer presence if not self
                        chatRoomUI.addUserToList(sender);
                        userLastHeartbeat.put(sender, System.currentTimeMillis()); // Also update heartbeat
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("[Controller] Failed to decrypt message from " + sender + ": " + e.getMessage());
            if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                SwingUtilities.invokeLater(() -> chatRoomUI.displaySystemMessage("[Error] Could not decrypt message from " + sender + "."));
            }
        }
    }

    private void handleJoinMessage(String joiningUser, String roomForMessage) {
        userLastHeartbeat.put(joiningUser, System.currentTimeMillis());
        SwingUtilities.invokeLater(() -> {
            if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                // This is a JOIN from another user
                chatRoomUI.displaySystemMessage(joiningUser + " has joined the room.");
                chatRoomUI.addUserToList(joiningUser);
                // Send a HEARTBEAT back to announce our presence to the new user.
                System.out.println("[Controller] User '" + joiningUser + "' joined. Sending our own HEARTBEAT as '" + this.currentUsername + "'.");
                sendSystemMessage(MessageType.HEARTBEAT);
            }
        });
    }

    private void handleLeaveMessage(String leavingUser, String roomForMessage) {
        userLastHeartbeat.remove(leavingUser);
        SwingUtilities.invokeLater(() -> {
            if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                chatRoomUI.displaySystemMessage(leavingUser + " has left the room.");
                chatRoomUI.removeUserFromList(leavingUser);
            }
        });
    }

    private void handleHeartbeatMessage(String heartbeatSender, String roomForMessage) {
        userLastHeartbeat.put(heartbeatSender, System.currentTimeMillis());
        if (!heartbeatSender.equals(this.currentUsername)) { // If heartbeat from another user
            SwingUtilities.invokeLater(() -> {
                if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                    // Ensure user is in list (handles cases where JOIN might have been missed)
                    chatRoomUI.addUserToList(heartbeatSender);
                }
            });
        }
    }

    private void handleFileShareOffer(MessageData offerData, String sender, String roomForMessage) {
        System.out.println("[Controller] Received FILE_SHARE_OFFER from " + sender + " for file: " + offerData.originalFilename); // Direct field access
        SwingUtilities.invokeLater(() -> {
            if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                // Example of using the fields directly:
                // chatRoomUI.displaySystemMessage(sender + " is offering file: '" +
                //                           offerData.originalFilename + "' (" + formatFileSize(offerData.originalFileSize) + ").");
                chatRoomUI.displayFileShareOffer(offerData);
            }
        });
    }

    private void handlePrivateChatRequest(MessageData requestMsg, String sender, String roomForMessage) {
        // This client is User B (recipient)
        final String requestRecipient = requestMsg.recipient;
        final String proposedRoom = requestMsg.proposedRoomName;
        final String proposedPass = requestMsg.proposedRoomPassword;

        if (this.currentUsername != null && this.currentUsername.equals(requestRecipient)) {
            System.out.println("[Controller] Received PRIVATE_CHAT_REQUEST from '" + sender + "' for me (" + currentUsername + ") in room: " + proposedRoom);
            SwingUtilities.invokeLater(() -> {
                if (chatRoomUI != null) {
                    chatRoomUI.displayPrivateChatRequest(sender, proposedRoom, proposedPass);
                }
            });
        }
    }

    private void handlePrivateChatAccepted(MessageData acceptedMsg, String sender, String roomForMessage) {
        // This client is User A (initiator)
        final String acceptorUsername = sender; // User B who accepted
        final String acceptedRecipient = acceptedMsg.recipient; // Should be User A (this.currentUsername)
        final String acceptedRoom = acceptedMsg.proposedRoomName; // The room name context

        if (this.currentUsername != null && this.currentUsername.equals(acceptedRecipient)) {
            System.out.println("[Controller] Received PRIVATE_CHAT_ACCEPTED from '" + acceptorUsername + "' for room: " + acceptedRoom);
            String originalPassword = pendingSentPrivateChatProposals.remove(acceptedRoom);

            if (originalPassword != null) {
                if (chatRoomUI != null) {
                    SwingUtilities.invokeLater(() -> chatRoomUI.displaySystemMessage(acceptorUsername + " accepted private chat for '" + acceptedRoom + "'. Joining..."));
                }
                joinOrSwitchToRoom(acceptedRoom, originalPassword); // User A joins
            } else {
                System.err.println("[Controller] Error: Received ACCEPT for room '" + acceptedRoom + "' but no pending proposal found.");
                if (chatRoomUI != null) {
                    SwingUtilities.invokeLater(() -> chatRoomUI.displaySystemMessage("Error joining accepted private chat: Proposal details lost."));
                }
            }
        }
    }

    private void handlePrivateChatDeclined(MessageData declinedMsg, String sender, String roomForMessage) {
        // This client is User A (initiator)
        final String declinerUsername = sender; // User B who declined
        final String declinedRecipient = declinedMsg.recipient; // Should be User A
        final String declinedRoomCtx = declinedMsg.proposedRoomName;

        if (this.currentUsername != null && this.currentUsername.equals(declinedRecipient)) {
            System.out.println("[Controller] Received PRIVATE_CHAT_DECLINED from '" + declinerUsername + "' for proposal: " + declinedRoomCtx);
            pendingSentPrivateChatProposals.remove(declinedRoomCtx);
            if (chatRoomUI != null) {
                SwingUtilities.invokeLater(() -> chatRoomUI.displaySystemMessage(declinerUsername + " declined your private chat request for '" + declinedRoomCtx + "'."));
            }
        }
    }


    @Override
    public void onError(final String message, final Exception e) {
        System.err.println("[Controller] Network Listener: Error: " + message + (e != null ? " (" + e.getClass().getSimpleName() + ")" : ""));
        if (e != null) e.printStackTrace(); // Always log for debugging

        if (message != null && message.contains("Existing subscription to channel")) {
            System.out.println("[Controller] Suppressing 'Existing subscription' error from chat display as it's usually benign.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (chatRoomUI != null && this.activeRoomName != null) { // Only show in chat if in a room
                chatRoomUI.displaySystemMessage("[Network Error] " + message);
            } else {
                showErrorDialog("Network Error: " + message); // General popup if not in a room context
            }
        });
    }

    private void showErrorDialog(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame, message, "Error", JOptionPane.ERROR_MESSAGE));
    }

    public List<ChatMessage> getChatHistory(String roomName) {
        synchronized(roomChatHistories) {
            return new ArrayList<>(roomChatHistories.getOrDefault(roomName, List.of()));
        }
    }

    private void sendSystemMessage(MessageType type) {
        if (networkService == null || !networkService.isConnectedExplicit()) {
            System.err.println("[Controller] Skipping system message " + type + ": Network not connected.");
            return;
        }
        if (currentUsername == null) {
            System.err.println("[Controller] Skipping system message " + type + ": currentUsername is null.");
            return;
        }
        // System.out.println("[Controller] Sending system message: " + type + " for user: " + currentUsername);
        MessageData sysMessage = new MessageData(type, this.currentUsername, this.activeRoomName); // Add room context
        networkService.sendChatMessage(sysMessage);
        if (type == MessageType.LEAVE) { // Small delay for LEAVE to go out
            try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public void notifyChatDownloaded(String roomName, String downloaderUsername) {
        if (networkService != null && networkService.isConnectedExplicit() &&
                Objects.equals(this.activeRoomName, roomName) &&
                Objects.equals(this.currentUsername, downloaderUsername)) {
            System.out.println("[Controller] Sending DOWNLOAD notification for user: " + this.currentUsername);
            sendSystemMessage(MessageType.DOWNLOAD);
        } else {
            System.err.println("[Controller] Conditions not met to send DOWNLOAD notification.");
        }
    }
    // --- Private Chat Methods ---
    public void requestPrivateChat(String targetUsername) {
        if (currentUsername == null || activeRoomName == null || !networkService.isConnectedExplicit()) {
            showErrorDialog("Cannot initiate private chat: Not fully connected.");
            return;
        }
        if (currentUsername.equals(targetUsername)) {
            showErrorDialog("You cannot start a private chat with yourself.");
            return;
        }

        String proposedRoomSuffix = UUID.randomUUID().toString().substring(0, 6);
        String proposedPrivateRoomName = ("private-" + currentUsername.replaceAll("[^a-zA-Z0-9]", "") +
                "-" + targetUsername.replaceAll("[^a-zA-Z0-9]", "") +
                "-" + proposedRoomSuffix).toLowerCase();
        proposedPrivateRoomName = proposedPrivateRoomName.substring(0, Math.min(proposedPrivateRoomName.length(), 60)); // Max length for channel

        Random random = new Random();
        String proposedPassword = String.format("%04d%04d", random.nextInt(10000), random.nextInt(10000)); // 8-digit simple pass

        pendingSentPrivateChatProposals.put(proposedPrivateRoomName, proposedPassword);

        MessageData requestMsg = new MessageData(this.currentUsername, targetUsername, proposedPrivateRoomName, proposedPassword, this.activeRoomName); // PRIVATE_CHAT_REQUEST

        System.out.println("[Controller] Sending PRIVATE_CHAT_REQUEST to " + targetUsername + " for room " + proposedPrivateRoomName + " (from current room: " + this.activeRoomName +")");
        boolean sent = networkService.sendChatMessage(requestMsg);

        if (sent && chatRoomUI != null) {
            final String msg = "Private chat request sent to " + targetUsername + ".";
            SwingUtilities.invokeLater(() -> chatRoomUI.displaySystemMessage(msg));
        } else if (!sent) {
            showErrorDialog("Failed to send private chat request to " + targetUsername + ".");
            pendingSentPrivateChatProposals.remove(proposedPrivateRoomName);
        }
    }

    public void acceptPrivateChat(String initiatorUsername, String acceptedRoomName, String passwordForAcceptedRoom) {
        if (currentUsername == null || !networkService.isConnectedExplicit()) return;

        System.out.println("[Controller] User '" + currentUsername + "' ACCEPTS private chat with '" + initiatorUsername + "' for room: " + acceptedRoomName);
        // Send ACCEPTED message back to initiator (User A), using current active room context for sending
        MessageData acceptMsg = new MessageData(MessageType.PRIVATE_CHAT_ACCEPTED, this.currentUsername, initiatorUsername, acceptedRoomName, this.activeRoomName);
        networkService.sendChatMessage(acceptMsg);

        // User B (this client) now joins the new private room
        joinOrSwitchToRoom(acceptedRoomName, passwordForAcceptedRoom);
    }

    public void declinePrivateChat(String initiatorUsername, String declinedRoomName) {
        if (currentUsername == null || !networkService.isConnectedExplicit()) return;
        System.out.println("[Controller] User '" + currentUsername + "' DECLINES private chat with '" + initiatorUsername + "' for room: " + declinedRoomName);
        MessageData declineMsg = new MessageData(MessageType.PRIVATE_CHAT_DECLINED, this.currentUsername, initiatorUsername, declinedRoomName, this.activeRoomName);
        networkService.sendChatMessage(declineMsg);
    }

    // --- File Sharing ---
    public boolean initiateFileShare(File fileToShare, String forRoomName) {
        if (!Objects.equals(this.activeRoomName, forRoomName)) {
            System.err.println("[Controller] File share room mismatch. Active: " + this.activeRoomName + ", Request for: " + forRoomName);
            if (chatRoomUI != null) chatRoomUI.fileShareAttemptFinished();
            return false;
        }
        if (this.currentUsername == null || !networkService.isConnectedExplicit()) {
            System.err.println("[Controller] Cannot share file: Not connected or no user.");
            if (chatRoomUI != null) chatRoomUI.fileShareAttemptFinished();
            return false;
        }

        System.out.println("[Controller] Initiating file share for '" + fileToShare.getName() + "' in room '" + forRoomName + "'");
        // Run lengthy operations in a background thread to avoid freezing UI
        new Thread(() -> {
            File encryptedTempFile = null;
            boolean success = false;
            try {
                SecureRandom random = SecureRandom.getInstanceStrong();
                byte[] oneTimeKeyBytes = new byte[32]; // 256-bit AES key
                random.nextBytes(oneTimeKeyBytes);

                encryptedTempFile = File.createTempFile("enc_share_", "_" + fileToShare.getName().replaceAll("[^a-zA-Z0-9._-]", "_"));
                encryptionService.encryptFileWithGivenKey(fileToShare, encryptedTempFile, oneTimeKeyBytes);

                if (!encryptionService.isRoomKeySet()) { // Should be set if connected to a room
                    throw new IllegalStateException("Room key is not set. Cannot encrypt file key for sharing.");
                }
                String encryptedOneTimeFileKeyBase64 = encryptionService.encryptDataWithRoomKey(oneTimeKeyBytes);

                String uploadName = fileToShare.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".enc";
                String downloadUrl = fileUploader.uploadFile(encryptedTempFile, uploadName); // uploadFile should handle its own errors and return null on failure

                if (downloadUrl == null) {
                    throw new IOException("File upload failed (transfer.sh might be down or file too large).");
                }

                String originalFileHash = calculateSHA256(fileToShare);

                MessageData fileOfferMessage = new MessageData(
                        this.currentUsername,
                        fileToShare.getName(),
                        fileToShare.length(),
                        downloadUrl,
                        encryptedOneTimeFileKeyBase64,
                        originalFileHash,
                        forRoomName // Room context for the message
                );
                networkService.sendChatMessage(fileOfferMessage);
                System.out.println("[Controller] File share offer sent for '" + fileToShare.getName() + "'");
                success = true;

            } catch (Exception e) {
                System.err.println("[Controller] Error during file share process for '" + fileToShare.getName() + "': " + e.getMessage());
                e.printStackTrace();
                final String fName = fileToShare.getName();
                final String errText = e.getMessage();
                SwingUtilities.invokeLater(() -> {
                    if (chatRoomUI != null) chatRoomUI.displaySystemMessage("Failed to share file '" + fName + "': " + errText);
                });
            } finally {
                if (encryptedTempFile != null && encryptedTempFile.exists()) {
                    if (!encryptedTempFile.delete()) {
                        System.err.println("[Controller] Failed to delete temporary encrypted file: " + encryptedTempFile.getAbsolutePath());
                    }
                }
                if (chatRoomUI != null) {
                    chatRoomUI.fileShareAttemptFinished(); // Notify UI attempt is done
                }
            }
        }).start();
        return true; // Indicates the process has started (async)
    }


    private String calculateSHA256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[8192];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    // --- Heartbeat Management ---
    private void startHeartbeatTimers() {
        stopHeartbeatTimers(); // Ensure no old timers are running

        heartbeatSendTimer = new Timer("HeartbeatSendTimer-" + currentUsername, true);
        heartbeatSendTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (networkService.isConnectedExplicit() && currentUsername != null && activeRoomName != null) {
                    sendSystemMessage(MessageType.HEARTBEAT);
                }
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS);

        userTimeoutCheckTimer = new Timer("UserTimeoutCheckTimer-" + currentUsername, true);
        userTimeoutCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkUserTimeouts();
            }
        }, USER_TIMEOUT_THRESHOLD_MS, USER_TIMEOUT_THRESHOLD_MS / 2);
        System.out.println("[Controller] Heartbeat timers started for " + currentUsername + " in room " + activeRoomName);
    }

    private void stopHeartbeatTimers() {
        if (heartbeatSendTimer != null) {
            heartbeatSendTimer.cancel();
            heartbeatSendTimer = null;
        }
        if (userTimeoutCheckTimer != null) {
            userTimeoutCheckTimer.cancel();
            userTimeoutCheckTimer = null;
        }
        userLastHeartbeat.clear(); // Clear tracked users when stopping (e.g. on disconnect or room switch)
        System.out.println("[Controller] Heartbeat timers stopped.");
    }

    // In ChatController.java
    private void checkUserTimeouts() {
        // Check if ChatRoom UI is available AND if we (controller) have an active room.
        if (chatRoomUI == null || this.activeRoomName == null) {
            // System.out.println("[Controller Timeout] Skipping check: No UI or no active room in controller.");
            return;
        }

        long currentTime = System.currentTimeMillis();
        List<String> timedOutUsers = new ArrayList<>();
        final String currentControllerActiveRoom = this.activeRoomName; // Capture for lambda context

        synchronized (userLastHeartbeat) {
            List<String> usersToCheck = new ArrayList<>(userLastHeartbeat.keySet());
            for (String user : usersToCheck) {
                if (user.equals(currentUsername)) {
                    userLastHeartbeat.put(user, currentTime);
                    continue;
                }
                Long lastSeen = userLastHeartbeat.get(user);
                if (lastSeen == null || (currentTime - lastSeen > USER_TIMEOUT_THRESHOLD_MS)) {
                    timedOutUsers.add(user);
                }
            }
            for (String user : timedOutUsers) {
                userLastHeartbeat.remove(user);
            }
        }

        if (!timedOutUsers.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                // Double-check UI and that the UI is still relevant for the room we just processed timeouts for.
                // This check is more about ensuring the UI object is still valid and the controller is still focused on that room.
                if (chatRoomUI != null && Objects.equals(currentControllerActiveRoom, this.activeRoomName)) {
                    for (String user : timedOutUsers) {
                        System.out.println("[Controller Timeout] User " + user + " timed out from room " + currentControllerActiveRoom);
                        chatRoomUI.displaySystemMessage(user + " has disconnected (timeout).");
                        chatRoomUI.removeUserFromList(user);
                    }
                }
            });
        }
    }
}