// src/main/java/org/example/NetworkListener.java
package com.application.Backend;

public interface NetworkListener {
    void onConnected();
    void onDisconnected();
    void onMessageReceived(MessageData messageData); // Use DTO
    void onError(String message, Exception e);
}