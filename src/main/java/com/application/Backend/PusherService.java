// src/main/java/org/example/PusherService.java
package com.application.Backend;

import com.google.gson.Gson;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;


public class PusherService {
    // --- Pusher Configuration ---
    private static final String PUSHER_APP_ID = "1976156";
    private static final String PUSHER_KEY = "5b3c44d07eb30e24b4af";
    private static final String PUSHER_SECRET = "774f79b39f0978ee01f5"; // INSECURE Client-Side Secret!
    private static final String PUSHER_CLUSTER = "ap2";
    // Keep channel/event names configurable or constant as needed
    private static final String CHANNEL_NAME = "public-chat-room";
    private static final String MESSAGE_EVENT = "secure-message-v1";

    private String channelName = "default-public-channel"; // Default or set dynamically
    private com.pusher.rest.Pusher pusherHttp;
    private Pusher pusherClient;
    private NetworkListener listener;
    private volatile boolean isConnected = false;

    public PusherService(NetworkListener listener) {
        this.listener = listener;
        // Don't initialize clients until channel is known? Or use default channel?
        // Let's initialize, but connect/subscribe require channel name
        initializeClients();
    }

    public String getChannelName() { // Add getter
        return this.channelName;
    }
    // New method to set the channel name *after* user provides room name
    // Inside PusherService.java
    public void setChannelName(String roomName) {
        // Use a different prefix or none, but avoid 'private-' and 'presence-'
        this.channelName = "chat-room-" + roomName.replaceAll("[^a-zA-Z0-9_-]", "-");
        // Or even just use the sanitized roomName if confident it won't clash with Pusher system names
        // this.channelName = roomName.replaceAll("[^a-zA-Z0-9_-]", "-");
        System.out.println("[Network] Target channel set to: " + this.channelName);
    }

    private void initializeClients() {
        try {
            // Init Sender (HTTP)
            pusherHttp = new com.pusher.rest.Pusher(PUSHER_APP_ID, PUSHER_KEY, PUSHER_SECRET);
            pusherHttp.setCluster(PUSHER_CLUSTER);
            pusherHttp.setEncrypted(true);
            System.out.println("[Network] Pusher HTTP client Initialized");

            // Init Listener (WebSocket)
            PusherOptions options = new PusherOptions().setCluster(PUSHER_CLUSTER);
            options.setEncrypted(true);
            pusherClient = new Pusher(PUSHER_KEY, options);
            System.out.println("[Network] Pusher WebSocket client Initialized");

        } catch (Exception e) {
            System.err.println("[Network] CRITICAL ERROR Initializing Pusher clients: " + e.getMessage());
            // Optionally notify listener of initialization failure
            if (listener != null) listener.onError("Pusher Client Init Failed", e);
            // Depending on app requirements, maybe throw runtime exception
        }
    }

    public void connect() {
        if (this.channelName == null || this.channelName.equals("default-public-channel")){
            System.err.println("[Network] Cannot connect: Channel name not set. Call setChannelName first.");
            return;
        }

        System.out.println("[Network] Attempting to connect WebSocket for channel: " + this.channelName);
        pusherClient.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                System.out.println("[Network] State changed: " + change.getPreviousState() + " -> " + change.getCurrentState());
                isConnected = (change.getCurrentState() == ConnectionState.CONNECTED);

                if (isConnected) {
                    subscribeToChannel(); // Subscribe automatically when connected
                    if (listener != null) listener.onConnected(); // Notify listener
                } else if (change.getCurrentState() == ConnectionState.DISCONNECTED &&
                        change.getPreviousState() != ConnectionState.DISCONNECTED) {
                    if (listener != null) listener.onDisconnected(); // Notify listener on disconnect
                }
            }

            @Override
            public void onError(String message, String code, Exception e) {
                System.err.println("[Network] Connection error: " + message + " Code: " + code);

                if (listener != null) listener.onError("Pusher Connection Error: " + message, e);
            }
        }, ConnectionState.ALL);
    }

    public void disconnect() {
        if (pusherClient != null) {
            System.out.println("[Network] Disconnecting WebSocket...");
            pusherClient.disconnect();
            isConnected = false;
            // Listener notified via onConnectionStateChange -> DISCONNECTED
        }
    }

    public boolean isConnected() {
        // Return internal flag, might be slightly delayed vs actual state but avoids null checks
        return isConnected;
        // OR more accurate: return pusherClient != null && pusherClient.getConnection().getState() == ConnectionState.CONNECTED;
    }

    private void subscribeToChannel() {
        if (pusherClient == null || !isConnected || this.channelName == null || this.channelName.equals("default-public-channel")) {
            System.err.println("[Network] Cannot subscribe: Check connection state or channel name.");
            return;
        }
        System.out.println("[Network] Subscribing to channel: " + this.channelName);
        try {
            // Subscribe using the dynamically set channel name
            Channel channel = pusherClient.subscribe(this.channelName);

            // Bind only to MESSAGE event now
            channel.bind(MESSAGE_EVENT, new SubscriptionEventListener() {
                @Override
                public void onEvent(PusherEvent event) {
                    //System.out.println("[Network] Received MESSAGE event raw: " + event.getData());
                    if (listener != null) {
                        // We expect Pusher to send the raw JSON object string now due to sending fix
                        try {
                            Gson gson = new Gson(); // Need gson instance here or pass one in
                            MessageData messageData = gson.fromJson(event.getData(), MessageData.class);
                            if (messageData != null && messageData.sender != null && messageData.encryptedData != null) {
                                listener.onMessageReceived(messageData);
                            } else {
                                System.err.println("[Network] Received incomplete/malformed MessageData: " + event.getData());
                            }
                        } catch (Exception e) { // Catch potential JsonSyntaxException etc.
                            System.err.println("[Network] Error parsing MessageData JSON: " + e.getMessage());
                            System.err.println("[Network] Raw message data: " + event.getData());
                            e.printStackTrace();
                        }
                    }
                }
            });

            System.out.println("[Network] Subscribed to channel '" + this.channelName + "' for event '" + MESSAGE_EVENT + "'");

        } catch (Exception e) { // Catch errors during subscribe/bind
            System.err.println("[Network] Error during subscribe/bind: " + e.getMessage());
            if(listener != null) listener.onError("Subscription Error", e);
            e.printStackTrace();
        }
    }

    public boolean sendChatMessage(MessageData messageData) {
        if (pusherHttp == null || this.channelName == null || this.channelName.equals("default-public-channel")) {
            System.err.println("[Network] Cannot send: PusherHTTP client or Channel name not ready.");
            return false;
        }
        try {
            //Pass the object directly, let pusher-http-java handle serialization ---
            // This assumes the library uses Gson or similar internally if you pass an object.
            // Check library docs if this fails. Alternative: pass a Map<String, String>.
            //System.out.println("[Network] Triggering '" + MESSAGE_EVENT + "' on '" + this.channelName + "' with data object: " + messageData);
            pusherHttp.trigger(this.channelName, MESSAGE_EVENT, messageData); // Pass object, NOT JSON string

            return true;
        } catch (Exception e) {
            System.err.println("[Network] Failed to trigger chat message: " + e.getMessage());
            e.printStackTrace(); // See details of Pusher API error
            if (listener != null) listener.onError("Failed chat send", e);
            return false;
        }
    }
    // Method for more direct state check
    public boolean isConnectedExplicit() {
        return pusherClient != null && pusherClient.getConnection().getState() == ConnectionState.CONNECTED;
    }
}