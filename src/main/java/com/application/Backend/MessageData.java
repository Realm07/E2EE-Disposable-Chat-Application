// src/main/java/com/application/Backend/MessageData.java
package com.application.Backend;

import com.google.gson.annotations.SerializedName;

enum MessageType {
    CHAT,
    JOIN,
    LEAVE,
    DOWNLOAD,       // For chat history download notification by a user
    HEARTBEAT,
    FILE_SHARE_OFFER,
    PRIVATE_CHAT_REQUEST,
    PRIVATE_CHAT_ACCEPTED,
    PRIVATE_CHAT_DECLINED
}

public class MessageData {

    @SerializedName("type")
    public MessageType type;

    @SerializedName("sender")
    public String sender;

    // Common field, primarily for CHAT messages.
    // For other types, if they need a payload beyond simple fields,
    // that payload could be JSON serialized and put here then encrypted by the room key.
    @SerializedName("encryptedData")
    public String encryptedData;

    // --- Fields for FILE_SHARE_OFFER ---
    @SerializedName("originalFilename")
    public String originalFilename;

    @SerializedName("originalFileSize")
    public long originalFileSize;

    @SerializedName("downloadUrl")
    public String downloadUrl;      // Link to the encrypted blob on transfer.sh

    @SerializedName("encryptedFileKey")
    public String encryptedFileKey; // One-time file key, itself encrypted by the current room's key

    @SerializedName("fileHash")
    public String fileHash;         // Optional: SHA-256 hash of original unencrypted file

    // --- Fields for PRIVATE_CHAT_REQUEST, _ACCEPTED, _DECLINED ---
    @SerializedName("recipient")
    public String recipient;        // Target user for the private chat interaction

    @SerializedName("proposedRoomName")
    public String proposedRoomName; // Name of the temporary private room

    @SerializedName("proposedRoomPassword") // Only populated in PRIVATE_CHAT_REQUEST
    public String proposedRoomPassword;


    // --- Constructors ---

    // Default constructor REQUIRED for Gson deserialization
    public MessageData() {}

    // Constructor for CHAT messages
    public MessageData(String sender, String encryptedDataPayload) {
        this.type = MessageType.CHAT;
        this.sender = sender;
        this.encryptedData = encryptedDataPayload; // This is the E2E encrypted chat text
    }

    // Constructor for simple system messages (JOIN, LEAVE, DOWNLOAD notification, HEARTBEAT)
    // These messages typically only need a type and a sender.
    public MessageData(MessageType type, String sender) {
        if (type == MessageType.CHAT ||
                type == MessageType.FILE_SHARE_OFFER ||
                type == MessageType.PRIVATE_CHAT_REQUEST ||
                type == MessageType.PRIVATE_CHAT_ACCEPTED ||
                type == MessageType.PRIVATE_CHAT_DECLINED) {
            throw new IllegalArgumentException(
                    "Use the specific constructor for CHAT, FILE_SHARE_OFFER, or PRIVATE_CHAT_* message types. This constructor is for simple system messages like JOIN, LEAVE, HEARTBEAT, DOWNLOAD (notification).");
        }
        this.type = type;
        this.sender = sender;
        // All other fields will be null or default for these simple types
    }

    // Constructor for FILE_SHARE_OFFER messages
    public MessageData(String sender, String originalFilename, long originalFileSize, String downloadUrl, String encryptedFileKey, String fileHash) {
        this.type = MessageType.FILE_SHARE_OFFER;
        this.sender = sender;
        this.originalFilename = originalFilename;
        this.originalFileSize = originalFileSize;
        this.downloadUrl = downloadUrl;
        this.encryptedFileKey = encryptedFileKey;
        this.fileHash = fileHash; // Can be null if not used
        // other specific fields (like encryptedData, recipient, proposedRoom*) will be null
    }

    // Constructor for PRIVATE_CHAT_REQUEST messages
    // User A (sender) proposes a room and password to User B (recipient)
    public MessageData(String sender, String recipient, String proposedRoomName, String proposedRoomPassword) {
        this.type = MessageType.PRIVATE_CHAT_REQUEST;
        this.sender = sender;
        this.recipient = recipient;
        this.proposedRoomName = proposedRoomName;
        this.proposedRoomPassword = proposedRoomPassword; // Plaintext here in this object.
        // The calling code in ChatController should decide if this
        // WHOLE MessageData object, or just this password part,
        // needs to be encrypted before sending via Pusher.
    }

    // Constructor for PRIVATE_CHAT_ACCEPTED or PRIVATE_CHAT_DECLINED messages
    // User B (sender) responds to User A (recipient)
    public MessageData(MessageType responseType, String sender, String recipient, String roomNameContext) {
        if (responseType != MessageType.PRIVATE_CHAT_ACCEPTED && responseType != MessageType.PRIVATE_CHAT_DECLINED) {
            throw new IllegalArgumentException("This constructor is only for PRIVATE_CHAT_ACCEPTED or PRIVATE_CHAT_DECLINED response types.");
        }
        this.type = responseType;
        this.sender = sender;
        this.recipient = recipient;
        this.proposedRoomName = roomNameContext; // The room name that was accepted or declined
        // other specific fields will be null
    }


    // toString for debugging (Consider adding all fields)
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MessageData{");
        sb.append("type=").append(type);
        sb.append(", sender='").append(sender).append('\'');
        if (encryptedData != null) sb.append(", encryptedData='PRESENT'");
        if (originalFilename != null) sb.append(", originalFilename='").append(originalFilename).append('\'');
        if (originalFileSize > 0) sb.append(", originalFileSize=").append(originalFileSize); // Only show if set meaningfully
        if (downloadUrl != null) sb.append(", downloadUrl='").append(downloadUrl).append('\'');
        if (encryptedFileKey != null) sb.append(", encryptedFileKey='PRESENT'");
        if (fileHash != null) sb.append(", fileHash='PRESENT'");
        if (recipient != null) sb.append(", recipient='").append(recipient).append('\'');
        if (proposedRoomName != null) sb.append(", proposedRoomName='").append(proposedRoomName).append('\'');
        if (proposedRoomPassword != null) sb.append(", proposedRoomPassword='***'"); // Don't log actual password
        sb.append('}');
        return sb.toString();
    }
}