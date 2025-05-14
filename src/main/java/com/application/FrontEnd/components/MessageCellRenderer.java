package com.application.FrontEnd.components;

import java.awt.BorderLayout;
import java.awt.CardLayout; // Added for CardLayout
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
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

import com.application.FrontEnd.MainFrame; // Import MainFrame
import com.application.Backend.MessageData; // Assuming this is used for ChatMessage attachments

public class MessageCellRenderer extends JPanel implements ListCellRenderer<MessageCellRenderer.ChatMessage>  {
    private InitialCircle initialCirclePanel; // For initials fallback
    private JLabel avatarImageLabel;          // For displaying image avatar
    private JTextArea messageArea;
    private JLabel senderLabel;
    private JPanel westPanelContainer;        // Holds avatar/initials and logo

    private MainFrame mainFrameInstance;      // Reference to MainFrame

    // CardLayout for swapping avatar image and initial circle
    private CardLayout avatarSlotCardLayout;
    private JPanel avatarDisplaySlotWithCardLayout; // Panel managed by CardLayout

    private static final String LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/corner-down-right.png";
    private static final int AVATAR_SIZE = 30; // Standard size for avatar/initial circle

    // Card names
    private static final String AVATAR_IMAGE_CARD_NAME = "AVATAR_IMAGE_CARD";
    private static final String INITIAL_CIRCLE_CARD_NAME = "INITIAL_CIRCLE_CARD";


    private final Color defaultMessageColor = new Color(230, 230, 230);
    private final Color defaultSenderColor = new Color(210,210,210);
    private final Color systemMessageColor = new Color(180, 220, 255);
    private final Color systemSenderColor = new Color(150, 180, 220);
    private final Color fileOfferColor = new Color(200, 255, 200);

    public MessageCellRenderer(MainFrame mainFrame){ // Constructor now accepts MainFrame
        this.mainFrameInstance = mainFrame;

        setLayout(new BorderLayout(5, 2));
        setOpaque(false);

        // --- West Panel Setup (Avatar/Initials + Logo) ---
        westPanelContainer = new JPanel();
        westPanelContainer.setLayout(new BoxLayout(westPanelContainer, BoxLayout.Y_AXIS));
        westPanelContainer.setOpaque(false);

        // --- Avatar Display Slot with CardLayout ---
        avatarSlotCardLayout = new CardLayout();
        avatarDisplaySlotWithCardLayout = new JPanel(avatarSlotCardLayout);
        avatarDisplaySlotWithCardLayout.setOpaque(false);
        avatarDisplaySlotWithCardLayout.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        // To see bounds during debug:
        // avatarDisplaySlotWithCardLayout.setOpaque(true);
        // avatarDisplaySlotWithCardLayout.setBackground(Color.GREEN);


        avatarImageLabel = new JLabel(); // Plain JLabel for the user's avatar image
        avatarImageLabel.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        avatarImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarImageLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatarImageLabel.setOpaque(false);
        // To see bounds during debug:
        // avatarImageLabel.setOpaque(true); avatarImageLabel.setBackground(Color.ORANGE); avatarImageLabel.setText("IMG");


        initialCirclePanel = new InitialCircle(); // For fallback initials
        // If InitialCircle needs explicit sizing:
        // initialCirclePanel.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        initialCirclePanel.setOpaque(false);
        // To see bounds during debug:
        // initialCirclePanel.setOpaque(true); initialCirclePanel.setBackground(Color.PINK);


        avatarDisplaySlotWithCardLayout.add(avatarImageLabel, AVATAR_IMAGE_CARD_NAME);
        avatarDisplaySlotWithCardLayout.add(initialCirclePanel, INITIAL_CIRCLE_CARD_NAME);
        // --- End Avatar Display Slot ---

        westPanelContainer.add(avatarDisplaySlotWithCardLayout); // Add the card panel

        JLabel actualLogoLabel = createLogoLabel(); // The fixed "arrow" logo
        actualLogoLabel.setOpaque(false);
        actualLogoLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // For BoxLayout parent
        westPanelContainer.add(Box.createRigidArea(new Dimension(0,2))); // Small space
        westPanelContainer.add(actualLogoLabel);
        // --- End West Panel Setup ---

        // --- Text Panel Setup (Sender + Message) ---
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);

        senderLabel = new JLabel();
        senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        senderLabel.setForeground(defaultSenderColor);
        senderLabel.setOpaque(false);

        messageArea = new JTextArea();
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setForeground(defaultMessageColor);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        messageArea.setBorder(null);

        textPanel.add(senderLabel, BorderLayout.NORTH);
        textPanel.add(messageArea, BorderLayout.CENTER);
        // --- End Text Panel Setup ---

        add(westPanelContainer, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
    }


    @Override
    public Component getListCellRendererComponent(JList<? extends ChatMessage> list,
                                                    ChatMessage value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
        if (value == null) {
            senderLabel.setText("Error");
            messageArea.setText("Invalid message data.");
            initialCirclePanel.setData('!', Color.RED);
            avatarSlotCardLayout.show(avatarDisplaySlotWithCardLayout, INITIAL_CIRCLE_CARD_NAME);
            avatarImageLabel.setIcon(null); // Clear any old image
            westPanelContainer.setVisible(true);
        } else {
            String sender = value.getSender();
            String messageText = value.getMessage();
            String messageType = value.getType();

            char initial = (sender != null && !sender.isEmpty()) ? Character.toUpperCase(sender.charAt(0)) : '?';
            Color userColor = getUserColor(sender);

            senderLabel.setForeground(defaultSenderColor);
            messageArea.setForeground(defaultMessageColor);
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            avatarImageLabel.setIcon(null); // Clear previous image from label before attempting new one
            westPanelContainer.setVisible(true);

            switch (messageType) {
                case "SYSTEM":
                    senderLabel.setForeground(systemSenderColor);
                    senderLabel.setFont(new Font("Segoe UI", Font.ITALIC | Font.BOLD, 14));
                    messageArea.setForeground(systemMessageColor);
                    messageArea.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                    westPanelContainer.setVisible(false);
                    senderLabel.setText(sender);
                    messageArea.setText(messageText);
                    break;
                case "FILE_OFFER_PLACEHOLDER":
                    senderLabel.setForeground(defaultSenderColor);
                    messageArea.setForeground(fileOfferColor);
                    senderLabel.setText(sender);
                    messageArea.setText(messageText);
                    attemptDisplayUserAvatar(sender, initial, userColor);
                    break;
                case "STANDARD":
                default:
                    senderLabel.setText(sender);
                    messageArea.setText(messageText);
                    attemptDisplayUserAvatar(sender, initial, userColor);
                    break;
            }
        }

        // --- TEXT AREA WIDTH CALCULATION FOR WRAPPING ---
        int listWidth = list.getWidth();
        if (listWidth <= 0 && list.getParent() instanceof JViewport) {
            listWidth = list.getParent().getWidth();
        }

        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, list);
        int actualCellContentWidth = listWidth;

        if (scrollPane != null) {
            JViewport viewport = scrollPane.getViewport();
            if (viewport != null) {
                actualCellContentWidth = viewport.getWidth();
            }
            if (scrollPane.getVerticalScrollBar() != null && scrollPane.getVerticalScrollBar().isVisible()) {
                actualCellContentWidth -= scrollPane.getVerticalScrollBar().getWidth();
            }
        }

        if (list.getFixedCellWidth() > 0 && list.getFixedCellWidth() != Short.MAX_VALUE && list.getFixedCellWidth() != Integer.MAX_VALUE) {
            actualCellContentWidth = Math.min(actualCellContentWidth, list.getFixedCellWidth());
        }

        if (actualCellContentWidth <= 0) {
            actualCellContentWidth = 300;
        }

        Insets rendererInsets = this.getInsets();
        int westPanelCurrentWidth = 0;
        if (westPanelContainer.isVisible()) {
            // Get preferred size of the CardLayout panel, as its content drives the size
            westPanelCurrentWidth = westPanelContainer.getPreferredSize().width;
        }

        int borderLayoutHGap = ((BorderLayout)getLayout()).getHgap();
        int textPanelAvailableWidth = actualCellContentWidth
                                    - rendererInsets.left
                                    - rendererInsets.right
                                    - westPanelCurrentWidth
                                    - (westPanelCurrentWidth > 0 ? borderLayoutHGap : 0);
        textPanelAvailableWidth = Math.max(50, textPanelAvailableWidth);

        if (messageArea.getWidth() != textPanelAvailableWidth) {
             messageArea.setSize(textPanelAvailableWidth, Short.MAX_VALUE);
        }
        // --- END TEXT AREA WIDTH CALCULATION ---

        if (isSelected) {
            setBackground(new Color(list.getSelectionBackground().getRed(),
                                    list.getSelectionBackground().getGreen(),
                                    list.getSelectionBackground().getBlue(), 100));
        } else {
            setBackground(null);
        }

        setEnabled(list.isEnabled());
        return this;
    }

    private void attemptDisplayUserAvatar(String sender, char initial, Color userColor) {
        String avatarPath = null;
        if (mainFrameInstance != null) {
            avatarPath = mainFrameInstance.getSelectedUserAvatarPathForRenderer();
            System.out.println("[MCR - " + sender + "] Path from MainFrame: '" + avatarPath + "'"); // MCR for MessageCellRenderer
        } else {
            System.out.println("[MCR - " + sender + "] MainFrameInstance is null!");
            initialCirclePanel.setData(initial, userColor); // Set data before showing
            avatarSlotCardLayout.show(avatarDisplaySlotWithCardLayout, INITIAL_CIRCLE_CARD_NAME);
            westPanelContainer.setVisible(true); // Ensure west panel is visible
            return;
        }

        boolean avatarLoadedSuccessfully = false;
        if (avatarPath != null && !avatarPath.isEmpty()) {
            try {
                URL imgUrl = MessageCellRenderer.class.getResource(avatarPath);
                if (imgUrl != null) {
                    System.out.println("[MCR - " + sender + "] Found image URL: " + imgUrl.toExternalForm());
                    ImageIcon icon = new ImageIcon(imgUrl);
                    if (icon.getImage() != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                        Image scaledImage = icon.getImage().getScaledInstance(
                                AVATAR_SIZE, AVATAR_SIZE, Image.SCALE_SMOOTH);
                        avatarImageLabel.setIcon(new ImageIcon(scaledImage));
                        avatarSlotCardLayout.show(avatarDisplaySlotWithCardLayout, AVATAR_IMAGE_CARD_NAME);
                        avatarLoadedSuccessfully = true;
                        System.out.println("[MCR - " + sender + "] Avatar loaded successfully: " + avatarPath);
                    } else {
                        System.err.println("[MCR - " + sender + "] ImageIcon bad data or zero dimensions for: " + avatarPath + ". Icon width: " + icon.getIconWidth());
                    }
                } else {
                    System.err.println("[MCR - " + sender + "] Avatar resource NOT FOUND (imgUrl was null) at path: " + avatarPath);
                }
            } catch (Exception e) {
                System.err.println("[MCR - " + sender + "] EXCEPTION loading avatar '" + avatarPath + "': " + e.getMessage());
                e.printStackTrace(); // Print full stack trace for detailed error
            }
        } else {
            System.out.println("[MCR - " + sender + "] Avatar path is null or empty.");
        }

        if (!avatarLoadedSuccessfully) {
            System.out.println("[MCR - " + sender + "] Fallback to initials.");
            initialCirclePanel.setData(initial, userColor); // Set data before showing
            avatarSlotCardLayout.show(avatarDisplaySlotWithCardLayout, INITIAL_CIRCLE_CARD_NAME);
            avatarImageLabel.setIcon(null); // Explicitly clear if avatar load failed
        }
        westPanelContainer.setVisible(true); // Ensure west panel is generally visible for non-system messages
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
                if (icon.getIconWidth() > 0) {
                    Image scaledImage = icon.getImage().getScaledInstance(
                            16, -1, Image.SCALE_SMOOTH);
                    ImageIcon scaledIcon = new ImageIcon(scaledImage);
                    label.setIcon(scaledIcon);
                    label.setPreferredSize(new Dimension(scaledIcon.getIconWidth(), scaledIcon.getIconHeight()));
                } else {
                     throw new RuntimeException("Logo ImageIcon width is 0 or invalid: " + LOGO_IMAGE_PATH);
                }
            } else {
                throw new RuntimeException("Logo resource not found: " + LOGO_IMAGE_PATH);
            }
        } catch (Exception e) {
            System.err.println("ERROR loading logo (" + LOGO_IMAGE_PATH + "): " + e.getMessage());
            label.setText(">");
            label.setForeground(Color.GRAY);
            Font fallbackFont = (MainFrame.sansationBold != null) ? MainFrame.sansationBold.deriveFont(10f) : new Font("SansSerif", Font.BOLD, 10);
            label.setFont(fallbackFont);
            label.setPreferredSize(new Dimension(16, 10));
        }
        return label;
    }

    private Color getUserColor(String username) {
        if (username == null || username.isEmpty() || username.equals("[System]")) {
            return Color.GRAY;
        }
        int hash = username.hashCode();
        int r = 100 + (Math.abs((hash & 0xFF0000) >> 16) % 128);
        int g = 100 + (Math.abs((hash & 0x00FF00) >> 8) % 128);
        int b = 100 + (Math.abs(hash & 0x0000FF) % 128);
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