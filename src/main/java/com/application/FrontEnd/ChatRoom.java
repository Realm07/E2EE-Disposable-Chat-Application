package com.application.FrontEnd;

// Assuming Backend classes are in this package now
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map; // Needed for maps
import java.util.Random;   // Needed for maps

import javax.swing.BorderFactory; // Needed for button colors
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.application.Backend.ChatController;
import com.application.FrontEnd.components.CustomButton;
import com.application.FrontEnd.components.CustomLabel;
import com.application.FrontEnd.components.CustomTextArea;
import com.application.FrontEnd.components.CustomTextField;

public class ChatRoom extends JPanel {

    // --- UI Components (Style based on snippet) ---
    private CustomButton sendButton;
    private CustomButton leaveRoomButton;
    private CustomTextArea chatArea;
    private CustomTextField chatTextField;
    private CustomLabel roomNameLabel;
    private CustomTextArea userNameArea;
    private JPanel roomTabPanel;
    private CustomButton addNewRoomButton;
    private CustomButton emojiButton; // Renamed from EmojiButton
    private CustomButton fileSendButton; // Renamed from FileSendButton

    // --- References & State (Keep existing) ---
    private MainFrame mainFrame;
    private ChatController chatController;
    private String currentUserName;
    private String activeRoomName;
    private Map<String, String> roomChatHistory;
    private Map<String, CustomButton> roomButtons;

    // --- Constructor (Integrate snippet's UI structure) ---
    public ChatRoom(String initialUsername, String initialRoomName, MainFrame mainFrame, ChatController chatController) {
        this.mainFrame = mainFrame;
        this.chatController = chatController;
        this.currentUserName = initialUsername; // Use existing name convention
        this.activeRoomName = initialRoomName; // Use existing name convention

        // Initialize state (Keep existing)
        this.roomChatHistory = new HashMap<>();
        this.roomChatHistory.put(initialRoomName, "");
        this.roomButtons = new HashMap<>();

        // --- UI Initialization (Adopt snippet's layout) ---
        Color backgroundColor = new Color(45, 45, 45); // Snippet's background
        this.setBackground(backgroundColor);
        this.setOpaque(true);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Snippet's border

        // --- Right Panel (User List & Room Info - renamed from leftColumnPanel in snippet) ---
        JPanel userDisplayPanel = new JPanel();
        userDisplayPanel.setLayout(new BoxLayout(userDisplayPanel, BoxLayout.Y_AXIS));
        userDisplayPanel.setOpaque(false);

        // Room Name Label (Snippet style)
        roomNameLabel = new CustomLabel(initialRoomName, 200, 30);
        roomNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        roomNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        roomNameLabel.setForeground(new Color(200, 200, 200));

        // User Name Area (Snippet style)
        userNameArea = new CustomTextArea(null);
        userNameArea.setBackground(new Color(60, 60, 60));
        userNameArea.setForeground(new Color(220, 220, 220));
        userNameArea.setEditable(false);
        userNameArea.append("- " + this.currentUserName + " (You)\n"); // Updated text

        JScrollPane userScrollPane = new JScrollPane(userNameArea);
        userScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        // userScrollPane.setPreferredSize(new Dimension(180, 50)); // PreferredSize often overridden by layout
        userScrollPane.setOpaque(false);
        userScrollPane.getViewport().setOpaque(false);
        userScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        userScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Custom Border for User List (Snippet style)
        Border userListBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY), "Online Users",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12),
                new Color(180, 180, 180));
        userScrollPane.setBorder(userListBorder);

        // Add components to user display panel
        userDisplayPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        userDisplayPanel.add(roomNameLabel);
        userDisplayPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        userDisplayPanel.add(userScrollPane);

        // Leave Button (Snippet style, rename "Leave" to "Leave App")
        leaveRoomButton = new CustomButton("Leave App", 100, 30, new Color(255, 77, 77));
        leaveRoomButton.setForeground(Color.WHITE);

        JPanel leaveButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        leaveButtonPanel.setOpaque(false);
        leaveButtonPanel.add(leaveRoomButton);

        // Assemble Right Panel
        JPanel rightColumnPanel = new JPanel(); // Changed name from leftColumnPanel
        rightColumnPanel.setLayout(new BorderLayout(0, 10));
        rightColumnPanel.setOpaque(false);
        rightColumnPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5)); // Snippet border
        rightColumnPanel.add(userDisplayPanel, BorderLayout.CENTER);
        rightColumnPanel.add(leaveButtonPanel, BorderLayout.SOUTH);

        // --- Left Panel (Chat Area & Input - Snippet style) ---
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout(0, 5));
        chatPanel.setOpaque(false);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5)); // Snippet border

        // Chat Area (Snippet style)
        chatArea = new CustomTextArea(null);
        chatArea.setBackground(new Color(60, 60, 60));
        chatArea.setForeground(new Color(230, 230, 230));
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setOpaque(false);
        chatScrollPane.getViewport().setOpaque(false);
        chatScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Custom Border for Chat History (Snippet style)
        Border chatBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY), "Chat History",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12),
                new Color(180, 180, 180));
        chatScrollPane.setBorder(chatBorder);

        // Input Area (Snippet style)
        chatTextField = new CustomTextField(300, 30);
        chatTextField.setBackground(new Color(70, 70, 70));
        chatTextField.setForeground(Color.WHITE);

        sendButton = new CustomButton("Send", 80, 30, new Color(102, 255, 102));
        sendButton.setForeground(Color.BLACK); // Snippet uses BLACK
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 15));

        emojiButton = new CustomButton("@", 50, 30, new Color(255, 255, 255)); // Placeholder
        emojiButton.setForeground(Color.BLACK);
        emojiButton.setFont(new Font("Segoe UI", Font.BOLD, 15));

        fileSendButton = new CustomButton("+", 50, 30, new Color(255, 255, 255)); // Placeholder
        fileSendButton.setForeground(Color.BLACK);
        fileSendButton.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0)); // Snippet border

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0)); // Snippet layout
        buttonPanel.setOpaque(false);
        // Add buttons in order for FlowLayout
        buttonPanel.add(emojiButton);
        buttonPanel.add(fileSendButton);
        buttonPanel.add(sendButton);

        inputPanel.add(chatTextField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // Room Tab Panel (Snippet style)
        roomTabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        roomTabPanel.setOpaque(false);

        Border RoomNameBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY),
            "Rooms",
            TitledBorder.CENTER, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12),
            new Color(180, 180, 180));
        roomTabPanel.setBorder(RoomNameBorder);

        // Add initial room button using the controller-aware method
        addRoomTabInternal(initialRoomName);

        // Add "+" button (Snippet style)
        addNewRoomButton = new CustomButton("+", 50, 30, new Color(150, 100, 150));
        roomTabPanel.add(addNewRoomButton);

        // Scroll pane for tabs (Snippet style)
        JScrollPane roomTabScrollPanel = new JScrollPane(roomTabPanel);
        roomTabScrollPanel.setOpaque(false);
        roomTabScrollPanel.getViewport().setOpaque(false);
        roomTabScrollPanel.setBorder(null);
        roomTabScrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        roomTabScrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollBar horizontalScrollBar = roomTabScrollPanel.getHorizontalScrollBar();
        horizontalScrollBar.setPreferredSize(new Dimension(0, 10)); // Snippet height

        // Assemble Left Panel
        chatPanel.add(roomTabScrollPanel, BorderLayout.NORTH);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        // --- Split Pane (Snippet style) ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setRightComponent(rightColumnPanel); // Use the renamed right panel
        splitPane.setLeftComponent(chatPanel);
        splitPane.setDividerLocation(600); // Use existing value or snippet's
        splitPane.setResizeWeight(0.8); // Adjust resize weight if needed (snippet used 0.5)
        splitPane.setOpaque(false);
        splitPane.setContinuousLayout(true); // Keep existing setting
        splitPane.setBorder(null);

        // Add split pane to the main panel
        add(splitPane, BorderLayout.CENTER);

        // Load initial chat history (Keep existing)
        chatArea.setText(roomChatHistory.getOrDefault(this.activeRoomName, ""));

        addEventListeners(); // Add listeners AFTER all components are created
    }

    // --- Methods Called by Controller (Keep existing logic) ---
    public void appendMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            String fullMessage = sender + ": " + message + "\n";
            chatArea.append(fullMessage);
            String currentHistory = roomChatHistory.getOrDefault(activeRoomName, "");
            roomChatHistory.put(activeRoomName, currentHistory + fullMessage);
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void displaySystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String fullMessage = "[System] " + message + "\n"; // Keep prefix
            chatArea.append(fullMessage);
            String currentHistory = roomChatHistory.getOrDefault(activeRoomName, "");
            roomChatHistory.put(activeRoomName, currentHistory + fullMessage);
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void clearChatArea() {
        SwingUtilities.invokeLater(() -> {
             chatArea.setText("");
        });
    }

    public void setActiveRoomNameLabel(String roomName) {
         SwingUtilities.invokeLater(() -> {
              this.roomNameLabel.setText(roomName);
         });
    }

    // --- Event Listeners (Keep controller interactions) ---
    private void addEventListeners() {
        // Send Action (Calls controller)
        ActionListener sendAction = e -> {
            String messageText = chatTextField.getText().trim();
            if (!messageText.isEmpty()) {
                chatController.sendMessage(messageText);
                chatTextField.setText("");
                // No local echo here - handled by controller relay
            }
        };
        sendButton.addActionListener(sendAction);
        chatTextField.addActionListener(sendAction); // Send on Enter too

        // Leave Button Action (Calls controller)
        leaveRoomButton.addActionListener(e -> {
            chatController.leaveRoom();
        });

        // Add New Room Button Action (Calls internal dialog)
        addNewRoomButton.addActionListener(e -> showAddRoomDialog());

        // Placeholder listeners for Emoji/File buttons (Snippet didn't have actions)
        emojiButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Emoji feature not implemented yet."));
        fileSendButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "File sending not implemented yet."));
    }


    // --- Multi-Room UI Methods (Keep controller interactions) ---

    /** Shows dialog to get new room name and password. Calls Controller on OK. */
    public void showAddRoomDialog() {
        // Keep existing implementation that calls chatController.joinOrSwitchToRoom
        JTextField roomNameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);

        JPanel dialogPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        dialogPanel.add(new JLabel("Room Name:"));
        dialogPanel.add(roomNameField);
        dialogPanel.add(new JLabel("Password:"));
        dialogPanel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(
                mainFrame,
                dialogPanel,
                "Join or Create New Room",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String newRoomName = roomNameField.getText().trim();
            char[] passwordChars = passwordField.getPassword();
            String newPassword = new String(passwordChars);
            java.util.Arrays.fill(passwordChars, ' ');

            if (!newRoomName.isEmpty() && !newPassword.isEmpty()) {
                chatController.joinOrSwitchToRoom(newRoomName, newPassword);
            } else {
                 JOptionPane.showMessageDialog(mainFrame, "Room Name and Password cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /** Adds a button for a new room tab INTERNALLY. Listener calls controller. */
    private void addRoomTabInternal(String roomName) {
        // Keep existing implementation that calls chatController.requestRoomSwitch
         if (roomButtons.containsKey(roomName)) return;

        Random random = new Random();
        // Use snippet's button style/color logic if desired, or keep simpler random
        CustomButton newRoomTabButton = new CustomButton(roomName, 80, 30,
                // new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200))); // Simpler random
                 new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255))); // Snippet random
        newRoomTabButton.setForeground(Color.WHITE);
        newRoomTabButton.setFont(new Font("Segoe UI", Font.BOLD, 12)); // Smaller font maybe

        // Action listener triggers switch VIA THE CONTROLLER
        newRoomTabButton.addActionListener(e -> {
             System.out.println("[UI] Switch room button clicked for: " + roomName);
             chatController.requestRoomSwitch(roomName);
        });
        roomButtons.put(roomName, newRoomTabButton);

        roomChatHistory.putIfAbsent(roomName, ""); // Ensure history exists

        int addButtonIndex = roomTabPanel.getComponentCount() - 1;
        if (addButtonIndex < 0) addButtonIndex = 0;
        roomTabPanel.add(newRoomTabButton, addButtonIndex);

        roomTabPanel.revalidate();
        roomTabPanel.repaint();
    }

     /** Public method for Controller to add tab IF needed externally */
     public void addRoomTab(String roomName) {
          // Keep existing implementation
          SwingUtilities.invokeLater(() -> addRoomTabInternal(roomName));
     }

    /** Method called by Controller to update UI after a successful room switch */
    public void updateUIForRoomSwitch(String newActiveRoom) {
        // Keep existing implementation
         SwingUtilities.invokeLater(() -> {
             this.activeRoomName = newActiveRoom;
             this.setActiveRoomNameLabel(newActiveRoom);
             this.chatArea.setText(roomChatHistory.getOrDefault(newActiveRoom, ""));
             this.chatArea.setCaretPosition(chatArea.getDocument().getLength());
             System.out.println("[UI] Switched view to room : " + activeRoomName);

             // Update tab highlighting (Keep existing)
             roomButtons.forEach((name, button) -> {
                if (name.equals(newActiveRoom)) {
                     button.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
                 } else {
                     // Reset border - Use UIManager or a default border
                     button.setBorder(UIManager.getBorder("Button.border"));
                 }
             });
         });
    }

    // Setters (Keep existing)
    public void setChatController(ChatController controller) {
        this.chatController = controller;
    }

    public void setMainFrame(MainFrame frame) {
        this.mainFrame = frame;
    }
}