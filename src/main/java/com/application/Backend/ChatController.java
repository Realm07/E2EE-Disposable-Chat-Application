// src/main/java/com/application/Backend/ChatController.java
package com.application.Backend;

import java.awt.GridLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set; // Needed for SwingUtilities and JOptionPane

import javax.swing.JLabel; // Needed for GridLayout
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

import com.application.FrontEnd.ChatRoom;
import com.application.FrontEnd.MainFrame;


/**
 * Controller coordinating actions between the Swing UI and backend services.
 */
public class ChatController implements NetworkListener {

    // References to UI components
    private MainFrame mainFrame;
    private ChatRoom chatRoomUI; // Null until user is in the chat room

    // References to Backend services
    private final EncryptionService encryptionService;
    private final PusherService networkService;

    // State
    private String currentUsername;
    private String activeRoomName; // Name of the currently connected/viewed room
    private Set<String> joinedRoomNames = new HashSet<>(); // Track rooms user has joined in UI

    private static final Map<String, String> PUBLIC_ROOM_KEYS = new HashMap<>();
    static {
        // IMPORTANT: These keys are NOT cryptographically secure passwords.
        // They just need to be non-empty strings for the PBKDF2 process.
        // Use the Room *Identifier* (like "Alpha"), not the display name.
        PUBLIC_ROOM_KEYS.put("Alpha",   "PublicAlphaKey_!@#");
        PUBLIC_ROOM_KEYS.put("Bravo",   "PublicBravoKey_$%^");
        PUBLIC_ROOM_KEYS.put("Charlie", "PublicCharlieKey_*()");
        PUBLIC_ROOM_KEYS.put("Delta",   "PublicDeltaKey_-+=");
        PUBLIC_ROOM_KEYS.put("Echo",    "PublicEchoKey_{}|");
    }
    // Helper to check if a room is public based on our map
    private boolean isPublicRoom(String roomName) {
        return PUBLIC_ROOM_KEYS.containsKey(roomName);
    }

    public ChatController(MainFrame mainFrame) {
        this.mainFrame = mainFrame; // Need this to switch views
        this.encryptionService = new EncryptionService();
        // Pass 'this' controller as the listener for network events
        this.networkService = new PusherService(this);
        System.out.println("[Controller] Initialized.");
    }

    /**
     * Called by LoginPage when the user attempts to join or create a room.
     */
    public void joinInitialRoom(String username, String roomName, String password) {
        System.out.println("[Controller] Initial Join attempt: user=" + username + ", room=" + roomName);
        if (username.isEmpty() || roomName.isEmpty() || password.isEmpty()) {
            showErrorDialog("Username, Room Name, and Password cannot be empty.");
            return;
        }
        this.currentUsername = username;

        // Switch UI first, then attempt connection
        mainFrame.switchToChatRoom(this.currentUsername, roomName);

        // Now that UI is switched, MainFrame should have called setActiveChatRoomUI.
        // Now, delegate actual room joining logic.
        joinOrSwitchToRoom(roomName, password);
    }
    public void joinPublicRoom(String username, String roomName) {
        System.out.println("[Controller] PUBLIC Room Join attempt: user=" + username + ", room=" + roomName);
        // Validation: Only check username and room name
        if (username.isEmpty() || roomName.isEmpty()) {
            showErrorDialog("Username and Room Name cannot be empty.");
            return;
        }
        // Check if it's a known public room (it should be if called from PublicServerRoom)
        if (!isPublicRoom(roomName)) {
            showErrorDialog("Internal Error: Unknown public room '" + roomName + "'.");
            return;
        }
        this.currentUsername = username;

        // Switch UI first
        mainFrame.switchToChatRoom(this.currentUsername, roomName);

        // Retrieve the predefined key/password for this public room
        String predefinedPassword = PUBLIC_ROOM_KEYS.get(roomName);

        // Delegate joining logic using the predefined password
        joinOrSwitchToRoom(roomName, predefinedPassword);
    }

    /**
     * Called by ChatRoom when the user clicks the Send button or presses Enter.
     */
    public void sendMessage(String plainTextMessage) {
        if (plainTextMessage == null || plainTextMessage.trim().isEmpty()) {
            return; // Don't send empty messages
        }
        if (!networkService.isConnected() || activeRoomName == null) {
            // More specific error message based on state
            String errorMsg = activeRoomName == null ? "Not connected to any room." :
                             "Not connected to room '" + activeRoomName + "'.";
            showErrorDialog(errorMsg + " Cannot send message.");
            return;
        }
        if (this.currentUsername == null) {
            showErrorDialog("Internal error: Username not set.");
            return;
        }

        System.out.println("[Controller] Sending message: " + plainTextMessage);
        try {
            // 1. Encrypt message using the derived room key
            String encryptedBase64 = encryptionService.encrypt(plainTextMessage);
            MessageData messageData = new MessageData(this.currentUsername, encryptedBase64);
            System.out.println("[Controller] Attempting to send message to room: " + activeRoomName + " on channel: " + networkService.getChannelName());

            // Send via Network Service (uses its current channelName)
            boolean sendInitiated = networkService.sendChatMessage(messageData);

            if (!sendInitiated) {
                 // PusherService might return false if e.g., client isn't authorized yet or other issues
                 showErrorDialog("Failed to initiate sending message (Network error). Please try again.");
            } else {
                 // Local echo removed. Message will appear when received back from network.
                 System.out.println("[Controller] Message send initiated successfully.");
                 // chatRoomUI.appendMessage(currentUsername, plainTextMessage); // REMOVED LOCAL ECHO
            }
        } catch (Exception e) {
            System.err.println("[Controller] Error encrypting/sending message: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Error sending message: " + e.getMessage());
        }
    }
    private void sendSystemMessage(MessageType type) {
        if (networkService == null || currentUsername == null || !networkService.isConnected()) {
            System.err.println("[Controller] Cannot send system message: " + type + ". Not ready or connected.");
            return;
        }
        System.out.println("[Controller] Sending system message: " + type + " for user: " + currentUsername);
        MessageData sysMessage = new MessageData(type, this.currentUsername);
        networkService.sendChatMessage(sysMessage);
        // Add small delay *after* sending leave message, *before* disconnecting
        if (type == MessageType.LEAVE) {
            try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public void leaveRoom() {
        System.out.println("[Controller] User '" + (currentUsername != null ? currentUsername : "Unknown") + "' initiated leaving via button.");
        // 1. Send LEAVE message *before* disconnecting
        if (currentUsername != null) { // Only send if we know who is leaving
            sendSystemMessage(MessageType.LEAVE);
        } else {
            System.err.println("[Controller] Cannot send LEAVE message: currentUsername is null.");
        }
        // 2. Disconnect network
        if (networkService != null) {
            networkService.disconnect();
            System.out.println("[Controller] Network service disconnect requested via leaveRoom.");
        }
        // 3. Clear state (username, activeRoom, UI ref AFTER sending message)
        String userLeaving = this.currentUsername; // Store before nulling
        this.currentUsername = null;
        this.activeRoomName = null;
        // Display local leaving message *if UI still exists*
        if (chatRoomUI != null && userLeaving != null) {
            final String msg = "[System] You have left the application.";
            SwingUtilities.invokeLater(() -> { if(chatRoomUI != null) chatRoomUI.displaySystemMessage(msg); });
        }
        this.chatRoomUI = null; // Release UI reference
        joinedRoomNames.clear();
        // 4. Switch UI
        System.out.println("[Controller] State cleared. Switching to login page.");
        if (mainFrame != null) {
            SwingUtilities.invokeLater(() -> mainFrame.switchToLoginPage());
        }
    }


    /**
     * Called by MainFrame's WindowListener when the application window is closing.
     * Performs cleanup, focusing on network disconnection.
     */
    public void handleApplicationShutdown() {
        System.out.println("[Controller] Application shutdown requested.");
        // 1. Send LEAVE message *before* disconnecting
        if (currentUsername != null && networkService != null && networkService.isConnected()) {
            sendSystemMessage(MessageType.LEAVE);
        } else {
            System.out.println("[Controller] Cannot send LEAVE on shutdown: No user/connection.");
        }
        // 2. Disconnect network
        if (networkService != null) {
            networkService.disconnect();
            System.out.println("[Controller] Network service disconnect requested during shutdown.");
        }
        System.out.println("[Controller] Shutdown cleanup complete.");
    }

    /**
     * Stores a reference to the currently active ChatRoom panel.
     * Called by MainFrame when switching to the chat view.
     * @param chatRoomUI The active ChatRoom instance.
     */
    public void setActiveChatRoomUI(ChatRoom chatRoomUI) {
        this.chatRoomUI = chatRoomUI;
        if (this.chatRoomUI != null) {
             // If we are already connected to the active room when UI is set,
             // display the connection message immediately.
             // Check if service is connected AND the channel name corresponds to the active room
             if (networkService.isConnected() && activeRoomName != null &&
                 networkService.getChannelName() != null &&
                 networkService.getChannelName().contains(activeRoomName)) { // Simple check
                this.chatRoomUI.displaySystemMessage("[System] Connected to room: " + activeRoomName);
             }
        }
    }

    // --- Main Logic for Joining/Switching Backend Connection ---
    // Inside ChatController.java -> joinOrSwitchToRoom(...) method

    public void joinOrSwitchToRoom(String roomName, String password) {
        System.out.println("[Controller] Attempting to join/switch backend to room: " + roomName);

        // 1. Disconnect from previous room if connected
        if (networkService.isConnected()) {
            // Send LEAVE from the *current* room before disconnecting
            if (this.currentUsername != null && this.activeRoomName != null) {
                sendSystemMessage(MessageType.LEAVE);
            }
            System.out.println("[Controller] Disconnecting from previous channel: " + networkService.getChannelName());
            networkService.disconnect();
            try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }

        // *** Clear the user list UI if it exists (This can happen independently) ***
        if (chatRoomUI != null) {
            SwingUtilities.invokeLater(() -> {
                if(chatRoomUI != null) chatRoomUI.clearUserList();
            });
        } else {
            // This condition might occur if joining the very first room from login before UI is fully set
            System.out.println("[Controller] Cannot clear user list (UI not ready yet or switching): chatRoomUI is null.");
        }

        // *** 2. Derive Key (Execute this *after* disconnect, *before* connect) ***
        System.out.println("[Controller] >> Attempting key derivation for room: " + roomName); // Add log
        if (!encryptionService.deriveRoomKey(roomName, password)) {
            // Adjust error message slightly - might be predefined key error too
            String errorMsg = isPublicRoom(roomName) ?
                    "Failed to derive key for public room '" + roomName + "' (Internal Error)." :
                    "Failed to derive key for room '" + roomName + "'. Check password.";
            showErrorDialog(errorMsg);
            this.activeRoomName = null; // Explicitly set no active room if key fails
            // Update UI to show failure state if applicable (e.g., in ChatRoom if already visible)
            if (chatRoomUI != null) {
                final String finalErrMsg = errorMsg; // Final for lambda
                SwingUtilities.invokeLater(() -> {
                    if(chatRoomUI != null) {
                        chatRoomUI.displaySystemMessage("[System] Key derivation failed: " + finalErrMsg);
                        chatRoomUI.setActiveRoomNameLabel("Key Error");
                    }
                });
            }
            return; // Stop processing if key derivation fails
        }
        System.out.println("[Controller] Key derived successfully for room: " + roomName); // Now this should always appear on success


        // --- BEGIN Network Setup (Execute only after successful key derivation) ---
        try {
            this.activeRoomName = roomName;
            System.out.println("[Controller] >> Setting activeRoomName to: " + this.activeRoomName);

            System.out.println("[Controller] >> Calling networkService.setChannelName with: " + roomName);
            networkService.setChannelName(roomName);
            System.out.println("[Controller] >> networkService.getChannelName() returned: " + networkService.getChannelName());

            System.out.println("[Controller] >> Calling networkService.connect()");
            networkService.connect();
            System.out.println("[Controller] >> networkService.connect() called.");

        } catch (Exception e) {
            System.err.println("[Controller] !!! UNEXPECTED ERROR during network setup in joinOrSwitchToRoom: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Internal error during network setup.");
            return; // Stop if network setup throws immediate error
        }
        // --- END Network Setup ---

        // 4. Update UI reference state (this part happens conceptually, actual UI updates driven by events)
        // Ensure the UI is informed about the target room name, even before connection completes.
        if (chatRoomUI != null) {
            final String finalRoomName = roomName; // Use final variable for lambda
            SwingUtilities.invokeLater(() -> {
                if(chatRoomUI != null) chatRoomUI.updateUIForRoomSwitch(finalRoomName);
            });
        } else {
            System.out.println("[Controller] UI update delayed: chatRoomUI is null during join/switch initiation.");
        }

        System.out.println("[Controller] Successfully initiated switch logic for room: " + roomName + ". Waiting for connection events...");
    }

    // --- Handling UI request to switch --- (Needs Password!)
    public void requestRoomSwitch(String targetRoomName) {
         System.out.println("[Controller] UI requests switch to room: " + targetRoomName);
         if (targetRoomName.equals(this.activeRoomName)) {
             System.out.println("[Controller] Already in room: " + targetRoomName);
             return; // No switch needed
         }

         // ** Need password for the target room to derive key **
         // Simple approach: Re-prompt the user every time they switch.
         JPasswordField passwordField = new JPasswordField(15);
         JPanel panel = new JPanel(new GridLayout(0,1));
         panel.add(new JLabel("Enter password for room: " + targetRoomName));
         panel.add(passwordField);

        // Ensure dialog is shown on EDT
        SwingUtilities.invokeLater(() -> {
            // Set focus to password field for better UX
            passwordField.requestFocusInWindow();

            int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Enter Room Password",
                                                     JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                 char[] passwordChars = passwordField.getPassword();
                 String password = new String(passwordChars);
                 java.util.Arrays.fill(passwordChars, ' '); // Clear temp password ASAP

                 if (!password.isEmpty()) {
                      // Now proceed with the actual switch logic
                      System.out.println("[Controller] Password received for " + targetRoomName + ". Proceeding with switch.");
                      joinOrSwitchToRoom(targetRoomName, password);
                 } else {
                      showErrorDialog("Password cannot be empty.");
                 }
             } else {
                 System.out.println("[Controller] Room switch cancelled by user for room: " + targetRoomName);
                 // If switch is cancelled, UI still shows the old room.
                 // Might need to reset the visual tab highlighting if it was optimistically set.
                 if (chatRoomUI != null && activeRoomName != null) {
                    chatRoomUI.updateUIForRoomSwitch(activeRoomName); // Force UI update to correct tab highlight
                 }
             }
        });
    }

    // --- NetworkListener Implementation ---
    // These methods are called by PusherService on its thread.
    // UI updates MUST be dispatched to the Swing Event Dispatch Thread (EDT).

    @Override
    public void onConnected() {
        // Called when connection to Pusher channel is successful
        String connectedChannel = networkService.getChannelName(); // Get the actual channel connected to
        System.out.println("[Controller] Network Listener: Connected event received for channel: " + connectedChannel);
        if (chatRoomUI != null && activeRoomName != null) {
            // Verify the connection is for the room we intended to connect to
            // Check if the connected channel name contains the active room name.
            // This relies on PusherService using the room name predictably in the channel name.
            if(connectedChannel != null && connectedChannel.contains(activeRoomName)) {
                System.out.println("[Controller] Confirmed connection to the active room: " + activeRoomName);
                chatRoomUI.displaySystemMessage("[System] Connected to room: " + activeRoomName);
                // Potentially enable input fields here if they were disabled during switch
            } else {
                System.err.println("[Controller] Network Listener: Connected event for unexpected channel '" + connectedChannel + "'. Expected channel containing '" + activeRoomName + "'.");
                 // This indicates a potential state mismatch or issue in channel naming/reporting.
                 chatRoomUI.displaySystemMessage("[System] Network connected (Channel: " + connectedChannel +")"); // Show generic message maybe
            }
        } else {
             System.out.println("[Controller] Network Listener: Connected event received, but UI or active room is null. UI State: " + (chatRoomUI != null) + ", Active Room: " + activeRoomName);
        }
    }

    @Override
    public void onDisconnected() {
        System.out.println("[Controller] Network Listener: Disconnected!");
        // This could happen intentionally (switching rooms) or unintentionally (network drop)
        // Avoid showing error dialog if disconnect was intentional (e.g., during a switch)
        // Need more state tracking to differentiate. For now, show a generic message.
        if (chatRoomUI != null) {
           chatRoomUI.displaySystemMessage("[System] Disconnected from chat service.");
           // Optionally disable input fields?
           // chatRoomUI.setInputEnabled(false); // Assuming such a method exists
        }
        // Don't show a pop-up error here, as it might interrupt a room switch flow.
        // showErrorDialog("Disconnected from chat service.");
    }

    @Override
    public void onMessageReceived(MessageData messageData) {
        if (messageData == null || messageData.sender == null || messageData.type == null) {
            System.err.println("[Controller] Network Listener: Received invalid MessageData (null fields).");
            return;
        }

        final String sender = messageData.sender; // Make final for lambda

//        // <<< IGNORE SELF-SENT MESSAGES >>>
//        if (sender.equals(this.currentUsername)) {
//            System.out.println("[Controller] Ignoring self-sent message of type " + messageData.type + " from " + sender);
//            return; // Don't process messages from self
//        }
        
        switch (messageData.type) {
            case CHAT:
                if (messageData.encryptedData == null) {
                    System.err.println("[Controller] Received CHAT message with null data from " + sender);
                    return;
                }
                System.out.println("[Controller] Network Listener: Encrypted CHAT received from " + sender);
                try {
                    String decryptedText = encryptionService.decrypt(messageData.encryptedData);
                    System.out.println("[Controller] Decrypted message from " + sender + ": " + decryptedText);
                    if (chatRoomUI != null) {
                        // Update UI on EDT
                        SwingUtilities.invokeLater(() -> {
                            if (chatRoomUI != null) { // Re-check inside EDT
                                chatRoomUI.appendMessage(sender, decryptedText);
                            }
                        });
                    } else { System.err.println("[Controller] chatRoomUI is null when CHAT received."); }
                } catch (Exception e) {
                    System.err.println("[Controller] Failed decrypt message from " + sender + ": " + e.getMessage());
                    if (chatRoomUI != null) {
                        SwingUtilities.invokeLater(() -> {
                            if (chatRoomUI != null) { // Re-check inside EDT
                                chatRoomUI.displaySystemMessage("[Error] Could not decrypt message from " + sender + ".");
                            }
                        });
                    }
                }
                break;

            case JOIN:
                System.out.println("[Controller] Network Listener: JOIN received from " + sender);
                if (chatRoomUI != null) {
                    SwingUtilities.invokeLater(() -> {
                        if (chatRoomUI != null) { // Re-check inside EDT
                            chatRoomUI.displaySystemMessage(sender + " has joined the room.");
                            chatRoomUI.addUserToList(sender); // Add to UI list
                        }
                    });
                } else { System.err.println("[Controller] chatRoomUI is null when JOIN received."); }
                break;

            case LEAVE:
                System.out.println("[Controller] Network Listener: LEAVE received from " + sender);
                if (chatRoomUI != null) {
                    SwingUtilities.invokeLater(() -> {
                        if (chatRoomUI != null) { // Re-check inside EDT
                            chatRoomUI.displaySystemMessage(sender + " has left the room.");
                            chatRoomUI.removeUserFromList(sender); // Remove from UI list
                        }
                    });
                } else { System.err.println("[Controller] chatRoomUI is null when LEAVE received."); }
                break;

            default:
                System.err.println("[Controller] Received message with unknown type: " + messageData.type);
        }
    }
    @Override
    public void onError(String message, Exception e) { // Unchanged in logic, but use displaySystemMessage
        System.err.println("[Controller] Network Listener: Error: " + message + (e != null ? " - " + e.getMessage() : ""));
        SwingUtilities.invokeLater(() -> { // Ensure UI update is on EDT
            if (chatRoomUI != null) {
                chatRoomUI.displaySystemMessage("[Network Error] " + message);
            } else {
                // If UI not available, show popup as fallback
                showErrorDialog("Network Error: " + message);
            }
            if (e != null) {
                e.printStackTrace(); // Log stack trace for debugging
            }
        });
    }
    // --- UI Utility Methods ---

    private void showErrorDialog(String message) {
         // Ensure dialog is shown on the Event Dispatch Thread
         SwingUtilities.invokeLater(() -> {
             JOptionPane.showMessageDialog(
                 mainFrame, // Parent component
                 message,
                 "Error", // Dialog title
                 JOptionPane.ERROR_MESSAGE
             );
         });
    }
}