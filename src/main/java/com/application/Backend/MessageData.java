// src/main/java/com/application/Backend/MessageData.java
package com.application.Backend;

// Define message types
enum MessageType {
    CHAT, JOIN, LEAVE, DOWNLOAD // <<< ADDED DOWNLOAD
}

public class MessageData {
    public MessageType type;
    public String sender;
    public String encryptedData; // Null for JOIN, LEAVE, DOWNLOAD

    // Constructor for CHAT messages
    public MessageData(String sender, String encryptedData) {
        this.type = MessageType.CHAT;
        this.sender = sender;
        this.encryptedData = encryptedData;
    }

    // Constructor for JOIN/LEAVE/DOWNLOAD messages
    public MessageData(MessageType type, String sender) {
        if (type == MessageType.CHAT) {
            throw new IllegalArgumentException("Use the constructor with encryptedData for CHAT messages.");
        }
        this.type = type;
        this.sender = sender;
        this.encryptedData = null; // Not used for these system messages
    }

    // Default constructor for GSON/JSON deserialization
    public MessageData() {}
}