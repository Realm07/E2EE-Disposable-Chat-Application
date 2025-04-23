// src/main/java/org/example/ConsoleChatApp.java
package com.application.Backend;

public class ConsoleChatApp implements NetworkListener {

    private final ConsoleUI ui;
    private final EncryptionService encryptionService;
    private final PusherService networkService;
    private String userName;


    public ConsoleChatApp() {
        this.ui = new ConsoleUI();
        this.encryptionService = new EncryptionService();
        this.networkService = new PusherService(this);
    }

    public void start() {
        // 1. Get Room Details and Username
        this.userName = ui.getUserInput("Enter your username: ");
        String roomName = ui.getRoomNameInput("Enter room name: ");
        String password = ui.getPasswordInput("Enter room password: ");
        ui.displaySystemMessage("Welcome, " + userName + "! Joining room: " + roomName);

        // 2. Initialize Crypto using Room/Password
        if (!encryptionService.deriveRoomKey(roomName, password)) {
            ui.displayError("Application cannot start: Failed to derive room key from password.");
            System.exit(1);
        }

        // 3. Configure and Connect Network Service
        networkService.setChannelName(roomName); // Tell PusherService which channel to use
        ui.displaySystemMessage("Connecting to chat service...");
        networkService.connect();

        // 4. Start Input Loop
        runInputLoop(); // Updated prompts inside

        // 5. Cleanup
        shutdown();
    }

    private void runInputLoop() {
        ui.displaySystemMessage("You are in room '" + networkService.getChannelName() + "'. Type messages or 'quit' to exit.");
        while (true) {
            String input = ui.getUserInput(userName + "> "); // Use simple prompt

            if (input.trim().isEmpty()) continue;

            if ("quit".equalsIgnoreCase(input.trim())) {
                break;
            }
            // Handle Message Sending (simplified)
            if (networkService.isConnected()) {
                ui.displayLocalMessage("You: " + input); // Echo locally
                handleSendChatMessage(input); // Pass plaintext directly
            } else {
                ui.displayError("Not connected. Message not sent.");
            }
        }
    }

    // Simplified send process - encrypts with single room key
    private void handleSendChatMessage(String plaintextMessage) {
        try {
            // 1. Encrypt using the derived room key
            String encryptedBase64 = encryptionService.encrypt(plaintextMessage);

            // 2. Create DTO (uses own username)
            MessageData messageData = new MessageData(this.userName, encryptedBase64);

            // 3. Send via Network Service
            if (!networkService.sendChatMessage(messageData)) {
                ui.displayError("Failed to send message (network error?).");
            }
        } catch (Exception e) {
            ui.displayError("Failed to encrypt or send message: " + e.getMessage());
            e.printStackTrace(); // Optional
        }
    }

    private void shutdown() { // Unchanged
        ui.displaySystemMessage("Disconnecting and shutting down...");
        networkService.disconnect();
        ui.close();
        ui.displaySystemMessage("Exited.");
    }

    // --- NetworkListener Implementation ---

    @Override
    public void onConnected() { // Simplified
        ui.displaySystemMessage("Successfully connected to room: " + networkService.getChannelName());
    }

    @Override
    public void onDisconnected() { // Unchanged
        ui.displayError("Disconnected from chat service.");
    }

    // Simplified message reception - decrypts with single room key
    @Override
    public void onMessageReceived(MessageData messageData) {
        // Ignore own messages (already handled by sender check in PusherService parsing ideally, but double check)
        if (messageData == null || messageData.sender == null || this.userName.equals(messageData.sender)) {
            return;
        }

        String sender = messageData.sender;
        String encryptedBase64 = messageData.encryptedData;


        try {
            // Decrypt using the single derived roomSecretKey
            String decryptedText = encryptionService.decrypt(encryptedBase64);
            ui.displayMessage(sender, decryptedText); // Display normally

        } catch (Exception e) {
            // Decryption failure likely means wrong password or tampered message
            ui.displayError("Failed to decrypt message from " + sender + ". Wrong password or corrupted data?");
            // ui.displaySystemMessage("[DEBUG] Raw encrypted data: " + encryptedBase64); // Debug if needed
            // e.printStackTrace();
        }
    }


    @Override
    public void onError(String message, Exception e) { // Unchanged
        ui.displayError("Network or System Error: " + message + (e != null ? " - " + e.getMessage() : ""));
    }

    // --- Main Entry Point --- Unchanged
    public static void main(String[] args) {
        ConsoleChatApp app = new ConsoleChatApp();
        app.start();
    }
}

