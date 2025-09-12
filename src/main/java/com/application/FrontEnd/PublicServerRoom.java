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
    // private static final String IMAGE_PATH_PREFIX = "/com/application/FrontEnd/images/"; // <-- REMOVED (No longer needed)

    // Room Data (Fixed list as requested)
    private static final String[] ROOM_IDS = {"Alpha", "Bravo", "Charlie", "Delta", "Echo"};
    private static final String[] ROOM_DISPLAY_NAMES = {"Room Alpha", "Room Bravo", "Room Charlie", "Room Delta", "Room Echo"};
    private static final int[] ROOM_USER_COUNTS = {5, 7, 15, 2, 9};
    // <-- REMOVED the ROOM_BG_IMAGE_FILES array as we are not using individual images anymore
    // private static final String[] ROOM_BG_IMAGE_FILES = {
    //        "BG_Alpha.jpg", "BG_Bravo.jpg", "BG_Charlie.jpg", "BG_Delta.jpg", "BG_Echo.jpg"
    // };

    // Styling Constants
    private static final Color PAGE_BACKGROUND_FALLBACK = new Color(30, 32, 34); // Very Dark Grey (almost black)
    private static final Color HEADER_RIBBON_BACKGROUND = new Color(44, 47, 51); // Darker Grey for panels
    private static final Color HEADER_RIBBON_TEXT = Color.LIGHT_GRAY;            // Muted text for headers
    private static final Color ROW_PANEL_BACKGROUND = new Color(44, 47, 51);     // Same as header
    private static final Color ROW_NAME_TEXT_COLOR = Color.WHITE;                // Bright white for primary text
    private static final Color ROW_COUNT_TEXT_COLOR = new Color(185, 187, 190);    // Lighter grey for secondary data
    private static final Color FOOTER_TEXT_COLOR = Color.GRAY;

    // Accent Color for Buttons and Highlights
    private static final Color ACCENT_COLOR = new Color(88, 101, 242);             // A nice, vibrant "Discord" blue/purple
    private static final Color ACCENT_COLOR_HOVER = new Color(110, 122, 245);      // A slightly lighter version for hover
    private static final Color BUTTON_TEXT_COLOR = Color.WHITE;


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
        centerPanel.setOpaque(false);

        JPanel headerRow = createHeaderRibbon();
        centerPanel.add(headerRow);

        JPanel roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        roomListPanel.setOpaque(false);
        roomListPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0)); // Padding top/bottom only

        // Fixed list of rooms
        for (int i = 0; i < ROOM_IDS.length; i++) {
            // <-- MODIFIED: Call to createRoomRow no longer needs an image path
            JPanel row = createRoomRow(
                    ROOM_DISPLAY_NAMES[i],
                    ROOM_USER_COUNTS[i],
                    ROOM_IDS[i]
            );
            roomListPanel.add(row);
            if (i < ROOM_IDS.length - 1) {
                roomListPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            }
        }

        centerPanel.add(roomListPanel);

        centerPanel.setMaximumSize(new Dimension(TARGET_CONTENT_WIDTH + 10, Short.MAX_VALUE));


        return centerPanel;
    }

    private int TARGET_CONTENT_WIDTH = 1000;

    private JPanel createHeaderRibbon() {
    final int ribbonHeight = 60;

    // Custom panel with rounded corners
    JPanel headerPanel = new JPanel(new GridBagLayout()) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Use our new panel background color
            g2d.setColor(HEADER_RIBBON_BACKGROUND);
            // Draw a rounded rectangle instead of a sharp one
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            g2d.dispose();
        }
        @Override public Dimension getPreferredSize() { return new Dimension(TARGET_CONTENT_WIDTH, ribbonHeight); }
        @Override public Dimension getMinimumSize() { return new Dimension(400, ribbonHeight);}
        @Override public Dimension getMaximumSize() { return new Dimension(Short.MAX_VALUE, ribbonHeight + 5); }
    };
        headerPanel.setOpaque(false); // We are custom painting, so it must be non-opaque
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 15, 0, 15);

        Font headerFont = MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(Font.PLAIN, 18f) : new Font("SansSerif", Font.PLAIN, 18);
        Color headerColor = HEADER_RIBBON_TEXT; // Use our new muted header text color

        JLabel nameHeader = new JLabel("Room Name");
        nameHeader.setFont(headerFont); nameHeader.setForeground(headerColor);
        gbc.gridx = 0; gbc.weightx = 0.45; gbc.anchor = GridBagConstraints.LINE_START; headerPanel.add(nameHeader, gbc);

        JLabel usersHeader = new JLabel("Connected Users");
        usersHeader.setFont(headerFont); usersHeader.setForeground(headerColor); usersHeader.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 1; gbc.weightx = 0.25; gbc.anchor = GridBagConstraints.CENTER; headerPanel.add(usersHeader, gbc);

        JLabel joinHeader = new JLabel("Enter Room");
        joinHeader.setFont(headerFont); joinHeader.setForeground(headerColor); joinHeader.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 2; gbc.weightx = 0.30; gbc.anchor = GridBagConstraints.CENTER; headerPanel.add(joinHeader, gbc);

        return headerPanel;
    }


    // <-- MODIFIED: Whole method updated for solid black background
    private JPanel createRoomRow(String roomDisplayName, int userCount, String roomIdentifier) {
    // A custom panel with rounded corners for a modern "card" look.
        JPanel rowPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                // This method allows us to paint a custom background with rounded corners.
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(ROW_PANEL_BACKGROUND); // Use our new dark grey panel color
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15); // The 15, 15 creates the corner radius
                g2d.dispose();
                super.paintComponent(g); // Important: Paint children (labels, button) AFTER the background
            }
        };
        rowPanel.setOpaque(false); // We must do this because we are painting our own background
        rowPanel.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25)); // Increased padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0; gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5); // Internal spacing

        // --- Room Name ---
        Font nameFont = MainFrame.sansationBold.deriveFont(20f);
        JLabel nameLabel = new JLabel(roomDisplayName);
        nameLabel.setFont(nameFont);
        nameLabel.setForeground(ROW_NAME_TEXT_COLOR); // Use white for high contrast
        gbc.gridx = 0; gbc.weightx = 0.45; gbc.anchor = GridBagConstraints.LINE_START;
        rowPanel.add(nameLabel, gbc);

        // --- User Count ---
        Font countFont = MainFrame.sansationBold.deriveFont(Font.BOLD, 22f);
        JLabel userLabel = new JLabel(String.valueOf(userCount));
        userLabel.setFont(countFont);
        userLabel.setForeground(ROW_COUNT_TEXT_COLOR); // Use the muted grey for secondary info
        userLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 1; gbc.weightx = 0.25; gbc.anchor = GridBagConstraints.CENTER;
        rowPanel.add(userLabel, gbc);

        // --- Join Button with Hover Effect ---
        JButton joinButton = new JButton("Join Room");
        joinButton.setFont(MainFrame.sansationBold.deriveFont(16f));
        joinButton.setForeground(BUTTON_TEXT_COLOR);
        joinButton.setBackground(ACCENT_COLOR); // Set the initial accent color
        joinButton.setOpaque(false); // Important for custom painting
        joinButton.setFocusPainted(false);
        joinButton.setBorderPainted(false);
        joinButton.setContentAreaFilled(false);
        joinButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // This Mouse Listener handles the hover effect
        joinButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                joinButton.setBackground(ACCENT_COLOR_HOVER); // Change to lighter color on hover
                joinButton.repaint(); // Tell the button to repaint itself
            }

            @Override
            public void mouseExited(MouseEvent e) {
                joinButton.setBackground(ACCENT_COLOR); // Change back to original color
                joinButton.repaint();
            }
        });

        joinButton.addActionListener(e -> {
            System.out.println("[PublicServerRoom] User '" + currentUsername + "' attempting to join public room: '" + roomIdentifier + "'");
            chatController.joinPublicRoom(currentUsername, roomIdentifier);
        });

        // We put the button in a container that will paint the background for us.
        // This gives us perfectly rounded corners on the button itself.
        JPanel buttonContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // The background color is taken from the button itself.
                g2.setColor(joinButton.getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        buttonContainer.setOpaque(false);
        buttonContainer.add(joinButton, BorderLayout.CENTER); // Add the button to fill the container

        gbc.gridx = 2; gbc.weightx = 0.30;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        // Set a fixed size for the button container
        buttonContainer.setPreferredSize(new Dimension(130, 45));
        rowPanel.add(buttonContainer, gbc);

        // Set row size constraints
        int rowHeight = 85;
        rowPanel.setPreferredSize(new Dimension(TARGET_CONTENT_WIDTH, rowHeight));
        rowPanel.setMinimumSize(new Dimension(400, rowHeight - 10));
        rowPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, rowHeight + 10));

        return rowPanel;
    }


    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setOpaque(false);
        JLabel versionLabel = new JLabel("v1.0.0");
        versionLabel.setFont(MainFrame.sansationBold.deriveFont(12f));
        versionLabel.setForeground(FOOTER_TEXT_COLOR);
        footerPanel.add(versionLabel);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 8, 15));
        return footerPanel;
    }

    // <-- REMOVED: The entire ImageBackgroundRowPanel inner class is no longer needed.


    // --- Inner Class for Overall Page Background ---
    private static class PageBackgroundPanel extends JPanel {
        private Image backgroundImage;
        private String imagePathUsed;
        private String errorMessage = null;

        public PageBackgroundPanel(String imagePath) {
            this.imagePathUsed = imagePath;
            try {
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
                this.errorMessage = e.getMessage();
                System.err.println("Error loading page background (" + imagePath + "): " + this.errorMessage);
                this.backgroundImage = null;
            }
            setOpaque(true);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                int w = getWidth(); int h = getHeight();
                int imgW = backgroundImage.getWidth(this); int imgH = backgroundImage.getHeight(this);
                if(imgW <= 0 || imgH <= 0) {
                    g2d.dispose();
                    drawPageErrorFallback(g, w, h);
                    return;
                }

                double imgAspect = (double) imgW / imgH; double panelAspect = (double) w / h;
                int drawW, drawH, drawX, drawY;
                if (panelAspect > imgAspect) { drawW = w; drawH = (int)(w / imgAspect); drawX = 0; drawY = (h - drawH) / 2; }
                else { drawH = h; drawW = (int)(h * imgAspect); drawX = (w - drawW) / 2; drawY = 0; }

                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(backgroundImage, drawX, drawY, drawW, drawH, this);
                g2d.dispose();
            } else {
                drawPageErrorFallback(g, getWidth(), getHeight());
            }
        }

        private void drawPageErrorFallback(Graphics g, int w, int h) {
            g.setColor(PAGE_BACKGROUND_FALLBACK);
            g.fillRect(0, 0, w, h);
            g.setColor(Color.RED);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            String text = "Page BG Load Error: " + (errorMessage != null ? errorMessage : "Unknown");
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, 20 , h/2 + fm.getAscent() / 2);
        }
    }

    private JButton createIconButton(String iconPath, String fallbackText, String tooltip) {
        JButton button = new JButton();
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension iconButtonSize = new Dimension(30, 30);
        button.setPreferredSize(iconButtonSize);
        button.setMinimumSize(iconButtonSize);
        button.setMaximumSize(iconButtonSize);

        try {
            URL iconUrl = getClass().getResource(iconPath);
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                if (icon.getIconWidth() > 0) {
                    Image scaledImage = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                    button.setIcon(new ImageIcon(scaledImage));
                } else { throw new IOException("Icon ImageIcon invalid"); }
            } else { throw new IOException("Icon resource not found: " + iconPath); }
        } catch (Exception ex) {
            System.err.println("Warning: Could not load icon " + iconPath + ". Using text fallback. " + ex.getMessage());
            button.setText(fallbackText);
            button.setFont(new Font("SansSerif", Font.BOLD, 20));
            button.setForeground(Color.BLACK);
        }
        return button;
    }
}