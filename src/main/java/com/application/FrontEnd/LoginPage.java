// src/main/java/com/application/FrontEnd/LoginPage.java
package com.application.FrontEnd;

// Backend/Component Imports
import com.application.Backend.ChatController;
import com.application.FrontEnd.components.CustomTextField; // Using your custom field
import com.application.FrontEnd.components.CustomButton; // Using your custom button

// Standard Java Swing and AWT imports
import javax.swing.*;
import javax.swing.border.MatteBorder; // For underline border
import java.awt.*;
import java.awt.event.*;
import javax.imageio.ImageIO; // For loading static images
import java.io.IOException;   // For ImageIO exceptions
import java.net.URL;        // For loading resources
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.border.Border; // Import Border
import javax.swing.border.CompoundBorder; // Import CompoundBorder
import javax.swing.border.EmptyBorder;

/**
 * Login Page UI with layered background image (BG_LoginPage.jpg), logo,
 * username input, and buttons for Public/Private Rooms.
 */
public class LoginPage extends JPanel {

    // --- UI Components ---
    private CustomTextField userNameField;
    private CustomButton publicRoomButton; // Re-added
    private CustomButton privateRoomButton;// Re-added
    private JButton infoButton;            // Top-left 'i' icon button
    private JLabel aboutLabel;
    private JLabel versionLabel;
    private JPanel formPanel;              // Panel holding centered form elements

    // --- References ---
    private MainFrame mainFrame;
    private ChatController chatController;

    // --- Fields for Layered Pane ---
    private JLayeredPane layeredPane;
    private BackgroundImagePanel backgroundPanel; // Inner class handles background

    // --- Resource Paths ---
    private static final String BACKGROUND_IMAGE_PATH = "/com/application/FrontEnd/images/BG_LoginPage.jpg"; // Correct JPG background
    private static final String LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/ICON_Logo.png";
    private static final String INFO_ICON_PATH = "/com/application/FrontEnd/images/ICON_Info.png"; // 'i' icon path
    // No submit arrow needed private static final String SUBMIT_ICON_PATH = "/com/application/FrontEnd/images/ICON_Front.png";

    // --- Constructor ---
    public LoginPage(MainFrame mainFrame, ChatController chatController) {
        this.mainFrame = mainFrame;
        this.chatController = chatController;

        // Main panel uses BorderLayout to hold the LayeredPane
        setLayout(new BorderLayout());

        // Initialize LayeredPane for background/foreground separation
        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        // Initialize and add Background Panel (Bottom Layer)
        backgroundPanel = new BackgroundImagePanel(BACKGROUND_IMAGE_PATH);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER); // Layer 0

        // Initialize and add Form Panel (Top Layer)
        createFormPanel(); // Use helper method to build the form panel
        layeredPane.add(formPanel, JLayeredPane.PALETTE_LAYER); // Layer ~100

        // Initialize and add absolutely positioned components (Highest Layer)
        infoButton = createIconButton(INFO_ICON_PATH, "i", "Info");
        layeredPane.add(infoButton, JLayeredPane.MODAL_LAYER); // Layer ~200

        versionLabel = new JLabel("v1.0.0");
        versionLabel.setForeground(Color.DARK_GRAY); // Style as before
        versionLabel.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(10f) : new Font("SansSerif", Font.BOLD, 10));
        layeredPane.add(versionLabel, JLayeredPane.MODAL_LAYER);

        // Add resize listener to correctly position components within layered pane
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeAndCenterComponents();
            }
            @Override
            public void componentShown(ComponentEvent e) {
                // Initial positioning after UI is shown
                SwingUtilities.invokeLater(LoginPage.this::resizeAndCenterComponents);
            }
        });

        // Attach Event Listeners to buttons etc.
        addEventListeners();
    }

    /** Creates and configures the transparent form panel with its components. */
    private void createFormPanel() {
        formPanel = new JPanel(new GridBagLayout()); // Use GridBagLayout
        formPanel.setOpaque(false); // Make transparent to see background through it

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER; // Each main component on a new line
        gbc.anchor = GridBagConstraints.CENTER;     // Center components horizontally
        gbc.insets = new Insets(5, 10, 5, 10); // Default padding

        // --- Form Components ---

        // Logo (Uses helper)
        JLabel logoLabel = createLogoLabel();
        gbc.gridy = 0; gbc.insets = new Insets(30, 10, 5, 10); // Adjust padding
        formPanel.add(logoLabel, gbc);

        // App Name
        JLabel appNameLabel = new JLabel("A N O C H A T");
        appNameLabel.setForeground(Color.BLACK);
        appNameLabel.setFont(MainFrame.sansationBold.deriveFont(36f));
        appNameLabel.setBorder(BorderFactory.createMatteBorder(0,0,1,0, Color.BLACK));
        gbc.gridy = 1; gbc.insets = new Insets(5, 10, 35, 10); // Adjust padding
        formPanel.add(appNameLabel, gbc);

        // Username Label
        JLabel labelUserName = new JLabel("Enter Username:");
        labelUserName.setForeground(Color.BLACK);
        labelUserName.setFont(MainFrame.sansationRegular.deriveFont(15f));
        gbc.gridy = 2; gbc.insets = new Insets(0, 10, 8, 10);
        formPanel.add(labelUserName, gbc);

        // Username Text Field (Underline style)
        userNameField = new CustomTextField(150, 35); // Keep constructor size
        userNameField.setFont(MainFrame.sansationRegular.deriveFont(16f));
        userNameField.setHorizontalAlignment(SwingConstants.CENTER); // Center text
        userNameField.setForeground(Color.BLACK);
        userNameField.setCaretColor(Color.BLACK);
        userNameField.setOpaque(false);

        int horizontalPadding = 30; // <<-- Adjust this value to change the line length
        Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK); // The 1px bottom line
        Border padding = BorderFactory.createEmptyBorder(0, horizontalPadding, 0, horizontalPadding); // Left/Right padding
        // Combine them: the padding is outside, the line is inside the padded area
        userNameField.setBorder(BorderFactory.createCompoundBorder(padding, line));


        gbc.gridy = 3; gbc.insets = new Insets(0, 10, 30, 10); // Space below field
        // Allow field to stretch a bit horizontally if needed by GridBagLayout
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.1; // Give it slight weight to expand if needed
        formPanel.add(userNameField, gbc);
        gbc.fill = GridBagConstraints.NONE; // Reset fill
        gbc.weightx = 0.0; // Reset weight

        // Public/Private Room Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); // Center buttons with gap
        buttonPanel.setOpaque(false); // Transparent panel

        publicRoomButton = new CustomButton("Public Rooms", 160, 45, new Color(240, 240, 240));
        publicRoomButton.setForeground(Color.BLACK);
        publicRoomButton.setFont(MainFrame.sansationBold.deriveFont(14f));

        privateRoomButton = new CustomButton("Private Room", 160, 45, new Color(90, 120, 180));
        privateRoomButton.setForeground(Color.WHITE);
        privateRoomButton.setFont(MainFrame.sansationBold.deriveFont(14f));

        buttonPanel.add(publicRoomButton);
        buttonPanel.add(privateRoomButton);
        gbc.gridy = 4; gbc.insets = new Insets(0, 10, 50, 10); // Space below buttons
        formPanel.add(buttonPanel, gbc);


        // About Label (near bottom)
        aboutLabel = new JLabel("About");
        aboutLabel.setForeground(Color.BLACK);
        aboutLabel.setFont(MainFrame.sansationRegular.deriveFont(14f));
        aboutLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        aboutLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showInfoDialog(); }
        });
        gbc.gridy = 5; gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weighty = 1.0; // Take up remaining vertical space below
        gbc.anchor = GridBagConstraints.PAGE_END; // Anchor towards bottom of space
        formPanel.add(aboutLabel, gbc);

        // Set preferred size for the form panel to guide centering
        formPanel.setPreferredSize(new Dimension(400, 500));
    }


    /** Helper to create icon buttons */
    private JButton createIconButton(String iconPath, String fallbackText, String tooltip) {
        JButton button = new JButton();
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension iconButtonSize = new Dimension(32, 32); // Increased size slightly
        button.setPreferredSize(iconButtonSize);
        button.setMinimumSize(iconButtonSize);
        button.setMaximumSize(iconButtonSize);

        try {
            URL iconUrl = getClass().getResource(iconPath);
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                if (icon.getIconWidth() > 0) {
                    // Scale icon to fit button - CHANGE SCALING HINT HERE
                    Image scaledImage = icon.getImage().getScaledInstance(
                            26,                          // target width
                            26,                          // target height
                            Image.SCALE_SMOOTH        // <<--- Use REPLICATE for sharpness
                    );
                    button.setIcon(new ImageIcon(scaledImage));
                } else { throw new IOException("Icon ImageIcon invalid: " + iconPath); }
            } else { throw new IOException("Icon resource not found: " + iconPath); }
        } catch (Exception ex) {
            System.err.println("Warning: Could not load icon " + iconPath + ". Using text fallback. " + ex.getMessage());
            button.setText(fallbackText);
            button.setFont(new Font("SansSerif", Font.BOLD, 18));
            button.setForeground(Color.BLACK); // Fallback text color
        }
        return button;
    }


    /** Helper to create the logo label, scaled */
    private JLabel createLogoLabel() {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        try {
            URL imgUrl = getClass().getResource(LOGO_IMAGE_PATH);
            if (imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                if (icon.getIconWidth() > 0) {
                    // Scale logo based on width - CHANGE SCALING HINT HERE
                    Image scaledImage = icon.getImage().getScaledInstance(
                            120,                         // target width (-1 for height maintains aspect ratio)
                            -1,                          // target height
                            Image.SCALE_SMOOTH     // <<--- Use REPLICATE for sharpness
                    );
                    label.setIcon(new ImageIcon(scaledImage));
                    // Set preferred size based on scaled icon to help layout
                    label.setPreferredSize(new Dimension(((ImageIcon)label.getIcon()).getIconWidth(), ((ImageIcon)label.getIcon()).getIconHeight()));
                } else throw new Exception("Logo ImageIcon invalid");
            } else { throw new Exception("Logo resource not found"); }
        } catch (Exception e) {
            System.err.println("ERROR loading logo (" + LOGO_IMAGE_PATH + "): " + e.getMessage());
            label.setText("AnonChat"); label.setForeground(Color.DARK_GRAY);
            label.setFont(MainFrame.sansationBold.deriveFont(24f));
            label.setPreferredSize(new Dimension(150, 60));
        }
        return label;
    }

    /** Recalculates bounds for layered components on resize */
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
                int formW = Math.min(formPrefSize.width, layeredWidth - 40);
                int formH = Math.min(formPrefSize.height, layeredHeight - 40);
                int x = (layeredWidth - formW) / 2;
                int y = (layeredHeight - formH) / 2;
                formPanel.setBounds(x, y, formW, formH);
            }

            // Position version label bottom-right
            if (versionLabel != null) {
                Dimension labelSize = versionLabel.getPreferredSize();
                int x = layeredWidth - labelSize.width - 15; int y = layeredHeight - labelSize.height - 10;
                versionLabel.setBounds(x, y, labelSize.width, labelSize.height);
            }

            // Position info button top-left
            if (infoButton != null) {
                Dimension btnSize = infoButton.getPreferredSize();
                infoButton.setBounds(15, 15, btnSize.width, btnSize.height);
            }

            layeredPane.revalidate();
            layeredPane.repaint();
        });
    }


    /** Attaches listeners to interactive components */
    public void addEventListeners() {
        // Action for Public Room button
        publicRoomButton.addActionListener(e -> {
            String currentUserName = userNameField.getText().trim();
            if (currentUserName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a User Name.", "Input Required", JOptionPane.WARNING_MESSAGE);
                userNameField.requestFocusInWindow();
                return;
            }
            System.out.println("User '" + currentUserName + "' navigating to Public Rooms.");
            mainFrame.switchToPublicRoom(currentUserName);
        });

        // Action for Private Room button
        privateRoomButton.addActionListener(e -> {
            String currentUserName = userNameField.getText().trim();
            if (currentUserName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a User Name.", "Input Required", JOptionPane.WARNING_MESSAGE);
                userNameField.requestFocusInWindow();
                return;
            }
            System.out.println("User '" + currentUserName + "' navigating to Private Room entry.");
            mainFrame.switchToPrivateRoom(currentUserName);
        });

        // Optional: Make Enter key in username field default to Public Rooms?
        // userNameField.addActionListener(e -> publicRoomButton.doClick());

        // Action for info button
        infoButton.addActionListener(e -> showInfoDialog());
    }

    /** Shows a simple 'About' dialog */
    private void showInfoDialog() {
        JOptionPane.showMessageDialog(mainFrame,
                "AnonChat E2EE v1.0.0\n\nA simple E2EE chat proof-of-concept.\n" +
                        "Frontend Design inspired by provided images.", // Updated text
                "About AnonChat",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // --- Inner Class for Background Image Panel (Handles ImageIO loading and COVER scaling) ---
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

} // End LoginPage class