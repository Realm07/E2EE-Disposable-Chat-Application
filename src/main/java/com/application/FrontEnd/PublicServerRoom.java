// src/main/java/com/application/FrontEnd/PublicServerRoom.java
package com.application.FrontEnd;

import com.application.Backend.ChatController;
import com.application.FrontEnd.components.*;

// Core Swing & AWT Imports
import javax.swing.*;
import java.awt.*;
import java.awt.event.*; // For action/mouse listeners
import java.awt.GridBagConstraints;

// Image Loading Imports
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;

public class PublicServerRoom extends JPanel {

    // --- References ---
    private MainFrame mainFrame;
    private String currentUsername;
    private ChatController chatController;

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
    private static final Color ROW_NAME_TEXT_COLOR = Color.BLACK;
    private static final Color ROW_COUNT_TEXT_COLOR = new Color(0, 140, 40); // Darker Green
    private static final Color ROW_BUTTON_TEXT_COLOR = Color.WHITE;
    private static final Color ROW_BUTTON_BACKGROUND_COLOR = new Color(60, 190, 60, 170); // ~66% Alpha Green
    private static final Color FOOTER_TEXT_COLOR = Color.BLACK;
    private static final float ROW_BACKGROUND_ALPHA = 0.66f; // Alpha for row background images
    private static final int ROW_BORDER_THICKNESS = 1; // Thinner border for rows


    // --- UI Component Fields ---
    private JLayeredPane layeredPane;
    private PageBackgroundPanel pageBackgroundPanel; // For overall BG

    // --- Constructor ---
    public PublicServerRoom(MainFrame mainFrame, String username, ChatController controller) {
        this.mainFrame = mainFrame;
        this.currentUsername = username;
        this.chatController = controller;

        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        pageBackgroundPanel = new PageBackgroundPanel(PAGE_BACKGROUND_PATH);
        layeredPane.add(pageBackgroundPanel, JLayeredPane.DEFAULT_LAYER);

        JPanel mainContentPanel = new JPanel(new BorderLayout(0, 0));
        mainContentPanel.setOpaque(false);

        JPanel topPanel = createTopPanel();
        mainContentPanel.add(topPanel, BorderLayout.NORTH);

        JPanel centerWrapperPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0,0));
        centerWrapperPanel.setOpaque(false);

        JPanel centerPanel = createCenterPanel();
        centerWrapperPanel.add(centerPanel);

        mainContentPanel.add(centerWrapperPanel, BorderLayout.CENTER);

        JPanel footerPanel = createFooterPanel();
        mainContentPanel.add(footerPanel, BorderLayout.SOUTH);

        layeredPane.add(mainContentPanel, JLayeredPane.PALETTE_LAYER);

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

    private void resizeAndPositionComponents() {
        SwingUtilities.invokeLater(() -> {
            int w = layeredPane.getWidth();
            int h = layeredPane.getHeight();
            if (w <= 0 || h <= 0) return;

            if (pageBackgroundPanel != null) pageBackgroundPanel.setBounds(0, 0, w, h);

            Component[] comps = layeredPane.getComponentsInLayer(JLayeredPane.PALETTE_LAYER.intValue());
            if (comps.length > 0 && comps[0] instanceof JPanel) {
                comps[0].setBounds(0, 0, w, h);
            }
            layeredPane.revalidate();
            layeredPane.repaint();
        });
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        // Back Button
        JButton backButton = createIconButton(ICON_BACK_PATH, "\u2190", "Back");
        backButton.addActionListener(e -> mainFrame.switchToLoginPage());

        // Title Label - Black, Larger Font
        JLabel titleLabel = new JLabel("Public Rooms");
        titleLabel.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(32f) : new Font("SansSerif", Font.BOLD, 32));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setForeground(Color.BLACK);

        JLabel rightPlaceholder = new JLabel();
        rightPlaceholder.setPreferredSize(backButton.getPreferredSize());

        topPanel.add(backButton, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(rightPlaceholder, BorderLayout.EAST);

        return topPanel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false); // Keep this transparent

        JPanel headerRow = createHeaderRibbon();
        // headerRow.setAlignmentX(Component.CENTER_ALIGNMENT); // REMOVED this line

        centerPanel.add(headerRow);

        JPanel roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        // roomListPanel.setAlignmentX(Component.CENTER_ALIGNMENT); // REMOVED this line
        roomListPanel.setOpaque(false);
        // Internal padding for room list items
        roomListPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0)); // Padding top/bottom only

        // Fixed list of rooms
        for (int i = 0; i < ROOM_IDS.length; i++) {
            String imagePath = IMAGE_PATH_PREFIX + ROOM_BG_IMAGE_FILES[i];
            JPanel row = createRoomRow(
                    ROOM_DISPLAY_NAMES[i],
                    ROOM_USER_COUNTS[i],
                    ROOM_IDS[i],
                    imagePath
            );
            // Add the row directly
            roomListPanel.add(row);
            if (i < ROOM_IDS.length - 1) {
                roomListPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            }
        }

        // Keep vertical glue if you want content pushed towards the top within centerPanel
        // centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(roomListPanel);
        // centerPanel.add(Box.createVerticalGlue());

        // Ensure the centerPanel doesn't stretch horizontally beyond its preferred content width
        // This helps FlowLayout center it correctly. Its width is governed by TARGET_CONTENT_WIDTH.
        centerPanel.setMaximumSize(new Dimension(TARGET_CONTENT_WIDTH + 10, Short.MAX_VALUE)); // Limit max width


        return centerPanel;
    }

    private int TARGET_CONTENT_WIDTH = 1000;

    private JPanel createHeaderRibbon() {
        final int ribbonHeight = 60;
        final int borderThickness = 2;

        JPanel headerPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // Let L&F paint background (set below)
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(HEADER_RIBBON_BORDER);
                g2d.setStroke(new BasicStroke(borderThickness));

                int w = getWidth();
                int h = getHeight();
                int halfStroke = borderThickness / 2;

                // Top border
                g2d.drawLine(0, halfStroke, w, halfStroke);
                // Bottom border
                g2d.drawLine(0, h - 1 - halfStroke, w, h - 1 - halfStroke);


                // Left border
                g2d.drawLine(halfStroke, 0, halfStroke, h);
                // Right border
                g2d.drawLine(w - 1 - halfStroke, 0, w - 1 - halfStroke, h);


                g2d.dispose();
            }
            @Override public Dimension getPreferredSize() { int prefContentWidth = super.getPreferredSize().width; return new Dimension(Math.max(TARGET_CONTENT_WIDTH, prefContentWidth), ribbonHeight); }
            @Override public Dimension getMinimumSize() { return new Dimension(400, ribbonHeight);}
            @Override public Dimension getMaximumSize() { return new Dimension(Short.MAX_VALUE, ribbonHeight + 5); }
        };
        headerPanel.setOpaque(true);
        headerPanel.setBackground(HEADER_RIBBON_BACKGROUND);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 15, 0, 15);

        Font headerFont = MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(20f) : new Font("SansSerif", Font.BOLD, 20); // Larger
        Color headerColor = HEADER_RIBBON_TEXT;

        JLabel nameHeader = new JLabel("Room Name");
        nameHeader.setFont(headerFont); nameHeader.setForeground(headerColor); nameHeader.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.weightx = 0.45; headerPanel.add(nameHeader, gbc);

        JLabel usersHeader = new JLabel("Connected Users");
        usersHeader.setFont(headerFont); usersHeader.setForeground(headerColor); usersHeader.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 1; gbc.weightx = 0.25; headerPanel.add(usersHeader, gbc);

        JLabel joinHeader = new JLabel("Enter Room");
        joinHeader.setFont(headerFont); joinHeader.setForeground(headerColor); joinHeader.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 2; gbc.weightx = 0.30; headerPanel.add(joinHeader, gbc);

        return headerPanel;
    }


    private JPanel createRoomRow(String roomDisplayName, int userCount, String roomIdentifier, String imagePath) {
        ImageBackgroundRowPanel rowPanel = new ImageBackgroundRowPanel(imagePath, ROW_BACKGROUND_ALPHA);
        rowPanel.setLayout(new GridBagLayout());
        rowPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 10, 0, 10);

        Font nameFont = MainFrame.sansationBold.deriveFont(20f);
        Font countFont = MainFrame.sansationBold.deriveFont(22f);
        // Font buttonFont = MainFrame.sansationBold.deriveFont(16f);
        // Font buttonFont = MainFrame.sansationBold.deriveFont(16f);

        JLabel nameLabel = new JLabel(roomDisplayName);
        nameLabel.setFont(nameFont); nameLabel.setForeground(ROW_NAME_TEXT_COLOR);
        nameLabel.setOpaque(false);
        gbc.gridx = 0; gbc.weightx = 0.45; gbc.anchor = GridBagConstraints.LINE_START;
        rowPanel.add(nameLabel, gbc);

        JLabel userLabel = new JLabel(String.valueOf(userCount));
        userLabel.setFont(countFont); userLabel.setForeground(ROW_COUNT_TEXT_COLOR);
        userLabel.setHorizontalAlignment(SwingConstants.CENTER);
        userLabel.setOpaque(false);
        gbc.gridx = 1; gbc.weightx = 0.25; gbc.anchor = GridBagConstraints.CENTER;
        rowPanel.add(userLabel, gbc);

        CustomButton joinButton = new CustomButton("Join Room", 120, 45, new Color(102, 255, 102, 120));
        joinButton.setForeground(ROW_BUTTON_TEXT_COLOR);
        joinButton.setBorder(BorderFactory.createEmptyBorder(8, 25, 8, 25));

        joinButton.addActionListener(e -> {
            System.out.println("[PublicServerRoom] User '" + currentUsername + "' attempting to join public room: '" + roomIdentifier + "'");
            chatController.joinPublicRoom(currentUsername, roomIdentifier);
        });
        JPanel buttonContainer = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ROW_BUTTON_BACKGROUND_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() {
                return joinButton.getPreferredSize();
            }
            @Override
            public Dimension getMinimumSize() {
                return joinButton.getMinimumSize();
            }
        };
        buttonContainer.setOpaque(false);
        buttonContainer.add(joinButton, new GridBagConstraints());

        gbc.gridx = 2; gbc.weightx = 0.30;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        rowPanel.add(buttonContainer, gbc);

        int rowHeight = 85;
        rowPanel.setPreferredSize(new Dimension(TARGET_CONTENT_WIDTH , rowHeight));
        rowPanel.setMinimumSize(new Dimension(400, rowHeight - 10));
        rowPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, rowHeight + 10));

        return rowPanel;
    }


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
        // private static final int BORDER_THICKNESS = 2; // Using the thicker row border

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
            // Don't call super.paintComponent(g) when opaque is false and painting background
            Graphics2D g2d = (Graphics2D) g.create();
            int w = getWidth();
            int h = getHeight();

            // 1. Optional Base Fill (Keep or adjust as needed)
            g2d.setColor(new Color(230, 235, 240, 80)); // Light semi-transparent base
            g2d.fillRect(0, 0, w, h);

            // 2. Draw the background image (center-cropped with alpha to COVER)
            if (backgroundImage != null) {
                int imgW = backgroundImage.getWidth(this);
                int imgH = backgroundImage.getHeight(this);

                if (imgW > 0 && imgH > 0) { // Ensure image dimensions are valid
                    // Apply alpha composite *before* drawing image
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    // Use higher quality interpolation hint for scaling
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                    // --- START REPLACEMENT ---
                    // Calculate scale factors to fill width and height separately
                    double scaleX = (double) w / imgW;
                    double scaleY = (double) h / imgH;

                    // Use the *larger* scale factor to ensure the image covers the entire panel
                    double scale = Math.max(scaleX, scaleY);

                    // Calculate the new dimensions of the image after scaling
                    int scaledW = (int) (imgW * scale);
                    int scaledH = (int) (imgH * scale);

                    // Calculate the top-left position to center the scaled image
                    // One of these will be <= 0, effectively clipping the image
                    int drawX = (w - scaledW) / 2;
                    int drawY = (h - scaledH) / 2;

                    // Draw the image using the calculated position and scaled dimensions
                    // This will paint the image scaled to cover the panel bounds
                    g2d.drawImage(backgroundImage, drawX, drawY, scaledW, scaledH, this);
                    // --- END REPLACEMENT ---

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

            // 3. Draw border AFTER background
            g2d.setColor(BORDER_COLOR);
            g2d.setStroke(new BasicStroke(PublicServerRoom.ROW_BORDER_THICKNESS));
            int inset = PublicServerRoom.ROW_BORDER_THICKNESS / 2;
            g2d.drawRect(inset, inset, w - PublicServerRoom.ROW_BORDER_THICKNESS, h - PublicServerRoom.ROW_BORDER_THICKNESS);

            g2d.dispose(); // Release the graphics copy resources
        } // End paintComponent in ImageBackgroundRowPanel

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