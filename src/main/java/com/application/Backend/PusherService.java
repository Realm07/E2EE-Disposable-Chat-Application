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
        System.out.println("[PusherService connect] Attempting to establish connection for channel: " + this.channelName);
        if (this.channelName == null || this.channelName.equals("default-public-channel") || this.channelName.startsWith("chat-room-default-public-channel")) {
            System.err.println("[PusherService connect] Cannot connect: Channel name invalid. Current: " + this.channelName);
            if (listener != null) listener.onError("Connection attempt failed: Invalid channel name.", null);
            return;
        }

        // --- Disconnect and nullify any existing client FIRST ---
        if (this.pusherClient != null) {
            System.out.println("[PusherService connect] Existing pusherClient found. Disconnecting and nullifying it.");
            try {
                // It's important that listeners on the old client's connection
                // don't interfere or try to act on a client that's being replaced.
                // The ConnectionEventListener is tied to the connect call, so a new one is made for the new client.
                this.pusherClient.disconnect(); // Request the old client to disconnect
                System.out.println("[PusherService connect] Old pusherClient.disconnect() called.");
            } catch (Exception e) {
                System.err.println("[PusherService connect] Exception during old client disconnect: " + e.getMessage());
            } finally {
                this.pusherClient = null; // Dereference the old client
                this.isConnectedFlag = false; // Reset flag
                System.out.println("[PusherService connect] Old pusherClient instance nullified.");
            }
            // A very brief pause to potentially allow underlying network resources to settle.
            // This is speculative and not a guaranteed fix for all scenarios.
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        // --- End Disconnect Old Client ---

        System.out.println("[PusherService connect] Initializing NEW Pusher WebSocket client instance for: " + this.channelName);
        try {
            PusherOptions options = new PusherOptions().setCluster(PUSHER_CLUSTER).setEncrypted(true);
            // *** Create the NEW Pusher client instance ***
            this.pusherClient = new Pusher(PUSHER_KEY, options);
            System.out.println("[PusherService connect] NEW Pusher WebSocket client instance created.");
        } catch (Exception e) {
            System.err.println("[PusherService connect] CRITICAL ERROR Initializing NEW Pusher WebSocket client: " + e.getMessage());
            if (listener != null) listener.onError("Pusher WebSocket Client Init Failed", e);
            return;
        }

        System.out.println("[PusherService connect] Connecting new client instance for channel: " + this.channelName);
        // Each time we connect with a new client instance, we provide a new ConnectionEventListener instance.
        this.pusherClient.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                // Important: Check if this event is for the CURRENT pusherClient instance,
                // especially if there were rapid connect/disconnects.
                // However, since we nullify the old one, this listener should only be active for the current one.
                System.out.println("[PusherService Listener] State changed: " +
                        change.getPreviousState() + " -> " + change.getCurrentState() +
                        " for channel " + PusherService.this.channelName); // Log current channel context

                PusherService.this.isConnectedFlag = (change.getCurrentState() == ConnectionState.CONNECTED);

                if (PusherService.this.isConnectedFlag) {
                    System.out.println("[PusherService Listener] Successfully connected for " + PusherService.this.channelName + ". Subscribing...");
                    subscribeToChannel(); // This will use the current this.pusherClient
                    if (PusherService.this.listener != null) PusherService.this.listener.onConnected();
                } else if (change.getCurrentState() == ConnectionState.DISCONNECTED &&
                        change.getPreviousState() != ConnectionState.DISCONNECTED &&
                        change.getPreviousState() != null ) { // Avoid multiple/initial disconnect events
                    System.out.println("[PusherService Listener] Disconnected event for " + PusherService.this.channelName);
                    if (PusherService.this.listener != null) PusherService.this.listener.onDisconnected();
                }
            }

            @Override
            public void onError(String message, String code, Exception e) {
                System.err.println("[PusherService Listener] Connection error for " + PusherService.this.channelName + ": " + message + " Code: " + code);
                if (e != null) e.printStackTrace();
                if (PusherService.this.listener != null) PusherService.this.listener.onError("Pusher Connection Error: " + message, e);
            }
        }, ConnectionState.ALL);
        System.out.println("[PusherService connect] Connect call initiated for: " + this.channelName);
    }

    public void disconnect() {
        System.out.println("[PusherService disconnect] Public disconnect called.");
        internalDisconnect(false); // 'false' means not part of a new connection sequence
    }
    /**
     * Internal disconnect logic.
     * @param isPartOfNewConnectionCycle true if called immediately before creating a new client instance.
     */
    private void internalDisconnect(boolean isPartOfNewConnectionCycle) {
        if (this.pusherClient != null) {
            System.out.println("[PusherService internalDisconnect] Disconnecting existing WebSocket client instance. Part of new connection: " + isPartOfNewConnectionCycle);
            // The main goal is to stop the client and allow it to be GC'd.
            // Explicit unsubscription or listener unbinding on the old instance is
            // less critical if we ensure it's fully replaced.
            try {
                this.pusherClient.disconnect();
                // If we really want to wait for the DISCONNECTED state before nullifying.
                // This could block if the state change never comes.
                // A more robust way involves CountDownLatch if waiting is essential.
                // For now, aggressive nullification after requesting disconnect.
                // System.out.println("[PusherService internalDisconnect] Waiting for old client to fully disconnect...");
                // Thread.sleep(200); // Short, speculative wait

            } catch (Exception e) {
                System.err.println("[PusherService internalDisconnect] Exception during old client disconnect: " + e.getMessage());
            } finally {
                this.pusherClient = null; // Most important step
                this.isConnectedFlag = false;
                System.out.println("[PusherService internalDisconnect] Current pusherClient field (old instance) nullified.");
            }
        } else {
            System.out.println("[PusherService internalDisconnect] No active WebSocket client to disconnect.");
            this.isConnectedFlag = false; // Ensure flag is consistent
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