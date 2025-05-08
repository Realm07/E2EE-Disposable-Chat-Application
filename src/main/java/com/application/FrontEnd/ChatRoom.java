package com.application.FrontEnd;

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
import java.net.URL; 
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.imageio.ImageIO; 
import java.io.IOException; 
import java.awt.event.*;

import com.application.Backend.ChatController;
import com.application.FrontEnd.components.MessageCellRenderer.ChatMessage;
import com.application.FrontEnd.components.*;

public class ChatRoom extends JPanel {

    // --- UI Components ---
    private CustomButton sendButton;
    private CustomButton leaveRoomButton;
    private CustomTextField chatTextField;
    private CustomLabel roomNameLabel;
    private CustomTextArea userNameArea; 
    private JPanel roomTabPanel;
    private CustomButton addNewRoomButton;
    private CustomButton emojiButton;
    private CustomButton fileSendButton;

    private JPanel mainContentPanel;
    private JLayeredPane layeredPane;
    private BackgroundImagePanel backgroundPanel; 
    

    private JList<ChatMessage> chatList; 
    private DefaultListModel<ChatMessage> chatListModel; 

    private static final String LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/gear.png";
    private static final String BACKGROUND_IMAGE_PATH = "/com/application/FrontEnd/images/BG_LoginPage.jpg";

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

        // --- Right Panel (User List & Room Info) ---
        JPanel userDisplayPanel = new JPanel();
        userDisplayPanel.setLayout(new BoxLayout(userDisplayPanel, BoxLayout.Y_AXIS));
        userDisplayPanel.setOpaque(false);

        roomNameLabel = new CustomLabel("Room: "+activeRoomName, 200, 30); // Ensure CustomLabel exists
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
        Border userListBorder = BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(Color.GRAY, Color.DARK_GRAY), "Online Users", TitledBorder.CENTER, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 15), new Color(180, 180, 180));
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

        JLabel systemButtonLabel = createLogoLabel();
        


        JPanel rightColumnPanel = new JPanel();
        rightColumnPanel.setLayout(new BorderLayout(0, 10));
        rightColumnPanel.setOpaque(false);
        rightColumnPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        rightColumnPanel.add(systemButtonLabel, BorderLayout.NORTH);
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
        // roomTabPanel.setOpaque(false);
        roomTabPanel.setBackground(new Color(115, 115, 115));
        roomTabPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        addRoomTabInternal(activeRoomName);

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
        splitPane.setDividerSize(0);

        mainContentPanel.add(splitPane, BorderLayout.CENTER);
    }
    
    // Logo
    private JLabel createLogoLabel() {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
    
        // Set opaque and background for the label itself
        label.setOpaque(true);
        label.setBackground(new Color(128, 128, 128)); // Your desired background
    
        // Add padding (EmptyBorder)
        label.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5)); // 5px padding all around
    
        try {
            URL imgUrl = getClass().getResource(LOGO_IMAGE_PATH);
            if (imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                if (icon.getIconWidth() > 0) {
                    Image scaledImage = icon.getImage().getScaledInstance(
                            30,
                            -1,
                            Image.SCALE_SMOOTH
                    );
                    label.setIcon(new ImageIcon(scaledImage));
                    // REMOVE THIS LINE: label.setPreferredSize(new Dimension(...));
                } else throw new Exception("Logo ImageIcon invalid");
            } else { throw new Exception("Logo resource not found"); }
        } catch (Exception e) {
            System.err.println("ERROR loading logo (" + LOGO_IMAGE_PATH + "): " + e.getMessage());
            label.setText("AnonChat");
            label.setForeground(Color.DARK_GRAY);
            label.setFont(MainFrame.sansationBold.deriveFont(24f));
        }
        return label;
    }

    // --- Methods Called by Controller ---

    ////////////////////
    /// 
    private void resizeAndLayoutLayeredComponents() {
        SwingUtilities.invokeLater(() -> {
            int layeredWidth = layeredPane.getWidth();
            int layeredHeight = layeredPane.getHeight();
            if (layeredWidth <= 0 || layeredHeight <= 0) return;

            // Resize background to fill the entire layered pane
            if (backgroundPanel != null) {
                backgroundPanel.setBounds(0, 0, layeredWidth, layeredHeight);
            }

            // Resize main content panel to fill the entire layered pane
            if (mainContentPanel != null) {
                mainContentPanel.setBounds(0, 0, layeredWidth, layeredHeight);
            }

            layeredPane.revalidate();
            layeredPane.repaint();
        });
    }
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
        CustomButton newRoomTabButton = new CustomButton(roomName, 80, 30, new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255), 120));
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

             System.out.println("[UI] Clearing chat list model.");
             this.chatListModel.clear(); 

             System.out.println("[UI] Requesting history from controller for: " + newActiveRoom);
             if (chatController == null) { 
                  System.err.println("[UI] Error: ChatController is null in updateUIForRoomSwitch!");
                  return;
             }
             List<ChatMessage> history = chatController.getChatHistory(newActiveRoom);
             System.out.println("[UI] Received " + history.size() + " messages from history for " + newActiveRoom);

             for (ChatMessage msg : history) {
                 this.chatListModel.addElement(msg);
             }
             System.out.println("[UI] Repopulated chat list model.");

             System.out.println("[UI] Updating tab button highlighting.");
             roomButtons.forEach((name, button) -> {
                 if(button != null) {
                     if (name.equals(newActiveRoom)) {
                          button.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2)); 
                      } else {
                          button.setBorder(UIManager.getBorder("Button.border")); 
                      }
                 } else {
                      System.err.println("[UI] Warning: Found null button reference for room '" + name + "' in roomButtons map.");
                 }
             });

             SwingUtilities.invokeLater(() -> {
                int lastIndex = chatListModel.getSize() - 1;
                if (lastIndex >= 0) {
                    chatList.ensureIndexIsVisible(lastIndex);
                }
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

        public BackgroundImagePanel(String imagePath) {
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
            // IMPORTANT: Background panel MUST be opaque IF it's the base layer covering everything.
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Crucial for opaque=true to clear background
            if (backgroundImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                // --- Draw image scaled to COVER ---
                int panelW = getWidth(); int panelH = getHeight();
                int imgW = backgroundImage.getWidth(this); int imgH = backgroundImage.getHeight(this);
                if(imgW <=0 || imgH <= 0) { g2d.dispose(); return; } // Basic check
                double imgAspect = (double) imgW / imgH; double panelAspect = (double) panelW / panelH;
                int drawW, drawH, drawX, drawY;
                if (panelAspect > imgAspect) { drawW = panelW; drawH = (int)(panelW / imgAspect); drawX = 0; drawY = (panelH - drawH) / 2; }
                else { drawH = panelH; drawW = (int)(panelH * imgAspect); drawX = (panelW - drawW) / 2; drawY = 0; }
                // Use interpolation for better quality when scaling
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(backgroundImage, drawX, drawY, drawW, drawH, this);
                // --- End Cover Scaling ---
                g2d.dispose();
            } else {
                // Draw fallback background and error message
                g.setColor(new Color(30, 50, 70)); // Dark blue fallback
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.YELLOW); g.setFont(new Font("SansSerif", Font.BOLD, 14));
                FontMetrics fm = g.getFontMetrics();
                String text = "BG Load Error: " + (errorMessage != null ? errorMessage : "Unknown");
                int msgWidth = fm.stringWidth(text);
                g.drawString(text, Math.max(5, (getWidth() - msgWidth) / 2), getHeight() / 2 + fm.getAscent() / 2 - fm.getHeight()/2); // Center slightly better
                String pathText = "(" + imagePathUsed + ")";
                msgWidth = fm.stringWidth(pathText);
                g.drawString(pathText, Math.max(5, (getWidth() - msgWidth) / 2), getHeight() / 2 + fm.getAscent() / 2 + fm.getHeight()/2);
            }
        }
    } // --- End Inner Class ---

} // End ChatRoom class