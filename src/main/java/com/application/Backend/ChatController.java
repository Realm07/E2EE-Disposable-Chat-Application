// src/main/java/com/application/Backend/ChatController.java
package com.application.Backend;

import java.awt.GridLayout;
import java.util.HashSet;
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

    /**
     * Called by ChatRoom when the user clicks the Leave Room button.
     */
    public void leaveRoom() {
        System.out.println("[Controller] User leaving application.");
        networkService.disconnect();
        this.currentUsername = null;
        this.activeRoomName = null;
        this.chatRoomUI = null; // Release UI reference
        joinedRoomNames.clear();
        // Clear any sensitive data if necessary (keys are re-derived anyway)
        // encryptionService.clearKeys(); // If such a method exists
        System.out.println("[Controller] State cleared. Switching to login page.");
        mainFrame.switchToLoginPage();
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
    public void joinOrSwitchToRoom(String roomName, String password) {
         System.out.println("[Controller] Attempting to join/switch backend to room: " + roomName);
         // 1. Disconnect from previous room's channel if connected
         if (networkService.isConnected()) {
             System.out.println("[Controller] Disconnecting from previous channel: " + networkService.getChannelName());
             networkService.disconnect();
             // Add a brief pause to allow disconnection before proceeding
             // A more robust solution would use callbacks or status checks from PusherService
             try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
         }

         // 2. Derive Key for the new room
         if (!encryptionService.deriveRoomKey(roomName, password)) {
             showErrorDialog("Failed to derive key for room '" + roomName + "'. Check password.");
             // State is now: disconnected, activeRoomName might be old or null.
             this.activeRoomName = null; // Explicitly set no active room if key fails
             if (chatRoomUI != null) {
                // Update UI to show failure, potentially disable input?
                chatRoomUI.displaySystemMessage("[System] Failed to join " + roomName + ". Please check password and try switching again.");
                // Maybe set room label to "Disconnected" or similar?
                chatRoomUI.setActiveRoomNameLabel("Disconnected");
             }
             return;
         }
         System.out.println("[Controller] Key derived successfully for room: " + roomName);

         // 3. Configure and Connect Network to new room
         this.activeRoomName = roomName; // Update active room *before* connecting
         networkService.setChannelName(roomName); // Let PusherService handle prefixing
         System.out.println("[Controller] Network service channel set to: " + networkService.getChannelName());
         System.out.println("[Controller] Attempting network connection for room: " + roomName);
         networkService.connect(); // Start async connection

          // 4. Update UI (Tell ChatRoom UI about the new active room state)
          // Ensure the tab exists in the UI & update its state
          if (chatRoomUI != null) {
             chatRoomUI.addRoomTab(roomName); // Add tab if not already there (handles duplicates internally)
             chatRoomUI.updateUIForRoomSwitch(roomName); // Update label, load history etc.
          } else {
             System.err.println("[Controller] chatRoomUI is null during join/switch. UI not updated.");
          }
          joinedRoomNames.add(roomName); // Track that we've joined this room in UI
          System.out.println("[Controller] Successfully initiated switch to room: " + roomName + ". Waiting for connection confirmation...");
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
        if (messageData == null || messageData.sender == null || messageData.encryptedData == null) {
             System.err.println("[Controller] Network Listener: Received invalid MessageData.");
            return;
        }
        System.out.println("[Controller] Network Listener: Encrypted message received from " + messageData.sender);

        // ** Only process message if it's for the currently ACTIVE room **
        // We infer this because we are only SUBSCRIBED to the active room's channel.
        // So any message arriving *must* be for the active room.

        try {
            // Decrypt using the key derived for the *activeRoomName*
            String decryptedText = encryptionService.decrypt(messageData.encryptedData);
            System.out.println("[Controller] Decrypted message from " + messageData.sender + ": " + decryptedText);
            // Update the ChatRoom UI (guaranteed to be the correct room's UI)
            if (chatRoomUI != null) {
                chatRoomUI.appendMessage(messageData.sender, decryptedText);
            } else {
                 System.err.println("[Controller] chatRoomUI is null when message received. Cannot display.");
            }
        } catch (Exception e) {
            System.err.println("[Controller] Failed decrypt message from " + messageData.sender + ": " + e.getMessage());
            // Show error in chat area instead of dialog to be less intrusive
             if (chatRoomUI != null) {
                  // Ensure system messages are clearly marked
                  chatRoomUI.displaySystemMessage("[Error] Could not decrypt message from " + messageData.sender + ".");
             }
            // Don't show a blocking dialog for every failed decryption
        }
    }

    @Override
    public void onError(String message, Exception e) { // Unchanged in logic, but use displaySystemMessage
        System.err.println("[Controller] Network Listener: Error: " + message + (e != null ? " - " + e.getMessage() : ""));
         if (chatRoomUI != null) {
             chatRoomUI.displaySystemMessage("[Network Error] " + message);
         } else {
            // If UI not available, show popup as fallback
            showErrorDialog("Network Error: " + message);
         }
         if (e != null) {
             e.printStackTrace(); // Log stack trace for debugging
         }
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