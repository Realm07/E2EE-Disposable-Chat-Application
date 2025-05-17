package com.application.FrontEnd;

// Swing & AWT Imports
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
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

    private JScrollPane roomTabScrollPanel;

    // --- Invitation Panel Components ---
    private JPanel invitationPanel;
    private JTextArea invitationLabel;
    private JLabel acceptInvitationButton;
    private JLabel denyInvitationButton;

    // --- Action Buttons from Backend (merged or adapted) ---
    private CustomButton leaveRoomButton; // Retained, common action
    // downloadChatButton (CustomButton) from backend, can coexist with DownloadButtonLabel (icon) for now
   

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
        splitPane.setRightComponent(rightColumnPanel);
        splitPane.setLeftComponent(leftColumnPanel);
        splitPane.setDividerLocation(800);
        splitPane.setResizeWeight(1);
        splitPane.setOpaque(false);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setDividerSize(0);

        mainContentPanel.add(splitPane, BorderLayout.CENTER);
    }

    public void refreshChatMessages() {
        SwingUtilities.invokeLater(() -> { // Ensure UI updates happen on the EDT
            if (chatList != null) {
                chatList.repaint(); // This tells the JList to redraw all its items
            }
        });
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(0, 5));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5)); // Padding on right for divider

        // --- Room Tab Panel ---
        roomTabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0)); // Added small vgap
        roomTabPanel.setOpaque(false); // Panel itself is transparent
        roomTabPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); // Padding for content

        if (this.activeRoomName != null) {
            addRoomTabInternal(activeRoomName); // Add initial room tab
        }
        addNewRoomButton = new CustomButton("+", 50, 30, new Color(150, 100, 150)); // Slightly smaller
        addNewRoomButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        addNewRoomButton.setBorder(BorderFactory.createEmptyBorder(0,0,5,0)); // Align better
        roomTabPanel.add(addNewRoomButton);

        // Scrollable container for room tabs with custom background
        roomTabScrollPanel = new JScrollPane(roomTabPanel){
            private int cornerRadius = 25;
            private float fillAlpha = 120 / 255.0f;
            private Color fillColor = new Color(102, 102, 102);
            // private float borderAlpha = 200 / 255.0f;
            // private Color borderColor = new Color(150, 150, 150);
            // private int borderThickness = 1;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillAlpha));
                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                // if (borderThickness > 0) {
                //     g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, borderAlpha));
                //     g2d.setColor(borderColor);
                //     g2d.setStroke(new BasicStroke(borderThickness));
                //     int B = borderThickness;
                //     g2d.drawRoundRect(B/2, B/2, getWidth() - B, getHeight() - B, cornerRadius - B, cornerRadius - B);
                // }
                g2d.dispose();
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
        chatList.setCellRenderer(new MessageCellRenderer(this.mainFrame));
        chatList.setBackground(new Color(60, 60, 60, 0)); // Transparent background for list
        chatList.setOpaque(false); // List itself is transparent to see panel bg
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chatList.setFocusable(false);
        chatList.setSelectionBackground(null);
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
        chatTextField = new CustomTextField(300, 30);
        chatTextField.setBackground(new Color(70, 70, 70));
        chatTextField.setForeground(Color.WHITE);
        chatTextField.setBorder(null);

        sendButton = new CustomButton("Send", 70, 33, new Color(102, 255, 102));
        sendButton.setForeground(Color.BLACK);
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 13));

        emojiButton = new CustomButton("\uD83D\uDE00", 40, 33, new Color(200, 200, 200));
        emojiButton.setForeground(Color.DARK_GRAY);
        emojiButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        fileSendButton = new CustomButton("+", 30, 30, new Color(255, 255, 255));
        fileSendButton.setForeground(Color.BLACK);
        fileSendButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        fileSendButton.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
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

        JPanel inputPanelHolder = new JPanel(new BorderLayout(0,5)){
            private int cornerRadius = 25;
            private float fillAlpha = 1f;
            private Color fillColor = new Color(70, 70, 70);
            private float borderAlpha = 1f;
            private Color borderColor = new Color(150,150,150);
            private int borderThickness = 1;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillAlpha));
                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                if (borderThickness > 0) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, borderAlpha));
                    g2d.setColor(borderColor);
                    g2d.setStroke(new BasicStroke(borderThickness));
                    int B = borderThickness;
                    g2d.drawRoundRect(B/2, B/2, getWidth() - B, getHeight() - B, cornerRadius - B, cornerRadius - B);
                }
                g2d.dispose();
            }
        };
        inputPanelHolder.setOpaque(false);
        inputPanelHolder.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        inputPanelHolder.add(inputFieldPanel);

        // Holder for input area with custom rounded background
        // JPanel inputPanelHolder = new JPanel(new BorderLayout(0,5)){
        //     private int cornerRadius = 25;
        //     private float fillAlpha = 120 / 255.0f;
        //     private Color fillColor = new Color(102, 102, 102);
        //     private float borderAlpha = 200 / 255.0f;
        //     private Color borderColor = new Color(150, 150, 150);
        //     private int borderThickness = 1;
        //     @Override
        //     protected void paintComponent(Graphics g) {
        //         super.paintComponent(g);
        //         Graphics2D g2d = (Graphics2D) g.create();
        //         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //         g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillAlpha));
        //         g2d.setColor(fillColor);
        //         g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        //         if (borderThickness > 0) {
        //             g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, borderAlpha));
        //             g2d.setColor(borderColor);
        //             g2d.setStroke(new BasicStroke(borderThickness));
        //             int B = borderThickness;
        //             g2d.drawRoundRect(B/2, B/2, getWidth() - B, getHeight() - B, cornerRadius - B, cornerRadius - B);
        //         }
        //         g2d.dispose();
        //     }
        // };
        // inputPanelHolder.setOpaque(false);
        // inputPanelHolder.setBorder(BorderFactory.createEmptyBorder(8,8,8,8)); // Padding around input elements
        // inputPanelHolder.add(inputFieldPanel);


        JPanel chatAreaPanel = new JPanel(new BorderLayout(0, 8)) { // Increased gap
            private int cornerRadius = 25;
            private float fillAlpha = 120 / 255.0f;
            private Color fillColor = new Color(102, 102, 102);
            // private float borderAlpha = 200 / 255.0f;
            // private Color borderColor = new Color(150, 150, 150);
            // private int borderThickness = 1;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillAlpha));
                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                // if (borderThickness > 0) {
                //     g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, borderAlpha));
                //     g2d.setColor(borderColor);
                //     g2d.setStroke(new BasicStroke(borderThickness));
                //     int B = borderThickness;
                //     g2d.drawRoundRect(B/2, B/2, getWidth() - B, getHeight() - B, cornerRadius - B, cornerRadius - B);
                // }
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
        JPanel rightPanel = new JPanel(new BorderLayout(0, 5));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 7, 0)); // Padding on left for divider

        // --- Top Icons Panel (Gear, Download) ---
        GearButtonLabel = createLogoLabel(GEAR_LOGO_IMAGE_PATH, 28);
        GearButtonLabel.setToolTipText("Settings");
        DownloadButtonLabel = createLogoLabel(DOWNLOAD_LOGO_IMAGE_PATH, 28);
        DownloadButtonLabel.setToolTipText("Download Chat History");

        // JPanel topIconsPanel = new JPanel(new GridBagLayout()){
        JPanel leftTopButtonPanel = new JPanel(new GridBagLayout()){
            private int cornerRadius = 25;
            private float fillAlpha = 120 / 255.0f;
            private Color fillColor = new Color(102, 102, 102);
            private float borderAlpha = 200 / 255.0f;
            private Color borderColor = new Color(150, 150, 150);
            private int borderThickness = 1;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillAlpha));
                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                if (borderThickness > 0) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, borderAlpha));
                    g2d.setColor(borderColor);
                    g2d.setStroke(new BasicStroke(borderThickness));
                    int B = borderThickness;
                    g2d.drawRoundRect(B/2, B/2, getWidth() - B, getHeight() - B, cornerRadius - B, cornerRadius - B);
                }
                g2d.dispose();
            }
        };
        leftTopButtonPanel.setOpaque(false);
        leftTopButtonPanel.setBorder(BorderFactory.createEmptyBorder(4,0,9,0));
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0; 
        gbc.gridy = 0; 
        gbc.anchor = GridBagConstraints.WEST; 
        gbc.weightx = 0.0; 
        gbc.fill = GridBagConstraints.NONE; 
        gbc.insets = new Insets(5, 30, 5, 5);

        if (DownloadButtonLabel != null) leftTopButtonPanel.add(DownloadButtonLabel, gbc);
        gbc.gridx = 1; 
        gbc.weightx = 1.0; 
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        gbc.insets = new Insets(5, 0, 5, 0);

        leftTopButtonPanel.add(Box.createHorizontalGlue(), gbc);
        gbc.gridx = 2; 
        gbc.anchor = GridBagConstraints.EAST; 
        gbc.weightx = 0.0; 
        gbc.fill = GridBagConstraints.NONE; 
        gbc.insets = new Insets(5, 5, 5, 30);
        if (GearButtonLabel != null) leftTopButtonPanel.add(GearButtonLabel, gbc);


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
            private int cornerRadius = 25;
            private float fillAlpha = 120 / 255.0f;
            private Color fillColor = new Color(102, 102, 102);
            private float borderAlpha = 200 / 255.0f;
            private Color borderColor = new Color(150, 150, 150);
            private int borderThickness = 1;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillAlpha));
                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                if (borderThickness > 0) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, borderAlpha));
                    g2d.setColor(borderColor);
                    g2d.setStroke(new BasicStroke(borderThickness));
                    int B = borderThickness;
                    g2d.drawRoundRect(B/2, B/2, getWidth() - B, getHeight() - B, cornerRadius - B, cornerRadius - B);
                }
                g2d.dispose();
            }
        };
        userDisplayPanelHolder.setOpaque(false);
        userDisplayPanelHolder.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        userDisplayPanelHolder.add(userScrollPane, BorderLayout.CENTER);


        // --- Invitation Panel ---
        invitationPanel = new JPanel(new BorderLayout(0,8)){ // Increased gap
            private int cornerRadius = 25;
            private float fillAlpha = 120 / 255.0f;
            private Color fillColor = new Color(102, 102, 102);
            private float borderAlpha = 200 / 255.0f;
            private Color borderColor = new Color(150, 150, 150);
            private int borderThickness = 1;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillAlpha));
                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                if (borderThickness > 0) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, borderAlpha));
                    g2d.setColor(borderColor);
                    g2d.setStroke(new BasicStroke(borderThickness));
                    int B = borderThickness;
                    g2d.drawRoundRect(B/2, B/2, getWidth() - B, getHeight() - B, cornerRadius - B, cornerRadius - B);
                }
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
        leaveRoomButton = new CustomButton("Leave Room", 120, 40, new Color(255, 77, 77));
        leaveRoomButton.setForeground(Color.WHITE);
        leaveRoomButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        JPanel leaveButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        leaveButtonPanel.setOpaque(false);
        leaveButtonPanel.add(leaveRoomButton);
        

        // Container for user list and invitation panel
        JPanel centerContentPanel = new JPanel(new BorderLayout(0, 10));
        centerContentPanel.setOpaque(false);
        centerContentPanel.add(userDisplayPanelHolder, BorderLayout.CENTER);
        centerContentPanel.add(invitationPanel, BorderLayout.SOUTH);

        rightPanel.add(leftTopButtonPanel, BorderLayout.NORTH);
        rightPanel.add(centerContentPanel, BorderLayout.CENTER);
        rightPanel.add(leaveButtonPanel, BorderLayout.SOUTH);
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
        // downloadChatButton.addActionListener(e -> downloadChatHistory());

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
        System.out.println("[ChatRoom DEBUG] addRoomTabInternal called for: " + roomName); // DEBUG
        if (roomName == null || roomName.trim().isEmpty()) { // Check for empty room name
            System.out.println("[ChatRoom DEBUG] Room name is null or empty. Tab not added.");
            return;
        }
        if (roomButtons.containsKey(roomName)) {
            System.out.println("[ChatRoom DEBUG] Tab for " + roomName + " already exists.");
            return;
        }

        Random random = new Random(roomName.hashCode());
        CustomButton tabButton = new CustomButton(roomName, 100, 28,
                new Color(100 + random.nextInt(100), 100 + random.nextInt(100), 100 + random.nextInt(100)));
        tabButton.setForeground(Color.WHITE);
        tabButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        final String rn = roomName; // Effectively final for lambda
        tabButton.addActionListener(e -> {
            System.out.println("[ChatRoom DEBUG] Tab clicked: " + rn); // DEBUG
            if (chatController != null) chatController.requestRoomSwitch(rn);
        });
        roomButtons.put(roomName, tabButton);
        System.out.println("[ChatRoom DEBUG] Created button for " + roomName + ". Text: '" + tabButton.getText() + "'"); // DEBUG

        int addButtonIndex = -1;
        if (roomTabPanel != null && addNewRoomButton != null) { // Ensure panel and button exist
            for (int i = 0; i < roomTabPanel.getComponentCount(); i++) {
                if (roomTabPanel.getComponent(i) == addNewRoomButton) {
                    addButtonIndex = i;
                    break;
                }
            }
            System.out.println("[ChatRoom DEBUG] Add new room button index: " + addButtonIndex); // DEBUG
            roomTabPanel.add(tabButton, (addButtonIndex != -1) ? addButtonIndex : roomTabPanel.getComponentCount());
            System.out.println("[ChatRoom DEBUG] Added tabButton to roomTabPanel. Component count: " + roomTabPanel.getComponentCount()); // DEBUG
            roomTabPanel.revalidate();
            roomTabPanel.repaint();
        } else {
            System.err.println("[ChatRoom DEBUG] roomTabPanel or addNewRoomButton is null in addRoomTabInternal!");
        }
    }

    public void addRoomTab(String roomName) {
        System.out.println("[ChatRoom|addRoomTab] Public API called for room: '" + roomName + "'");
        // This is the public entry point, so it's good practice to ensure UI work happens on EDT.
        SwingUtilities.invokeLater(() -> addRoomTabInternal(roomName));
    }

    // Update this method to accept the list of users for the new room from the controller
    // In your NEW ChatRoom.java's updateUIForRoomSwitch method
    public void updateUIForRoomSwitch(String newActiveRoom, List<String> usersInNewRoom) {
    // This method now assumes it's already on the EDT
        System.out.println("[ChatRoom|updateUIForRoomSwitch] Called for room: '" + newActiveRoom + "'. Current activeRoomName (before set): '" + this.activeRoomName + "'");
        this.activeRoomName = newActiveRoom;
        setChatScrollPaneTitle(newActiveRoom);

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
        updateUserList(usersInNewRoom != null ? usersInNewRoom : new ArrayList<>());

        System.out.println("[ChatRoom|updateUIForRoomSwitch] Updating tab highlights. Target active: '" + newActiveRoom + "'. Buttons in map: " + roomButtons.keySet());
        final String finalActiveRoom = newActiveRoom;
        roomButtons.forEach((name, button) -> {
            if (button != null) {
                boolean isActive = Objects.equals(name, finalActiveRoom);
                System.out.println("[ChatRoom|updateUIForRoomSwitch]  Processing tab: '" + name + "'. Is active? " + isActive + ". Button text: '" + button.getText() + "'");
                if (isActive) {
                    button.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
                    System.out.println("[ChatRoom|updateUIForRoomSwitch]   Applied ACTIVE border to '" + name + "'");
                } else {
                    Border defaultBorder = UIManager.getBorder("Button.border");
                    button.setBorder(defaultBorder != null ? defaultBorder : BorderFactory.createEmptyBorder(2, 2, 2, 2));
                    System.out.println("[ChatRoom|updateUIForRoomSwitch]   Applied " + (defaultBorder != null ? "default L&F" : "fallback empty") + " border to '" + name + "'");
                }
            } else {
                System.err.println("[ChatRoom|updateUIForRoomSwitch] Null button found in roomButtons map for key: " + name);
            }
        });

        if (roomTabPanel != null) {
            roomTabPanel.revalidate();
            roomTabPanel.repaint();
        }
        if (roomTabScrollPanel != null) {
            roomTabScrollPanel.revalidate();
            roomTabScrollPanel.repaint();
            System.out.println("[ChatRoom|updateUIForRoomSwitch] Revalidated roomTabPanel & roomTabScrollPanel.");
        }
        System.out.println("[ChatRoom|updateUIForRoomSwitch] Finished for room: '" + newActiveRoom + "'.");
    }

    public void reInitializeForNewSession(String username, String sessionInitialRoomName, java.util.List<String> usersInRoom) {
        SwingUtilities.invokeLater(() -> { // Outer invokeLater ensures all of this block is on EDT
            System.out.println("[ChatRoom|reInitialize] Called. User: '" + username + "', Session Initial Room: '" + sessionInitialRoomName + "'");
            this.currentUserName = username != null ? username : "UnknownUser";

            System.out.println("[ChatRoom|reInitialize] Clearing all room tabs. Current buttons before clear: " + roomButtons.keySet());
            clearAllRoomTabs(); // Now calls the direct version
            System.out.println("[ChatRoom|reInitialize] Room tabs cleared. Current buttons after clear: " + roomButtons.keySet());

            if (sessionInitialRoomName != null && !sessionInitialRoomName.trim().isEmpty()) {
                System.out.println("[ChatRoom|reInitialize] Session Initial Room ('" + sessionInitialRoomName + "') is valid. Adding tab for it.");
                addRoomTabInternal(sessionInitialRoomName); // Calls the direct version
            } else {
                System.out.println("[ChatRoom|reInitialize] Session Initial Room is null or empty. No initial tab will be added by reInitialize.");
            }
            // At this point, roomButtons map should correctly have sessionInitialRoomName if it was valid.

            System.out.println("[ChatRoom|reInitialize] Calling updateUIForRoomSwitch for Session Initial Room: '" + sessionInitialRoomName + "'");
            updateUIForRoomSwitch(sessionInitialRoomName, usersInRoom); // Now calls the direct version

            System.out.println("[ChatRoom|reInitialize] After updateUIForRoomSwitch. Current activeRoomName: '" + this.activeRoomName + "'. Buttons in map: " + roomButtons.keySet());
            if (sessionInitialRoomName != null && roomButtons.containsKey(sessionInitialRoomName)) {
                CustomButton btn = roomButtons.get(sessionInitialRoomName);
                if (btn != null) {
                    System.out.println("[ChatRoom|reInitialize] Initial room tab ('" + btn.getText() + "') border: " + (btn.getBorder() != null ? btn.getBorder().getClass().getSimpleName() : "null") +
                                    ", Visible: " + btn.isVisible() + ", Parent: " + (btn.getParent() != null ? btn.getParent().getClass().getSimpleName() : "null"));
                }
            } else if (sessionInitialRoomName != null) {
                System.err.println("[ChatRoom|reInitialize] CRITICAL: Tab for session initial room '" + sessionInitialRoomName + "' NOT FOUND in roomButtons map after all setup!");
            }
            System.out.println("[ChatRoom|reInitialize] Finished.");
        });
    }

    public void clearAllRoomTabs() {
        // This method now assumes it's already on the EDT
        System.out.println("[ChatRoom|clearAllRoomTabs] Called.");
        if (roomTabPanel == null || roomButtons == null || addNewRoomButton == null) {
            System.err.println("[ChatRoom|clearAllRoomTabs] Cannot clear: roomTabPanel, roomButtons, or addNewRoomButton is null.");
            return;
        }
        roomTabPanel.removeAll();
        roomButtons.clear(); // Clears the map directly
        roomTabPanel.add(addNewRoomButton);
        System.out.println("[ChatRoom|clearAllRoomTabs] Cleared. Component count in roomTabPanel: " + roomTabPanel.getComponentCount());
        roomTabPanel.revalidate();
        roomTabPanel.repaint();
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