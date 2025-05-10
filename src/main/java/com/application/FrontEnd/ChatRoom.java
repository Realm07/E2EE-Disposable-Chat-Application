package com.application.FrontEnd;

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
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects; // Import Objects for Objects.equals
import java.util.Random;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.awt.event.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import com.application.Backend.ChatController;
import com.application.FrontEnd.components.MessageCellRenderer.ChatMessage;
import com.application.FrontEnd.components.*;

public class ChatRoom extends JPanel {

    // --- UI Components ---
    private CustomButton sendButton;
    private CustomButton leaveRoomButton;
    private CustomTextField chatTextField;
    // private CustomLabel roomNameLabel; // Replaced by chatScrollPaneBorder title
    // private CustomTextArea userNameArea; // Replaced by userEntriesPanel
    private JPanel userEntriesPanel; // For user list with icons
    private JPanel roomTabPanel;
    private CustomButton addNewRoomButton;
    private CustomButton emojiButton;
    private CustomButton fileSendButton;

    private JPanel mainContentPanel;
    private JLayeredPane layeredPane;
    private BackgroundImagePanel backgroundPanel;


    private JList<ChatMessage> chatList;
    private DefaultListModel<ChatMessage> chatListModel;
    private JScrollPane chatScrollPane;
    private TitledBorder chatScrollPaneBorder;

    // --- Invitation Panel Components (now class members) ---
    private JPanel invitationPanel;
    private JTextArea invitationLabel;
    private JLabel acceptInvitationButton;
    private JLabel denyInvitationButton;

    private static final String GEAR_LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/gear.png";
    private static final String DOWNLOAD_LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/downloading.png";
    private static final String BACKGROUND_IMAGE_PATH = "/com/application/FrontEnd/images/BG_LoginPage.jpg";
    private static final String ACCEPT_LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/check.png";
    private static final String DENY_LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/close.png";
    private static final String CHAT_ICON_PATH = "/com/application/FrontEnd/images/chat_icon.png"; // Icon for private chat


    // --- References & State ---
    private MainFrame mainFrame;
    private ChatController chatController;
    private String currentUserName;
    private String activeRoomName; // UI's view of the active room
    private Map<String, CustomButton> roomButtons; // Map room name to its tab button
    private JLabel GearButtonLabel;
    private JLabel DownloadButtonLabel;

    // --- Constructor ---
    public ChatRoom(String initialUsername, String initialRoomName, MainFrame mainFrame, ChatController chatController) {
        this.mainFrame = mainFrame;
        this.chatController = chatController;
        this.currentUserName = initialUsername;
        this.activeRoomName = initialRoomName; // Initial room view

        this.roomButtons = new HashMap<>();

        // Initialize chat list model and component
        this.chatListModel = new DefaultListModel<>();
        this.chatList = new JList<>(this.chatListModel);

        setLayout(new BorderLayout());

        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        backgroundPanel = new BackgroundImagePanel(BACKGROUND_IMAGE_PATH);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

        createMainPanel();
        layeredPane.add(mainContentPanel, JLayeredPane.PALETTE_LAYER);

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeAndLayoutLayeredComponents();
            }
            @Override
            public void componentShown(ComponentEvent e) {
                // Initial positioning after UI is shown
                SwingUtilities.invokeLater(ChatRoom.this::resizeAndLayoutLayeredComponents);
            }
        });

        addEventListeners(); // Add listeners AFTER all components are created
    }

    public void createMainPanel(){
        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setOpaque(false);
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chatList.setCellRenderer(new MessageCellRenderer()); // Ensure Renderer exists
        chatList.setBackground(new Color(60, 60, 60));
        chatList.setOpaque(true);
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        Color defaultTextColor = Color.WHITE;
        Color listAreaBackground = new Color(60, 60, 60, 180);
        chatList.setSelectionBackground(null);
        chatList.setSelectionBackground(listAreaBackground); // Make selection BG same as normal BG
        chatList.setSelectionForeground(defaultTextColor);

        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chatList.setFocusable(false);
        chatList.setOpaque(false);

        // --- Right Panel (User List & Room Info) ---
        JPanel userDisplayPanel = new JPanel();
        userDisplayPanel.setLayout(new BoxLayout(userDisplayPanel, BoxLayout.Y_AXIS));
        userDisplayPanel.setOpaque(false);

        // --- User List Panel (replaces userNameArea) ---
        userEntriesPanel = new JPanel();
        userEntriesPanel.setLayout(new BoxLayout(userEntriesPanel, BoxLayout.Y_AXIS));
        userEntriesPanel.setOpaque(false);
        userEntriesPanel.setBackground(new Color(60, 60, 60)); // Similar to old CustomTextArea background

        // Add current user. This list will be primarily managed by updateUserList.
        addUserEntry(this.currentUserName, (this.currentUserName != null ? this.currentUserName : "You") + " (You)", false);


        JScrollPane userScrollPane = new JScrollPane(userEntriesPanel);
        userScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        userScrollPane.setOpaque(false);
        userScrollPane.getViewport().setOpaque(false);
        userScrollPane.getViewport().setBackground(new Color(60,60,60,180)); // Ensure viewport background
        userScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        userScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        Border userListBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY),
            "Online Users", TitledBorder.CENTER,
            TitledBorder.TOP, new Font("SansSerif",
            Font.BOLD, 20),
            new Color(180, 180, 180));
        userScrollPane.setBorder(userListBorder);

        userDisplayPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        userDisplayPanel.add(userScrollPane);

        JPanel userDisplayPanelHolder = new JPanel(new BorderLayout()){
            private int cornerRadius = 25;
            private float fillAlpha = 120 / 255.0f;
            private Color fillColor = new Color(102, 102, 102);
            private float borderAlpha = 200 / 255.0f;
            private Color borderColor = new Color(150, 150, 150);
            private int borderThickness = 1;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // Important for opaque false if parent needs to draw through
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
        userDisplayPanelHolder.setOpaque(false); // Make holder transparent so its paintComponent is primary
        userDisplayPanelHolder.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        userDisplayPanelHolder.add(userDisplayPanel);

        leaveRoomButton = new CustomButton("Leave App", 100, 30, new Color(255, 77, 77));
        leaveRoomButton.setForeground(Color.WHITE);
        JPanel leaveButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        leaveButtonPanel.setOpaque(false);
        leaveButtonPanel.add(leaveRoomButton);

        GearButtonLabel = createLogoLabel(GEAR_LOGO_IMAGE_PATH, 30);
        DownloadButtonLabel = createLogoLabel(DOWNLOAD_LOGO_IMAGE_PATH, 30);
        if (GearButtonLabel != null) { // Added null check for safety, though createLogoLabel should return non-null
            GearButtonLabel.addMouseListener(new MouseAdapter(){
                @Override
                public void mouseClicked(MouseEvent e){
                    mainFrame.switchToSettingRoom();
                }
            });
        }


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

        //Chat room invitation panel (now uses class members)
        invitationPanel = new JPanel(new BorderLayout(0,10)){
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
        invitationPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5)); // Add some padding

        JPanel invitationButtonPanel = new JPanel(new GridBagLayout());
        invitationButtonPanel.setOpaque(false);

        invitationLabel = new JTextArea("No active invitations."); // Initial text
        invitationLabel.setFont(new Font("SansSerif", Font.PLAIN, 14)); // Slightly smaller font
        invitationLabel.setForeground(Color.WHITE);
        invitationLabel.setLineWrap(true);
        invitationLabel.setWrapStyleWord(true);
        invitationLabel.setBorder(null);
        invitationLabel.setOpaque(false);
        invitationLabel.setEditable(false);
        invitationLabel.setFocusable(false);

        acceptInvitationButton = createLogoLabel(ACCEPT_LOGO_IMAGE_PATH, 25);
        denyInvitationButton = createLogoLabel(DENY_LOGO_IMAGE_PATH, 25);

        GridBagConstraints invGbc = new GridBagConstraints();

        invGbc.gridx = 0; 
        invGbc.gridy = 0; 
        invGbc.anchor = GridBagConstraints.WEST; 
        invGbc.weightx = 0.0; 
        invGbc.fill = GridBagConstraints.NONE; 
        invGbc.insets = new Insets(0, 0, 0, 5);
        if (acceptInvitationButton != null) invitationButtonPanel.add(acceptInvitationButton, invGbc);

        invGbc.gridx = 1; 
        invGbc.weightx = 1.0; 
        invGbc.fill = GridBagConstraints.HORIZONTAL; 
        invGbc.insets = new Insets(0,0,0,0);
        invitationButtonPanel.add(Box.createHorizontalGlue(), invGbc);

        invGbc.gridx = 2; 
        invGbc.anchor = GridBagConstraints.EAST; 
        invGbc.weightx = 0.0; 
        invGbc.fill = GridBagConstraints.NONE; 
        invGbc.insets = new Insets(0, 5, 0, 0);
        if (denyInvitationButton != null) invitationButtonPanel.add(denyInvitationButton, invGbc);

        invitationPanel.add(invitationLabel, BorderLayout.CENTER);
        invitationPanel.add(invitationButtonPanel, BorderLayout.SOUTH);
        invitationPanel.setVisible(false);

        JPanel rightCenterContentPanel = new JPanel(new BorderLayout(0,15));
        rightCenterContentPanel.setOpaque(false);
        rightCenterContentPanel.add(userDisplayPanelHolder, BorderLayout.CENTER);
        rightCenterContentPanel.add(invitationPanel, BorderLayout.SOUTH);

        JPanel rightColumnPanel = new JPanel();
        rightColumnPanel.setLayout(new BorderLayout(0, 10));
        rightColumnPanel.setOpaque(false);
        rightColumnPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        rightColumnPanel.add(leftTopButtonPanel, BorderLayout.NORTH);
        rightColumnPanel.add(rightCenterContentPanel, BorderLayout.CENTER);
        rightColumnPanel.add(leaveButtonPanel, BorderLayout.SOUTH);

        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout(0, 5));
        chatPanel.setOpaque(false);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        this.chatScrollPane = new JScrollPane(chatList);
        chatScrollPane.setOpaque(false);
        chatScrollPane.getViewport().setOpaque(false);
        chatScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.chatScrollPaneBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY),
            (this.activeRoomName != null ? this.activeRoomName : "Chat"), // Handle null activeRoomName
            TitledBorder.CENTER,
            TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 20),
            new Color(255,255,255));
        this.chatScrollPane.setBorder(this.chatScrollPaneBorder);

        chatTextField = new CustomTextField(300, 30);
        chatTextField.setBackground(new Color(70, 70, 70));
        chatTextField.setForeground(Color.WHITE);
        chatTextField.setBorder(null);

        sendButton = new CustomButton("Send", 80, 30, new Color(102, 255, 102));
        sendButton.setForeground(Color.BLACK);
        sendButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        emojiButton = new CustomButton("\uD83D\uDE00", 40, 30, new Color(255, 255, 255));
        emojiButton.setForeground(Color.BLACK);
        emojiButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        fileSendButton = new CustomButton("+", 30, 30, new Color(255, 255, 255));
        fileSendButton.setForeground(Color.BLACK);
        fileSendButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        fileSendButton.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(emojiButton);
        buttonPanel.add(sendButton);
        inputPanel.add(fileSendButton, BorderLayout.WEST);
        inputPanel.add(chatTextField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

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
        inputPanelHolder.add(inputPanel);

        roomTabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        roomTabPanel.setOpaque(false);
        roomTabPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        if (this.activeRoomName != null) { // Only add initial tab if room name is not null
            addRoomTabInternal(activeRoomName);
        }


        addNewRoomButton = new CustomButton("+", 50, 30, new Color(150, 100, 150));
        addNewRoomButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        addNewRoomButton.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
        roomTabPanel.add(addNewRoomButton);

        JScrollPane roomTabScrollPanel = new JScrollPane(roomTabPanel){
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
        roomTabScrollPanel.setOpaque(false);
        roomTabScrollPanel.getViewport().setOpaque(false);
        roomTabScrollPanel.setBorder(null);
        roomTabScrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        roomTabScrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollBar horizontalScrollBar = roomTabScrollPanel.getHorizontalScrollBar();
        horizontalScrollBar.setPreferredSize(new Dimension(0, 10));

        JPanel chatAreaPanel = new JPanel(new BorderLayout(0, 5)) {
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
        chatAreaPanel.setOpaque(false);
        chatAreaPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        chatAreaPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatAreaPanel.add(inputPanelHolder, BorderLayout.SOUTH);

        chatPanel.add(roomTabScrollPanel, BorderLayout.NORTH);
        chatPanel.add(chatAreaPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setRightComponent(rightColumnPanel);
        splitPane.setLeftComponent(chatPanel);
        splitPane.setDividerLocation(800);
        splitPane.setResizeWeight(1);
        splitPane.setOpaque(false);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setDividerSize(5);

        mainContentPanel.add(splitPane, BorderLayout.CENTER);
    }

    private JLabel createLogoLabel(String newPath, int width) {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setOpaque(false);
        label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        try {
            URL imgUrl = getClass().getResource(newPath);
            if (imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                if (icon.getIconWidth() > 0) {
                    Image scaledImage = icon.getImage().getScaledInstance(width, -1, Image.SCALE_SMOOTH);
                    label.setIcon(new ImageIcon(scaledImage));
                } else throw new Exception("Logo ImageIcon invalid width for " + newPath);
            } else { throw new Exception("Logo resource not found: " + newPath); }
        } catch (Exception e) {
            System.err.println("ERROR loading logo (" + newPath + "): " + e.getMessage());
            String fallbackText = "Icon?";
            Font fallbackFont = new Font("Arial", Font.BOLD, (int)(width * 0.6));
            if (newPath != null) { // Avoid NPE on newPath itself
                if (newPath.equals(ACCEPT_LOGO_IMAGE_PATH)) fallbackText = "✔️";
                else if (newPath.equals(DENY_LOGO_IMAGE_PATH)) fallbackText = "❌";
                else if (newPath.equals(GEAR_LOGO_IMAGE_PATH) || newPath.equals(DOWNLOAD_LOGO_IMAGE_PATH)) {
                     fallbackText = "AnonChat"; // For gear/download, use app name
                     fallbackFont = MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(Font.PLAIN, 24f) : new Font("Arial", Font.BOLD, 24);
                }
            }
            label.setText(fallbackText);
            label.setForeground(Color.WHITE);
            label.setFont(fallbackFont);
        }
        return label;
    }

    private JLabel createSmallIconLabel(String iconPath, int size, String tooltip) {
        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        try {
            URL imgUrl = getClass().getResource(iconPath);
            if (imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                Image scaledImage = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaledImage));
                if (tooltip != null) {
                    iconLabel.setToolTipText(tooltip);
                }
            } else {
                System.err.println("Icon resource not found: " + iconPath);
                iconLabel.setText("[C]");
                iconLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
                iconLabel.setForeground(Color.LIGHT_GRAY);
            }
        } catch (Exception e) {
            iconLabel.setText("[C]");
            iconLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
            iconLabel.setForeground(Color.LIGHT_GRAY);
            System.err.println("Error loading icon " + iconPath + ": " + e.getMessage());
        }
        return iconLabel;
    }

    private void addUserEntry(String actualUsername, String displayName, boolean addChatIcon) {
        JPanel entryPanel = new JPanel(new BorderLayout(5, 0));
        entryPanel.setOpaque(false);
        entryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        entryPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        JLabel nameLabel = new JLabel(displayName != null ? displayName : (actualUsername != null ? actualUsername : "Unknown User"));
        nameLabel.setForeground(new Color(230, 230, 230));
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        entryPanel.add(nameLabel, BorderLayout.CENTER);

        // Only add chat icon if addChatIcon is true AND it's not the current user
        if (addChatIcon && !Objects.equals(actualUsername, this.currentUserName)) {
            JLabel chatIconLabel = createSmallIconLabel(CHAT_ICON_PATH, 16, "Request private chat with " + (actualUsername != null ? actualUsername : "user"));
            if (chatIconLabel != null && chatIconLabel.getIcon() != null) {
                chatIconLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                chatIconLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (chatController != null && actualUsername != null) { // Ensure actualUsername is not null
                            System.out.println("[UI] Requesting private chat with: " + actualUsername);
                            chatController.requestPrivateChat(actualUsername);
                        } else {
                            System.err.println("[UI] Cannot request private chat. Controller: " + chatController + ", Target User: " + actualUsername);
                        }
                    }
                });
                entryPanel.add(chatIconLabel, BorderLayout.EAST);
            }
        }
        userEntriesPanel.add(entryPanel);
    }

    public void updateUserList(List<String> onlineUsernames) {
        SwingUtilities.invokeLater(() -> {
            if (userEntriesPanel == null) return; // Safety check
            userEntriesPanel.removeAll();
            addUserEntry(this.currentUserName, (this.currentUserName != null ? this.currentUserName : "You") + " (You)", false);

            if (onlineUsernames != null) {
                for (String username : onlineUsernames) {
                    if (username == null) continue; // Skip null usernames from the list
                    if (!Objects.equals(username, this.currentUserName)) {
                        addUserEntry(username, username, true);
                    }
                }
            }
            userEntriesPanel.revalidate();
            userEntriesPanel.repaint();
            System.out.println("[UI] User list updated with " + (onlineUsernames != null ? onlineUsernames.size() : 0) + " users.");
        });
    }


    public void displayPrivateChatRequest(String fromUser, String proposedRoomName, String proposedPassword) {
        SwingUtilities.invokeLater(() -> {
            if (chatController == null) {
                System.err.println("[UI] ChatController is null. Cannot handle private chat request display.");
                JOptionPane.showMessageDialog(this, "Internal error: Chat controller not available for invitation.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (fromUser == null || proposedRoomName == null || proposedPassword == null) {
                System.err.println("[UI] Received private chat request with null parameters. Ignoring.");
                return;
            }

            String message = String.format("%s has invited you to a private chat in room '%s'. Accept or Decline?",
                    fromUser, proposedRoomName);
            invitationLabel.setText(message);

            // Clear existing listeners
            for (MouseListener ml : acceptInvitationButton.getMouseListeners()) acceptInvitationButton.removeMouseListener(ml);
            for (MouseListener ml : denyInvitationButton.getMouseListeners()) denyInvitationButton.removeMouseListener(ml);

            acceptInvitationButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            acceptInvitationButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    System.out.println("[UI] Accepted private chat with " + fromUser + " for room " + proposedRoomName);
                    chatController.acceptPrivateChat(fromUser, proposedRoomName, proposedPassword);
                    invitationPanel.setVisible(false);
                }
            });

            denyInvitationButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            denyInvitationButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    System.out.println("[UI] Declined private chat with " + fromUser + " for room " + proposedRoomName);
                    chatController.declinePrivateChat(fromUser, proposedRoomName);
                    invitationPanel.setVisible(false);
                }
            });

            invitationPanel.setVisible(true);
            if (mainFrame != null) {
                 mainFrame.toFront();
                 mainFrame.requestFocus();
            }
            System.out.println("[UI] Displayed private chat request from " + fromUser + " for room " + proposedRoomName);
        });
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

    public void appendMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            ChatMessage msg = new ChatMessage(sender, message);
            chatListModel.addElement(msg);
            SwingUtilities.invokeLater(() -> {
                int lastIndex = chatListModel.getSize() - 1;
                if (lastIndex >= 0) chatList.ensureIndexIsVisible(lastIndex);
            });
        });
    }

    public void displaySystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            ChatMessage msg = new ChatMessage("[System]", message);
            chatListModel.addElement(msg);
            SwingUtilities.invokeLater(() -> {
                 int lastIndex = chatListModel.getSize() - 1;
                 if (lastIndex >= 0) chatList.ensureIndexIsVisible(lastIndex);
            });
        });
    }

    public void setChatScrollPaneTitle(String roomName) {
        SwingUtilities.invokeLater(() -> {
            if (this.chatScrollPaneBorder != null) {
                this.chatScrollPaneBorder.setTitle(roomName != null ? roomName : "Chat"); // Handle null roomName
                if (this.chatScrollPane != null) this.chatScrollPane.repaint();
                System.out.println("[UI] ChatScrollPane title updated to: " + (roomName != null ? roomName : "Chat"));
            } else System.err.println("[UI] chatScrollPaneBorder is null, cannot update title.");
        });
    }

    private void addEventListeners() {
        ActionListener sendAction = e -> {
            String messageText = chatTextField.getText().trim();
            if (!messageText.isEmpty()) {
                if (chatController != null) chatController.sendMessage(messageText);
                chatTextField.setText("");
            }
        };
        // GearButtonLabel listeners are added during its creation in createMainPanel
        sendButton.addActionListener(sendAction);
        chatTextField.addActionListener(sendAction);
        leaveRoomButton.addActionListener(e -> { if (chatController != null) chatController.leaveRoom(); });
        addNewRoomButton.addActionListener(e -> showAddRoomDialog());
        emojiButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Emoji feature not implemented yet."));
        fileSendButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "File sending not implemented yet."));
    }

    public void showAddRoomDialog() {
        JTextField roomNameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JPanel dialogPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        dialogPanel.add(new JLabel("Room Name:"));
        dialogPanel.add(roomNameField);
        dialogPanel.add(new JLabel("Password:"));
        dialogPanel.add(passwordField);
        SwingUtilities.invokeLater(roomNameField::requestFocusInWindow);

        int result = JOptionPane.showConfirmDialog(mainFrame, dialogPanel, "Join or Create New Room", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String newRoomName = roomNameField.getText().trim();
            char[] passwordChars = passwordField.getPassword();
            String newPassword = new String(passwordChars);
            java.util.Arrays.fill(passwordChars, ' ');
            if (newRoomName == null || newRoomName.isEmpty() || newPassword.isEmpty()) { // Check for null newRoomName as well
                 JOptionPane.showMessageDialog(mainFrame, "Room Name and Password cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            } else {
                System.out.println("[UI] Requesting join/switch via controller for: " + newRoomName);
                if (chatController != null) chatController.joinOrSwitchToRoom(newRoomName, newPassword);
            }
        }
    }

    private void addRoomTabInternal(String roomName) {
         if (roomName == null) {
             System.err.println("[UI] Attempted to add a tab with a null room name. Skipping.");
             return;
         }
         if (roomButtons.containsKey(roomName)) {
             System.out.println("[UI] Tab button for " + roomName + " already exists. Skipping.");
             return;
         }
        System.out.println("[UI] Creating tab button for: " + roomName);
        Random random = new Random();
        // Ensure CustomButton handles null roomName gracefully if it's possible, though we added a check above.
        CustomButton newRoomTabButton = new CustomButton(roomName, 80, 30, new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255), 120));
        newRoomTabButton.setForeground(Color.WHITE);
        newRoomTabButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        newRoomTabButton.addActionListener(e -> {
             System.out.println("[UI] Switch room tab button clicked for: " + roomName);
             if (chatController != null) chatController.requestRoomSwitch(roomName);
        });
        roomButtons.put(roomName, newRoomTabButton);
        int addButtonIndex = -1;
        for(int i=0; i < roomTabPanel.getComponentCount(); i++){
            if(roomTabPanel.getComponent(i) == addNewRoomButton){
                addButtonIndex = i;
                break;
            }
        }
        if (addButtonIndex == -1) addButtonIndex = roomTabPanel.getComponentCount();
        roomTabPanel.add(newRoomTabButton, addButtonIndex);
        roomTabPanel.revalidate();
        roomTabPanel.repaint();
        System.out.println("[UI] Tab button added and panel revalidated for: " + roomName);
    }

     public void addRoomTab(String roomName) {
          if (roomName == null) {
              System.err.println("[UI] Received request to add tab for a null room name. Ignoring.");
              return;
          }
          System.out.println("[UI] Received request to add tab for: " + roomName);
          SwingUtilities.invokeLater(() -> addRoomTabInternal(roomName));
     }

    public void updateUIForRoomSwitch(String newActiveRoom) {
         SwingUtilities.invokeLater(() -> {
             System.out.println("[UI] Updating UI view for room switch to: " + (newActiveRoom != null ? newActiveRoom : "null room"));
             this.activeRoomName = newActiveRoom;
             this.setChatScrollPaneTitle(newActiveRoom); // Already handles null
             System.out.println("[UI] Clearing chat list model.");
             this.chatListModel.clear();
             System.out.println("[UI] Requesting history from controller for: " + (newActiveRoom != null ? newActiveRoom : "null room"));
             if (chatController == null) {
                  System.err.println("[UI] Error: ChatController is null in updateUIForRoomSwitch!");
                  return;
             }
             List<ChatMessage> history = (newActiveRoom != null) ? chatController.getChatHistory(newActiveRoom) : null; // Avoid asking history for null room
             System.out.println("[UI] Received " + (history != null ? history.size() : 0) + " messages from history for " + (newActiveRoom != null ? newActiveRoom : "null room"));
             if(history != null) {
                 for (ChatMessage msg : history) {
                     this.chatListModel.addElement(msg);
                 }
             }
             System.out.println("[UI] Repopulated chat list model.");
             System.out.println("[UI] Updating tab button highlighting.");
             roomButtons.forEach((name, button) -> { // 'name' here can be null if a null key was put in roomButtons
                 if(button != null) {
                     if (Objects.equals(name, newActiveRoom)) { // Use Objects.equals for null-safe comparison
                          button.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
                      } else {
                          Border defaultBorder = UIManager.getBorder("Button.border");
                          button.setBorder(defaultBorder != null ? defaultBorder : BorderFactory.createEmptyBorder(2,2,2,2)); // Fallback if UIManager returns null
                      }
                 } else System.err.println("[UI] Warning: Found null button reference for room '" + name + "' in roomButtons map.");
             });
             SwingUtilities.invokeLater(() -> {
                int lastIndex = chatListModel.getSize() - 1;
                if (lastIndex >= 0) chatList.ensureIndexIsVisible(lastIndex);
             });
            System.out.println("[UI] Finished updating UI for room switch.");
         });
    }

    public void setChatController(ChatController controller) { this.chatController = controller; }
    public void setMainFrame(MainFrame frame) { this.mainFrame = frame; }


    private static class BackgroundImagePanel extends JPanel {
        private Image backgroundImage;
        private String errorMessage = null;
        private String imagePathUsed;
        public BackgroundImagePanel(String imagePath) { /* ... same as before ... */
            this.imagePathUsed = imagePath;
            try {
                URL imgUrl = getClass().getResource(imagePath);
                if (imgUrl != null) {
                    this.backgroundImage = ImageIO.read(imgUrl);
                    if (this.backgroundImage == null) throw new IOException("ImageIO returned null for " + imagePath);
                    System.out.println("[BGPanel] Loaded: " + imagePath);
                } else { throw new IOException("Resource not found: " + imagePath); }
            } catch (IOException e) {
                this.errorMessage = "Ex loading background: " + e.getMessage();
                System.err.println(errorMessage); this.backgroundImage = null;
            }
            setOpaque(true);
        }
        @Override
        protected void paintComponent(Graphics g) { /* ... same as before ... */
            super.paintComponent(g);
            if (backgroundImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                int panelW = getWidth(); int panelH = getHeight();
                int imgW = backgroundImage.getWidth(this); int imgH = backgroundImage.getHeight(this);
                if(imgW <=0 || imgH <= 0) { g2d.dispose(); return; }
                double imgAspect = (double) imgW / imgH; double panelAspect = (double) panelW / panelH;
                int drawW, drawH, drawX, drawY;
                if (panelAspect > imgAspect) { drawW = panelW; drawH = (int)(panelW / imgAspect); drawX = 0; drawY = (panelH - drawH) / 2; }
                else { drawH = panelH; drawW = (int)(panelH * imgAspect); drawX = (panelW - drawW) / 2; drawY = 0; }
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(backgroundImage, drawX, drawY, drawW, drawH, this);
                g2d.dispose();
            } else {
                g.setColor(new Color(30, 50, 70));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.YELLOW); g.setFont(new Font("SansSerif", Font.BOLD, 14));
                FontMetrics fm = g.getFontMetrics();
                String text = "BG Load Error: " + (errorMessage != null ? errorMessage : "Unknown");
                int msgWidth = fm.stringWidth(text);
                g.drawString(text, Math.max(5, (getWidth() - msgWidth) / 2), getHeight() / 2 + fm.getAscent() / 2 - fm.getHeight()/2);
                String pathText = "(" + imagePathUsed + ")";
                msgWidth = fm.stringWidth(pathText);
                g.drawString(pathText, Math.max(5, (getWidth() - msgWidth) / 2), getHeight() / 2 + fm.getAscent() / 2 + fm.getHeight()/2);
            }
        }
    }
}