package com.application.Backend.dto; // Or your chosen package

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List; // If payload can be a list
import java.util.Map;  // If payload can be a map

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientSignalingMessage {
    private String type;
    private String fromUser; // Who sent this signal to me / Who am I sending to
    private String toUser;   // Who this signal is intended for
    private String room;
    private Object payload;  // Can be String (SDP), Map (ICE candidate), List (peers)

    // Jackson requires a no-args constructor
    public ClientSignalingMessage() {}

    // Constructor for sending
    public ClientSignalingMessage(String type, String fromUser, String toUser, String room, Object payload) {
        this.type = type;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.room = room;
        this.payload = payload;
    }
    // Simpler constructor for client sending join (server extracts user/room from payload or top-level)
    public ClientSignalingMessage(String type, String room, Map<String, String> joinPayload) {
        this.type = type;
        this.room = room;
        this.payload = joinPayload;
        this.fromUser = joinPayload.get("user"); // Set top-level fromUser too
    }


    // Getters and Setters...
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getFromUser() { return fromUser; }
    public void setFromUser(String fromUser) { this.fromUser = fromUser; }
    public String getToUser() { return toUser; }
    public void setToUser(String toUser) { this.toUser = toUser; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    // Specific payload classes for clarity (optional but good)
    // These would be used when deserializing the 'payload' Object based on 'type'
    public static class SdpPayload {
        @JsonProperty("type") // "offer" or "answer"
        private String sdpType;
        private String sdp;
        public SdpPayload() {}
        // Getters/Setters
        public String getSdpType() { return sdpType; }
        public void setSdpType(String sdpType) { this.sdpType = sdpType; }
        public String getSdp() { return sdp; }
        public void setSdp(String sdp) { this.sdp = sdp; }
    }

    public static class IceCandidatePayload {
        private String candidate;
        private String sdpMid;
        private int sdpMLineIndex;
        public IceCandidatePayload() {}
        // Getters/Setters
        public String getCandidate() { return candidate; }
        public void setCandidate(String candidate) { this.candidate = candidate; }
        public String getSdpMid() { return sdpMid; }
        public void setSdpMid(String sdpMid) { this.sdpMid = sdpMid; }
        public int getSdpMLineIndex() { return sdpMLineIndex; }
        public void setSdpMLineIndex(int sdpMLineIndex) { this.sdpMLineIndex = sdpMLineIndex; }
    }
    public static class RoomPeersPayload {
        private List<String> users;
        public RoomPeersPayload() {}
        public List<String> getUsers() { return users; }
        public void setUsers(List<String> users) { this.users = users; }
    }

    public static class UserEventPayload { // For user_joined, user_left
        private String user;
        public UserEventPayload() {}
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
    }
}