// src/main/java/com/application/Backend/ChatController.java
package com.application.Backend;

import java.awt.GridLayout;
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
import com.application.FrontEnd.components.MessageCellRenderer.ChatMessage; // Explicit import for clarity

/**
 * Controller coordinating actions between the Swing UI and backend services.
 * Manages application state, including per-room chat history.
 */
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

    public ChatController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.encryptionService = new EncryptionService(); // Ensure constructor works
        this.networkService = new PusherService(this); // Ensure constructor accepts NetworkListener
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
        System.out.println("[Controller] User '" + (currentUsername != null ? currentUsername : "Unknown") + "' initiated leaving.");
        if (chatRoomUI != null && currentUsername != null) {
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
        if (networkService != null && networkService.isConnected()) {
            System.out.println("[Controller] Disconnecting network service due to shutdown...");
            networkService.disconnect();
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            System.out.println("[Controller] Network service disconnect requested during shutdown.");
        } else if (networkService != null) {
            System.out.println("[Controller] Network service already disconnected or null during shutdown.");
        }
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

        if (networkService.isConnected()) {
            System.out.println("[Controller] Disconnecting from previous room: " + activeRoomName);
            networkService.disconnect();
        } else {
             System.out.println("[Controller] Not currently connected, no disconnect needed.");
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
        final String connectedChannel = networkService.getChannelName(); // Capture final variable for lambda
        System.out.println("[Controller] Network Listener: Connected event received for channel: " + connectedChannel);
        SwingUtilities.invokeLater(() -> { // Ensure UI updates are on EDT
            if (chatRoomUI != null && activeRoomName != null) {
                // Check if the connection is for the room we currently think is active
                if (connectedChannel != null && connectedChannel.contains(activeRoomName)) {
                    System.out.println("[Controller] Confirmed connection to the active room: " + activeRoomName);
                    // Connection confirmed, NOW update the main UI view (highlight, clear chat, load history)
                    System.out.println("[Controller] Notifying UI to update view for connection to: " + activeRoomName);
                    chatRoomUI.updateUIForRoomSwitch(activeRoomName);
                } else {
                    System.err.println("[Controller] Network Listener: Connected event for unexpected channel '" + connectedChannel + "'. Expected channel containing '" + activeRoomName + "'. State mismatch?");
                    chatRoomUI.displaySystemMessage("[Warning] Connected to network channel: " + connectedChannel);
                    // Decide how to handle mismatch - maybe force UI to reflect actual connection?
                }
            } else {
                 System.out.println("[Controller] Network Listener: Connected event ignored, UI or active room is null/changed. UI State: " + (chatRoomUI != null) + ", Active Room: " + activeRoomName);
            }
        });
    }

    @Override
    public void onDisconnected() {
        System.out.println("[Controller] Network Listener: Disconnected!");
         SwingUtilities.invokeLater(() -> { // Ensure UI updates are on EDT
            if (chatRoomUI != null) {
               chatRoomUI.displaySystemMessage("[System] Disconnected from chat service.");
               // Potentially disable input fields etc.
            }
        });
    }

    @Override
    public void onMessageReceived(final MessageData messageData) { // Ensure MessageData class exists
        if (messageData == null || messageData.sender == null || messageData.encryptedData == null) {
             System.err.println("[Controller] Network Listener: Received invalid MessageData.");
             return;
        }
        // Process only if we know which room is active
        final String roomForMessage = this.activeRoomName; // Capture current active room
        if (roomForMessage == null) {
             System.err.println("[Controller] Message received but no active room set. Ignoring.");
             return;
        }
        // Assume messages only arrive for the currently ACTIVE (subscribed) room
        System.out.println("[Controller] Network Listener: Encrypted message received from " + messageData.sender + " for active room " + roomForMessage);

        try {
            // Decryption must happen off the EDT if potentially slow
            final String decryptedText = encryptionService.decrypt(messageData.encryptedData);
            System.out.println("[Controller] Decrypted message from " + messageData.sender + ": " + decryptedText);

            // Create the ChatMessage object
            final ChatMessage chatMessage = new ChatMessage(messageData.sender, decryptedText);

            // Add to the history map for the room the message belongs to
            // Use computeIfAbsent in a thread-safe manner if accessed by multiple threads,
            // but here it's likely okay if only modified from network thread + controller methods
             synchronized(roomChatHistories) { // Basic synchronization if needed
                 roomChatHistories.computeIfAbsent(roomForMessage, k -> new ArrayList<>()).add(chatMessage);
             }
            System.out.println("[Controller] Added message to history map for room: " + roomForMessage);

            // Update UI on EDT
            SwingUtilities.invokeLater(() -> {
                // Double check if the UI is still active and showing the SAME room
                if (chatRoomUI != null && roomForMessage.equals(this.activeRoomName)) {
                    // Pass strings to match existing appendMessage signature
                    chatRoomUI.appendMessage(chatMessage.getSender(), chatMessage.getMessage());
                } else {
                    System.out.println("[Controller] Message for room '" + roomForMessage + "' received, but UI is not active or shows different room ('"+this.activeRoomName+"'). Message stored, not displayed immediately.");
                    // Optionally add unread indicator to the room tab here?
                }
            });

        } catch (Exception e) {
            System.err.println("[Controller] Failed decrypt message from " + messageData.sender + ": " + e.getMessage());
             // Update UI on EDT
             SwingUtilities.invokeLater(() -> {
                if (chatRoomUI != null) {
                    chatRoomUI.displaySystemMessage("[Error] Could not decrypt message from " + messageData.sender + ".");
                }
             });
        }
    }

    @Override
    public void onError(final String message, final Exception e) {
        System.err.println("[Controller] Network Listener: Error: " + message + (e != null ? " - " + e.getMessage() : ""));
        // Update UI on EDT
         SwingUtilities.invokeLater(() -> {
             if (chatRoomUI != null) {
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

    public void acceptPrivateChat(String fromUser, String proposedRoomName, String proposedPassword) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'acceptPrivateChat'");
    }

    public void declinePrivateChat(String fromUser, String proposedRoomName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'declinePrivateChat'");
    }

    public void requestPrivateChat(String actualUsername) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'requestPrivateChat'");
    }
}