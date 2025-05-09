// src/main/java/com/application/Backend/MessageData.java
package com.application.Backend;

import com.google.gson.annotations.SerializedName; // Optional, but can help

enum MessageType {
    CHAT, JOIN, LEAVE, DOWNLOAD, HEARTBEAT
}

public class MessageData {

    // Use annotations for clarity, though often optional for simple names
    @SerializedName("type")
    public MessageType type;

    @SerializedName("sender")
    public String sender;

    // Crucially, this field CAN be null for non-CHAT types
    @SerializedName("encryptedData")
    public String encryptedData;

    // Constructor for CHAT messages
    public MessageData(String sender, String encryptedData) {
        this.type = MessageType.CHAT;
        this.sender = sender;
        this.encryptedData = encryptedData;
    }

    public MessageData(MessageType type, String sender) {
        if (type == MessageType.CHAT) {
            throw new IllegalArgumentException("Use the constructor with encryptedData for CHAT messages.");
        }
        this.type = type;
        this.sender = sender;
        this.encryptedData = null;
    }

    // Default constructor REQUIRED for Gson deserialization
    public MessageData() {}
}