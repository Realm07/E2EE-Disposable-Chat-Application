// src/main/java/com/application/FrontEnd/LoginPage.java
package com.application.FrontEnd;

import com.application.Backend.ChatController;
import com.application.FrontEnd.components.CustomTextField; // Using your custom field

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class LoginPage extends JPanel {

    // --- UI Components ---
    private CustomTextField userNameField;
    private JButton submitButton;   // Arrow button
    private JButton infoButton;     // Top-left 'i' icon button
    private JLabel aboutLabel;
    private JLabel versionLabel;
    private JPanel formPanel;       // Panel holding the centered form elements

    // --- References ---
    private MainFrame mainFrame;
    private ChatController chatController;

    // --- Fields for Layered Pane ---
    private JLayeredPane layeredPane;
    private BackgroundImagePanel backgroundPanel; // Inner class handles background

    // --- Resource Paths ---
    private static final String BACKGROUND_IMAGE_PATH = "/com/application/FrontEnd/images/BG_LoginPage.jpg"; // Correct JPG background
    private static final String LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/ICON_Logo.png";
    private static final String INFO_ICON_PATH = "/com/application/FrontEnd/images/ICON_Info.png";
    private static final String SUBMIT_ICON_PATH = "/com/application/FrontEnd/images/ICON_Front.png";

    // --- Constructor ---
    public LoginPage(MainFrame mainFrame, ChatController chatController) {
        this.mainFrame = mainFrame;
        this.chatController = chatController;

        // Set main layout for this LoginPage panel
        setLayout(new BorderLayout());

        // Create and add the Layered Pane
        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER); // Make layered pane fill this panel

        // Create and add the Background Panel (Bottom Layer)
        backgroundPanel = new BackgroundImagePanel(BACKGROUND_IMAGE_PATH);
        // Initial bounds are small, resize listener will fix this
        backgroundPanel.setBounds(0, 0, 1, 1);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER); // Layer 0

        // Create and add the Form Panel (Top Layer)
        createFormPanel(); // Use helper method
        // Initial bounds are small, resize listener will fix this
        formPanel.setBounds(0, 0, 1, 1);
        layeredPane.add(formPanel, JLayeredPane.PALETTE_LAYER); // Layer ~100

        // Create and add absolutely positioned elements (Highest Layer)
        infoButton = createIconButton(INFO_ICON_PATH, "i", "Info");
        infoButton.setBounds(15, 15, 32, 32); // Initial position
        layeredPane.add(infoButton, JLayeredPane.MODAL_LAYER); // Layer ~200

        versionLabel = new JLabel("v1.0.0");
        versionLabel.setForeground(Color.DARK_GRAY); // Color from previous version
        versionLabel.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(10f) : new Font("SansSerif", Font.BOLD, 10));
        versionLabel.setBounds(0, 0, 100, 20); // Dummy initial bounds
        layeredPane.add(versionLabel, JLayeredPane.MODAL_LAYER);

        // Add resize listener to the LAYERED PANE
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                System.out.println("[LoginPage] Layered Pane Resized: " + layeredPane.getSize());
                resizeAndCenterComponents();
            }
            // Optional: Also trigger on componentShown to ensure initial positioning
            @Override
            public void componentShown(ComponentEvent e) {
                System.out.println("[LoginPage] Layered Pane Shown");
                // Give layout manager a chance to settle before initial positioning
                SwingUtilities.invokeLater(LoginPage.this::resizeAndCenterComponents);
            }
        });

        // Attach Event Listeners to interactive components
        addEventListeners();
    }

    /** Creates and configures the form panel with its components. */
    private void createFormPanel() {
        formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false); // Make transparent

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 10, 5, 10); // Consistent padding

        // Logo
        JLabel logoLabel = createLogoLabel();
        gbc.gridy = 0; gbc.insets = new Insets(20, 10, 5, 10); // Adjust top/bottom padding
        formPanel.add(logoLabel, gbc);

        // App Name
        JLabel appNameLabel = new JLabel("A N O N C H A T");
        appNameLabel.setForeground(Color.BLACK);
        appNameLabel.setFont(MainFrame.sansationBold.deriveFont(18f));
        appNameLabel.setBorder(BorderFactory.createMatteBorder(0,0,1,0, Color.BLACK)); // Underline
        gbc.gridy = 1; gbc.insets = new Insets(5, 10, 30, 10); // Space below
        formPanel.add(appNameLabel, gbc);

        // Username Label
        JLabel labelUserName = new JLabel("Enter Username:");
        labelUserName.setForeground(Color.BLACK);
        labelUserName.setFont(MainFrame.sansationRegular.deriveFont(15f));
        gbc.gridy = 2; gbc.insets = new Insets(0, 10, 8, 10);
        formPanel.add(labelUserName, gbc);

        // Input Panel (Username Field + Submit Button)
        JPanel inputPanel = createInputPanel();
        gbc.gridy = 3; gbc.insets = new Insets(0, 10, 40, 10); // Space below
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(inputPanel, gbc);

        // About Label
        aboutLabel = new JLabel("About");
        aboutLabel.setForeground(Color.BLACK);
        aboutLabel.setFont(MainFrame.sansationRegular.deriveFont(14f));
        aboutLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        aboutLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showInfoDialog(); }
            // Removed hover effect for black text
        });
        gbc.gridy = 4; gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weighty = 10.0; // Pushes the "About" slightly down if there's extra vertical space
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(aboutLabel, gbc);
    }


    /** Helper to create the combined Username Field and Submit Button Panel */
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0)); // Horizontal gap 8
        inputPanel.setOpaque(false);
        // Constrain size
        Dimension inputSize = new Dimension(260, 35); // Defined size for the input area
        inputPanel.setPreferredSize(inputSize);
        inputPanel.setMaximumSize(inputSize);
        inputPanel.setMinimumSize(inputSize);


        userNameField = new CustomTextField(200, 30); // Custom component
        userNameField.setFont(MainFrame.sansationRegular.deriveFont(16f));
        userNameField.setHorizontalAlignment(SwingConstants.LEFT);
        userNameField.setForeground(Color.BLACK);
        userNameField.setCaretColor(Color.BLACK);
        userNameField.setOpaque(false);
        userNameField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK)); // Underline

        submitButton = createIconButton(SUBMIT_ICON_PATH, "\u2192", "Go"); // Right arrow

        inputPanel.add(userNameField, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);

        return inputPanel;
    }

    /** Helper to create icon buttons (info, submit) */
    private JButton createIconButton(String iconPath, String fallbackText, String tooltip) {
        JButton button = new JButton();
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension iconButtonSize = new Dimension(30, 30); // Fixed size for icons
        button.setPreferredSize(iconButtonSize);
        button.setMinimumSize(iconButtonSize);
        button.setMaximumSize(iconButtonSize);

        try {
            URL iconUrl = getClass().getResource(iconPath);
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                if (icon.getIconWidth() > 0) {
                    Image scaledImage = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH); // Scale icon
                    button.setIcon(new ImageIcon(scaledImage));
                } else { throw new IOException("Icon ImageIcon invalid"); }
            } else { throw new IOException("Icon resource not found: " + iconPath); }
        } catch (Exception ex) {
            System.err.println("Warning: Could not load icon " + iconPath + ". Using text fallback. " + ex.getMessage());
            button.setText(fallbackText);
            button.setFont(new Font("SansSerif", Font.BOLD, 20));
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
                    Image scaledImage = icon.getImage().getScaledInstance(100, -1, Image.SCALE_SMOOTH); // Scale width=100
                    label.setIcon(new ImageIcon(scaledImage));
                    label.setPreferredSize(new Dimension(100, ((ImageIcon)label.getIcon()).getIconHeight())); // Set preferred size based on scaled icon
                } else throw new Exception("Logo ImageIcon invalid");
            } else { throw new Exception("Logo resource not found"); }
        } catch (Exception e) {
            // Fallback: Display text logo
            System.err.println("ERROR loading logo (" + LOGO_IMAGE_PATH + "): " + e.getMessage());
            label.setText("AnonChat Logo"); // Placeholder text
            label.setForeground(Color.DARK_GRAY); // Darker text for fallback
            label.setFont(MainFrame.sansationBold.deriveFont(18f));
            label.setPreferredSize(new Dimension(150, 50)); // Give fallback text some size
        }
        return label;
    }

    /** Recalculates bounds for layered components on resize */
    private void resizeAndCenterComponents() {
        // Use invokeLater to ensure calculations happen after component hierarchy is potentially updated
        SwingUtilities.invokeLater(() -> {
            int layeredWidth = layeredPane.getWidth();
            int layeredHeight = layeredPane.getHeight();
            if (layeredWidth <= 0 || layeredHeight <= 0) return; // Skip if no size yet

            // Resize background to fill layered pane
            if (backgroundPanel != null) {
                backgroundPanel.setBounds(0, 0, layeredWidth, layeredHeight);
            }

            // Center form panel (recalculate preferred size in case content changed)
            if (formPanel != null) {
                Dimension formPrefSize = formPanel.getPreferredSize(); // Recalculate preferred size
                int formW = Math.min(formPrefSize.width, layeredWidth - 40); // Don't exceed pane width
                int formH = Math.min(formPrefSize.height, layeredHeight - 40); // Don't exceed pane height
                int x = (layeredWidth - formW) / 2;
                int y = (layeredHeight - formH) / 2;
                formPanel.setBounds(x, y, formW, formH);
                System.out.println("[LoginPage Resize] Form panel bounds set to: " + formPanel.getBounds()); // Debug
            }

            // Position version label bottom-right
            if (versionLabel != null) {
                Dimension labelSize = versionLabel.getPreferredSize();
                int x = layeredWidth - labelSize.width - 15;
                int y = layeredHeight - labelSize.height - 10;
                versionLabel.setBounds(x, y, labelSize.width, labelSize.height);
            }

            // Position info button top-left
            if (infoButton != null) {
                Dimension btnSize = infoButton.getPreferredSize();
                infoButton.setBounds(15, 15, btnSize.width, btnSize.height);
            }

            // Crucial: Revalidate the layered pane AFTER setting bounds
            layeredPane.revalidate();
            layeredPane.repaint();
        });
    }

    /** Attaches listeners to interactive components */
    public void addEventListeners() {
        // Action for submit button and Enter key in username field
        ActionListener loginAction = e -> {
            String currentUserName = userNameField.getText().trim();
            if (currentUserName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a User Name.", "Input Required", JOptionPane.WARNING_MESSAGE);
                userNameField.requestFocusInWindow(); // Put focus back
                return;
            }
            System.out.println("User entered: " + currentUserName + ". Switching to Public Rooms view.");
            mainFrame.switchToPublicRoom(currentUserName); // Go to public rooms list
        };

        submitButton.addActionListener(loginAction);
        userNameField.addActionListener(loginAction); // Trigger on Enter

        // Action for info button
        infoButton.addActionListener(e -> showInfoDialog());
    }

    /** Shows a simple 'About' dialog */
    private void showInfoDialog() {
        JOptionPane.showMessageDialog(mainFrame, // Parent frame
                "AnonChat E2EE v1.0.0\n\nA simple E2EE chat proof-of-concept.\n" +
                        "Images & concept @ Realm07.", // Example text
                "About AnonChat",               // Dialog title
                JOptionPane.INFORMATION_MESSAGE); // Icon type
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
                if(imgW <= 0 || imgH <= 0) { g2d.dispose(); return; } // Check image loaded ok
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
                g.setColor(new Color(20, 30, 40)); // Dark blue fallback
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