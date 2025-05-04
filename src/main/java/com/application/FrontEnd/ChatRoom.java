// src/main/java/com/application/FrontEnd/ChatRoom.java
package com.application.FrontEnd;

// Swing & AWT Imports (ensure all needed are present)
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
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.net.URL; // Needed for BackgroundGifPanel resource loading
import java.util.Collections; // Potentially needed for empty list
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.imageio.ImageIO; // Needed for BackgroundGifPanel if using static images
import java.io.IOException; // Needed for BackgroundGifPanel error handling


// Backend and Component Imports
import com.application.Backend.ChatController;
// Import Renderer and its inner class explicitly or via wildcard
import com.application.FrontEnd.components.MessageCellRenderer;
import com.application.FrontEnd.components.MessageCellRenderer.ChatMessage;
// Ensure custom component classes exist and are imported (use wildcard or individual)
import com.application.FrontEnd.components.*;

public class ChatRoom extends JPanel {

    // --- UI Components ---
    private CustomButton sendButton;
    private CustomButton leaveRoomButton;
    private CustomTextField chatTextField;
    private CustomLabel roomNameLabel;
    private CustomTextArea userNameArea; // Ensure this class exists
    private JPanel roomTabPanel;
    private CustomButton addNewRoomButton;
    private CustomButton emojiButton;
    private CustomButton fileSendButton;

    private JList<ChatMessage> chatList; // Use specific type
    private DefaultListModel<ChatMessage> chatListModel; // Use specific type

    // --- References & State ---
    private MainFrame mainFrame;
    private ChatController chatController;
    private String currentUserName;
    private String activeRoomName; // UI's view of the active room
    private Map<String, CustomButton> roomButtons; // Map room name to its tab button

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
        chatList.setCellRenderer(new MessageCellRenderer()); // Ensure Renderer exists
        chatList.setBackground(new Color(60, 60, 60));
        chatList.setOpaque(true);
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chatList.setFocusable(false); // Prevent focus stealing

        // --- Build the rest of the UI layout ---
        Color backgroundColor = new Color(45, 45, 45);
        this.setBackground(backgroundColor);
        this.setOpaque(true);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Right Panel (User List & Room Info) ---
        JPanel userDisplayPanel = new JPanel();
        userDisplayPanel.setLayout(new BoxLayout(userDisplayPanel, BoxLayout.Y_AXIS));
        userDisplayPanel.setOpaque(false);

        roomNameLabel = new CustomLabel("Room: "+initialRoomName, 200, 30); // Ensure CustomLabel exists
        roomNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        roomNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        roomNameLabel.setForeground(new Color(200, 200, 200));

        // Ensure CustomTextArea exists and behaves like JTextArea
        userNameArea = new CustomTextArea(null);
        userNameArea.setBackground(new Color(60, 60, 60));
        userNameArea.setForeground(new Color(220, 220, 220));
        userNameArea.setEditable(false);
        userNameArea.setLineWrap(true); // Good default for usernames
        userNameArea.setWrapStyleWord(true);
        userNameArea.append("- " + this.currentUserName + " (You)\n");

        JScrollPane userScrollPane = new JScrollPane(userNameArea);
        userScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        userScrollPane.setOpaque(false);
        userScrollPane.getViewport().setOpaque(false);
        userScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        userScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        Border userListBorder = BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY), "Online Users", TitledBorder.CENTER, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), new Color(180, 180, 180));
        userScrollPane.setBorder(userListBorder);

        userDisplayPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        userDisplayPanel.add(roomNameLabel);
        userDisplayPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        userDisplayPanel.add(userScrollPane);

        leaveRoomButton = new CustomButton("Leave App", 100, 30, new Color(255, 77, 77)); // Ensure CustomButton exists
        leaveRoomButton.setForeground(Color.WHITE);
        JPanel leaveButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        leaveButtonPanel.setOpaque(false);
        leaveButtonPanel.add(leaveRoomButton);

        JPanel rightColumnPanel = new JPanel();
        rightColumnPanel.setLayout(new BorderLayout(0, 10));
        rightColumnPanel.setOpaque(false);
        rightColumnPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        rightColumnPanel.add(userDisplayPanel, BorderLayout.CENTER);
        rightColumnPanel.add(leaveButtonPanel, BorderLayout.SOUTH);

        // --- Left Panel (Chat Area & Input) ---
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout(0, 5));
        chatPanel.setOpaque(false);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        JScrollPane chatScrollPane = new JScrollPane(chatList);
        chatScrollPane.setOpaque(false);
        chatScrollPane.getViewport().setOpaque(false);
        chatScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // chatScrollPane.setBorder(BorderFactory.createEmptyBorder()); // Titled border overrides this
        Border chatBorder = BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY), "Chat History", TitledBorder.CENTER, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), new Color(180, 180, 180));
        chatScrollPane.setBorder(chatBorder);

        // Input Area
        chatTextField = new CustomTextField(300, 30); // Ensure CustomTextField exists
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

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(emojiButton);
        buttonPanel.add(fileSendButton);
        buttonPanel.add(sendButton);
        inputPanel.add(chatTextField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // Room Tab Panel
        roomTabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        roomTabPanel.setOpaque(false);
        Border RoomNameBorder = BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY), "Rooms", TitledBorder.CENTER, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), new Color(180, 180, 180));
        roomTabPanel.setBorder(RoomNameBorder);

        // Add initial room button using the internal method
        addRoomTabInternal(initialRoomName);

        addNewRoomButton = new CustomButton("+", 50, 30, new Color(150, 100, 150));
        roomTabPanel.add(addNewRoomButton); // Add "+" button at the end

        // Scroll pane for tabs
        JScrollPane roomTabScrollPanel = new JScrollPane(roomTabPanel);
        roomTabScrollPanel.setOpaque(false);
        roomTabScrollPanel.getViewport().setOpaque(false);
        roomTabScrollPanel.setBorder(null);
        roomTabScrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        roomTabScrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollBar horizontalScrollBar = roomTabScrollPanel.getHorizontalScrollBar();
        horizontalScrollBar.setPreferredSize(new Dimension(0, 10)); // Minimal height

        // Assemble Left Panel
        chatPanel.add(roomTabScrollPanel, BorderLayout.NORTH);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        // --- Split Pane ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setRightComponent(rightColumnPanel);
        splitPane.setLeftComponent(chatPanel);
        splitPane.setDividerLocation(800); // Adjust as needed
        splitPane.setResizeWeight(0.8); // Give more weight to chat panel
        splitPane.setOpaque(false);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null); // No border for the split pane itself

        // Add split pane to the main panel
        add(splitPane, BorderLayout.CENTER);
        addEventListeners(); // Add listeners AFTER all components are created
    }


    // --- Methods Called by Controller ---

    /** Appends a single message (sender, text) to the currently visible chat list. */
    public void appendMessage(String sender, String message) {
        // Ensure executed on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            ChatMessage msg = new ChatMessage(sender, message);
            chatListModel.addElement(msg);
            // Scroll to bottom (use nested invokeLater for timing)
            SwingUtilities.invokeLater(() -> {
                int lastIndex = chatListModel.getSize() - 1;
                if (lastIndex >= 0) {
                    chatList.ensureIndexIsVisible(lastIndex);
                }
            });
        });
    }

    /** Displays a system message in the chat list. */
    public void displaySystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            ChatMessage msg = new ChatMessage("[System]", message);
            chatListModel.addElement(msg);
            SwingUtilities.invokeLater(() -> { // Nested for scroll timing
                 int lastIndex = chatListModel.getSize() - 1;
                 if (lastIndex >= 0) {
                     chatList.ensureIndexIsVisible(lastIndex);
                 }
            });
        });
    }

    /** Sets the text of the room name label. */
    public void setActiveRoomNameLabel(String roomName) {
         SwingUtilities.invokeLater(() -> {
              this.roomNameLabel.setText("Room: " + roomName);
         });
    }

    // --- Event Listeners ---
    private void addEventListeners() {
        // Send Action
        ActionListener sendAction = e -> {
            String messageText = chatTextField.getText().trim();
            if (!messageText.isEmpty()) {
                chatController.sendMessage(messageText); // Send through controller
                chatTextField.setText(""); // Clear input field
            }
        };
        sendButton.addActionListener(sendAction);
        chatTextField.addActionListener(sendAction); // Send on Enter

        // Leave Button Action
        leaveRoomButton.addActionListener(e -> chatController.leaveRoom());

        // Add New Room Button Action
        addNewRoomButton.addActionListener(e -> showAddRoomDialog());

        // Placeholder listeners
        emojiButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Emoji feature not implemented yet."));
        fileSendButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "File sending not implemented yet."));
    }

    // --- Multi-Room UI Methods ---

    /** Shows dialog to get new room name and password. Calls Controller on OK. */
    public void showAddRoomDialog() {
        JTextField roomNameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JPanel dialogPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        dialogPanel.add(new JLabel("Room Name:"));
        dialogPanel.add(roomNameField);
        dialogPanel.add(new JLabel("Password:"));
        dialogPanel.add(passwordField);
        // Focus the room name field initially on EDT
        SwingUtilities.invokeLater(roomNameField::requestFocusInWindow);

        int result = JOptionPane.showConfirmDialog(mainFrame, dialogPanel, "Join or Create New Room", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String newRoomName = roomNameField.getText().trim();
            char[] passwordChars = passwordField.getPassword();
            String newPassword = new String(passwordChars);
            java.util.Arrays.fill(passwordChars, ' '); // Clear password from memory
            if (!newRoomName.isEmpty() && !newPassword.isEmpty()) {
                System.out.println("[UI] Requesting join/switch via controller for: " + newRoomName);
                // Let the controller handle backend logic and UI callbacks
                chatController.joinOrSwitchToRoom(newRoomName, newPassword);
            } else {
                 JOptionPane.showMessageDialog(mainFrame, "Room Name and Password cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /** Adds a button for a new room tab INTERNALLY. Called on EDT. */
    private void addRoomTabInternal(String roomName) {
         if (roomButtons.containsKey(roomName)) {
             System.out.println("[UI] Tab button for " + roomName + " already exists. Skipping.");
             return; // Don't add duplicate buttons
         }

        System.out.println("[UI] Creating tab button for: " + roomName);
        Random random = new Random();
        // Ensure CustomButton class exists and constructor matches
        CustomButton newRoomTabButton = new CustomButton(roomName, 80, 30, new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
        newRoomTabButton.setForeground(Color.WHITE);
        newRoomTabButton.setFont(new Font("Segoe UI", Font.BOLD, 12)); // Smaller font for tabs

        // ActionListener triggers CONTROLLER to handle the switch
        newRoomTabButton.addActionListener(e -> {
             System.out.println("[UI] Switch room tab button clicked for: " + roomName);
             chatController.requestRoomSwitch(roomName); // Controller handles logic
        });
        roomButtons.put(roomName, newRoomTabButton); // Store button reference

        // Add the new button *before* the "+" button
        int addButtonIndex = -1;
        for(int i=0; i < roomTabPanel.getComponentCount(); i++){
            if(roomTabPanel.getComponent(i) == addNewRoomButton){
                addButtonIndex = i;
                break;
            }
        }
        if (addButtonIndex == -1) addButtonIndex = roomTabPanel.getComponentCount(); // Add at end if "+" not found

        roomTabPanel.add(newRoomTabButton, addButtonIndex);

        // Revalidate and repaint the panel containing the tabs
        roomTabPanel.revalidate();
        roomTabPanel.repaint();
        System.out.println("[UI] Tab button added and panel revalidated for: " + roomName);
    }

     /** Public method called by Controller to add tab, ensuring it runs on EDT. */
     public void addRoomTab(String roomName) {
          System.out.println("[UI] Received request to add tab for: " + roomName);
          SwingUtilities.invokeLater(() -> addRoomTabInternal(roomName));
     }

    /**
     * Updates the UI when switching to a different room view.
     * Called by Controller (typically from onConnected or after password prompt).
     * Ensures execution on the Event Dispatch Thread.
     */
    public void updateUIForRoomSwitch(String newActiveRoom) {
         SwingUtilities.invokeLater(() -> { // Ensure all UI updates happen on the EDT
             System.out.println("[UI] Updating UI view for room switch to: " + newActiveRoom);
             this.activeRoomName = newActiveRoom; // Update internal state
             this.setActiveRoomNameLabel(newActiveRoom); // Update room name display

             // --- HISTORY RESTORATION ---
             System.out.println("[UI] Clearing chat list model.");
             this.chatListModel.clear(); // Clear the visual list

             System.out.println("[UI] Requesting history from controller for: " + newActiveRoom);
             if (chatController == null) { // Safety check
                  System.err.println("[UI] Error: ChatController is null in updateUIForRoomSwitch!");
                  return;
             }
             // Get history (Controller provides a copy)
             List<ChatMessage> history = chatController.getChatHistory(newActiveRoom);
             System.out.println("[UI] Received " + history.size() + " messages from history for " + newActiveRoom);

             // Repopulate the list model
             for (ChatMessage msg : history) {
                 this.chatListModel.addElement(msg);
             }
             System.out.println("[UI] Repopulated chat list model.");
             // --- END HISTORY RESTORATION ---

             // Update tab button highlighting
             System.out.println("[UI] Updating tab button highlighting.");
             roomButtons.forEach((name, button) -> {
                 // Check if button reference is not null before setting border
                 if(button != null) {
                     if (name.equals(newActiveRoom)) {
                          button.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2)); // Highlight active
                      } else {
                          button.setBorder(UIManager.getBorder("Button.border")); // Reset others
                      }
                 } else {
                      System.err.println("[UI] Warning: Found null button reference for room '" + name + "' in roomButtons map.");
                 }
             });

             // Scroll to the bottom after repopulating (nested invokeLater for timing)
             SwingUtilities.invokeLater(() -> {
                int lastIndex = chatListModel.getSize() - 1;
                if (lastIndex >= 0) {
                    chatList.ensureIndexIsVisible(lastIndex);
                }
             });
            System.out.println("[UI] Finished updating UI for room switch.");
         });
    }

    // --- Setters ---
    public void setChatController(ChatController controller) { this.chatController = controller; }
    public void setMainFrame(MainFrame frame) { this.mainFrame = frame; }

    // --- Inner Class for Background (Ensure it uses correct imports) ---
    // Included as requested - verify image loading path and error handling
    private class BackgroundGifPanel extends JPanel {
        private Image gifImage;
        private String errorMessage = null;
        private String imagePathUsed;

        public BackgroundGifPanel(String imagePath) {
            this.imagePathUsed = imagePath;
            try {
                URL gifUrl = getClass().getResource(imagePath); // Use class loader resource lookup
                if (gifUrl != null) {
                    ImageIcon icon = new ImageIcon(gifUrl);
                    // Check if the image loaded correctly (basic check)
                    if (icon.getIconWidth() == -1 || icon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                         this.errorMessage = "Failed to load GIF: ImageIcon status error";
                         System.err.println("[BackgroundGifPanel] " + this.errorMessage + " (" + imagePath + ")");
                         this.gifImage = null;
                    } else {
                        this.gifImage = icon.getImage(); // Get the Image object
                        System.out.println("[BackgroundGifPanel] GIF Image loaded successfully from: " + imagePath);
                    }
                } else {
                    this.errorMessage = "GIF resource not found";
                    System.err.println("[BackgroundGifPanel] " + this.errorMessage + " (" + imagePath + ")");
                    this.gifImage = null;
                }
            } catch (Exception e) {
                this.errorMessage = "Exception loading GIF: " + e.getMessage();
                System.err.println("[BackgroundGifPanel] " + this.errorMessage + " (" + imagePath + ")");
                e.printStackTrace();
                this.gifImage = null;
            }
             setOpaque(false); // Usually false if drawing background image fully
        }

        @Override
        protected void paintComponent(Graphics g) {
            // Don't call super.paintComponent(g) if opaque is false and you paint the full area
            // super.paintComponent(g);

            if (gifImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                // Optional: Better scaling quality
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                // Draw the image, scaling it to the component's current width and height.
                // 'this' as ImageObserver ensures animation repaint.
                g2d.drawImage(gifImage, 0, 0, getWidth(), getHeight(), this);
                g2d.dispose();
            } else {
                 // Draw fallback background and error message
                 // Paint parent's background first if Opaque is false for proper layering
                 // if (getParent() != null) {
                 //    g.setColor(getParent().getBackground());
                 //    g.fillRect(0, 0, getWidth(), getHeight());
                 // }
                g.setColor(new Color(77, 0, 77)); // Dark purple fallback
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.YELLOW);
                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                String text = "BG Load Error: " + (errorMessage != null ? errorMessage : "Unknown");
                FontMetrics fm = g.getFontMetrics();
                int msgWidth = fm.stringWidth(text);
                // Basic centering
                g.drawString(text, Math.max(5, (getWidth() - msgWidth) / 2) , getHeight() / 2 + fm.getAscent() / 2 - fm.getHeight()/2);
                String pathText = "(" + imagePathUsed + ")";
                msgWidth = fm.stringWidth(pathText);
                g.drawString(pathText, Math.max(5, (getWidth() - msgWidth) / 2) , getHeight() / 2 + fm.getAscent() / 2 + fm.getHeight()/2);
            }
        }
    } // End BackgroundGifPanel inner class

} // End ChatRoom class