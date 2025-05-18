package com.application.Backend;

import com.application.Backend.dto.ClientSignalingMessage; // Import your client-side DTO

public interface NetworkListener {

    // Signaling Server Events
    void onSignalingMessage(ClientSignalingMessage message); // For SDP, ICE, peer lists from server
    void onSignalingDisconnected();
    void onSignalingConnectionError(String errorMessage, Exception e); // More specific error for signaling

    // WebRTC P2P Events (will be triggered by ChatController's WebRTC logic)
    void onPeerConnected(String peerUserName);          // DataChannel to a peer is open
    void onPeerDisconnected(String peerUserName);       // DataChannel to a peer is closed/failed
    void onMessageReceived(MessageData messageData);    // Application MessageData received over a P2P DataChannel

    // Generic/Other Errors
    void onError(String genericMessage, Exception e);
}