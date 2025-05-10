// src/main/java/com/application/Backend/ChatController.java
package com.application.Backend;

import java.awt.GridLayout;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List; // Required for List
import java.util.Map;
import java.util.Set; // Required for HashSet

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

// Frontend imports
import com.application.FrontEnd.ChatRoom;
import com.application.FrontEnd.MainFrame;
// Assuming MessageCellRenderer and its inner ChatMessage class are correctly located
import com.application.FrontEnd.components.MessageCellRenderer;
import com.application.FrontEnd.components.MessageCellRenderer.ChatMessage; // Explicit import for clarity

import java.util.Timer;
import java.util.TimerTask;

import java.io.File;
import java.io.FileOutputStream; // For writing encrypted temp file
import java.io.FileInputStream;  // For reading original file
import java.io.IOException;
import java.nio.file.Files;      // For deleting temp file
import java.nio.file.Path;
import java.security.MessageDigest; // For SHA-256 hash
import java.security.SecureRandom;

public class ChatController implements NetworkListener { // Ensure NetworkListener interface exists

    // References to UI components
    private MainFrame mainFrame;
    private ChatRoom chatRoomUI; // Null until user is in the chat room

    // References to Backend services (Ensure these classes exist and are implemented)
    private final EncryptionService encryptionService;
    private final PusherService networkService;

    // State
    private String currentUsername;
    private String activeRoomName; // Name of the currently connected/viewed room
    private Set<String> joinedRoomNames = new HashSet<>(); // Track rooms user has joined in UI

    // Store chat history per room
    private Map<String, List<ChatMessage>> roomChatHistories = new HashMap<>();

    // --- Heartbeat Management ---
    private Timer heartbeatSendTimer;
    private Map<String, Long> userLastHeartbeat = new HashMap<>(); // Username -> Timestamp
    private Timer userTimeoutCheckTimer;
    private static final long HEARTBEAT_INTERVAL_MS = 20 * 1000; // Send heartbeat every 20 seconds
    private static final long USER_TIMEOUT_THRESHOLD_MS = HEARTBEAT_INTERVAL_MS * 3; // Timeout if no heartbeat for 60s
    // --- End Heartbeat Management ---

    // --- Public Room Handling ---
    private static final Map<String, String> PUBLIC_ROOM_KEYS = new HashMap<>();
    static {
        PUBLIC_ROOM_KEYS.put("Alpha",   "PublicAlphaKey_!@#");
        PUBLIC_ROOM_KEYS.put("Bravo",   "PublicBravoKey_$%^");
        PUBLIC_ROOM_KEYS.put("Charlie", "PublicCharlieKey_*()");
        PUBLIC_ROOM_KEYS.put("Delta",   "PublicDeltaKey_-+=");
        PUBLIC_ROOM_KEYS.put("Echo",    "PublicEchoKey_{}|");
    }
    private boolean isPublicRoom(String roomName) {
        return PUBLIC_ROOM_KEYS.containsKey(roomName);
    }
    // --- End Public Room Handling ---

    private final FileUploader fileUploader; // <<< NEW: Instance of FileUploader

    public ChatController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.encryptionService = new EncryptionService();
        this.networkService = new PusherService(this);
        this.fileUploader = new FileUploader(); // <<< Initialize FileUploader
        System.out.println("[Controller] Initialized.");
    }

    /**
     * Called by LoginPage for initial join. Switches UI and delegates connection.
     */
    public void joinInitialRoom(String username, String roomName, String password) {
        System.out.println("[Controller] Initial Join attempt: user=" + username + ", room=" + roomName);
        if (username.isEmpty() || roomName.isEmpty() || password.isEmpty()) {
            showErrorDialog("Username, Room Name, and Password cannot be empty.");
            return;
        }
        this.currentUsername = username;
        mainFrame.switchToChatRoom(this.currentUsername, roomName); // UI switch first
        joinOrSwitchToRoom(roomName, password); // Backend connection/switch
    }

    /**
     * Called by PublicServerRoom for public room join. Switches UI and delegates connection.
     */
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
        mainFrame.switchToChatRoom(this.currentUsername, roomName); // UI switch first
        String predefinedPassword = PUBLIC_ROOM_KEYS.get(roomName);
        if (predefinedPassword == null) {
             showErrorDialog("Internal Error: Missing key for public room '" + roomName + "'.");
             return;
        }
        joinOrSwitchToRoom(roomName, predefinedPassword); // Backend connection/switch
    }

    /**
     * Called by ChatRoom to send a message. Encrypts and sends via network service.
     */
    public void sendMessage(String plainTextMessage) {
        if (plainTextMessage == null || plainTextMessage.trim().isEmpty()) return;
        if (!networkService.isConnected() || activeRoomName == null) {
            String errorMsg = activeRoomName == null ? "Not connected to any room." : "Not connected to room '" + activeRoomName + "'.";
            showErrorDialog(errorMsg + " Cannot send message.");
            return;
        }
        if (this.currentUsername == null) {
            showErrorDialog("Internal error: Username not set.");
            return;
        }
        System.out.println("[Controller] Sending message: " + plainTextMessage);
        try {
            String encryptedBase64 = encryptionService.encrypt(plainTextMessage);
            // Ensure MessageData class exists and constructor matches
            MessageData messageData = new MessageData(this.currentUsername, encryptedBase64);
            System.out.println("[Controller] Attempting send to room: " + activeRoomName + ", channel: " + networkService.getChannelName());
            boolean sendInitiated = networkService.sendChatMessage(messageData);
            if (!sendInitiated) {
                 showErrorDialog("Failed to initiate sending message (Network error). Please try again.");
            } else {
                 System.out.println("[Controller] Message send initiated successfully.");
            }
        } catch (Exception e) {
            System.err.println("[Controller] Error encrypting/sending message: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Error sending message: " + e.getMessage());
        }
    }


    /**
     * Called by ChatRoom leave button. Disconnects, clears all state, switches UI to login.
     */
    public void leaveRoom() {
        System.out.println("[Controller] User '" + (currentUsername != null ? currentUsername : "Unknown") + "' initiated leaving via button.");
        stopHeartbeatTimers(); // Stop heartbeats
        if (currentUsername != null) {
            sendSystemMessage(MessageType.LEAVE); // Send LEAVE before actual disconnect
        }
        if (networkService != null) networkService.disconnect();if (chatRoomUI != null && currentUsername != null) {
            chatRoomUI.displaySystemMessage(currentUsername + " is leaving the application.");
        } else {
            System.out.println("[Controller] Cannot display leaving message: currentUsername=" + currentUsername + ", chatRoomUI=" + (chatRoomUI != null));
        }
        if (networkService != null) {
            System.out.println("[Controller] Disconnecting network service...");
            networkService.disconnect();
            System.out.println("[Controller] Network service disconnect requested.");
        } else {
            System.out.println("[Controller] Network service is null, cannot disconnect.");
        }

        // Clear state *after* potential UI message and disconnect call
        this.currentUsername = null;
        this.activeRoomName = null;
        this.chatRoomUI = null;
        joinedRoomNames.clear();
        roomChatHistories.clear(); // Clear stored history
        // encryptionService.clearKeys(); // If applicable

        System.out.println("[Controller] State cleared. Switching to login page.");
        if (mainFrame != null) {
            SwingUtilities.invokeLater(() -> mainFrame.switchToLoginPage());
        } else {
            System.err.println("[Controller] MainFrame reference is null, cannot switch to login page.");
        }
    }

    /**
     * Handles application shutdown cleanup (network disconnect).
     */
    public void handleApplicationShutdown() {
        System.out.println("[Controller] Application shutdown requested.");
        stopHeartbeatTimers(); // Stop heartbeats
        if (currentUsername != null && networkService != null && networkService.isConnectedExplicit()) {
            sendSystemMessage(MessageType.LEAVE);
        }
        if (networkService != null) networkService.disconnect();
        System.out.println("[Controller] Shutdown cleanup complete.");
    }

    /**
     * Sets the reference to the active ChatRoom UI instance. Called by MainFrame.
     */
    public void setActiveChatRoomUI(ChatRoom chatRoomUI) {
        this.chatRoomUI = chatRoomUI;
        // Check if already connected to the room when UI becomes active
        if (this.chatRoomUI != null) {
             if (networkService.isConnected() && activeRoomName != null &&
                 networkService.getChannelName() != null &&
                 networkService.getChannelName().contains(activeRoomName)) {
                this.chatRoomUI.displaySystemMessage("[System] Connected to room: " + activeRoomName);
             }
        }
    }

    /**
     * Core logic for connecting/subscribing to a room channel.
     * Disconnects previous, derives key, sets network channel, initiates connection.
     * Notifies UI to add the visual tab immediately.
     */
    public void joinOrSwitchToRoom(String roomName, String password) {
        System.out.println("[Controller] Attempting to join/switch backend to room: " + roomName);
        stopHeartbeatTimers(); // Stop heartbeats for the old room

        if (networkService.isConnected()) {
            if(this.currentUsername != null && this.activeRoomName != null) {
                sendSystemMessage(MessageType.LEAVE); // Leave current room
            }
            System.out.println("[Controller] Disconnecting from previous channel: " + networkService.getChannelName());
            networkService.disconnect();
            try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }

        System.out.println("[Controller] Deriving key for room: " + roomName);
        if (!encryptionService.deriveRoomKey(roomName, password)) {
            showErrorDialog("Failed to derive key for room '" + roomName + "'. Check password.");
            // If key fails, we are effectively not in any room
            this.activeRoomName = null;
            // Could potentially tell UI to reset highlighting if needed
            return;
        }
        System.out.println("[Controller] Key derived successfully for room: " + roomName);

        try {
            this.activeRoomName = roomName; // Update active room STATE before connecting
            System.out.println("[Controller] >> Set activeRoomName to: " + this.activeRoomName);

            networkService.setChannelName(roomName); // Tell service channel name
            String actualChannelName = networkService.getChannelName(); // Verify
            System.out.println("[Controller] >> networkService channel name is now: " + actualChannelName);

            if (actualChannelName == null || !actualChannelName.contains(roomName)) {
                 System.err.println("[Controller] !!! Channel name error after setting! Expected containing '" + roomName + "', got '" + actualChannelName + "'. Aborting connect.");
                 showErrorDialog("Internal error setting up network channel for room '" + roomName + "'.");
                 this.activeRoomName = null; // Revert active room state
                 return;
            }

            System.out.println("[Controller] >> Calling networkService.connect() for channel: " + actualChannelName);
            networkService.connect(); // Initiate asynchronous connection
            System.out.println("[Controller] >> networkService.connect() called.");

        } catch (Exception e) {
            System.err.println("[Controller] !!! UNEXPECTED ERROR during network setup: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Internal error during network setup: " + e.getMessage());
             this.activeRoomName = null; // Revert active room state
            return;
        }

        // Add the visual tab immediately for user feedback
        if (chatRoomUI != null) {
            if (!joinedRoomNames.contains(roomName)) {
                System.out.println("[Controller] Notifying UI to add visual tab for: " + roomName);
                chatRoomUI.addRoomTab(roomName);
                joinedRoomNames.add(roomName);
            } else {
                 System.out.println("[Controller] UI tab for " + roomName + " already exists/tracked.");
            }
            // DO NOT call updateUIForRoomSwitch here; wait for onConnected confirmation.
        } else {
            System.err.println("[Controller] Cannot add room tab immediately: chatRoomUI reference is null!");
        }
        System.out.println("[Controller] Successfully initiated join/switch logic for room: " + roomName + ". Waiting for connection events...");
    }

    /**
     * Handles UI request (tab click) to switch view to an already joined room.
     * Uses predefined key for public rooms, prompts for password for private rooms.
     */
    public void requestRoomSwitch(String targetRoomName) {
         System.out.println("[Controller] UI requested switch to room: " + targetRoomName);
         // Avoid unnecessary switching if already connected to the target room
         if (targetRoomName.equals(this.activeRoomName) && networkService.isConnected()) {
             System.out.println("[Controller] Already connected to room: " + targetRoomName + ". Switch request ignored.");
             // Ensure UI is correctly highlighted just in case
             if(chatRoomUI != null) SwingUtilities.invokeLater(() -> chatRoomUI.updateUIForRoomSwitch(targetRoomName));
             return;
         }

         if (isPublicRoom(targetRoomName)) {
             System.out.println("[Controller] Switching to public room '" + targetRoomName + "' using predefined key.");
             String predefinedPassword = PUBLIC_ROOM_KEYS.get(targetRoomName);
              if (predefinedPassword == null) {
                 showErrorDialog("Internal Error: Missing key for public room '" + targetRoomName + "'.");
                 return;
              }
              joinOrSwitchToRoom(targetRoomName, predefinedPassword); // Use main switch logic
         } else {
             // Private room: Prompt for password
             JPasswordField passwordField = new JPasswordField(15);
             JPanel panel = new JPanel(new GridLayout(0,1));
             panel.add(new JLabel("Enter password for room: " + targetRoomName));
             panel.add(passwordField);
             SwingUtilities.invokeLater(() -> { // Show dialog on EDT
                 passwordField.requestFocusInWindow();
                 int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Enter Room Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                 if (result == JOptionPane.OK_OPTION) {
                      char[] passwordChars = passwordField.getPassword();
                      String password = new String(passwordChars);
                      java.util.Arrays.fill(passwordChars, ' ');
                      if (!password.isEmpty()) {
                           System.out.println("[Controller] Password received for " + targetRoomName + ". Proceeding with switch.");
                           joinOrSwitchToRoom(targetRoomName, password); // Use main switch logic
                      } else {
                           showErrorDialog("Password cannot be empty.");
                           // Revert highlight back to original active room if password entry failed
                           if (chatRoomUI != null && activeRoomName != null) { chatRoomUI.updateUIForRoomSwitch(activeRoomName); }
                      }
                  } else {
                      System.out.println("[Controller] Room switch cancelled by user for room: " + targetRoomName);
                      // Revert highlight back to original active room if user cancelled
                      if (chatRoomUI != null && activeRoomName != null) { chatRoomUI.updateUIForRoomSwitch(activeRoomName); }
                  }
             });
         }
    }

    // --- NetworkListener Implementation ---

    @Override
    public void onConnected() {
        final String connectedChannel = networkService.getChannelName();
        final String roomName = this.activeRoomName;
        System.out.println("[Controller] Network Listener: Connected. Channel: " + connectedChannel + ", Room: " + roomName);

        SwingUtilities.invokeLater(() -> {
            if (chatRoomUI != null && roomName != null && connectedChannel != null && connectedChannel.contains(roomName)) {
                System.out.println("[Controller] Confirmed connection to active room: " + roomName);
                chatRoomUI.updateUIForRoomSwitch(roomName); // Clears lists, adds self, loads history
                chatRoomUI.displaySystemMessage("Welcome to " + roomName + "! You are connected.");

                // Send JOIN message and start heartbeats
                sendSystemMessage(MessageType.JOIN);
                userLastHeartbeat.put(this.currentUsername, System.currentTimeMillis()); // Track self
                startHeartbeatTimers(); // Start sending our heartbeats and checking for others
            } else {
                System.err.println("[Controller] Connected event: Mismatch or null state. UI: " + (chatRoomUI != null) + ", Room: " + roomName + ", Channel: " + connectedChannel);
                if (chatRoomUI != null && roomName != null) { // Try to update UI if possible
                    chatRoomUI.updateUIForRoomSwitch(roomName);
                    chatRoomUI.displaySystemMessage("[Warning] Connected to " + connectedChannel + ". Expected for " + roomName);
                }
            }
        });
    }

    @Override
    public void onDisconnected() {
        System.out.println("[Controller] Network Listener: Disconnected!");
        stopHeartbeatTimers(); // Stop heartbeats when disconnected
        SwingUtilities.invokeLater(() -> {
            if (chatRoomUI != null) {
                chatRoomUI.displaySystemMessage("[System] Disconnected from chat service.");
            }
        });
    }

// --- File Sharing Logic ---

    /**
     * Called by ChatRoom UI to initiate a file share.
     * This method will handle encryption, uploading, and sending the offer.
     * It should be run on a background thread by the ChatRoom UI.
     *
     * @param roomName The current active room.
     * @param senderUsername The user initiating the share.
     * @param fileToShare The original, unencrypted file selected by the user.
     */
    public void initiateFileShare(String roomName, String senderUsername, File fileToShare) {
        System.out.println("[Controller] Initiating file share for '" + fileToShare.getName() + "' in room '" + roomName + "' by user '" + senderUsername + "'");

        if (!this.activeRoomName.equals(roomName) || !this.currentUsername.equals(senderUsername)) {
            System.err.println("[Controller] File share request mismatch: Active/Current user/room does not match request parameters.");
            if (chatRoomUI != null) chatRoomUI.fileShareAttemptFinished(); // Decrement counter
            return;
        }

        File encryptedTempFile = null;
        try {
            // 1. Generate a one-time AES key for this file
            SecureRandom random = SecureRandom.getInstanceStrong();
            byte[] oneTimeKeyBytes = new byte[32]; // 256-bit AES key
            random.nextBytes(oneTimeKeyBytes);
            // System.out.println("[Controller] Generated one-time file key (bytes length): " + oneTimeKeyBytes.length);

            // 2. Encrypt the file content with this one-time key
            // For simplicity, we'll use a slightly different method in EncryptionService
            // or do it directly here for file streams. Let's assume EncryptionService can handle File objects.
            // Create a temporary file for the encrypted content.
            encryptedTempFile = File.createTempFile("enc_share_", "_" + fileToShare.getName());
            // System.out.println("[Controller] Created temp encrypted file: " + encryptedTempFile.getAbsolutePath());

            // This part needs careful implementation in EncryptionService or here:
            // Option A: Modify EncryptionService to take File in, File out, and a SecretKey
            // encryptionService.encryptFile(fileToShare, encryptedTempFile, new SecretKeySpec(oneTimeKeyBytes, "AES"));

            // Option B: Simpler stream encryption (less robust error handling here for brevity)
            // This uses the ONE-TIME KEY, NOT the room key for file content.
            System.out.println("[Controller] Encrypting file content with one-time key...");
            encryptionService.encryptFileWithGivenKey(fileToShare, encryptedTempFile, oneTimeKeyBytes);
            System.out.println("[Controller] File content encrypted to temp file.");


            // 3. Encrypt the one-time file key with the current room's E2EE key
            //    The room key MUST be derived and available in encryptionService.
            if (encryptionService.isRoomKeySet()) { // Add a check in EncryptionService
                String encryptedOneTimeFileKeyBase64 = encryptionService.encryptDataWithRoomKey(oneTimeKeyBytes);
                System.out.println("[Controller] One-time file key encrypted with room key.");

                // 4. Upload the encryptedTempFile to transfer.sh
                System.out.println("[Controller] Uploading encrypted file to transfer.sh...");
                String downloadUrl = fileUploader.uploadFile(encryptedTempFile, fileToShare.getName() + ".enc"); // Add .enc
                System.out.println("[Controller] Encrypted file uploaded. Download URL: " + downloadUrl);

                // 5. (Optional) Calculate hash of the ORIGINAL unencrypted file
                // String originalFileHash = calculateSHA256(fileToShare); // Implement calculateSHA256

                // 6. Create and send the FILE_SHARE_OFFER message
                MessageData fileOfferMessage = new MessageData(
                        senderUsername,
                        fileToShare.getName(),
                        fileToShare.length(),
                        downloadUrl,
                        encryptedOneTimeFileKeyBase64,
                        null // Pass originalFileHash here if calculated
                );
                networkService.sendChatMessage(fileOfferMessage);
                System.out.println("[Controller] File share offer sent for '" + fileToShare.getName() + "'");

                // Notify UI locally (optional, as sender will also receive their own offer)
                // if (chatRoomUI != null) {
                //    chatRoomUI.displaySystemMessage("You started sharing '" + fileToShare.getName() + "'.");
                // }

            } else {
                throw new IllegalStateException("Room key is not set. Cannot encrypt file key.");
            }

        } catch (Exception e) {
            System.err.println("[Controller] Error during file share process for '" + fileToShare.getName() + "': " + e.getMessage());
            e.printStackTrace();
            // Notify UI of failure
            if (chatRoomUI != null) {
                final String fName = fileToShare.getName();
                SwingUtilities.invokeLater(() -> chatRoomUI.displaySystemMessage("Failed to share file: " + fName));
            }
        } finally {
            // 7. Clean up the temporary encrypted file
            if (encryptedTempFile != null && encryptedTempFile.exists()) {
                if (encryptedTempFile.delete()) {
                    System.out.println("[Controller] Deleted temporary encrypted file: " + encryptedTempFile.getName());
                } else {
                    System.err.println("[Controller] Failed to delete temporary encrypted file: " + encryptedTempFile.getName());
                }
            }
            // 8. Notify ChatRoom UI that the attempt is finished to decrement counter
            if (chatRoomUI != null) {
                chatRoomUI.fileShareAttemptFinished();
            }
        }
    }

    // Helper to calculate SHA-256 hash (optional)
    private String calculateSHA256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[8192]; // Read in 8KB chunks
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    // --- End File Sharing Logic ---
    @Override
    public void onMessageReceived(MessageData messageData) {
        if (messageData == null) { // Check if parsing failed in PusherService
            System.err.println("[Controller] onMessageReceived called with null messageData object. Parsing likely failed in PusherService.");
            // No further processing possible
            return;
        }
        if (messageData.sender == null || messageData.type == null) {
            System.err.println("[Controller] Received MessageData with null sender or type. Sender: " + messageData.sender + ", Type: " + messageData.type);
            return;
        }

        final String sender = messageData.sender;
        final String roomForMessage = this.activeRoomName;
        if (roomForMessage == null) {
            System.err.println("[Controller] Message received but no active room set. Ignoring.");
            return;
        }

        // Ignore self-sent system messages to avoid feedback loops for announcements.
        // Keep ignoring self for CHAT is a UI preference (do you want to see your own messages echoed?).
        // For this fix, we specifically need to process HEARTBEATS from self (to update timestamp),
        // but not act on self-JOIN/LEAVE as announcements.
        if (messageData.sender.equals(this.currentUsername) &&
                (messageData.type == MessageType.JOIN ||
                        messageData.type == MessageType.LEAVE ||
                        messageData.type == MessageType.DOWNLOAD)) { // Keep ignoring self-DOWNLOAD display
            System.out.println("[Controller] Ignoring self-sent system message: " + messageData.type + " from " + messageData.sender);
            return;
        }
        // Self-HEARTBEATS will pass through to update the local timestamp.
        // Self-CHAT will also pass through to be added to history and displayed (if not filtered by a different mechanism).


        // Optionally ignore self-sent system messages if needed, but chat messages are fine.
        // if (sender.equals(this.currentUsername) && messageData.type != MessageType.CHAT) {
        //     System.out.println("[Controller] Ignoring self-sent system message: " + messageData.type);
        //     return;
        // }

        System.out.println("[Controller] Network Listener: Processing " + messageData.type + " from " + sender + " for active room " + roomForMessage);
        switch (messageData.type) {
            case CHAT:
                if (messageData.encryptedData == null) {
                    System.err.println("[Controller] Received CHAT message with null encryptedData from " + sender);
                    return;
                }
                try {
                    final String decryptedText = encryptionService.decrypt(messageData.encryptedData);
                    final ChatMessage chatMessage = new ChatMessage(sender, decryptedText);
                    synchronized(roomChatHistories) {
                        roomChatHistories.computeIfAbsent(roomForMessage, k -> new ArrayList<>()).add(chatMessage);
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                            chatRoomUI.appendMessage(chatMessage.getSender(), chatMessage.getMessage());
                            // <<< If receiving chat from someone not in list, add them (infer presence) >>>
                            if (!sender.equals(this.currentUsername)) { // Don't add self this way
                                chatRoomUI.addUserToList(sender);
                            }
                            // <<< End infer presence >>>
                        }
                    });
                } catch (Exception e) {
                    System.err.println("[Controller] Failed decrypt message from " + sender + ": " + e.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        if (chatRoomUI != null) chatRoomUI.displaySystemMessage("[Error] Could not decrypt message from " + sender + ".");
                    });
                }
                break; // <<< Ensure break statements! >>>

            case JOIN:
                final String joiningUser = sender;
                userLastHeartbeat.put(joiningUser, System.currentTimeMillis()); // Track new user
                SwingUtilities.invokeLater(() -> {
                    if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                        if (!joiningUser.equals(this.currentUsername)) { // Message from ANOTHER user joining
                            chatRoomUI.displaySystemMessage(joiningUser + " has joined the room.");
                            chatRoomUI.addUserToList(joiningUser);
                            // This client (currentUsername) is already in the room.
                            // Send a HEARTBEAT to let the new user (joiningUser) know about us.
                            System.out.println("[Controller] User '" + joiningUser + "' joined. Sending our own HEARTBEAT as '" + this.currentUsername + "' to announce presence.");
                            sendSystemMessage(MessageType.HEARTBEAT);
                        } else {
                            // This is our OWN JOIN message coming back.
                            // We don't need to display "[currentUsername] has joined".
                            // Our presence in the list is handled by updateUIForRoomSwitch in onConnected.
                            System.out.println("[Controller] Received own JOIN message for " + joiningUser + ". No action needed here as UI already updated.");
                            // chatRoomUI.addUserToList(joiningUser); // Already done in updateUIForRoomSwitch
                        }
                    }
                });
                break;

            case LEAVE:
                final String leavingUser = sender;
                userLastHeartbeat.remove(leavingUser);
                SwingUtilities.invokeLater(() -> {
                    if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                        chatRoomUI.displaySystemMessage(leavingUser + " has left the room.");
                        chatRoomUI.removeUserFromList(leavingUser);
                    }
                });
                break;
            case HEARTBEAT:
                final String heartbeatSender = sender;
                userLastHeartbeat.put(heartbeatSender, System.currentTimeMillis());
                // If this heartbeat is from another user, ensure they are in our list.
                // This handles cases where we join and they were already there but haven't sent a JOIN to us.
                if (!heartbeatSender.equals(this.currentUsername)) {
                    SwingUtilities.invokeLater(() -> {
                        if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                            // System.out.println("[Controller] Processing HEARTBEAT from " + heartbeatSender + ", ensuring they are in list.");
                            chatRoomUI.addUserToList(heartbeatSender);
                        }
                    });
                } else {
                    // It's our own heartbeat echoed back. Fine, timestamp is updated.
                    // System.out.println("[Controller] Processed own HEARTBEAT for " + heartbeatSender);
                }
                break;
            case FILE_SHARE_OFFER: // <<< NEW: Handle incoming file share offer >>>
                System.out.println("[Controller] Received FILE_SHARE_OFFER from " + sender + " for file: " + messageData.originalFilename);
                final MessageData offer = messageData; // Capture for lambda
                SwingUtilities.invokeLater(() -> {
                    if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                        // Let ChatRoom UI decide how to display this.
                        // It will need a way to render this custom message.
                        // For now, just a system message to confirm receipt.
                        chatRoomUI.displaySystemMessage(sender + " is offering to share file: '" +
                                offer.originalFilename + "' (" + formatFileSize(offer.originalFileSize) + ").");
                        // The actual download logic will be triggered by UI interaction in MessageCellRenderer
                        // For now, the controller just relays the offer info.
                        // We can also directly add it to the chatListModel if MessageCellRenderer is ready
                        // chatRoomUI.appendFileOfferMessage(offer); // (New method needed in ChatRoom)
                    }
                });
                break;
            case DOWNLOAD:
                SwingUtilities.invokeLater(() -> {
                    if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                        if (!sender.equals(this.currentUsername)) {
                            chatRoomUI.displaySystemMessage(sender + " has downloaded the chat history.");
                        }
                    }
                });
                break;
            default:
                System.err.println("[Controller] Received message with unknown type: " + messageData.type);
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", size / Math.pow(1024, exp), pre);
    }

    @Override
    public void onError(final String message, final Exception e) {
        System.err.println("[Controller] Network Listener: Error: " + message + (e != null ? " - " + e.getMessage() : ""));
        if (message != null && message.contains("Existing subscription to channel")) {
            System.out.println("[Controller] Suppressing 'Existing subscription' error from chat display.");
            // Log the stack trace for debugging if needed, but don't show in chat
            if (e != null) { e.printStackTrace(); }
            return; // Exit without calling displaySystemMessage
        }
        SwingUtilities.invokeLater(() -> {
            if (chatRoomUI != null) {
                // Display other network errors
                chatRoomUI.displaySystemMessage("[Network Error] " + message);
            } else {
                // Show popup only if UI isn't available (less intrusive)
                showErrorDialog("Network Error: " + message);
            }
        });
         if (e != null) {
             e.printStackTrace(); // Log stack trace for debugging
         }
    }

    // --- UI Utility Methods ---
    private void showErrorDialog(String message) {
         SwingUtilities.invokeLater(() -> {
             JOptionPane.showMessageDialog(mainFrame, message, "Error", JOptionPane.ERROR_MESSAGE);
         });
    }

    // --- Getter for chat history ---
    /**
     * Retrieves the stored chat history for a specific room.
     * Returns a defensive copy or ensure thread safety if accessed across threads.
     * @param roomName The name of the room.
     * @return A List of ChatMessage objects, or an empty list if no history exists.
     */
    public List<ChatMessage> getChatHistory(String roomName) {
         synchronized(roomChatHistories) { // Synchronize access
             // Return a defensive copy to prevent modification of the internal list by the UI
             return new ArrayList<>(roomChatHistories.getOrDefault(roomName, List.of())); // Use List.of() for empty immutable list if Java 9+
             // Or for older Java: return new ArrayList<>(roomChatHistories.getOrDefault(roomName, Collections.emptyList()));
         }
    }
    private void sendSystemMessage(MessageType type) {
        // Check connection state FIRST
        if (networkService == null || !networkService.isConnectedExplicit()) { // Use explicit check
            System.err.println("[Controller] Skipping system message " + type + ": Network not connected.");
            return;
        }
        // Check username
        if (currentUsername == null) {
            System.err.println("[Controller] Skipping system message " + type + ": currentUsername is null.");
            return;
        }

        System.out.println("[Controller] Sending system message: " + type + " for user: " + currentUsername);
        MessageData sysMessage = new MessageData(type, this.currentUsername);
        networkService.sendChatMessage(sysMessage);
        // Delay for LEAVE is still useful
        if (type == MessageType.LEAVE) {
            try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public void notifyChatDownloaded(String roomName, String downloaderUsername) {
        System.out.println("[Controller] User '" + downloaderUsername + "' initiated download for room: " + roomName);

        // Ensure we are connected, the action is for the active room, and it's the current user performing it.
        if (networkService != null && networkService.isConnectedExplicit() &&
                this.activeRoomName != null && this.activeRoomName.equals(roomName) &&
                this.currentUsername != null && this.currentUsername.equals(downloaderUsername)) {

            System.out.println("[Controller] Sending DOWNLOAD notification for user: " + this.currentUsername);
            sendSystemMessage(MessageType.DOWNLOAD); // sendSystemMessage uses this.currentUsername
        } else {
            System.err.println("[Controller] Conditions not met to send DOWNLOAD notification. " +
                    "Connected: " + (networkService != null && networkService.isConnectedExplicit()) +
                    ", Active Room Match: " + (this.activeRoomName != null && this.activeRoomName.equals(roomName)) +
                    ", User Match: " + (this.currentUsername != null && this.currentUsername.equals(downloaderUsername)));
        }
    }
    private void startHeartbeatTimers() {
        stopHeartbeatTimers(); // Stop any existing timers first

        // Timer to send heartbeats
        heartbeatSendTimer = new Timer("HeartbeatSendTimer-" + currentUsername, true); // Daemon thread
        heartbeatSendTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (networkService.isConnectedExplicit() && currentUsername != null) {
                    // System.out.println("[Controller Heartbeat] Sending HEARTBEAT for " + currentUsername);
                    sendSystemMessage(MessageType.HEARTBEAT);
                }
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS);
        System.out.println("[Controller] Heartbeat SEND timer started for " + currentUsername);

        // Timer to check for timed-out users
        userTimeoutCheckTimer = new Timer("UserTimeoutCheckTimer-" + currentUsername, true); // Daemon thread
        userTimeoutCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkUserTimeouts();
            }
        }, USER_TIMEOUT_THRESHOLD_MS, USER_TIMEOUT_THRESHOLD_MS / 2); // Check more frequently than threshold
        System.out.println("[Controller] User TIMEOUT CHECK timer started for " + currentUsername);
    }

    private void stopHeartbeatTimers() {
        if (heartbeatSendTimer != null) {
            heartbeatSendTimer.cancel();
            heartbeatSendTimer = null;
            System.out.println("[Controller] Heartbeat SEND timer stopped.");
        }
        if (userTimeoutCheckTimer != null) {
            userTimeoutCheckTimer.cancel();
            userTimeoutCheckTimer = null;
            System.out.println("[Controller] User TIMEOUT CHECK timer stopped.");
        }
        userLastHeartbeat.clear(); // Clear last known heartbeats when timers stop (e.g., room switch)
    }

    private void checkUserTimeouts() {
        if (chatRoomUI == null) return; // No UI to update

        long currentTime = System.currentTimeMillis();
        List<String> timedOutUsers = new ArrayList<>();

        // Iterate over a copy of keys to avoid ConcurrentModificationException if removing
        new ArrayList<>(userLastHeartbeat.keySet()).forEach(user -> {
            // Don't time out self
            if (user.equals(currentUsername)) {
                userLastHeartbeat.put(user, currentTime); // Keep self updated
                return;
            }
            Long lastSeen = userLastHeartbeat.get(user);
            if (lastSeen == null || (currentTime - lastSeen > USER_TIMEOUT_THRESHOLD_MS)) {
                timedOutUsers.add(user);
            }
        });

        for (String user : timedOutUsers) {
            System.out.println("[Controller Timeout] User " + user + " timed out.");
            userLastHeartbeat.remove(user); // Remove from tracking
            final String finalUser = user; // For lambda
            SwingUtilities.invokeLater(() -> {
                if (chatRoomUI != null) { // Re-check UI
                    chatRoomUI.displaySystemMessage(finalUser + " has disconnected (timeout).");
                    chatRoomUI.removeUserFromList(finalUser);
                }
            });
        }
    }
}