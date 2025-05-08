package com.application.FrontEnd;

// Core Swing & AWT Imports
import javax.swing.*;
import javax.swing.text.DefaultCaret;

import java.awt.*;
import java.awt.event.*;

// Image Loading Imports
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;


public class InfoPage extends JPanel {

    // --- References ---
    private MainFrame mainFrame;

    // --- Constants ---
    private static final String BACK_ICON_PATH = "/com/application/FrontEnd/images/ICON_Back.png";
    private static final String BACKGROUND_GIF_PATH = "/com/application/FrontEnd/images/BG_PublicRooms.png";
    private static final String LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/ICON_Logo.png";
    private static final String GITHUB_IMAGE_PATH = "/com/application/FrontEnd/images/github.png";

    // --- UI Component Fields ---
    private JLayeredPane layeredPane;
    private BackgroundImagePanel backgroundPanel;
    private JPanel mainContentPanel;
    private JTextArea infoTextArea;
    private JLabel versionLabel;


    private String infoText;


    public InfoPage(MainFrame mainFrame){
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout());
        setOpaque(false);

        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        backgroundPanel = new BackgroundImagePanel(BACKGROUND_GIF_PATH);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

        mainContentPanel = new JPanel(new BorderLayout(0, 0));
        mainContentPanel.setOpaque(false);

        JPanel topPanel = createTopPanel();
        mainContentPanel.add(topPanel, BorderLayout.NORTH);

        JPanel centerContentPanel = createCenterContentPanel();
        mainContentPanel.add(centerContentPanel, BorderLayout.CENTER);

        layeredPane.add(mainContentPanel, JLayeredPane.PALETTE_LAYER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeAndPositionComponents();
            }
             @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(InfoPage.this::resizeAndPositionComponents);
            }
        });

        System.out.println("InfoPage structure initialized.");
    }

    private void resizeAndPositionComponents() {
        SwingUtilities.invokeLater(() -> {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            if (backgroundPanel != null) backgroundPanel.setBounds(0, 0, w, h);
            if (mainContentPanel != null) mainContentPanel.setBounds(0, 0, w, h);

            layeredPane.revalidate();
            layeredPane.repaint();
        });
    }


    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JButton backButton = createIconButton(BACK_ICON_PATH, "\u2190", "Back to Login");
        backButton.addActionListener(e -> {
            System.out.println("InfoPage Back button clicked - returning to Login Page");
            mainFrame.switchToLoginPage();
        });
        
        //Top panel
        JPanel topInfoPanel = new JPanel();
        topInfoPanel.setLayout(new BoxLayout(topInfoPanel, BoxLayout.Y_AXIS));

        //Top panel app logo
        JLabel topLogo = createLogoLabel(LOGO_IMAGE_PATH, 120, 60);

        //Top panel respository part 
        JPanel midPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        midPanel.setOpaque(false);
        JLabel gitHubLogo = createLogoLabel(GITHUB_IMAGE_PATH, 30, 30);

        JLabel aboutLabel = new JLabel("Repository");
        aboutLabel.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(20f) : new Font("SansSerif", Font.BOLD, 30));
        aboutLabel.setForeground(Color.BLACK);

        midPanel.add(gitHubLogo, BorderLayout.WEST);
        midPanel.add(aboutLabel, BorderLayout.EAST);
        

        //version label
        versionLabel = new JLabel("Version 1.0.0");
        versionLabel.setForeground(Color.DARK_GRAY); 

        topInfoPanel.add(topLogo, BorderLayout.NORTH);
        topInfoPanel.add(midPanel, BorderLayout.CENTER);
        topInfoPanel.add(versionLabel, BorderLayout.SOUTH);
        topInfoPanel.setOpaque(false);


        topLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        midPanel.setAlignmentX(Component.CENTER_ALIGNMENT); // And its children need to be managed too
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel rightPlaceholder = new JLabel();
        rightPlaceholder.setPreferredSize(backButton.getPreferredSize());

        topPanel.add(backButton, BorderLayout.WEST);
        topPanel.add(topInfoPanel, BorderLayout.CENTER);
        topPanel.add(rightPlaceholder, BorderLayout.EAST);
        

        return topPanel;
    }

    private JPanel createCenterContentPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 40, 40));
        centerPanel.setOpaque(false);

        infoText = "AnoChat is a secure end to end encrypted, disposable chat application, enabling anonymous real-time converstion";
                        
        infoTextArea = new JTextArea(infoText);
        infoTextArea.setFont(new Font("SansSerif", Font.PLAIN, 18));
        infoTextArea.setForeground(Color.BLACK);
        infoTextArea.setLineWrap(true);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setEditable(false);
        infoTextArea.setFocusable(false);
        infoTextArea.setOpaque(false);
        infoTextArea.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(infoTextArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200, 150), 1));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        DefaultCaret caret = (DefaultCaret) infoTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        // scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());

        centerPanel.add(scrollPane, BorderLayout.CENTER);

        return centerPanel;
    }

    private JPanel createBottomContentPanel(){
        JPanel bottomContentPanel = new JPanel();
        bottomContentPanel.setLayout(new BorderLayout());
        bottomContentPanel.setOpaque(false);


        return bottomContentPanel;
    }

    private JLabel createLogoLabel(String path, int width, int height) {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        try {
            URL imgUrl = getClass().getResource(path);
            if (imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                if (icon.getIconWidth() > 0) {
                    // Scale logo based on width - CHANGE SCALING HINT HERE
                    Image scaledImage = icon.getImage().getScaledInstance(
                            width,                         // target width (-1 for height maintains aspect ratio)
                            -1,                          // target height
                            Image.SCALE_SMOOTH     // <<--- Use REPLICATE for sharpness
                    );
                    label.setIcon(new ImageIcon(scaledImage));
                    // Set preferred size based on scaled icon to help layout
                    label.setPreferredSize(new Dimension(((ImageIcon)label.getIcon()).getIconWidth(), ((ImageIcon)label.getIcon()).getIconHeight()));
                } else throw new Exception("Logo ImageIcon invalid");
            } else { throw new Exception("Logo resource not found"); }
        } catch (Exception e) {
            System.err.println("ERROR loading logo (" + path + "): " + e.getMessage());
            label.setText("AnonChat"); label.setForeground(Color.DARK_GRAY);
            label.setFont(MainFrame.sansationBold.deriveFont(24f));
            label.setPreferredSize(new Dimension((width + 30), height));
        }
        return label;
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
                 if (icon.getImage() != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                    Image scaledImage = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                    button.setIcon(new ImageIcon(scaledImage));
                    System.out.println("[createIconButton] Icon loaded and scaled: " + iconPath);
                } else {
                     throw new IOException("ImageIcon created but image data is invalid or dimensions are zero for: " + iconPath);
                }
            } else {
                throw new IOException("Icon resource not found at path: " + iconPath);
            }
        } catch (Exception ex) {
            System.err.println("Warning: Could not load icon " + iconPath + ". Using text fallback. Error: " + ex.getMessage());
            button.setText(fallbackText);
            button.setFont(new Font("SansSerif", Font.BOLD, 20));
            button.setForeground(Color.WHITE);
        }
        return button;
    }


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

} // End InfoPage class