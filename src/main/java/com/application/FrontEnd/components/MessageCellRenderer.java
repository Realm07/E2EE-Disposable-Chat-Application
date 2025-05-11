package com.application.FrontEnd.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets; // Added for Insets
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;


import com.application.FrontEnd.MainFrame;
import com.application.Backend.MessageData;


public class MessageCellRenderer extends JPanel implements ListCellRenderer<MessageCellRenderer.ChatMessage>  {
    private InitialCircle initialCirclePanel;
    private JTextArea messageArea;
    private JLabel senderLabel;

    private static final String LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/corner-down-right.png";

    private final Color defaultMessageColor = new Color(230, 230, 230);
    private final Color defaultSenderColor = new Color(210,210,210);
    private final Color systemMessageColor = new Color(180, 220, 255);
    private final Color systemSenderColor = new Color(150, 180, 220);
    private final Color fileOfferColor = new Color(200, 255, 200);

    public MessageCellRenderer(){
        setLayout(new BorderLayout(5, 2)); // hgap=5, vgap=2
        setOpaque(false);

        JLabel logoLabel = createLogoLabel();
        logoLabel.setOpaque(false);

        initialCirclePanel = new InitialCircle(); // Assuming InitialCircle sets its preferred size
        initialCirclePanel.setOpaque(false);

        JPanel circleWrapper = new JPanel();
        circleWrapper.setLayout(new BoxLayout(circleWrapper, BoxLayout.Y_AXIS));
        circleWrapper.setOpaque(false);
        
        circleWrapper.add(initialCirclePanel);
        circleWrapper.add(logoLabel);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);

        senderLabel = new JLabel();
        senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        senderLabel.setForeground(defaultSenderColor);
        senderLabel.setOpaque(false);
        senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        messageArea = new JTextArea();
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setForeground(defaultMessageColor);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        messageArea.setBorder(null); // Important for accurate width calculation

        textPanel.add(senderLabel, BorderLayout.NORTH);
        textPanel.add(messageArea, BorderLayout.SOUTH);

        add(circleWrapper, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5)); // top, left, bottom, right
    }


    @Override
    public Component getListCellRendererComponent(JList<? extends ChatMessage> list,
                                                    ChatMessage value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
        if (value == null) {
            senderLabel.setText("Error");
            messageArea.setText("Invalid message data.");
            initialCirclePanel.setData('!', Color.RED);
            initialCirclePanel.setVisible(true);
        } else {
            String sender = value.getSender();
            String messageText = value.getMessage();
            String messageType = value.getType();

            char initial = (sender != null && !sender.isEmpty()) ? sender.charAt(0) : '?';
            Color userColor = getUserColor(sender);

            senderLabel.setForeground(defaultSenderColor);
            messageArea.setForeground(defaultMessageColor);
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            initialCirclePanel.setVisible(true);

            switch (messageType) {
                case "SYSTEM":
                    senderLabel.setForeground(systemSenderColor);
                    senderLabel.setFont(new Font("Segoe UI", Font.ITALIC | Font.BOLD, 14));
                    messageArea.setForeground(systemMessageColor);
                    messageArea.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                    initialCirclePanel.setVisible(false);
                    senderLabel.setText(sender);
                    messageArea.setText(messageText);
                    break;
                case "FILE_OFFER_PLACEHOLDER":
                    senderLabel.setForeground(defaultSenderColor);
                    messageArea.setForeground(fileOfferColor);
                    senderLabel.setText(sender);
                    messageArea.setText(messageText + " (Click to interact - TBD)");
                    initialCirclePanel.setData(initial, userColor);
                    break;
                case "STANDARD":
                default:
                    senderLabel.setText(sender);
                    messageArea.setText(messageText);
                    initialCirclePanel.setData(initial, userColor);
                    break;
            }
        }

        // --- MODIFIED TEXT AREA WIDTH CALCULATION FOR WRAPPING ---
        int actualCellContentWidth = list.getWidth();

        // --- CORRECTED JScrollPane CHECK ---
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, list);

        if (scrollPane != null) { // If list is indeed inside a JScrollPane
            JViewport viewport = scrollPane.getViewport();
            if (viewport != null) {
                actualCellContentWidth = viewport.getWidth(); // Width of the visible area for the list
            }
            // Subtract scrollbar width if visible
            if (scrollPane.getVerticalScrollBar() != null && scrollPane.getVerticalScrollBar().isVisible()) {
                actualCellContentWidth -= scrollPane.getVerticalScrollBar().getWidth();
            }
        } else if (actualCellContentWidth <= 0 && list.getParent() instanceof JViewport) {
            // Fallback: if list.getWidth() was 0 but it's directly in a viewport (e.g. not in a JScrollPane but still in a viewport)
            JViewport viewport = (JViewport) list.getParent();
            actualCellContentWidth = viewport.getWidth();
        }
        // --- END CORRECTED JScrollPane CHECK ---


        // Use fixed cell width if available and valid, but don't exceed actual available viewport width
        if (list.getFixedCellWidth() > 0 && list.getFixedCellWidth() != Short.MAX_VALUE && list.getFixedCellWidth() != Integer.MAX_VALUE) {
            if (actualCellContentWidth > 0) {
                actualCellContentWidth = Math.min(actualCellContentWidth, list.getFixedCellWidth());
            } else {
                actualCellContentWidth = list.getFixedCellWidth();
            }
        }

        if (actualCellContentWidth <= 0) {
            actualCellContentWidth = 300;
        }

        Insets rendererInsets = this.getInsets();

        int westComponentWidth = 0;
        if (initialCirclePanel.isVisible()) {
            westComponentWidth = initialCirclePanel.getPreferredSize().width;
        }

        int borderLayoutHGap = ((BorderLayout)getLayout()).getHgap();

        int textPanelAvailableWidth = actualCellContentWidth
                                    - rendererInsets.left
                                    - rendererInsets.right
                                    - (initialCirclePanel.isVisible() ? westComponentWidth : 0)
                                    - (initialCirclePanel.isVisible() ? borderLayoutHGap : 0);

        textPanelAvailableWidth = Math.max(20, textPanelAvailableWidth);

        if (messageArea.getWidth() != textPanelAvailableWidth || messageArea.getHeight() == 0) {
            messageArea.setSize(textPanelAvailableWidth, Integer.MAX_VALUE);
        }
        // --- END TEXT AREA WIDTH CALCULATION ---

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            senderLabel.setForeground(list.getSelectionForeground());
            messageArea.setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
        }

        senderLabel.setOpaque(false);
        messageArea.setOpaque(false);
        initialCirclePanel.setOpaque(false);

        setEnabled(list.isEnabled());
        return this;
    }
        
    private JLabel createLogoLabel() {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setOpaque(false);
        try {
            URL imgUrl = getClass().getResource(LOGO_IMAGE_PATH);
            if (imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                if (icon.getIconWidth() > 0) { // Check if image loaded
                    Image scaledImage = icon.getImage().getScaledInstance(
                            20, -1, Image.SCALE_SMOOTH); // Scale to 20px width
                    ImageIcon scaledIcon = new ImageIcon(scaledImage);
                    label.setIcon(scaledIcon);
                    // Set preferred size based on scaled image
                    label.setPreferredSize(new Dimension(scaledIcon.getIconWidth(), scaledIcon.getIconHeight()));
                } else {
                     throw new Exception("Logo ImageIcon width is 0 or invalid.");
                }
            } else {
                throw new Exception("Logo resource not found: " + LOGO_IMAGE_PATH);
            }
        } catch (Exception e) {
            System.err.println("ERROR loading logo (" + LOGO_IMAGE_PATH + "): " + e.getMessage());
            label.setText(">");
            label.setForeground(Color.GRAY);
            if (MainFrame.sansationBold != null) {
                label.setFont(MainFrame.sansationBold.deriveFont(10f));
            } else {
                label.setFont(new Font("SansSerif", Font.BOLD, 10));
            }
            label.setPreferredSize(new Dimension(20, 10)); // Fallback preferred size
        }
        return label;
    }


    private Color getUserColor(String username) {
        if (username == null || username.isEmpty() || username.equals("[System]")) {
            return Color.GRAY;
        }
        int hash = username.hashCode();
        // Ensure colors are not too dark or too light, keep them somewhat vibrant
        int r = 100 + (Math.abs((hash & 0xFF0000) >> 16) % 128); // Range 100-227
        int g = 100 + (Math.abs((hash & 0x00FF00) >> 8) % 128);  // Range 100-227
        int b = 100 + (Math.abs(hash & 0x0000FF) % 128);         // Range 100-227
        return new Color(r, g, b);
    }

    public static class ChatMessage {
        private String sender;
        private String message;
        private String type;
        private MessageData attachment;

        public ChatMessage(String sender, String message, String type) {
            this.sender = sender;
            this.message = message;
            this.type = type;
        }

        public ChatMessage(String sender, String message) {
            this(sender, message, "STANDARD");
        }

        public String getSender() { return sender; }
        public String getMessage() { return message; }
        public String getType() { return type; }

        public MessageData getAttachment() { return attachment; }
        public void setAttachment(MessageData attachment) { this.attachment = attachment; }

        @Override
        public String toString() {
            return type + " - " + sender + ": " + message;
        }
    }
}