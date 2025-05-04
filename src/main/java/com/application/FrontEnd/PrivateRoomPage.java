package com.application.FrontEnd;

import com.application.FrontEnd.components.*; // Keep your custom components

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.imageio.ImageIO; // Needed for static background image
import java.io.IOException;   // Needed for static background image
import javax.swing.*;
import javax.swing.border.MatteBorder; // For underline border
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class PrivateRoomPage extends JPanel {

    // --- UI Components ---
    private CustomTextField roomNameField;
    private JPasswordField passwordField; // Using JPasswordField is better for passwords
    private CustomButton createButton;
    private CustomButton joinButton;
    private JButton backButton;
    private JLabel versionLabel;
    private JPanel formPanel; // Panel holding centered form elements

    // --- References ---
    private MainFrame mainFrame;
    private String currentUserName; // Store the username passed from LoginPage

    // --- Fields for Layered Pane ---
    private JLayeredPane layeredPane;
    private BackgroundImagePanel backgroundPanel; // Use static image background

    // --- Resource Paths (using LoginPage paths for consistency) ---
    // *** USE THE SAME BACKGROUND AS LOGIN PAGE ***
    private static final String BACKGROUND_IMAGE_PATH = "/com/application/FrontEnd/images/BG_PublicRooms.png";
    private static final String BACK_ICON_PATH = "/com/application/FrontEnd/images/ICON_Back.png"; // Make sure this exists
    // No logo needed here

    // --- Constructor ---
    public PrivateRoomPage(MainFrame mainFrame, String userName) {
        this.mainFrame = mainFrame;
        this.currentUserName = userName; // Store the username

        // Main panel uses BorderLayout to hold the LayeredPane
        setLayout(new BorderLayout());

        // Initialize LayeredPane
        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        // Initialize and add Background Panel (Bottom Layer)
        backgroundPanel = new BackgroundImagePanel(BACKGROUND_IMAGE_PATH);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER); // Layer 0

        // Initialize and add Form Panel (Top Layer)
        createFormPanel(); // Helper method to build the form panel
        layeredPane.add(formPanel, JLayeredPane.PALETTE_LAYER); // Layer ~100

        // Initialize and add absolutely positioned components (Highest Layer)
        backButton = createIconButton(BACK_ICON_PATH, "<", "Go Back");
        layeredPane.add(backButton, JLayeredPane.MODAL_LAYER); // Layer ~200

        versionLabel = new JLabel("v1.0.0");
        versionLabel.setForeground(Color.DARK_GRAY); // Style like LoginPage
        versionLabel.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(10f) : new Font("SansSerif", Font.BOLD, 10));
        layeredPane.add(versionLabel, JLayeredPane.MODAL_LAYER);

        // Add resize listener to position components
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeAndCenterComponents();
            }
            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(PrivateRoomPage.this::resizeAndCenterComponents);
            }
        });

        // Attach Event Listeners
        addEventListeners();
    }

    /** Creates and configures the transparent form panel with its components. */
    private void createFormPanel() {
        formPanel = new JPanel(new GridBagLayout()); // Use GridBagLayout
        formPanel.setOpaque(false); // Make transparent

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER; // Each main component on a new line
        gbc.anchor = GridBagConstraints.CENTER;     // Center components horizontally
        gbc.insets = new Insets(10, 15, 10, 15); // Default padding

        // --- Form Components ---

        // Title Label
        JLabel titleLabel = new JLabel("Private Rooms");
        titleLabel.setForeground(Color.BLACK); // Dark text on light background
        // Use Sansation Bold if available, fallback otherwise
        Font titleFont = MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(28f) : new Font("SansSerif", Font.BOLD, 28);
        titleLabel.setFont(titleFont);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0; gbc.insets = new Insets(20, 15, 30, 15); // Top padding, more bottom padding
        formPanel.add(titleLabel, gbc);

        // Room Name Label
        JLabel labelRoomName = new JLabel("Room Name:");
        labelRoomName.setForeground(Color.BLACK);
        labelRoomName.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(15f) : new Font("SansSerif", Font.PLAIN, 15));
        gbc.gridy = 1; gbc.insets = new Insets(5, 15, 2, 15); // Reduced padding
        gbc.anchor = GridBagConstraints.LINE_START; // Align label left
        gbc.gridwidth = 1; // Take one cell
        formPanel.add(labelRoomName, gbc);

        // Room Name Text Field (Underline style)
        roomNameField = new CustomTextField(250, 35); // Consistent size
        roomNameField.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(16f) : new Font("SansSerif", Font.PLAIN, 16));
        roomNameField.setForeground(Color.BLACK);
        roomNameField.setCaretColor(Color.BLACK);
        roomNameField.setOpaque(false);
        roomNameField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK)); // Underline
        gbc.gridy = 2; gbc.insets = new Insets(0, 15, 15, 15); // Padding below field
        gbc.anchor = GridBagConstraints.CENTER; // Center field below label
        gbc.gridwidth = GridBagConstraints.REMAINDER; // Take rest of line
        gbc.fill = GridBagConstraints.HORIZONTAL; // Allow to stretch slightly
        gbc.weightx = 0.1;
        formPanel.add(roomNameField, gbc);
        gbc.fill = GridBagConstraints.NONE; // Reset fill
        gbc.weightx = 0.0;

        // Room Password Label
        JLabel labelPassword = new JLabel("Room Password:");
        labelPassword.setForeground(Color.BLACK);
        labelPassword.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(15f) : new Font("SansSerif", Font.PLAIN, 15));
        gbc.gridy = 3; gbc.insets = new Insets(5, 15, 2, 15);
        gbc.anchor = GridBagConstraints.LINE_START; // Align label left
        gbc.gridwidth = 1;
        formPanel.add(labelPassword, gbc);

        // Password Field (Underline style)
        passwordField = new JPasswordField(); // Use standard password field
        passwordField.setPreferredSize(new Dimension(250, 35)); // Set size
        passwordField.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(16f) : new Font("SansSerif", Font.PLAIN, 16));
        passwordField.setForeground(Color.BLACK);
        passwordField.setCaretColor(Color.BLACK);
        passwordField.setOpaque(false);
        passwordField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK)); // Underline
        gbc.gridy = 4; gbc.insets = new Insets(0, 15, 30, 15); // More space below password
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.1;
        formPanel.add(passwordField, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;

        // Create/Join Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); // Center buttons with gap
        buttonPanel.setOpaque(false); // Transparent panel

        // Style buttons like LoginPage (Create = light, Join = blue)
        createButton = new CustomButton("Create", 150, 45, new Color(240, 240, 240)); // Adjusted text, size, color
        createButton.setForeground(Color.BLACK);
        createButton.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(14f) : new Font("SansSerif", Font.BOLD, 14));

        joinButton = new CustomButton("Join", 150, 45, new Color(90, 120, 180)); // Adjusted text, size, color
        joinButton.setForeground(Color.WHITE);
        joinButton.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(14f) : new Font("SansSerif", Font.BOLD, 14));

        buttonPanel.add(createButton);
        buttonPanel.add(joinButton);
        gbc.gridy = 5; gbc.insets = new Insets(10, 15, 20, 15); // Space below buttons
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        formPanel.add(buttonPanel, gbc);

        // Set preferred size for the form panel to guide centering
        formPanel.setPreferredSize(new Dimension(400, 450)); // Adjust height as needed
    }

    /** Helper to create icon buttons (Copied from LoginPage) */
    private JButton createIconButton(String iconPath, String fallbackText, String tooltip) {
        JButton button = new JButton();
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Use a slightly larger size for the back arrow if desired
        Dimension iconButtonSize = new Dimension(40, 40); // Adjust size if needed
        button.setPreferredSize(iconButtonSize);
        button.setMinimumSize(iconButtonSize);
        button.setMaximumSize(iconButtonSize);

        try {
            URL iconUrl = getClass().getResource(iconPath);
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                if (icon.getIconWidth() > 0) {
                    // Scale icon nicely to fit button (adjust scaled size if needed)
                    Image scaledImage = icon.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
                    button.setIcon(new ImageIcon(scaledImage));
                } else { throw new IOException("Icon ImageIcon invalid: " + iconPath); }
            } else { throw new IOException("Icon resource not found: " + iconPath); }
        } catch (Exception ex) {
            System.err.println("Warning: Could not load icon " + iconPath + ". Using text fallback. " + ex.getMessage());
            button.setText(fallbackText);
            button.setFont(new Font("SansSerif", Font.BOLD, 20)); // Make fallback text visible
            button.setForeground(Color.BLACK); // Fallback text color
        }
        return button;
    }


    /** Recalculates bounds for layered components on resize (Adapted from LoginPage) */
    private void resizeAndCenterComponents() {
        SwingUtilities.invokeLater(() -> {
            int layeredWidth = layeredPane.getWidth();
            int layeredHeight = layeredPane.getHeight();
            if (layeredWidth <= 0 || layeredHeight <= 0) return;

            // Resize background
            if (backgroundPanel != null) backgroundPanel.setBounds(0, 0, layeredWidth, layeredHeight);

            // Center form panel
            if (formPanel != null) {
                Dimension formPrefSize = formPanel.getPreferredSize();
                int formW = Math.min(formPrefSize.width, layeredWidth - 40); // Leave margins
                int formH = Math.min(formPrefSize.height, layeredHeight - 40);
                int x = (layeredWidth - formW) / 2;
                int y = (layeredHeight - formH) / 2;
                formPanel.setBounds(x, y, formW, formH);
                // Need to revalidate the formPanel itself if its size changes relative to contents
                formPanel.revalidate();
            }

            // Position version label bottom-right
            if (versionLabel != null) {
                Dimension labelSize = versionLabel.getPreferredSize();
                int x = layeredWidth - labelSize.width - 15;
                int y = layeredHeight - labelSize.height - 10;
                versionLabel.setBounds(x, y, labelSize.width, labelSize.height);
            }

            // Position back button top-left
            if (backButton != null) {
                Dimension btnSize = backButton.getPreferredSize();
                backButton.setBounds(15, 15, btnSize.width, btnSize.height); // Standard padding
            }

            layeredPane.revalidate();
            layeredPane.repaint();
        });
    }

    /** Attaches listeners to interactive components */
    public void addEventListeners() {
        // Action for Back button
        backButton.addActionListener(e -> {
            System.out.println("Back button clicked on PrivateRoomPage");
            mainFrame.switchToLoginPage(); // Go back to Login Page
        });

        // Action for Create button
        createButton.addActionListener(e -> {
            String roomName = roomNameField.getText().trim();
            String password = new String(passwordField.getPassword()); // Get password safely

            if (currentUserName.isEmpty() || roomName.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username, Room Name, and Password cannot be empty.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            System.out.println("Attempting to Create/Join room '" + roomName + "' for user '" + currentUserName + "'");
            // TODO: Implement actual room creation logic (likely via ChatController)
            // For now, just switch to ChatRoom UI assuming creation/join success
            // This assumes creation automatically joins. Adjust if backend logic differs.
            // Pass password to controller if needed for creation/initial key derivation
            mainFrame.getChatController().joinInitialRoom(currentUserName, roomName, password);

            // Clear password field after use
            passwordField.setText("");
        });

        // Action for Join button
        joinButton.addActionListener(e -> {
            String roomName = roomNameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (currentUserName.isEmpty() || roomName.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username, Room Name, and Password cannot be empty.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            System.out.println("Attempting to Join room '" + roomName + "' for user '" + currentUserName + "'");
            // TODO: Implement actual room joining logic/validation (via ChatController)
            // For now, just switch to ChatRoom UI assuming join success
            mainFrame.getChatController().joinInitialRoom(currentUserName, roomName, password);

            // Clear password field after use
            passwordField.setText("");
        });

        // Optional: Pressing Enter in password field triggers Join button
        passwordField.addActionListener(e -> joinButton.doClick());
        // Optional: Pressing Enter in room name field moves focus to password
        roomNameField.addActionListener(e -> passwordField.requestFocusInWindow());
    }

    // --- Inner Class for Background Image Panel (Copied DIRECTLY from LoginPage) ---
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
                    System.out.println("[BGPanel] Loaded: " + imagePath + " for PrivateRoomPage");
                } else { throw new IOException("Resource not found: " + imagePath); }
            } catch (IOException e) {
                this.errorMessage = "Ex loading background: " + e.getMessage();
                System.err.println(errorMessage); this.backgroundImage = null;
            }
            setOpaque(true); // Background panel must be opaque
        }

        @Override
        protected void paintComponent(Graphics g) {
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
                g.setColor(new Color(30, 50, 70)); // Dark blue fallback
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
    } // --- End Inner Class ---

} // End PrivateRoomPage class