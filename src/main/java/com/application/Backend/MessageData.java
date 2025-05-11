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

    // *** NEW: Room context for general messages ***
    @SerializedName("roomContext")
    public String roomContext;      // Indicates which room this message pertains to (e.g., for JOIN, LEAVE, HEARTBEAT)

    // Common field, primarily for CHAT messages.
    @SerializedName("encryptedData")
    public String encryptedData;    // E2E encrypted chat text or other sensitive payload

    // --- Fields for FILE_SHARE_OFFER ---
    @SerializedName("originalFilename")
    public String originalFilename;

    @SerializedName("originalFileSize")
    public long originalFileSize;

    @SerializedName("downloadUrl")
    public String downloadUrl;      // Link to the encrypted blob

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

    // Constructor for CHAT messages (sent within a specific room)
    public MessageData(String sender, String encryptedDataPayload, String roomContext) {
        this.type = MessageType.CHAT;
        this.sender = sender;
        this.encryptedData = encryptedDataPayload;
        this.roomContext = roomContext; // Room where the chat message belongs
    }

    // Constructor for simple system messages (JOIN, LEAVE, DOWNLOAD notification, HEARTBEAT)
    public MessageData(MessageType type, String sender, String roomContext) {
        if (type == MessageType.CHAT ||
                type == MessageType.FILE_SHARE_OFFER ||
                type == MessageType.PRIVATE_CHAT_REQUEST ||
                type == MessageType.PRIVATE_CHAT_ACCEPTED ||
                type == MessageType.PRIVATE_CHAT_DECLINED) {
            throw new IllegalArgumentException(
                    "Use the specific constructor for CHAT, FILE_SHARE_OFFER, or PRIVATE_CHAT_* message types. This constructor is for simple system messages like JOIN, LEAVE, HEARTBEAT, DOWNLOAD (notification), which also require a roomContext.");
        }
        this.type = type;
        this.sender = sender;
        this.roomContext = roomContext; // Room where the system event occurred
    }

    // Constructor for FILE_SHARE_OFFER messages (sent within a specific room)
    public MessageData(String sender, String originalFilename, long originalFileSize, String downloadUrl, String encryptedFileKey, String fileHash, String roomContext) {
        this.type = MessageType.FILE_SHARE_OFFER;
        this.sender = sender;
        this.originalFilename = originalFilename;
        this.originalFileSize = originalFileSize;
        this.downloadUrl = downloadUrl;
        this.encryptedFileKey = encryptedFileKey;
        this.fileHash = fileHash; // Can be null if not used
        this.roomContext = roomContext; // Room where the file offer is made
    }

    // Constructor for PRIVATE_CHAT_REQUEST messages
    // User A (sender) proposes a room and password to User B (recipient)
    // The request is typically sent from a 'common' or 'active' room of User A.
    public MessageData(String sender, String recipient, String proposedRoomName, String proposedRoomPassword, String originatingRoomContext) {
        this.type = MessageType.PRIVATE_CHAT_REQUEST;
        this.sender = sender;
        this.recipient = recipient;
        this.proposedRoomName = proposedRoomName;
        this.proposedRoomPassword = proposedRoomPassword;
        this.roomContext = originatingRoomContext; // Room from which the request was initiated
    }

    // Constructor for PRIVATE_CHAT_ACCEPTED or PRIVATE_CHAT_DECLINED messages
    // User B (sender) responds to User A (recipient)
    // The response is typically sent from User B's current active room (which might be the 'common' room or another).
    public MessageData(MessageType responseType, String sender, String recipient, String proposedRoomNameContext, String respondingRoomContext) {
        if (responseType != MessageType.PRIVATE_CHAT_ACCEPTED && responseType != MessageType.PRIVATE_CHAT_DECLINED) {
            throw new IllegalArgumentException("This constructor is only for PRIVATE_CHAT_ACCEPTED or PRIVATE_CHAT_DECLINED response types.");
        }
        this.type = responseType;
        this.sender = sender;
        this.recipient = recipient;
        this.proposedRoomName = proposedRoomNameContext; // The room name that was accepted/declined (this is key)
        this.roomContext = respondingRoomContext; // Room from which the response was sent
    }

    // Getter for room context for easier access in ChatController
    public String getRoomContext() {
        return roomContext;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MessageData{");
        sb.append("type=").append(type);
        sb.append(", sender='").append(sender).append('\'');
        if (roomContext != null) sb.append(", roomContext='").append(roomContext).append('\''); // Added roomContext
        if (encryptedData != null) sb.append(", encryptedData='PRESENT'");
        if (originalFilename != null) sb.append(", originalFilename='").append(originalFilename).append('\'');
        if (originalFileSize > 0) sb.append(", originalFileSize=").append(originalFileSize);
        if (downloadUrl != null) sb.append(", downloadUrl='").append(downloadUrl).append('\'');
        if (encryptedFileKey != null) sb.append(", encryptedFileKey='PRESENT'");
        if (fileHash != null) sb.append(", fileHash='PRESENT'");
        if (recipient != null) sb.append(", recipient='").append(recipient).append('\'');
        if (proposedRoomName != null) sb.append(", proposedRoomName='").append(proposedRoomName).append('\'');
        if (proposedRoomPassword != null) sb.append(", proposedRoomPassword='***'");
        sb.append('}');
        return sb.toString();
    }
}