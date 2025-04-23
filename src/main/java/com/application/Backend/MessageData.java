package com.application.Backend;

/**
 * Data Transfer Object (DTO) representing the data structure
 * sent over the network (via JSON).
 * Contains the sender's identity and the encrypted payload.
 */
public class MessageData {
    // Fields should likely be private with getters/setters,
    // but public fields are simpler for Gson and this stage.
    public String sender;
    public String encryptedData; // Base64 encoded [IV + Ciphertext]

    // Constructor for creating message data before sending
    public MessageData(String sender, String encryptedData) {
        this.sender = sender;
        this.encryptedData = encryptedData;
    }

    // Optional: Add getters if fields become private
    // public String getSender() { return sender; }
    // public String getEncryptedData() { return encryptedData; }
}