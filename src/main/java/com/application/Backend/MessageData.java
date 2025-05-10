// src/main/java/com/application/Backend/MessageData.java
package com.application.Backend;

import com.google.gson.annotations.SerializedName;

enum MessageType {
    CHAT,
    JOIN,
    LEAVE,
    DOWNLOAD, // For chat history download notification
    HEARTBEAT,
    FILE_SHARE_OFFER // <<< NEW: For offering a file
}

public class MessageData {

    @SerializedName("type")
    public MessageType type;

    @SerializedName("sender")
    public String sender;

    // For CHAT messages
    @SerializedName("encryptedData")
    public String encryptedData;

    // --- Fields for FILE_SHARE_OFFER ---
    @SerializedName("originalFilename")
    public String originalFilename; // e.g., "MyDocument.pdf"

    @SerializedName("originalFileSize")
    public long originalFileSize;   // Size in bytes

    @SerializedName("downloadUrl")
    public String downloadUrl;      // Link from transfer.sh (to encrypted blob)

    @SerializedName("encryptedFileKey")
    public String encryptedFileKey; // Room-key-encrypted one-time file key

    @SerializedName("fileHash") // Optional: SHA-256 hash of original unencrypted file
    public String fileHash;
    // --- End Fields for FILE_SHARE_OFFER ---


    // Constructor for CHAT messages
    public MessageData(String sender, String encryptedData) {
        this.type = MessageType.CHAT;
        this.sender = sender;
        this.encryptedData = encryptedData;
    }

    // Constructor for simple system messages (JOIN, LEAVE, DOWNLOAD notification, HEARTBEAT)
    public MessageData(MessageType type, String sender) {
        if (type == MessageType.CHAT || type == MessageType.FILE_SHARE_OFFER) {
            throw new IllegalArgumentException("Use the specific constructor for CHAT or FILE_SHARE_OFFER messages.");
        }
        this.type = type;
        this.sender = sender;
        // Other fields will be null
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
        // encryptedData will be null
    }


    // Default constructor REQUIRED for Gson deserialization
    public MessageData() {}

    // toString for debugging (optional)
    @Override
    public String toString() {
        return "MessageData{" +
                "type=" + type +
                ", sender='" + sender + '\'' +
                (encryptedData != null ? ", encryptedData='PRESENT'" : "") +
                (originalFilename != null ? ", originalFilename='" + originalFilename + '\'' : "") +
                (originalFileSize > 0 ? ", originalFileSize=" + originalFileSize : "") +
                (downloadUrl != null ? ", downloadUrl='" + downloadUrl + '\'' : "") +
                (encryptedFileKey != null ? ", encryptedFileKey='PRESENT'" : "") +
                (fileHash != null ? ", fileHash='PRESENT'" : "") +
                '}';
    }
}