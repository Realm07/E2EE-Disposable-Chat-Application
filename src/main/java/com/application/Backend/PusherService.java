// src/main/java/com/application/Backend/PusherService.java
package com.application.Backend;

import com.google.gson.Gson;
import com.pusher.client.Pusher; // Keep this import
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

public class PusherService {
    // --- Pusher Configuration ---
    private static final String PUSHER_APP_ID = "1976156"; // Your App ID
    private static final String PUSHER_KEY = "5b3c44d07eb30e24b4af";    // Your App Key
    private static final String PUSHER_SECRET = "774f79b39f0978ee01f5"; // Your App Secret (SERVER-SIDE ONLY ideally)
    private static final String PUSHER_CLUSTER = "ap2"; // Your cluster
    private static final String MESSAGE_EVENT = "secure-message-v1"; // Your event name

    private String channelName = "default-public-channel";
    private com.pusher.rest.Pusher pusherHttp; // For sending messages
    private Pusher pusherClient;               // For receiving messages (WebSocket) - will be re-initialized
    private NetworkListener listener;
    private volatile boolean isConnectedFlag = false; // Internal flag

    public PusherService(NetworkListener listener) {
        this.listener = listener;
        // Initialize only the HTTP client here
        try {
            pusherHttp = new com.pusher.rest.Pusher(PUSHER_APP_ID, PUSHER_KEY, PUSHER_SECRET);
            pusherHttp.setCluster(PUSHER_CLUSTER);
            pusherHttp.setEncrypted(true); // Use HTTPS for sending
            System.out.println("[PusherService] Pusher HTTP client Initialized");
        } catch (Exception e) {
            System.err.println("[PusherService] CRITICAL ERROR Initializing Pusher HTTP client: " + e.getMessage());
            if (listener != null) listener.onError("Pusher HTTP Client Init Failed", e);
        }
    }

    public String getChannelName() {
        return this.channelName;
    }

    public void setChannelName(String roomName) {
        System.out.println("[PusherService] >> setChannelName called with roomName: " + roomName);
        this.channelName = "chat-room-" + roomName.replaceAll("[^a-zA-Z0-9_-]", "-");
        System.out.println("[PusherService] Target channel set to: " + this.channelName);
    }

    public void connect() {
        System.out.println("[PusherService] >> connect() method entered.");
        if (this.pusherClient != null) {
            System.out.println("[PusherService] Existing pusherClient found. Attempting full cleanup...");
            // Remove all bindings for the old client explicitly
            // This requires knowing the channel name it was subscribed to.
            // If we don't have the old channel name reliably, this is harder.
            // For now, we rely on the new instance strategy.

            // Ensure all listeners are removed for the old client.
            // The Pusher library should ideally handle this on disconnect, but let's be explicit.
            // This is tricky as listeners are usually on channels.
            // pusherClient.getConnection().unbind(ConnectionState.ALL, existingConnectionEventListener); // Would need to store listener

            this.pusherClient.disconnect();
            System.out.println("[PusherService] Old pusherClient disconnect requested.");
            this.pusherClient = null;
            this.isConnectedFlag = false;
            System.out.println("[PusherService] Old pusherClient nullified.");
            // Give a slightly longer pause to ensure resources MIGHT be released by the OS/library
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }


        // --- Create and initialize a NEW Pusher client instance ---
        System.out.println("[PusherService] Initializing NEW Pusher WebSocket client instance.");
        try {
            PusherOptions options = new PusherOptions().setCluster(PUSHER_CLUSTER);
            options.setEncrypted(true); // Use WSS
            this.pusherClient = new Pusher(PUSHER_KEY, options); // Create new instance
            System.out.println("[PusherService] NEW Pusher WebSocket client instance initialized.");
        } catch (Exception e) {
            System.err.println("[PusherService] CRITICAL ERROR Initializing NEW Pusher WebSocket client: " + e.getMessage());
            if (listener != null) listener.onError("Pusher WebSocket Client Init Failed", e);
            return; // Cannot proceed if client init fails
        }
        // --- End New Client Initialization ---


        System.out.println("[PusherService] Attempting to connect WebSocket for channel: " + this.channelName);
        this.pusherClient.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                System.out.println("[PusherService] State changed: " + change.getPreviousState() + " -> " + change.getCurrentState());
                isConnectedFlag = (change.getCurrentState() == ConnectionState.CONNECTED);

                if (isConnectedFlag) {
                    System.out.println("[PusherService] Successfully connected. Subscribing to channel...");
                    subscribeToChannel(); // Subscribe automatically when connected
                    if (listener != null) listener.onConnected();
                } else if (change.getCurrentState() == ConnectionState.DISCONNECTED &&
                        change.getPreviousState() != ConnectionState.DISCONNECTED && // Avoid multiple disconnect events
                        change.getPreviousState() != null) { // Ensure previous state was not also null (initial)
                    System.out.println("[PusherService] Disconnected from WebSocket.");
                    if (listener != null) listener.onDisconnected();
                }
            }

            @Override
            public void onError(String message, String code, Exception e) {
                System.err.println("[PusherService] Connection error: " + message + " Code: " + code);
                if (e != null) e.printStackTrace(); // Print stack trace for Pusher errors
                if (listener != null) listener.onError("Pusher Connection Error: " + message, e);
            }
        }, ConnectionState.ALL);
    }

    public void disconnect() {
        System.out.println("[PusherService] >> disconnect() method entered.");
        if (this.pusherClient != null) {
            // No need to explicitly unsubscribe if we are creating a new client instance on each connect.
            // The old client instance will be garbage collected along with its subscriptions.
            System.out.println("[PusherService] Disconnecting WebSocket client instance...");
            this.pusherClient.disconnect();
            this.pusherClient = null; // Nullify the reference
            this.isConnectedFlag = false;
            System.out.println("[PusherService] WebSocket client instance disconnected and nullified.");
        } else {
            System.out.println("[PusherService] No active WebSocket client to disconnect.");
            this.isConnectedFlag = false; // Ensure flag is false
        }
    }

    public boolean isConnectedExplicit() {
        // Check the current client instance, not just the flag
        return this.pusherClient != null && this.pusherClient.getConnection().getState() == ConnectionState.CONNECTED;
    }

    public boolean isConnected() {
        // Use the internal flag, which is updated by state changes.
        // This is generally fine, but isConnectedExplicit() is more direct for pre-send checks.
        return isConnectedFlag;
    }

    private void subscribeToChannel() {
        System.out.println("[PusherService] >> subscribeToChannel() method entered.");
        if (this.pusherClient == null || !isConnectedExplicit() || this.channelName == null || this.channelName.startsWith("chat-room-default-public-channel")) {
            System.err.println("[PusherService] Cannot subscribe: Client null, not connected, or channel invalid. Connected: " + (this.pusherClient != null && isConnectedExplicit()) + ", Channel: " + this.channelName);
            return;
        }
        System.out.println("[PusherService] Subscribing to channel: " + this.channelName);
        try {
            Channel channel = this.pusherClient.subscribe(this.channelName); // Use current client instance

            channel.bind(MESSAGE_EVENT, new SubscriptionEventListener() {
                @Override
                public void onEvent(PusherEvent event) {
                    System.out.println("[PusherService Raw Event] EventName: " + event.getEventName() + ", ChannelName: " + event.getChannelName() + ", Raw Data: " + event.getData());
                    MessageData messageData = null;
                    if (listener != null) {
                        try {
                            Gson gson = new Gson();
                            messageData = gson.fromJson(event.getData(), MessageData.class);
                            if (messageData == null || messageData.sender == null || messageData.type == null) {
                                System.err.println("[PusherService] Parsed MessageData has null essential fields. Raw: " + event.getData());
                                messageData = null;
                            } else if (messageData.type == MessageType.CHAT && messageData.encryptedData == null) {
                                System.err.println("[PusherService] Parsed CHAT MessageData missing encryptedData. Raw: " + event.getData());
                                messageData = null;
                            }
                        } catch (Exception e) {
                            System.err.println("[PusherService] Exception during Gson parsing: " + e.getMessage() + ". Raw: " + event.getData());
                            messageData = null;
                        }
                        if (messageData != null) {
                            listener.onMessageReceived(messageData);
                        } else {
                            System.err.println("[PusherService] Skipping onMessageReceived due to parsing failure or invalid data for raw: " + event.getData());
                        }
                    }
                }
            });
            System.out.println("[PusherService] Successfully bound to event '" + MESSAGE_EVENT + "' on channel '" + this.channelName + "'");
        } catch (IllegalArgumentException e) {
            // This is where "Already subscribed" would be caught if it still happens
            System.err.println("[PusherService] Error during subscribe/bind (Possibly already subscribed?): " + e.getMessage());
            if(listener != null) listener.onError("Subscription Error: " + e.getMessage(), e);
            // e.printStackTrace(); // Less verbose unless specifically debugging this
        } catch (Exception e) {
            System.err.println("[PusherService] General error during subscribe/bind: " + e.getMessage());
            if(listener != null) listener.onError("Subscription Error", e);
            e.printStackTrace();
        }
    }

    public boolean sendChatMessage(MessageData messageData) {
        if (pusherHttp == null || this.channelName == null || this.channelName.equals("default-public-channel") || this.channelName.startsWith("chat-room-default-public-channel")) {
            System.err.println("[PusherService] Cannot send HTTP: PusherHTTP client or Channel name not ready. Channel: " + this.channelName);
            return false;
        }
        // No need to check WebSocket connection state for HTTP sending via pusherHttp
        try {
            System.out.println("[PusherService] Triggering '" + MESSAGE_EVENT + "' on '" + this.channelName + "' via HTTP with data: " + messageData.type + " from " + messageData.sender);
            pusherHttp.trigger(this.channelName, MESSAGE_EVENT, messageData);
            return true;
        } catch (Exception e) {
            System.err.println("[PusherService] Failed to trigger chat message via HTTP: " + e.getMessage());
            e.printStackTrace();
            if (listener != null) listener.onError("Failed chat send (HTTP)", e);
            return false;
        }
    }
}