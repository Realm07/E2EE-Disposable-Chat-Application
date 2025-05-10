// src/main/java/com/application/FrontEnd/ChatRoom.java
package com.application.FrontEnd;

// Swing & AWT Imports
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;
import java.util.ArrayList; // Needed for history list copy
import java.util.HashMap;
import java.util.HashSet; // Needed for user list set
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;      // Needed for user list set
import java.util.stream.Collectors; // For sorting users

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.imageio.ImageIO;
import java.io.BufferedWriter; // For file writing
import java.io.File;         // For file chooser
import java.io.FileWriter;   // For file writing
import java.io.IOException;  // For file I/O errors

// Backend and Component Imports
import com.application.Backend.ChatController;
import com.application.FrontEnd.components.MessageCellRenderer;
import com.application.FrontEnd.components.MessageCellRenderer.ChatMessage;
import com.application.FrontEnd.components.*; // Assuming Custom components exist here

public class ChatRoom extends JPanel {

    // --- UI Components ---
    private CustomButton sendButton;
    private CustomButton leaveRoomButton;
    private CustomButton downloadChatButton; // Added
    private CustomTextField chatTextField;
    private CustomLabel roomNameLabel;
    private CustomTextArea userNameArea; // For displaying users
    private JPanel roomTabPanel;
    private CustomButton addNewRoomButton;
    private CustomButton emojiButton;
    private CustomButton fileSendButton;
    private JList<ChatMessage> chatList;
    private DefaultListModel<ChatMessage> chatListModel;

    // --- References & State ---
    private MainFrame mainFrame;
    private ChatController chatController;
    private String currentUserName;
    private String activeRoomName;
    private Map<String, CustomButton> roomButtons;
    private Set<String> onlineUsers; // State for user list

    // --- Constants ---
    private static final String CHAT_BACKGROUND_PATH = "/com/application/FrontEnd/images/BG_ChatRoom.jpg";

    // --- Constructor ---
    public ChatRoom(String initialUsername, String initialRoomName, MainFrame mainFrame, ChatController chatController) {
        this.mainFrame = mainFrame;
        this.chatController = chatController;
        this.currentUserName = initialUsername;
        this.activeRoomName = initialRoomName;

        this.roomButtons = new HashMap<>();
        this.onlineUsers = new HashSet<>(); // Initialize user set

        // Initialize chat list model and component
        this.chatListModel = new DefaultListModel<>();
        this.chatList = new JList<>(this.chatListModel);
        chatList.setCellRenderer(new MessageCellRenderer());
        chatList.setBackground(new Color(60, 60, 60));
        chatList.setOpaque(true); // List itself is opaque
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chatList.setFocusable(false);

        // --- Setup Layout and Background ---
        setLayout(new BorderLayout());
        setOpaque(false); // This main panel is transparent for layering

        JLayeredPane layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        ChatBackgroundPanel backgroundPanel = new ChatBackgroundPanel(CHAT_BACKGROUND_PATH);
        backgroundPanel.setBounds(0, 0, 10, 10); // Placeholder bounds
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER); // Add background first

        // --- Build UI Elements ---
        JPanel rightColumnPanel = createRightPanel();
        JPanel chatPanel = createLeftPanel();

        // --- Create and Configure Split Pane ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setRightComponent(rightColumnPanel);
        splitPane.setLeftComponent(chatPanel);
        splitPane.setDividerLocation(800); // Adjust initial divider location
        splitPane.setResizeWeight(0.8); // Chat panel gets more resize weight
        splitPane.setOpaque(false); // Make split pane transparent
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setBounds(0, 0, 10, 10); // Placeholder bounds

        // --- Add Split Pane (Content) to Layered Pane ---
        layeredPane.add(splitPane, JLayeredPane.PALETTE_LAYER); // Add content above background

        // --- Add Resize Listener ---
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeAndPositionComponents(layeredPane, backgroundPanel, splitPane);
            }
            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> resizeAndPositionComponents(layeredPane, backgroundPanel, splitPane));
            }
        });

        // Add self to user list initially (will be cleared on first real update)
        //addUserToList(this.currentUserName); // Let onConnected handle initial population

        addEventListeners(); // Setup actions
    }

    /** Creates the right panel containing user list and action buttons. */
    private JPanel createRightPanel() {
        JPanel userDisplayPanel = new JPanel();
        userDisplayPanel.setLayout(new BoxLayout(userDisplayPanel, BoxLayout.Y_AXIS));
        userDisplayPanel.setOpaque(false);

        roomNameLabel = new CustomLabel("Room: " + activeRoomName, 200, 30);
        roomNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        roomNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        roomNameLabel.setForeground(new Color(200, 200, 200));

        userNameArea = new CustomTextArea(null); // User list display
        userNameArea.setBackground(new Color(60, 60, 60));
        userNameArea.setForeground(new Color(220, 220, 220));
        userNameArea.setEditable(false);
        userNameArea.setLineWrap(true);
        userNameArea.setWrapStyleWord(true);

        JScrollPane userScrollPane = new JScrollPane(userNameArea);
        userScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        userScrollPane.setOpaque(false);
        userScrollPane.getViewport().setOpaque(false);
        userScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        userScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        Border userListBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY), "Online Users", TitledBorder.CENTER, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), new Color(180, 180, 180));
        userScrollPane.setBorder(userListBorder);

        userDisplayPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        userDisplayPanel.add(roomNameLabel);
        userDisplayPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        userDisplayPanel.add(userScrollPane);

        // Action Buttons
        leaveRoomButton = new CustomButton("Leave App", 100, 30, new Color(255, 77, 77));
        leaveRoomButton.setForeground(Color.WHITE);

        downloadChatButton = new CustomButton("Download Chat", 120, 30, new Color(70, 130, 180));
        downloadChatButton.setForeground(Color.WHITE);

        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        actionButtonsPanel.setOpaque(false);
        actionButtonsPanel.add(leaveRoomButton);
        actionButtonsPanel.add(downloadChatButton);

        // Assemble Right Panel
        JPanel rightColumnPanel = new JPanel(new BorderLayout(0, 10));
        rightColumnPanel.setOpaque(false);
        rightColumnPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        rightColumnPanel.add(userDisplayPanel, BorderLayout.CENTER);
        rightColumnPanel.add(actionButtonsPanel, BorderLayout.SOUTH);
        return rightColumnPanel;
    }

    /** Creates the left panel containing room tabs, chat list, and input area. */
    private JPanel createLeftPanel() {
        // Room Tab Panel
        roomTabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        roomTabPanel.setOpaque(false);
        Border RoomNameBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY), "Rooms", TitledBorder.CENTER, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), new Color(180, 180, 180));
        roomTabPanel.setBorder(RoomNameBorder);
        addRoomTabInternal(this.activeRoomName); // Add initial room tab
        addNewRoomButton = new CustomButton("+", 50, 30, new Color(150, 100, 150));
        roomTabPanel.add(addNewRoomButton);

        JScrollPane roomTabScrollPanel = new JScrollPane(roomTabPanel);
        roomTabScrollPanel.setOpaque(false);
        roomTabScrollPanel.getViewport().setOpaque(false);
        roomTabScrollPanel.setBorder(null);
        roomTabScrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        roomTabScrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollBar horizontalScrollBar = roomTabScrollPanel.getHorizontalScrollBar();
        horizontalScrollBar.setPreferredSize(new Dimension(0, 10));

        // Chat List Area
        JScrollPane chatScrollPane = new JScrollPane(chatList);
        chatScrollPane.setOpaque(false);
        chatScrollPane.getViewport().setOpaque(false); // Make viewport transparent too
        chatScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Border chatBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY), "Chat History", TitledBorder.CENTER, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), new Color(180, 180, 180));
        chatScrollPane.setBorder(chatBorder);

        // Input Area
        chatTextField = new CustomTextField(300, 30);
        chatTextField.setBackground(new Color(70, 70, 70));
        chatTextField.setForeground(Color.WHITE);

        sendButton = new CustomButton("Send", 80, 30, new Color(102, 255, 102));
        sendButton.setForeground(Color.BLACK);
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        emojiButton = new CustomButton("@", 50, 30, new Color(255, 255, 255));
        emojiButton.setForeground(Color.BLACK);
        emojiButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        fileSendButton = new CustomButton("+", 50, 30, new Color(255, 255, 255));
        fileSendButton.setForeground(Color.BLACK);
        fileSendButton.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(emojiButton);
        buttonPanel.add(fileSendButton);
        buttonPanel.add(sendButton);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        inputPanel.add(chatTextField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // Assemble Left Panel
        JPanel chatPanel = new JPanel(new BorderLayout(0, 5));
        chatPanel.setOpaque(false); // Make transparent
        chatPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        chatPanel.add(roomTabScrollPanel, BorderLayout.NORTH);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        return chatPanel;
    }


    /** Recalculates bounds for layered components */
    private void resizeAndPositionComponents(JLayeredPane layers, JPanel bgPanel, JSplitPane contentPane) {
        SwingUtilities.invokeLater(() -> {
            int w = layers.getWidth();
            int h = layers.getHeight();
            if (w <= 0 || h <= 0) return;
            if (bgPanel != null) bgPanel.setBounds(0, 0, w, h);
            if (contentPane != null) contentPane.setBounds(0, 0, w, h);
            layers.revalidate();
            layers.repaint();
        });
    }

    // --- Methods Called by Controller ---

    public void appendMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            ChatMessage msg = new ChatMessage(sender, message);
            chatListModel.addElement(msg);
            SwingUtilities.invokeLater(() -> { // Scroll after add
                int lastIndex = chatListModel.getSize() - 1;
                if (lastIndex >= 0) chatList.ensureIndexIsVisible(lastIndex);
            });
        });
    }

    public void displaySystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            ChatMessage msg = new ChatMessage("[System]", message); // Use specific sender
            chatListModel.addElement(msg);
            SwingUtilities.invokeLater(() -> { // Scroll after add
                int lastIndex = chatListModel.getSize() - 1;
                if (lastIndex >= 0) chatList.ensureIndexIsVisible(lastIndex);
            });
        });
    }

    public void setActiveRoomNameLabel(String roomName) {
        SwingUtilities.invokeLater(() -> this.roomNameLabel.setText("Room: " + roomName));
    }

    // --- User List Management Methods ---

    public void addUserToList(String username) {
        if (username == null || username.trim().isEmpty()) return;
        SwingUtilities.invokeLater(() -> {
            // Check if the user is ACTUALLY added or if it was already there
            boolean newAddition = onlineUsers.add(username);
            if (newAddition) {
                System.out.println("[UI ChatRoom] User '" + username + "' ADDED to internal set. Calling updateUserListUI.");
                updateUserListUI(); // This MUST be called to refresh the JTextArea
            } else {
                System.out.println("[UI ChatRoom] User '" + username + "' was already in internal set. List should be up-to-date.");
            }
        });
    }

    public void removeUserFromList(String username) {
        if (username == null || username.trim().isEmpty()) return;
        SwingUtilities.invokeLater(() -> {
            boolean wasRemoved = onlineUsers.remove(username);
            if (wasRemoved) {
                System.out.println("[UI ChatRoom] User '" + username + "' REMOVED from internal set. Calling updateUserListUI.");
                updateUserListUI(); // This MUST be called
            } else {
                System.out.println("[UI ChatRoom] User '" + username + "' not found in internal set for removal.");
            }
        });
    }


    private void updateUserListUI() {
        // This method is already invoked on EDT by its callers (addUserToList, removeUserFromList, updateUIForRoomSwitch)
        System.out.println("[UI ChatRoom] Updating userNameArea. Current onlineUsers: " + onlineUsers); // Log current set
        userNameArea.setText(""); // Clear existing text

        // Add current user (self) first, if available
        if (currentUserName != null && !currentUserName.trim().isEmpty()) {
            userNameArea.append("- " + currentUserName + " (You)\n");
        } else {
            System.out.println("[UI ChatRoom] currentUserName is null/empty during updateUserListUI.");
        }

        // Add other distinct users from the set
        onlineUsers.stream()
                .filter(user -> !user.equals(currentUserName)) // Ensure self isn't added twice if logic elsewhere adds it
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(user -> {
                    System.out.println("[UI ChatRoom] Appending to userNameArea: - " + user);
                    userNameArea.append("- " + user + "\n");
                });
        // userNameArea.revalidate(); // JTextArea usually revalidates/repaints itself on setText/append
        // userNameArea.repaint();
    }

    // --- Event Listeners ---
    private void addEventListeners() {
        // Send Action
        ActionListener sendAction = e -> {
            String messageText = chatTextField.getText().trim();
            if (!messageText.isEmpty()) {
                chatController.sendMessage(messageText);
                chatTextField.setText("");
            }
        };
        sendButton.addActionListener(sendAction);
        chatTextField.addActionListener(sendAction);

        // Leave Button Action
        leaveRoomButton.addActionListener(e -> chatController.leaveRoom());

        // Download Chat Button Action
        downloadChatButton.addActionListener(e -> {
            System.out.println("[UI] Download chat button clicked for room: " + activeRoomName);
            downloadChatHistory();
        });

        // Add New Room Button Action
        addNewRoomButton.addActionListener(e -> showAddRoomDialog());

        // Placeholder listeners
        emojiButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Emoji feature not implemented yet."));
        fileSendButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "File sending not implemented yet."));
    }

    // --- Download Chat History Method ---
    private void downloadChatHistory() {
        if (activeRoomName == null || chatController == null) {
            JOptionPane.showMessageDialog(this, "Cannot download chat. No active room or controller.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<ChatMessage> history = chatController.getChatHistory(activeRoomName); // Get history
        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chat history is empty for " + activeRoomName + ".", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Chat History");
        fileChooser.setSelectedFile(new File(activeRoomName + "_chat_history.txt")); // Suggest filename

        int userSelection = fileChooser.showSaveDialog(this.mainFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            System.out.println("[UI] Saving chat history to: " + fileToSave.getAbsolutePath());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                writer.write("Chat History for Room: " + activeRoomName + "\n");
                writer.write("Downloaded by: " + currentUserName + "\n");
                writer.write("============================================\n\n");
                for (ChatMessage msg : history) {
                    // Handle potential null sender/message in history defensively
                    String sender = msg.getSender() != null ? msg.getSender() : "[Unknown]";
                    String message = msg.getMessage() != null ? msg.getMessage() : "[Empty Message]";
                    writer.write(sender + ": " + message + "\n");
                }
                writer.flush();
                JOptionPane.showMessageDialog(this.mainFrame, "Chat history saved to:\n" + fileToSave.getAbsolutePath(), "Download Successful", JOptionPane.INFORMATION_MESSAGE);

                // Notify controller AFTER successful save
                chatController.notifyChatDownloaded(activeRoomName, currentUserName);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this.mainFrame, "Error saving chat history: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        } else {
            System.out.println("[UI] Chat download cancelled by user.");
        }
    }


    // --- Multi-Room UI Methods ---

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
            if (!newRoomName.isEmpty() && !newPassword.isEmpty()) {
                System.out.println("[UI] Requesting join/switch via controller for: " + newRoomName);
                chatController.joinOrSwitchToRoom(newRoomName, newPassword);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Room Name and Password cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void addRoomTabInternal(String roomName) {
        if (roomButtons.containsKey(roomName)) {
            System.out.println("[UI] Tab button for " + roomName + " already exists. Skipping.");
            return;
        }
        System.out.println("[UI] Creating tab button for: " + roomName);
        Random random = new Random();
        CustomButton newRoomTabButton = new CustomButton(roomName, 80, 30, new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
        newRoomTabButton.setForeground(Color.WHITE);
        newRoomTabButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        newRoomTabButton.addActionListener(e -> {
            System.out.println("[UI] Switch room tab button clicked for: " + roomName);
            chatController.requestRoomSwitch(roomName);
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
        System.out.println("[UI] Received request to add tab for: " + roomName);
        SwingUtilities.invokeLater(() -> addRoomTabInternal(roomName));
    }

    public void updateUIForRoomSwitch(String newActiveRoom) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("[UI] Updating UI view for room switch to: " + newActiveRoom);
            this.activeRoomName = newActiveRoom;
            this.setActiveRoomNameLabel(newActiveRoom);

            // Restore History
            this.chatListModel.clear();
            if (chatController != null) {
                List<ChatMessage> history = chatController.getChatHistory(newActiveRoom);
                for (ChatMessage msg : history) this.chatListModel.addElement(msg);
            }

            // --- Manage User List for the new room ---
            System.out.println("[UI ChatRoom] Resetting user list for new room: " + newActiveRoom);
            onlineUsers.clear();
            if (this.currentUserName != null) { // Guard against null
                onlineUsers.add(this.currentUserName);
            }
            updateUserListUI(); // CRITICAL: This updates the JTextArea
            // --- End User List Management ---

            // Update tab button highlighting
            System.out.println("[UI] Updating tab button highlighting.");
            roomButtons.forEach((name, button) -> {
                if(button != null) {
                    if (name.equals(newActiveRoom)) {
                        button.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
                    } else {
                        button.setBorder(UIManager.getBorder("Button.border"));
                    }
                }
            });

            // Scroll chat to bottom
            SwingUtilities.invokeLater(() -> {
                int lastIndex = chatListModel.getSize() - 1;
                if (lastIndex >= 0) chatList.ensureIndexIsVisible(lastIndex);
            });
            System.out.println("[UI] Finished updating UI for room switch to " + newActiveRoom);
        });
    }

    public void clearUserList() {
        SwingUtilities.invokeLater(() -> {
            System.out.println("[UI] Clearing user list (explicit call).");
            onlineUsers.clear();
            updateUserListUI(); // Update UI to reflect empty state (will then add self if currentUsername is set)
        });
    }
    //todo: monis, uncomment the if block and print statement after implementing UI overhaul and adding download file stuff
    public void fileShareAttemptFinished() {
        SwingUtilities.invokeLater(() -> { // Ensure UI-related state is updated on EDT
//            if (concurrentUploads > 0) {
//                concurrentUploads--;
//            }
//            System.out.println("[UI ChatRoom] A file share attempt processing finished. Concurrent uploads now: " + concurrentUploads);
        });
    }

    // --- Setters ---
    public void setChatController(ChatController controller) { this.chatController = controller; }
    public void setMainFrame(MainFrame frame) { this.mainFrame = frame; }

    // --- Inner Class for Background ---
    private static class ChatBackgroundPanel extends JPanel {
        private Image backgroundImage;
        private String imagePathUsed;
        private String errorMessage = null;

        public ChatBackgroundPanel(String imagePath) {
            this.imagePathUsed = imagePath;
            try {
                URL imgUrl = getClass().getResource(imagePath);
                if (imgUrl != null) {
                    this.backgroundImage = ImageIO.read(imgUrl);
                    if (this.backgroundImage == null) throw new IOException("ImageIO.read returned null: " + imagePath);
                    System.out.println("[ChatBGPanel] Loaded: " + imagePath);
                } else { throw new IOException("Resource not found: " + imagePath); }
            } catch (IOException e) {
                this.errorMessage = e.getMessage();
                System.err.println("Error loading chat background (" + imagePath + "): " + this.errorMessage);
                this.backgroundImage = null;
            }
            setOpaque(true); // Background must be opaque
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                int w = getWidth(); int h = getHeight();
                int imgW = backgroundImage.getWidth(this); int imgH = backgroundImage.getHeight(this);
                if (imgW <= 0 || imgH <= 0) { g2d.dispose(); drawErrorFallback(g, w, h); return; }
                // COVER Scaling Logic
                double imgAspect = (double) imgW / imgH; double panelAspect = (double) w / h;
                int drawW, drawH, drawX, drawY;
                if (panelAspect > imgAspect) { drawW = w; drawH = (int)(w / imgAspect); drawX = 0; drawY = (h - drawH) / 2; }
                else { drawH = h; drawW = (int)(h * imgAspect); drawX = (w - drawW) / 2; drawY = 0; }
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(backgroundImage, drawX, drawY, drawW, drawH, this);
                g2d.dispose();
            } else { drawErrorFallback(g, getWidth(), getHeight()); }
        }
        private void drawErrorFallback(Graphics g, int w, int h) {
            g.setColor(new Color(30, 30, 30)); g.fillRect(0, 0, w, h);
            g.setColor(Color.RED); g.setFont(new Font("SansSerif", Font.BOLD, 14));
            String text = "Chat BG Load Error: " + (errorMessage != null ? errorMessage : "Unknown");
            FontMetrics fm = g.getFontMetrics(); int msgWidth = fm.stringWidth(text);
            g.drawString(text, Math.max(5, (w - msgWidth) / 2), h / 2 + fm.getAscent() / 2 - fm.getHeight()/2);
            String pathText = "(" + imagePathUsed + ")"; msgWidth = fm.stringWidth(pathText);
            g.drawString(pathText, Math.max(5, (w - msgWidth) / 2), h / 2 + fm.getAscent() / 2 + fm.getHeight()/2);
        }
    } // End ChatBackgroundPanel inner class
} // End ChatRoom class