package com.application.FrontEnd;

// Swing & AWT Imports
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter; // For file writing
import java.io.File;         // For file chooser
import java.io.FileWriter;   // For file writing
import java.io.IOException;  // For file I/O errors
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

// Backend and Component Imports
import com.application.Backend.ChatController;
// Assuming MessageData class/interface exists for file sharing offers
import com.application.Backend.MessageData; // Placeholder, ensure this exists
import com.application.FrontEnd.components.CustomButton;
import com.application.FrontEnd.components.CustomLabel;
import com.application.FrontEnd.components.CustomTextField;
import com.application.FrontEnd.components.MessageCellRenderer;
import com.application.FrontEnd.components.MessageCellRenderer.ChatMessage;


public class ChatRoom extends JPanel {

    // --- UI Components (Frontend Dominant) ---
    private JLayeredPane layeredPane;
    private BackgroundImagePanel backgroundPanel;
    private JPanel mainContentPanel;

    private JPanel userEntriesPanel; // Replaces CustomTextArea userNameArea for user list
    private JScrollPane chatScrollPane;
    private TitledBorder chatScrollPaneBorder;
    private JList<ChatMessage> chatList;
    private DefaultListModel<ChatMessage> chatListModel;
    private CustomTextField chatTextField;
    private CustomButton sendButton;
    private CustomButton emojiButton;
    private CustomButton fileSendButton; // Frontend's "+" button, will trigger shareFile
    private JPanel roomTabPanel;
    private CustomButton addNewRoomButton;

    private JLabel GearButtonLabel;
    private JLabel DownloadButtonLabel; // For chat history download

    // --- Invitation Panel Components ---
    private JPanel invitationPanel;
    private JTextArea invitationLabel;
    private JLabel acceptInvitationButton;
    private JLabel denyInvitationButton;

    // --- Action Buttons from Backend (merged or adapted) ---
    private CustomButton leaveRoomButton; // Retained, common action
    // downloadChatButton (CustomButton) from backend, can coexist with DownloadButtonLabel (icon) for now
    private CustomButton downloadChatButton;

    // --- References & State ---
    private MainFrame mainFrame;
    private ChatController chatController;
    private String currentUserName;
    private String activeRoomName;
    private Map<String, CustomButton> roomButtons; // For room tabs
    private Set<String> onlineUsers; // Backend's user list state

    // --- File Sharing State (Backend) ---
    private static final int MAX_CONCURRENT_UPLOADS = 3;
    private static final long FILE_UPLOAD_COOLDOWN_MS = 10000; // 10 seconds
    private int concurrentUploads = 0;
    private long lastFileUploadTime = 0;

    // --- Constants ---
    private static final String GEAR_LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/gear.png";
    private static final String DOWNLOAD_LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/downloading.png";
    private static final String DEFAULT_BACKGROUND_IMAGE_PATH = "/com/application/FrontEnd/images/BG_ChatRoom.jpg"; // Default, can be changed
    private static final String ACCEPT_LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/check.png";
    private static final String DENY_LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/close.png";
    private static final String CHAT_ICON_PATH = "/com/application/FrontEnd/images/chat_icon.png";

    // --- Constructor ---
    public ChatRoom(String initialUsername, String initialRoomName, MainFrame mainFrame, ChatController chatController) {
        this.mainFrame = mainFrame;
        this.chatController = chatController;
        this.currentUserName = initialUsername;
        this.activeRoomName = initialRoomName;

        this.roomButtons = new HashMap<>();
        this.onlineUsers = new HashSet<>();

        this.chatListModel = new DefaultListModel<>();
        this.chatList = new JList<>(this.chatListModel);
        // Initial user in list (will be refined by updateUserList calls)
        if (this.currentUserName != null) {
            this.onlineUsers.add(this.currentUserName);
        }

        setLayout(new BorderLayout());
        setOpaque(false);

        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        backgroundPanel = new BackgroundImagePanel(DEFAULT_BACKGROUND_IMAGE_PATH);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

        createMainPanel(); // Builds mainContentPanel
        layeredPane.add(mainContentPanel, JLayeredPane.PALETTE_LAYER);

        // Resize listener for layered pane
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeAndLayoutLayeredComponents();
            }
            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(ChatRoom.this::resizeAndLayoutLayeredComponents);
            }
        });

        // Initial UI setup based on initial room
        if (this.activeRoomName != null) {
            setChatScrollPaneTitle(this.activeRoomName);
            // The controller should provide the initial user list for the initial room.
            // This might happen in a subsequent call after connection establishment.
            // For now, initialize with current user only.
            updateUserList(new ArrayList<>(this.onlineUsers));
        }
        addEventListeners();
    }

    private void resizeAndLayoutLayeredComponents() {
        SwingUtilities.invokeLater(() -> {
            int layeredWidth = layeredPane.getWidth();
            int layeredHeight = layeredPane.getHeight();
            if (layeredWidth <= 0 || layeredHeight <= 0) return;
            if (backgroundPanel != null) backgroundPanel.setBounds(0, 0, layeredWidth, layeredHeight);
            if (mainContentPanel != null) mainContentPanel.setBounds(0, 0, layeredWidth, layeredHeight);
            layeredPane.revalidate();
            layeredPane.repaint();
        });
    }

    public void createMainPanel() {
        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setOpaque(false); // Main content is transparent to see background
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel rightColumnPanel = createRightPanel();
        JPanel leftColumnPanel = createLeftPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(leftColumnPanel);
        splitPane.setRightComponent(rightColumnPanel);
        splitPane.setDividerLocation(0.75); // Initial proportion for left panel
        splitPane.setResizeWeight(0.75);   // Left panel gets more resize weight
        splitPane.setOpaque(false);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setDividerSize(8);

        mainContentPanel.add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(0, 5));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5)); // Padding on right for divider

        // --- Room Tab Panel ---
        roomTabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3)); // Added small vgap
        roomTabPanel.setOpaque(false); // Panel itself is transparent
        roomTabPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5)); // Padding for content

        if (this.activeRoomName != null) {
            addRoomTabInternal(activeRoomName); // Add initial room tab
        }
        addNewRoomButton = new CustomButton("+", 40, 28, new Color(150, 100, 150)); // Slightly smaller
        addNewRoomButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        addNewRoomButton.setBorder(BorderFactory.createEmptyBorder(0,0,2,0)); // Align better
        roomTabPanel.add(addNewRoomButton);

        // Scrollable container for room tabs with custom background
        JScrollPane roomTabScrollPanel = new JScrollPane(roomTabPanel){
            private final int cornerRadius = 20;
            private final float fillAlpha = 120 / 255.0f; // Semi-transparent
            private final Color fillColor = new Color(80, 80, 80);
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillAlpha));
                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                g2d.dispose();
                super.paintComponent(g); // Paints children (roomTabPanel)
            }
        };
        roomTabScrollPanel.setOpaque(false);
        roomTabScrollPanel.getViewport().setOpaque(false);
        roomTabScrollPanel.setBorder(null); // No extra border for the scroll pane itself
        roomTabScrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        roomTabScrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollBar horizontalScrollBar = roomTabScrollPanel.getHorizontalScrollBar();
        horizontalScrollBar.setPreferredSize(new Dimension(0, 8)); // Slimmer scrollbar
        horizontalScrollBar.setOpaque(false);


        // --- Chat List Area ---
        chatList.setCellRenderer(new MessageCellRenderer());
        chatList.setBackground(new Color(60, 60, 60, 0)); // Transparent background for list
        chatList.setOpaque(false); // List itself is transparent to see panel bg
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chatList.setFocusable(false);
        chatList.setSelectionBackground(new Color(80, 80, 80, 180)); // Selection color
        chatList.setSelectionForeground(Color.WHITE);

        chatScrollPane = new JScrollPane(chatList);
        chatScrollPane.setOpaque(false);
        chatScrollPane.getViewport().setOpaque(false);
        chatScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Dynamic title border for chat scroll pane
        this.chatScrollPaneBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY),
                (this.activeRoomName != null ? this.activeRoomName : "Chat"),
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 16), // Slightly smaller title font
                new Color(220, 220, 220));
        this.chatScrollPane.setBorder(this.chatScrollPaneBorder);


        // --- Input Area ---
        chatTextField = new CustomTextField(300, 35); // Slightly taller
        chatTextField.setBackground(new Color(50, 50, 50));
        chatTextField.setForeground(Color.WHITE);
        chatTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatTextField.setBorder(BorderFactory.createEmptyBorder(5,10,5,10)); // Padding inside text field

        sendButton = new CustomButton("Send", 70, 33, new Color(100, 220, 100));
        sendButton.setForeground(Color.BLACK);
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 13));

        emojiButton = new CustomButton("\uD83D\uDE00", 40, 33, new Color(200, 200, 200));
        emojiButton.setForeground(Color.DARK_GRAY);
        emojiButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        fileSendButton = new CustomButton("+", 40, 33, new Color(200, 200, 200));
        fileSendButton.setForeground(Color.DARK_GRAY);
        fileSendButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        fileSendButton.setToolTipText("Share a file");


        JPanel inputControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        inputControlsPanel.setOpaque(false);
        inputControlsPanel.add(emojiButton);
        inputControlsPanel.add(sendButton);

        JPanel inputFieldPanel = new JPanel(new BorderLayout(5, 0));
        inputFieldPanel.setOpaque(false);
        inputFieldPanel.add(fileSendButton, BorderLayout.WEST);
        inputFieldPanel.add(chatTextField, BorderLayout.CENTER);
        inputFieldPanel.add(inputControlsPanel, BorderLayout.EAST);

        // Holder for input area with custom rounded background
        JPanel inputPanelHolder = new JPanel(new BorderLayout(0,5)){
            private final int cornerRadius = 20;
            private final Color fillColor = new Color(70, 70, 70); // Solid color for input field bg
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                g2d.dispose();
            }
        };
        inputPanelHolder.setOpaque(false);
        inputPanelHolder.setBorder(BorderFactory.createEmptyBorder(8,8,8,8)); // Padding around input elements
        inputPanelHolder.add(inputFieldPanel);


        // Chat area (list + input) with custom background
        JPanel chatAreaPanel = new JPanel(new BorderLayout(0, 8)) { // Increased gap
            private final int cornerRadius = 25;
            private final float fillAlpha = 120 / 255.0f;
            private final Color fillColor = new Color(60, 60, 60); // Main chat area background
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillAlpha));
                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                g2d.dispose();
            }
        };
        chatAreaPanel.setOpaque(false);
        chatAreaPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); // Padding for content
        chatAreaPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatAreaPanel.add(inputPanelHolder, BorderLayout.SOUTH);

        leftPanel.add(roomTabScrollPanel, BorderLayout.NORTH);
        leftPanel.add(chatAreaPanel, BorderLayout.CENTER);
        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0)); // Padding on left for divider

        // --- Top Icons Panel (Gear, Download) ---
        GearButtonLabel = createLogoLabel(GEAR_LOGO_IMAGE_PATH, 28);
        GearButtonLabel.setToolTipText("Settings");
        DownloadButtonLabel = createLogoLabel(DOWNLOAD_LOGO_IMAGE_PATH, 28);
        DownloadButtonLabel.setToolTipText("Download Chat History");

        JPanel topIconsPanel = new JPanel(new GridBagLayout()){
            private final int cornerRadius = 20;
            private final float fillAlpha = 120 / 255.0f;
            private final Color fillColor = new Color(80, 80, 80);
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillAlpha));
                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                g2d.dispose();
            }
        };
        topIconsPanel.setOpaque(false);
        topIconsPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 10, 2, 10);
        if (DownloadButtonLabel != null) topIconsPanel.add(DownloadButtonLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; topIconsPanel.add(Box.createHorizontalGlue(), gbc); // Spacer
        gbc.gridx = 2; gbc.weightx = 0.0;
        if (GearButtonLabel != null) topIconsPanel.add(GearButtonLabel, gbc);


        // --- User List Panel ---
        userEntriesPanel = new JPanel();
        userEntriesPanel.setLayout(new BoxLayout(userEntriesPanel, BoxLayout.Y_AXIS));
        userEntriesPanel.setOpaque(false); // Transparent, background from scrollpane viewport or holder

        JScrollPane userScrollPane = new JScrollPane(userEntriesPanel);
        userScrollPane.setOpaque(false);
        userScrollPane.getViewport().setOpaque(false);
        userScrollPane.getViewport().setBackground(new Color(60,60,60,0)); // Transparent viewport
        userScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        userScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        Border userListBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(), // No inner border
                "Online Users", TitledBorder.CENTER,
                TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 14),
                new Color(200, 200, 200));
        userScrollPane.setBorder(userListBorder);
        userScrollPane.setPreferredSize(new Dimension(150, 200)); // Default size


        // User list panel holder (with custom background)
        JPanel userDisplayPanelHolder = new JPanel(new BorderLayout()){
            private final int cornerRadius = 25;
            private final float fillAlpha = 120 / 255.0f;
            private final Color fillColor = new Color(80, 80, 80);
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillAlpha));
                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                g2d.dispose();
            }
        };
        userDisplayPanelHolder.setOpaque(false);
        userDisplayPanelHolder.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        userDisplayPanelHolder.add(userScrollPane, BorderLayout.CENTER);


        // --- Invitation Panel ---
        invitationPanel = new JPanel(new BorderLayout(0,8)){ // Increased gap
            private final int cornerRadius = 20;
            private final float fillAlpha = 150 / 255.0f; // Slightly more opaque
            private final Color fillColor = new Color(90, 90, 90); // Darker for emphasis
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillAlpha));
                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                g2d.dispose();
            }
        };
        invitationPanel.setOpaque(false);
        invitationPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        invitationLabel = new JTextArea("No active invitations.");
        invitationLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        invitationLabel.setForeground(Color.WHITE);
        invitationLabel.setLineWrap(true);
        invitationLabel.setWrapStyleWord(true);
        invitationLabel.setOpaque(false);
        invitationLabel.setEditable(false);
        invitationLabel.setFocusable(false);

        acceptInvitationButton = createLogoLabel(ACCEPT_LOGO_IMAGE_PATH, 24);
        acceptInvitationButton.setToolTipText("Accept Invitation");
        denyInvitationButton = createLogoLabel(DENY_LOGO_IMAGE_PATH, 24);
        denyInvitationButton.setToolTipText("Decline Invitation");

        JPanel invitationButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0)); // Centered buttons
        invitationButtons.setOpaque(false);
        invitationButtons.add(acceptInvitationButton);
        invitationButtons.add(denyInvitationButton);

        invitationPanel.add(invitationLabel, BorderLayout.CENTER);
        invitationPanel.add(invitationButtons, BorderLayout.SOUTH);
        invitationPanel.setVisible(false); // Initially hidden
        invitationPanel.setPreferredSize(new Dimension(150, 100)); // Default size


        // --- Action Buttons Panel (Leave, Download) ---
        leaveRoomButton = new CustomButton("Leave App", 120, 33, new Color(230, 90, 90));
        leaveRoomButton.setForeground(Color.WHITE);
        leaveRoomButton.setFont(new Font("SansSerif", Font.BOLD, 13));

        downloadChatButton = new CustomButton("DL Chat", 100, 33, new Color(80, 150, 220)); // Shorter text
        downloadChatButton.setForeground(Color.WHITE);
        downloadChatButton.setFont(new Font("SansSerif", Font.BOLD, 13));

        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        actionButtonsPanel.setOpaque(false);
        actionButtonsPanel.add(downloadChatButton);
        actionButtonsPanel.add(leaveRoomButton);

        // Container for user list and invitation panel
        JPanel centerContentPanel = new JPanel(new BorderLayout(0, 10));
        centerContentPanel.setOpaque(false);
        centerContentPanel.add(userDisplayPanelHolder, BorderLayout.CENTER);
        centerContentPanel.add(invitationPanel, BorderLayout.SOUTH);

        rightPanel.add(topIconsPanel, BorderLayout.NORTH);
        rightPanel.add(centerContentPanel, BorderLayout.CENTER);
        rightPanel.add(actionButtonsPanel, BorderLayout.SOUTH);
        return rightPanel;
    }

    private JLabel createLogoLabel(String imagePath, int size) {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setOpaque(false);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        try {
            URL imgUrl = getClass().getResource(imagePath);
            if (imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                Image scaledImage = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                label.setIcon(new ImageIcon(scaledImage));
            } else {
                throw new IOException("Resource not found: " + imagePath);
            }
        } catch (Exception e) {
            System.err.println("Error loading logo (" + imagePath + "): " + e.getMessage());
            label.setText("Icon"); // Fallback text
            label.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, size / 3)));
            label.setForeground(Color.LIGHT_GRAY);
        }
        return label;
    }

    private JLabel createSmallIconLabel(String iconPath, int size, String tooltip) {
        JLabel iconLabel = createLogoLabel(iconPath, size); // Leverage existing method
        if (tooltip != null) iconLabel.setToolTipText(tooltip);
        return iconLabel;
    }

    // --- User List Management ---
    private void addUserEntry(String actualUsername, String displayName, boolean addChatIcon) {
        JPanel entryPanel = new JPanel(new BorderLayout(5, 0));
        entryPanel.setOpaque(false);
        entryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        entryPanel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8)); // More padding

        JLabel nameLabel = new JLabel(displayName != null ? displayName : (actualUsername != null ? actualUsername : "Unknown"));
        nameLabel.setForeground(new Color(220, 220, 220));
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        entryPanel.add(nameLabel, BorderLayout.CENTER);

        if (addChatIcon && actualUsername != null && !Objects.equals(actualUsername, this.currentUserName)) {
            JLabel chatIconLabel = createSmallIconLabel(CHAT_ICON_PATH, 16, "Request private chat with " + actualUsername);
            if (chatIconLabel != null && chatIconLabel.getIcon() != null) { // Check if icon loaded
                chatIconLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (chatController != null) {
                            chatController.requestPrivateChat(actualUsername);
                        }
                    }
                });
                entryPanel.add(chatIconLabel, BorderLayout.EAST);
            }
        }
        userEntriesPanel.add(entryPanel);
    }

    public void updateUserList(List<String> usernames) {
        SwingUtilities.invokeLater(() -> {
            if (userEntriesPanel == null) return;
            userEntriesPanel.removeAll();

            // Add current user first
            addUserEntry(this.currentUserName, (this.currentUserName != null ? this.currentUserName : "You") + " (You)", false);

            if (usernames != null) {
                List<String> sortedUsernames = new ArrayList<>(usernames);
                sortedUsernames.sort(String.CASE_INSENSITIVE_ORDER); // Sort for consistent display
                for (String username : sortedUsernames) {
                    if (username != null && !Objects.equals(username, this.currentUserName)) {
                        addUserEntry(username, username, true);
                    }
                }
            }
            userEntriesPanel.revalidate();
            userEntriesPanel.repaint();
        });
    }

    public void addUserToList(String username) {
        if (username == null || username.trim().isEmpty()) return;
        SwingUtilities.invokeLater(() -> {
            if (onlineUsers.add(username)) {
                updateUserList(new ArrayList<>(onlineUsers));
            }
        });
    }

    public void removeUserFromList(String username) {
        if (username == null || username.trim().isEmpty()) return;
        SwingUtilities.invokeLater(() -> {
            if (onlineUsers.remove(username)) {
                updateUserList(new ArrayList<>(onlineUsers));
            }
        });
    }

    public void clearUserList() {
        SwingUtilities.invokeLater(() -> {
            onlineUsers.clear();
            // Add self back to the set if currentUserName is known, as updateUserList expects it for "You"
            if (this.currentUserName != null) {
                onlineUsers.add(this.currentUserName);
            }
            updateUserList(new ArrayList<>(onlineUsers)); // Will display only "(You)" if currentUserName exists
        });
    }


    // --- Message Display ---
    public void appendMessage(String sender, String message, String type) {
        SwingUtilities.invokeLater(() -> {
            ChatMessage msg = new ChatMessage(sender, message, type);
            chatListModel.addElement(msg);
            // Scroll to bottom
            int lastIndex = chatListModel.getSize() - 1;
            if (lastIndex >= 0) {
                chatList.ensureIndexIsVisible(lastIndex);
            }
        });
    }
    public void appendMessage(String sender, String message) { // Overload for backward compatibility or simple messages
        appendMessage(sender, message, "STANDARD");
    }


    public void displaySystemMessage(String message) {
        appendMessage("[System]", message, "SYSTEM");
    }

    // --- Room Management ---
    public void setChatScrollPaneTitle(String roomName) {
        SwingUtilities.invokeLater(() -> {
            if (this.chatScrollPaneBorder != null) {
                this.chatScrollPaneBorder.setTitle(roomName != null ? roomName : "Chat");
                if (this.chatScrollPane != null) this.chatScrollPane.repaint();
            }
        });
    }

    // This method updates the small "Room: Name" label if it exists,
    // and the chat scroll pane title.
    public void setActiveRoomName(String roomName) {
        this.activeRoomName = roomName;
        // If there's a specific label for room name display elsewhere (e.g. in right panel)
        // Example: if (this.roomNameDisplayLabel != null) this.roomNameDisplayLabel.setText("Room: " + roomName);
        setChatScrollPaneTitle(roomName); // This updates the chat area's TitledBorder
    }


    private void addEventListeners() {
        ActionListener sendAction = e -> {
            String messageText = chatTextField.getText().trim();
            if (!messageText.isEmpty() && chatController != null) {
                chatController.sendMessage(messageText);
                chatTextField.setText("");
            }
        };
        sendButton.addActionListener(sendAction);
        chatTextField.addActionListener(sendAction);

        leaveRoomButton.addActionListener(e -> { if (chatController != null) chatController.leaveRoom(); });
        downloadChatButton.addActionListener(e -> downloadChatHistory());

        if (DownloadButtonLabel != null) {
            DownloadButtonLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    downloadChatHistory();
                }
            });
        }
        if (GearButtonLabel != null) {
            GearButtonLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (mainFrame != null) mainFrame.switchToSettingRoom();
                }
            });
        }

        addNewRoomButton.addActionListener(e -> showAddRoomDialog());
        emojiButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Emoji feature coming soon!", "Info", JOptionPane.INFORMATION_MESSAGE));

        fileSendButton.addActionListener(e -> handleShareFileAction()); // Connect to file sharing logic
    }

    private void downloadChatHistory() {
        if (activeRoomName == null || chatController == null) {
            JOptionPane.showMessageDialog(this, "Cannot download. No active room or controller.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<ChatMessage> history = chatController.getChatHistory(activeRoomName);
        if (history == null || history.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chat history is empty for " + activeRoomName + ".", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // ... (rest of downloadChatHistory method, unchanged, seems fine)
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Chat History");
        fileChooser.setSelectedFile(new File(activeRoomName + "_chat_history.txt"));

        int userSelection = fileChooser.showSaveDialog(this.mainFrame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                writer.write("Chat History for Room: " + activeRoomName + "\n");
                writer.write("Downloaded by: " + currentUserName + "\n");
                writer.write("============================================\n\n");
                for (ChatMessage msg : history) {
                    String sender = msg.getSender() != null ? msg.getSender() : "[Unknown]";
                    String message = msg.getMessage() != null ? msg.getMessage() : "[Empty Message]";
                    writer.write(sender + ": " + message + "\n");
                }
                writer.flush();
                JOptionPane.showMessageDialog(this.mainFrame, "Chat history saved to:\n" + fileToSave.getAbsolutePath(), "Download Successful", JOptionPane.INFORMATION_MESSAGE);
                if (chatController != null) chatController.notifyChatDownloaded(activeRoomName, currentUserName);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this.mainFrame, "Error saving chat history: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void showAddRoomDialog() {
        // ... (showAddRoomDialog method, unchanged, seems fine)
        JTextField roomNameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JPanel dialogPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        dialogPanel.add(new JLabel("Room Name:"));
        dialogPanel.add(roomNameField);
        dialogPanel.add(new JLabel("Password (if any):"));
        dialogPanel.add(passwordField);
        SwingUtilities.invokeLater(roomNameField::requestFocusInWindow);

        int result = JOptionPane.showConfirmDialog(mainFrame, dialogPanel, "Join or Create Room", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String newRoomName = roomNameField.getText().trim();
            String newPassword = new String(passwordField.getPassword());
            java.util.Arrays.fill(passwordField.getPassword(), ' ');

            if (newRoomName.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Room Name cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            } else {
                if (chatController != null) chatController.joinOrSwitchToRoom(newRoomName, newPassword);
            }
        }
    }

    private void addRoomTabInternal(String roomName) {
        if (roomName == null || roomButtons.containsKey(roomName)) return;

        Random random = new Random(roomName.hashCode()); // Consistent color per room name
        CustomButton tabButton = new CustomButton(roomName, 100, 28,
                new Color(100 + random.nextInt(100), 100 + random.nextInt(100), 100 + random.nextInt(100)));
        tabButton.setForeground(Color.WHITE);
        tabButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tabButton.addActionListener(e -> {
            if (chatController != null) chatController.requestRoomSwitch(roomName);
        });
        roomButtons.put(roomName, tabButton);

        int addButtonIndex = -1;
        for (int i = 0; i < roomTabPanel.getComponentCount(); i++) {
            if (roomTabPanel.getComponent(i) == addNewRoomButton) {
                addButtonIndex = i;
                break;
            }
        }
        roomTabPanel.add(tabButton, (addButtonIndex != -1) ? addButtonIndex : roomTabPanel.getComponentCount());
        roomTabPanel.revalidate();
        roomTabPanel.repaint();
    }

    public void addRoomTab(String roomName) {
        SwingUtilities.invokeLater(() -> addRoomTabInternal(roomName));
    }

    // Update this method to accept the list of users for the new room from the controller
    public void updateUIForRoomSwitch(String newActiveRoom, List<String> usersInNewRoom) {
        SwingUtilities.invokeLater(() -> {
            this.activeRoomName = newActiveRoom;
            setActiveRoomName(newActiveRoom); // Updates title border and potentially other labels

            // Clear and reload chat history
            this.chatListModel.clear();
            if (chatController != null && newActiveRoom != null) {
                List<ChatMessage> history = chatController.getChatHistory(newActiveRoom);
                if (history != null) {
                    for (ChatMessage msg : history) this.chatListModel.addElement(msg);
                }
                if (!chatListModel.isEmpty()) {
                    chatList.ensureIndexIsVisible(chatListModel.getSize() - 1);
                }
            }

            // Update internal user set and then call frontend's updateUserList
            this.onlineUsers.clear();
            if (usersInNewRoom != null) {
                this.onlineUsers.addAll(usersInNewRoom);
            }
            // Ensure current user is in the set if not provided by controller for some reason (should be)
            if (this.currentUserName != null && !this.onlineUsers.contains(this.currentUserName)) {
                this.onlineUsers.add(this.currentUserName);
            }
            // Call frontend's method to refresh UI for user list
            // Pass the list from controller; updateUserList knows to handle currentUserName for "(You)"
            updateUserList(usersInNewRoom != null ? usersInNewRoom : new ArrayList<>());


            // Update tab button highlighting
            roomButtons.forEach((name, button) -> {
                if (button != null) {
                    button.setBorder(name.equals(newActiveRoom) ?
                            BorderFactory.createLineBorder(Color.CYAN, 2) :
                            UIManager.getBorder("Button.border"));
                }
            });
        });
    }

    public void reInitializeForNewSession(String username, String roomName, java.util.List<String> usersInRoom) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("[ChatRoom] Re-initializing for user: " + username + ", room: " + roomName);
            this.currentUserName = username != null ? username : "UnknownUser";
            // activeRoomName is set by updateUIForRoomSwitch

            clearAllRoomTabs(); // Clear old tabs

            if (roomName != null /*&& !roomName.equals("DefaultRoom")*/) { // Assuming "DefaultRoom" might not have a tab
                addRoomTabInternal(roomName); // Add current room tab first
            }

            // This will handle setting activeRoomName, titles, history, and user list
            updateUIForRoomSwitch(roomName, usersInRoom);

            // If other tabs are known (e.g., from a persistent list of joined rooms), add them back
            // For now, only the current active room's tab is restored by this re-init.

            mainContentPanel.revalidate(); // Revalidate main layout
            mainContentPanel.repaint();
        });
    }

    public void clearAllRoomTabs() {
        SwingUtilities.invokeLater(() -> {
            if (roomTabPanel == null || roomButtons == null || addNewRoomButton == null) return;
            roomTabPanel.removeAll();
            roomButtons.clear();
            roomTabPanel.add(addNewRoomButton); // Re-add the "+" button
            roomTabPanel.revalidate();
            roomTabPanel.repaint();
        });
    }


    // --- File Sharing Methods (Backend Integration) ---
    private void handleShareFileAction() {
        if (concurrentUploads >= MAX_CONCURRENT_UPLOADS) {
            JOptionPane.showMessageDialog(this, "Maximum concurrent uploads reached. Please wait.", "Upload Limit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFileUploadTime < FILE_UPLOAD_COOLDOWN_MS) {
            JOptionPane.showMessageDialog(this, "Please wait before uploading another file.", "Upload Cooldown", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select File to Share");
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (chatController != null) {
                if (chatController.initiateFileShare(selectedFile, activeRoomName)) {
                    concurrentUploads++;
                    lastFileUploadTime = System.currentTimeMillis();
                    // Display a temporary message or update UI to show upload in progress
                    displaySystemMessage("Sharing file: " + selectedFile.getName() + "...");
                } else {
                    // Controller might have already shown an error (e.g., file too large)
                }
            }
        }
    }

    public void fileShareAttemptFinished() {
        SwingUtilities.invokeLater(() -> {
            if (concurrentUploads > 0) {
                concurrentUploads--;
            }
            // Potentially update UI if there was a visual indicator for "uploads in progress"
            System.out.println("[UI] A file share attempt finished. Concurrent uploads now: " + concurrentUploads);
        });
    }

    // To be implemented: Display file offer in chat list
    // In ChatRoom.java
    public void displayFileShareOffer(MessageData offerData) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("[UI] Placeholder: Display file share offer for: " + offerData.originalFilename); // Direct field access
            appendMessage("[File Offer]",
                    "File: " + offerData.originalFilename + // Direct field access
                            " (" + formatFileSize(offerData.originalFileSize) + " bytes). Click to download (not implemented).", // Direct field access
                    "FILE_OFFER_PLACEHOLDER");
            // The actual implementation will add a special ChatMessage that the renderer can handle
            // to make it clickable and show a download icon.
        });
    }

    // Helper for formatting file size in ChatRoom (if needed, ChatController also has one)
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    // To be implemented: Handle click on a file offer to download
    public void initiateFileDownload(MessageData offerData) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("[UI] Placeholder: Initiate file download for: " + offerData.originalFilename);
            // This would typically involve:
            // 1. Showing a JFileChooser to select save location.
            // 2. Calling a method in ChatController: chatController.downloadSharedFile(offerData, savePath);
            JOptionPane.showMessageDialog(this, "File download for '" + offerData.originalFilename + "' is not yet implemented.", "Coming Soon", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // --- Private Chat Request Display (Frontend Integration) ---
    public void displayPrivateChatRequest(String fromUser, String proposedRoomName, String proposedPassword) {
        SwingUtilities.invokeLater(() -> {
            if (chatController == null || fromUser == null || proposedRoomName == null || proposedPassword == null) {
                System.err.println("[UI] Invalid private chat request params. Ignoring.");
                return;
            }
            String message = String.format("%s invites you to private chat '%s'. Accept?", fromUser, proposedRoomName);
            invitationLabel.setText(message);

            // Clear and re-add listeners to ensure correct context
            for (MouseListener ml : acceptInvitationButton.getMouseListeners()) acceptInvitationButton.removeMouseListener(ml);
            for (MouseListener ml : denyInvitationButton.getMouseListeners()) denyInvitationButton.removeMouseListener(ml);

            acceptInvitationButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    chatController.acceptPrivateChat(fromUser, proposedRoomName, proposedPassword);
                    invitationPanel.setVisible(false);
                }
            });
            denyInvitationButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    chatController.declinePrivateChat(fromUser, proposedRoomName);
                    invitationPanel.setVisible(false);
                }
            });
            invitationPanel.setVisible(true);
            if (mainFrame != null) { mainFrame.toFront(); mainFrame.requestFocus(); } // Bring window to front
        });
    }

    // --- Background Image Control ---
    public void setBackgroundImage(String newImagePath) {
        SwingUtilities.invokeLater(() -> {
            if (this.backgroundPanel != null) {
                this.backgroundPanel.setImage(newImagePath);
            }
        });
    }

    // --- Setters for Controller/MainFrame ---
    public void setChatController(ChatController controller) { this.chatController = controller; }
    public void setMainFrame(MainFrame frame) { this.mainFrame = frame; }


    // --- Inner Class for Background Image Panel ---
    private static class BackgroundImagePanel extends JPanel {
        private Image backgroundImage;
        private String imagePathUsed;
        private String errorMessage = null;

        public BackgroundImagePanel(String imagePath) {
            this.imagePathUsed = imagePath;
            loadImage(imagePath);
            setOpaque(true); // Essential for background to be drawn correctly
        }

        public void setImage(String newImagePath) {
            this.imagePathUsed = newImagePath;
            loadImage(newImagePath);
            repaint(); // Trigger repaint for new image or error display
        }

        private void loadImage(String imagePath) {
            if (imagePath == null || imagePath.trim().isEmpty()) {
                this.errorMessage = "Image path is null or empty.";
                this.backgroundImage = null;
                System.err.println("[BGPanel] " + this.errorMessage);
                return;
            }
            try {
                URL imgUrl = getClass().getResource(imagePath);
                if (imgUrl != null) {
                    this.backgroundImage = ImageIO.read(imgUrl);
                    if (this.backgroundImage == null) {
                        throw new IOException("ImageIO.read returned null for path: " + imagePath);
                    }
                    this.errorMessage = null; // Clear previous errors
                    System.out.println("[BGPanel] Loaded image: " + imagePath);
                } else {
                    throw new IOException("Resource not found: " + imagePath + ". Check classpath and path string.");
                }
            } catch (IOException e) {
                this.errorMessage = "Error loading background: " + e.getMessage();
                System.err.println("[BGPanel] " + this.errorMessage + " (Path: " + imagePath + ")");
                this.backgroundImage = null;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Clears the panel
            if (backgroundImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                int panelW = getWidth();
                int panelH = getHeight();
                int imgW = backgroundImage.getWidth(this);
                int imgH = backgroundImage.getHeight(this);

                if (imgW <= 0 || imgH <= 0) { // Invalid image dimensions
                    g2d.dispose();
                    drawErrorFallback(g, panelW, panelH);
                    return;
                }

                // Cover scaling logic (image covers panel, maintains aspect ratio, crops if needed)
                double imgAspect = (double) imgW / imgH;
                double panelAspect = (double) panelW / panelH;
                int drawW, drawH, drawX, drawY;

                if (panelAspect > imgAspect) { // Panel is wider than image aspect ratio
                    drawW = panelW;
                    drawH = (int) (panelW / imgAspect);
                    drawX = 0;
                    drawY = (panelH - drawH) / 2; // Center vertically
                } else { // Panel is taller or same aspect ratio
                    drawH = panelH;
                    drawW = (int) (panelH * imgAspect);
                    drawX = (panelW - drawW) / 2; // Center horizontally
                    drawY = 0;
                }
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(backgroundImage, drawX, drawY, drawW, drawH, this);
                g2d.dispose();
            } else {
                drawErrorFallback(g, getWidth(), getHeight());
            }
        }

        private void drawErrorFallback(Graphics g, int w, int h) {
            g.setColor(new Color(30, 30, 40)); // Dark fallback background
            g.fillRect(0, 0, w, h);
            g.setColor(Color.YELLOW);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fm = g.getFontMetrics();
            String text = "BG Load Error: " + (errorMessage != null ? errorMessage : "Image not available");
            int msgWidth = fm.stringWidth(text);
            g.drawString(text, Math.max(5, (w - msgWidth) / 2), h / 2 - fm.getHeight() / 2 + fm.getAscent());

            String pathText = "Path: " + (imagePathUsed != null ? imagePathUsed : "undefined");
            msgWidth = fm.stringWidth(pathText);
            g.drawString(pathText, Math.max(5, (w - msgWidth) / 2), h / 2 + fm.getHeight() / 2 + fm.getAscent());
        }
    }
}