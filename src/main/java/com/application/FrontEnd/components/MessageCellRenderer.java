package com.application.FrontEnd.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image; // Added for Image
import java.net.URL;   // Added for URL

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon; // Added for ImageIcon
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

// Assuming InitialCircle is in the same package or imported correctly
// Assuming MainFrame is accessible for font (if used, consider passing font as param)
import com.application.FrontEnd.MainFrame;
import com.application.Backend.MessageData; // To store file offer details if needed for click actions


public class MessageCellRenderer extends JPanel implements ListCellRenderer<MessageCellRenderer.ChatMessage>  { // Generic changed to ChatMessage
    private InitialCircle initialCirclePanel;
    private JTextArea messageArea;
    private JLabel senderLabel;
    // private JLabel senderLabel; // Duplicate senderLabel declaration removed

    // Use a constant for logo path (already good)
    private static final String LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/corner-down-right.png"; // Placeholder, you might not need this logo here anymore

    private final Color defaultMessageColor = new Color(230, 230, 230);
    private final Color defaultSenderColor = new Color(210,210,210);
    private final Color systemMessageColor = new Color(180, 220, 255); // Bluish for system messages
    private final Color systemSenderColor = new Color(150, 180, 220);
    private final Color fileOfferColor = new Color(200, 255, 200); // Greenish for file offers

    public MessageCellRenderer(){
        setLayout(new BorderLayout(5, 2));
        setOpaque(false); // Renderer panel should be transparent if list background handles drawing

        // initialCirclePanel is for the user's initial
        initialCirclePanel = new InitialCircle();
        initialCirclePanel.setOpaque(false);

        JPanel circleWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        circleWrapper.setOpaque(false);
        circleWrapper.add(initialCirclePanel);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        senderLabel = new JLabel();
        senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 13)); // Slightly smaller
        senderLabel.setForeground(defaultSenderColor);
        senderLabel.setOpaque(false);
        senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        messageArea = new JTextArea();
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setForeground(defaultMessageColor);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Slightly smaller
        messageArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0)); // Indent message from sender
        messageArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(senderLabel);
        textPanel.add(messageArea);

        add(circleWrapper, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8)); // Consistent padding
    }

    // createLogoLabel might not be needed here if the avatar (InitialCircle) serves its purpose
    // If you still need a different logo for system messages or other types, keep it.
    // For now, I'm assuming it's not used in the main message rendering path.

    @Override
    public Component getListCellRendererComponent(JList<? extends ChatMessage> list, // Generic type specified
                                                  ChatMessage value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        if (value == null) { // Handle null values gracefully
            senderLabel.setText("Error");
            messageArea.setText("Invalid message data.");
            initialCirclePanel.setData('!', Color.RED);
            return this;
        }

        String sender = value.getSender();
        String messageText = value.getMessage();
        String messageType = value.getType(); // Get the message type

        char initial = (sender != null && !sender.isEmpty()) ? sender.charAt(0) : '?';
        Color userColor = getUserColor(sender);

        // Default styling
        senderLabel.setForeground(defaultSenderColor);
        messageArea.setForeground(defaultMessageColor);
        senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        initialCirclePanel.setVisible(true); // Default to visible

        switch (messageType) {
            case "SYSTEM":
                senderLabel.setForeground(systemSenderColor);
                senderLabel.setFont(new Font("Segoe UI", Font.ITALIC | Font.BOLD, 13));
                messageArea.setForeground(systemMessageColor);
                messageArea.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                initialCirclePanel.setVisible(false); // Hide avatar for system messages
                // For system messages, senderLabel might just show "[System]" or be part of messageArea
                senderLabel.setText(sender); // e.g., "[System]"
                messageArea.setText(messageText);
                break;
            case "FILE_OFFER_PLACEHOLDER": // Adjust this type if you change it in ChatRoom/Controller
                senderLabel.setForeground(defaultSenderColor); // Or a specific file offer sender color
                messageArea.setForeground(fileOfferColor);
                // Make message text look clickable (e.g., underline or specific font) - future enhancement
                // For now, just color differentiation
                senderLabel.setText(sender + ":");
                messageArea.setText(messageText + " (Click to interact - TBD)"); // Placeholder for click
                // TODO: If value.getAttachment() exists and contains MessageData for the file,
                // you could store it here to handle clicks later (e.g., using a JList MouseListener).
                break;
            case "STANDARD":
            default: // Handles "STANDARD" and any other unknown types as standard chat
                senderLabel.setText(sender + ":");
                messageArea.setText(messageText);
                initialCirclePanel.setData(initial, userColor);
                break;
        }

        // Selection and focus handling (can be customized further)
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            // You might want to change text colors on selection too for better contrast
            // senderLabel.setForeground(list.getSelectionForeground());
            // messageArea.setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground()); // Use list's background (should be transparent)
        }

        setEnabled(list.isEnabled());
        return this;
    }

    private Color getUserColor(String username) {
        if (username == null || username.isEmpty() || username.equals("[System]")) { // Avoid color for System
            return Color.GRAY; // Default for unknown or system
        }
        // Simple hash-based color generation (same as before)
        int hash = username.hashCode();
        int r = 100 + (Math.abs((hash & 0xFF0000) >> 16) % 128);
        int g = 100 + (Math.abs((hash & 0x00FF00) >> 8) % 128);
        int b = 100 + (Math.abs(hash & 0x0000FF) % 128);
        return new Color(r, g, b);
    }

    // --- Inner ChatMessage Class ---
    public static class ChatMessage {
        private String sender;
        private String message;
        private String type; // Added type field
        private MessageData attachment; // Optional: For file offers or other complex data

        // Updated constructor to include type
        public ChatMessage(String sender, String message, String type) {
            this.sender = sender;
            this.message = message;
            this.type = type;
        }

        // Convenience constructor (defaults to "STANDARD" type)
        public ChatMessage(String sender, String message) {
            this(sender, message, "STANDARD");
        }

        public String getSender() { return sender; }
        public String getMessage() { return message; }
        public String getType() { return type; } // Getter for type

        public MessageData getAttachment() { return attachment; }
        public void setAttachment(MessageData attachment) { this.attachment = attachment; }

        @Override
        public String toString() {
            // This toString is mainly for debugging or if the object is directly rendered by a default renderer.
            // Our custom renderer uses the individual fields.
            return type + " - " + sender + ": " + message;
        }
    }
}