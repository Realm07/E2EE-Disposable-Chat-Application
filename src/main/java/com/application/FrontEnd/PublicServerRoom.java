// src/main/java/com/application/FrontEnd/PublicServerRoom.java
package com.application.FrontEnd;

// Core Swing & AWT Imports
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*; // For action/mouse listeners
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
// Image Loading Imports
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;

// Backend/Component Imports (Adjust if needed)
import com.application.Backend.ChatController; // Keep if controller needed later
// Removed CustomButton/Label - using standard components styled here

/**
 * UI Panel displaying the list of fixed public chat rooms, styled to match the target design.
 * Features a full-panel background image, custom header ribbon, and styled room rows
 * with individual backgrounds.
 */
public class PublicServerRoom extends JPanel {

    // --- References ---
    private MainFrame mainFrame;
    private String currentUsername;
    // private ChatController chatController; // Uncomment if controller needed

    // --- Constants ---
    // Resource Paths (Ensure these files exist in src/main/resources/com/application/FrontEnd/images/)
    private static final String PAGE_BACKGROUND_PATH = "/com/application/FrontEnd/images/BG_PublicRooms.png";
    private static final String ICON_BACK_PATH = "/com/application/FrontEnd/images/ICON_Back.png";
    private static final String IMAGE_PATH_PREFIX = "/com/application/FrontEnd/images/";

    // Room Data (Fixed list as requested)
    private static final String[] ROOM_IDS = {"Alpha", "Bravo", "Charlie", "Delta", "Echo"};
    private static final String[] ROOM_DISPLAY_NAMES = {"Room Alpha", "Room Bravo", "Room Charlie", "Room Delta", "Room Echo"};
    private static final int[] ROOM_USER_COUNTS = {5, 7, 15, 2, 9};
    private static final String[] ROOM_BG_IMAGE_FILES = {
            "BG_Alpha.jpg", "BG_Bravo.jpg", "BG_Charlie.jpg", "BG_Delta.jpg", "BG_Echo.jpg"
    };

    // Styling Constants
    private static final Color PANEL_BACKGROUND_FALLBACK = new Color(217, 222, 227); // Fallback if BG image fails
    private static final Color HEADER_RIBBON_BACKGROUND = Color.WHITE;
    private static final Color HEADER_RIBBON_BORDER = Color.BLACK;
    private static final Color HEADER_RIBBON_TEXT = Color.BLACK;
    private static final Color ROW_BORDER_COLOR = Color.BLACK;
    private static final Color ROW_NAME_TEXT_COLOR = Color.BLACK;
    private static final Color ROW_COUNT_TEXT_COLOR = new Color(0, 140, 40); // Darker Green
    private static final Color ROW_BUTTON_TEXT_COLOR = Color.WHITE;
    private static final Color ROW_BUTTON_BORDER_COLOR = Color.WHITE;
    private static final Color ROW_BUTTON_BACKGROUND_COLOR = new Color(60, 190, 60, 170); // ~66% Alpha Green
    private static final Color FOOTER_TEXT_COLOR = Color.BLACK;
    private static final float ROW_BACKGROUND_ALPHA = 0.66f; // Alpha for row background images
    private static final int HEADER_BORDER_THICKNESS = 2;
    private static final int ROW_BORDER_THICKNESS = 1; // Thinner border for rows


    // --- UI Component Fields ---
    private JLayeredPane layeredPane;
    private PageBackgroundPanel pageBackgroundPanel; // For overall BG

    // --- Constructor ---
    public PublicServerRoom(MainFrame mainFrame, String username /*, ChatController controller */) {
        this.mainFrame = mainFrame;
        this.currentUsername = username;
        // this.chatController = controller;

        setLayout(new BorderLayout(0, 0));
        setOpaque(false); // This main panel is just a container for the layered pane

        // Create and add the Layered Pane
        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        // Create and add the Page Background Panel (Bottom Layer)
        pageBackgroundPanel = new PageBackgroundPanel(PAGE_BACKGROUND_PATH);
        layeredPane.add(pageBackgroundPanel, JLayeredPane.DEFAULT_LAYER); // Layer 0

        // Create the main content overlay panel (Transparent)
        JPanel mainContentPanel = new JPanel(new BorderLayout(0, 0));
        mainContentPanel.setOpaque(false);

        // --- Create and Add Sub-Panels to mainContentPanel ---
        JPanel topPanel = createTopPanel(); // Has transparent background
        mainContentPanel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = createCenterPanel(); // Holds ribbon + list, transparent background
        mainContentPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel footerPanel = createFooterPanel(); // Transparent background
        mainContentPanel.add(footerPanel, BorderLayout.SOUTH);

        // Add the main content overlay panel ABOVE the background
        layeredPane.add(mainContentPanel, JLayeredPane.PALETTE_LAYER); // Layer ~100

        // Add resize listener to size/position layered components correctly
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeAndPositionComponents();
            }
            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(PublicServerRoom.this::resizeAndPositionComponents);
            }
        });
    }

    /** Recalculates bounds for layered components */
    private void resizeAndPositionComponents() {
        // Use invokeLater to ensure calculations happen after EDT has processed events
        SwingUtilities.invokeLater(() -> {
            int w = layeredPane.getWidth();
            int h = layeredPane.getHeight();
            if (w <= 0 || h <= 0) return; // Avoid calculations if panel not ready

            // Background panel fills the entire layered pane
            if (pageBackgroundPanel != null) pageBackgroundPanel.setBounds(0, 0, w, h);

            // Main content panel also fills the entire layered pane, overlaying the background
            Component[] comps = layeredPane.getComponentsInLayer(JLayeredPane.PALETTE_LAYER.intValue());
            if (comps.length > 0 && comps[0] instanceof JPanel) {
                comps[0].setBounds(0, 0, w, h);
            }

            // Revalidate the layered pane to ensure children layouts are updated
            layeredPane.revalidate();
            layeredPane.repaint();
        });
    }

    /** Creates the top panel (transparent) with back button and centered title. */
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false); // Allow page background to show through
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25)); // Increased Padding

        // Back Button
        JButton backButton = createIconButton(ICON_BACK_PATH, "\u2190", "Back");
        backButton.addActionListener(e -> mainFrame.switchToLoginPage());

        // Title Label - Black, Larger Font
        JLabel titleLabel = new JLabel("Public Rooms");
        titleLabel.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(32f) : new Font("SansSerif", Font.BOLD, 32));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setForeground(Color.BLACK); // Black title text

        // Placeholder for balance if needed (can be empty label)
        JLabel rightPlaceholder = new JLabel();
        rightPlaceholder.setPreferredSize(backButton.getPreferredSize()); // Match size

        topPanel.add(backButton, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(rightPlaceholder, BorderLayout.EAST);

        return topPanel;
    }

    /** Creates the center area containing the header ribbon and room list */
    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false); // Transparent, shows page background

        // Header Ribbon
        JPanel headerRow = createHeaderRibbon();
        centerPanel.add(headerRow);

        // Room List Panel
        JPanel roomListPanel = new JPanel();
        // Stack rows vertically
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        roomListPanel.setOpaque(false);
        // Padding around the entire list
        roomListPanel.setBorder(BorderFactory.createEmptyBorder(20, 35, 20, 35)); // Adjusted padding

        // Fixed list of rooms
        for (int i = 0; i < ROOM_IDS.length; i++) {
            String imagePath = IMAGE_PATH_PREFIX + ROOM_BG_IMAGE_FILES[i];
            JPanel row = createRoomRow(
                    ROOM_DISPLAY_NAMES[i],
                    ROOM_USER_COUNTS[i],
                    ROOM_IDS[i],
                    imagePath
            );
            // Set maximum height to control vertical stretching by BoxLayout
            row.setMaximumSize(new Dimension(Short.MAX_VALUE, row.getPreferredSize().height + 5)); // Allow slight growth
            roomListPanel.add(row);
            // Add spacing *between* rows
            if (i < ROOM_IDS.length - 1) {
                roomListPanel.add(Box.createRigidArea(new Dimension(0, 15))); // Increased spacing
            }
        }
        // Add glue to push rows towards the top if extra space exists
        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(roomListPanel);
        centerPanel.add(Box.createVerticalGlue());


        return centerPanel;
    }


    /** Creates the white header ribbon with thicker borders and larger centered fonts. */
    private JPanel createHeaderRibbon() {
        final int ribbonHeight = 60; // Increased height
        final int borderThickness = 2;

        // Panel with custom painting for borders
        JPanel headerPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // Let L&F paint background (set below)
                Graphics2D g2d = (Graphics2D) g.create();
                // Draw thicker top/bottom borders
                g2d.setColor(HEADER_RIBBON_BORDER);
                g2d.setStroke(new BasicStroke(borderThickness));
                // Adjust y-coordinate based on stroke width for precise placement
                g2d.drawLine(0, borderThickness / 2, getWidth(), borderThickness / 2); // Top
                g2d.drawLine(0, getHeight() - 1 - borderThickness / 2, getWidth(), getHeight() - 1 - borderThickness / 2); // Bottom
                g2d.dispose();
            }
            // Define size constraints
            @Override public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, ribbonHeight); }
            @Override public Dimension getMinimumSize() { return new Dimension(400, ribbonHeight);}
            @Override public Dimension getMaximumSize() { return new Dimension(Short.MAX_VALUE, ribbonHeight + 5); }
        };
        headerPanel.setOpaque(true); // Ribbon needs to be opaque white
        headerPanel.setBackground(HEADER_RIBBON_BACKGROUND);
        // Only internal padding - custom painting handles border lines
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        // GridBagConstraints setup (Same weights, increased font size)
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 15, 0, 15); // Increased internal spacing

        Font headerFont = MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(20f) : new Font("SansSerif", Font.BOLD, 20); // Larger
        Color headerColor = HEADER_RIBBON_TEXT;

        // Room Name Header
        JLabel nameHeader = new JLabel("Room Name");
        nameHeader.setFont(headerFont); nameHeader.setForeground(headerColor); nameHeader.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.weightx = 0.45; headerPanel.add(nameHeader, gbc);

        // Connected Users Header
        JLabel usersHeader = new JLabel("Connected Users");
        usersHeader.setFont(headerFont); usersHeader.setForeground(headerColor); usersHeader.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 1; gbc.weightx = 0.25; headerPanel.add(usersHeader, gbc);

        // Enter Room Header
        JLabel joinHeader = new JLabel("Enter Room");
        joinHeader.setFont(headerFont); joinHeader.setForeground(headerColor); joinHeader.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 2; gbc.weightx = 0.30; headerPanel.add(joinHeader, gbc);

        return headerPanel;
    }


    /** Creates a single room row panel with updated styling */
    private JPanel createRoomRow(String roomDisplayName, int userCount, String roomIdentifier, String imagePath) {
        // Use the custom panel for rows with alpha
        ImageBackgroundRowPanel rowPanel = new ImageBackgroundRowPanel(imagePath, ROW_BACKGROUND_ALPHA);
        rowPanel.setLayout(new GridBagLayout());
        // Padding *inside* the row panel (around components)
        rowPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Increased padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.BOTH; // Fill cells vertically+horizontally
        gbc.insets = new Insets(0, 10, 0, 10); // Spacing between components

        // Fonts - Increased sizes
        Font nameFont = MainFrame.sansationBold.deriveFont(20f); // Larger Bold
        Font countFont = MainFrame.sansationBold.deriveFont(22f); // Larger Bold
        Font buttonFont = MainFrame.sansationBold.deriveFont(16f); // Larger Bold

        // Room Name Label (Standard JLabel)
        JLabel nameLabel = new JLabel(roomDisplayName);
        nameLabel.setFont(nameFont); nameLabel.setForeground(ROW_NAME_TEXT_COLOR); // Black text
        nameLabel.setOpaque(false); // Transparent over background image
        gbc.gridx = 0; gbc.weightx = 0.45; gbc.anchor = GridBagConstraints.LINE_START; // Align text left
        rowPanel.add(nameLabel, gbc);

        // User Count Label (Standard JLabel)
        JLabel userLabel = new JLabel(String.valueOf(userCount));
        userLabel.setFont(countFont); userLabel.setForeground(ROW_COUNT_TEXT_COLOR); // Green text
        userLabel.setHorizontalAlignment(SwingConstants.CENTER);
        userLabel.setOpaque(false);
        gbc.gridx = 1; gbc.weightx = 0.25; gbc.anchor = GridBagConstraints.CENTER; // Center label
        rowPanel.add(userLabel, gbc);

        // --- Join Button with Custom Background Panel ---
        JButton joinButton = new JButton("Enter Room");
        joinButton.setFont(buttonFont);
        joinButton.setForeground(ROW_BUTTON_TEXT_COLOR);
        joinButton.setOpaque(false); // Button itself must be transparent
        joinButton.setContentAreaFilled(false); // Don't paint default background
        joinButton.setBorderPainted(true); // Paint the border
        joinButton.setFocusPainted(false);
        joinButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        joinButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ROW_BUTTON_BORDER_COLOR, 1), // White border
                BorderFactory.createEmptyBorder(8, 25, 8, 25) // Button padding
        ));
        joinButton.addActionListener(e -> mainFrame.switchToChatRoom(currentUsername, roomIdentifier));

        // Container panel that paints the semi-transparent green background
        JPanel buttonContainer = new JPanel(new GridBagLayout()) { // Use GBL to center button easily
            @Override
            protected void paintComponent(Graphics g) {
                // Do NOT call super.paintComponent(g) if setOpaque(false)
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ROW_BUTTON_BACKGROUND_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); // Rounded corners
                g2.dispose();
            }
            @Override // Ensure container requests enough space for the button + padding
            public Dimension getPreferredSize() {
                return joinButton.getPreferredSize();
            }
            @Override // And respects minimum size
            public Dimension getMinimumSize() {
                return joinButton.getMinimumSize();
            }
        };
        buttonContainer.setOpaque(false); // Container is transparent
        // Add button to the container using GridBagConstraints to center it
        buttonContainer.add(joinButton, new GridBagConstraints());

        gbc.gridx = 2; gbc.weightx = 0.30;
        gbc.fill = GridBagConstraints.NONE; // Don't stretch the button container panel
        gbc.anchor = GridBagConstraints.CENTER; // Center the container panel
        rowPanel.add(buttonContainer, gbc); // Add the container with the button
        // --- End Button ---

        // Set overall row size constraints AFTER adding components
        int rowHeight = 75; // Desired height for rows
        rowPanel.setPreferredSize(new Dimension(600, rowHeight));
        rowPanel.setMinimumSize(new Dimension(400, rowHeight -5));
        rowPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, rowHeight + 5));

        return rowPanel;
    }


    /** Creates the footer panel with version info. */
    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setOpaque(false);
        JLabel versionLabel = new JLabel("v1.0.0");
        versionLabel.setFont(MainFrame.sansationBold.deriveFont(12f)); // Bold version
        versionLabel.setForeground(FOOTER_TEXT_COLOR); // Black text
        footerPanel.add(versionLabel);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 8, 15));
        return footerPanel;
    }


    private static class ImageBackgroundRowPanel extends JPanel {
        private Image backgroundImage;
        private String imagePathUsed;
        private float alpha;
        // Moved constants inside or made them static final in outer class
        private static final Color BORDER_COLOR = Color.BLACK;
        private static final int BORDER_THICKNESS = 2; // Using the thicker row border

        public ImageBackgroundRowPanel(String imagePath, float alpha) {
            this.imagePathUsed = imagePath;
            this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
            try {
                // Load Image using ImageIO from classpath resource URL
                URL imgUrl = getClass().getResource(imagePath);
                if (imgUrl != null) {
                    this.backgroundImage = ImageIO.read(imgUrl);
                    if (this.backgroundImage == null) {
                        // Throw specific error if read fails but URL was found
                        throw new IOException("ImageIO.read returned null for path: " + imagePath);
                    }
                    System.out.println("[RowBGPanel] Loaded: " + imagePath); // Success log
                } else {
                    // Throw specific error if resource URL itself wasn't found
                    throw new IOException("Resource not found at path: " + imagePath);
                }
            } catch (IOException e) {
                // Handle ANY IOException during loading
                System.err.println("Error loading row background (" + imagePath + "): " + e.getMessage());
                this.backgroundImage = null; // Ensure image is null on error
            }
            setOpaque(false); // IMPORTANT: Panel must be non-opaque for alpha blending and custom painting
        }

        @Override
        protected void paintComponent(Graphics g) {
            // DO NOT call super.paintComponent(g) when opaque is false and we paint the full area.
            // super.paintComponent(g); // Remove this if opaque is false

            Graphics2D g2d = (Graphics2D) g.create(); // Work on a copy of the graphics context
            int w = getWidth();
            int h = getHeight();

            // 1. Optional: Fill with parent's background color first for smoother blending
            // Container parent = getParent();
            // if (parent != null) {
            //     g2d.setColor(parent.getBackground());
            //     g2d.fillRect(0, 0, w, h);
            // } else {
            //      // Fallback if no parent background available (shouldn't happen in normal Swing hierarchy)
            g2d.setColor(new Color(230, 235, 240, 100)); // Use a semi-transparent fallback base
            g2d.fillRect(0, 0, w, h);
            // }

            // 2. Draw the background image (center-cropped with alpha)
            if (backgroundImage != null) {
                int imgW = backgroundImage.getWidth(this); // Use 'this' as observer
                int imgH = backgroundImage.getHeight(this);

                if (imgW > 0 && imgH > 0) { // Ensure image dimensions are valid
                    // Apply alpha composite *before* drawing image
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                    // Calculate center-crop scaling (Cover logic)
                    double imgAspect = (double) imgW / imgH;
                    double panelAspect = (double) w / h;
                    int drawW, drawH, drawX, drawY;

                    if (panelAspect > imgAspect) { // Panel wider than image: Fit height, crop width
                        drawH = h;
                        drawW = (int) (h * imgAspect);
                        drawX = (w - drawW) / 2; // Center horizontally
                        drawY = 0;
                    } else { // Panel taller than or same aspect as image: Fit width, crop height
                        drawW = w;
                        drawH = (int) (w / imgAspect);
                        drawX = 0;
                        drawY = (h - drawH) / 2; // Center vertically
                    }

                    // Use higher quality interpolation hint for scaling
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    // Draw the calculated portion of the image scaled into the panel bounds
                    g2d.drawImage(backgroundImage, drawX, drawY, drawW, drawH, this);

                    // Reset composite to default opaque drawing for subsequent elements (like border)
                    g2d.setComposite(AlphaComposite.SrcOver);
                } else {
                    // Draw fallback if image loaded but has invalid dimensions
                    drawErrorFallback(g2d, w, h, "Invalid Dims");
                }
            } else {
                // Draw fallback if image failed to load initially
                drawErrorFallback(g2d, w, h, "Load Failed");
            }

            // 3. Draw thicker black outline AFTER drawing background/image
            g2d.setColor(BORDER_COLOR);
            g2d.setStroke(new BasicStroke(BORDER_THICKNESS));
            // Draw slightly inside bounds to account for stroke width centering on the path
            g2d.drawRect(BORDER_THICKNESS / 2, BORDER_THICKNESS / 2,
                    w - BORDER_THICKNESS, h - BORDER_THICKNESS);

            g2d.dispose(); // Release the graphics copy resources
        }

        // Helper to draw error state, providing more context
        private void drawErrorFallback(Graphics2D g2d, int w, int h, String errorType) {
            g2d.setColor(new Color(210, 210, 210)); // Lighter grey fallback
            g2d.fillRect(0, 0, w, h);
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
            String errMsg = "BG Err ("+ errorType +"): " + imagePathUsed.substring(imagePathUsed.lastIndexOf('/')+1);
            FontMetrics fm = g2d.getFontMetrics();
            // Basic centering of error text
            g2d.drawString(errMsg, (w-fm.stringWidth(errMsg))/2, (h-fm.getHeight())/2 + fm.getAscent());
        }

        // Size hints (Keep these to help BoxLayout size the rows)
        @Override public Dimension getPreferredSize() { return new Dimension(600, 75); }
        @Override public Dimension getMinimumSize() { return new Dimension(400, 70); }
        @Override public Dimension getMaximumSize() { return new Dimension(Short.MAX_VALUE, 80); }

    } // End ImageBackgroundRowPanel


    // --- Inner Class for Overall Page Background ---
    private static class PageBackgroundPanel extends JPanel {
        private Image backgroundImage;
        private String imagePathUsed;
        private String errorMessage = null; // Store error message

        public PageBackgroundPanel(String imagePath) {
            this.imagePathUsed = imagePath;
            try {
                // Load Image using ImageIO
                URL imgUrl = getClass().getResource(imagePath);
                if (imgUrl != null) {
                    this.backgroundImage = ImageIO.read(imgUrl);
                    if (this.backgroundImage == null) {
                        throw new IOException("ImageIO.read returned null for page background: " + imagePath);
                    }
                    System.out.println("[PageBGPanel] Loaded: " + imagePath);
                } else {
                    throw new IOException("Page background resource not found: " + imagePath);
                }
            } catch (IOException e) {
                this.errorMessage = e.getMessage(); // Store error for painting
                System.err.println("Error loading page background (" + imagePath + "): " + this.errorMessage);
                this.backgroundImage = null; // Ensure it's null on error
            }
            // This panel IS the background, so it should be opaque.
            setOpaque(true);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Let default clear the background
            if (backgroundImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                // Draw background scaled to COVER the whole panel
                int w = getWidth(); int h = getHeight();
                int imgW = backgroundImage.getWidth(this); int imgH = backgroundImage.getHeight(this);
                if(imgW <= 0 || imgH <= 0) { // Check if image actually loaded dimensions
                    g2d.dispose();
                    drawPageErrorFallback(g, w, h); // Use Graphics 'g' from parameter
                    return;
                }

                // COVER Scaling Logic (same as before)
                double imgAspect = (double) imgW / imgH; double panelAspect = (double) w / h;
                int drawW, drawH, drawX, drawY;
                if (panelAspect > imgAspect) { drawW = w; drawH = (int)(w / imgAspect); drawX = 0; drawY = (h - drawH) / 2; }
                else { drawH = h; drawW = (int)(h * imgAspect); drawX = (w - drawW) / 2; drawY = 0; }

                // Use higher quality interpolation
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(backgroundImage, drawX, drawY, drawW, drawH, this);
                g2d.dispose();
            } else {
                // Draw error fallback directly using 'g'
                drawPageErrorFallback(g, getWidth(), getHeight());
            }
        }

        // Helper for page background error
        private void drawPageErrorFallback(Graphics g, int w, int h) {
            g.setColor(PANEL_BACKGROUND_FALLBACK); // Use the defined fallback color
            g.fillRect(0, 0, w, h);
            g.setColor(Color.RED);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            String text = "Page BG Load Error: " + (errorMessage != null ? errorMessage : "Unknown");
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, 20 , h/2 + fm.getAscent() / 2);
        }
    } // End PageBackgroundPanel

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
} // End PublicServerRoom class