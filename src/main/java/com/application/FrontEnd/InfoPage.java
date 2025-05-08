package com.application.FrontEnd;

// Core Swing & AWT Imports
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;

// Image Loading Imports
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.awt.GridBagConstraints;

public class InfoPage extends JPanel {

    // --- References ---
    private MainFrame mainFrame;

    // --- Constants ---
    private static final String BACK_ICON_PATH = "/com/application/FrontEnd/images/ICON_Back.png";
    // !!! CRITICAL: Update this path to the light grey/blue wave background image from your target screenshot !!!
    private static final String INFO_PAGE_BACKGROUND_PATH = "/com/application/FrontEnd/images/BG_LoginPage.jpg"; // EXAMPLE - RENAME THIS
    private static final String LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/ICON_Logo.png";
    private static final String GITHUB_IMAGE_PATH = "/com/application/FrontEnd/images/github.png";

    // --- UI Component Fields ---
    private JLayeredPane layeredPane;
    private BackgroundImagePanel backgroundPanel;
    private JPanel mainContentPanel; // Panel for all info sections, to be centered
    private JButton backButton;       // Separate back button
    private JTextArea infoTextArea;
    private JLabel versionLabel;
    // private JLabel bottomScreenVersionLabel; // Optional, for v1.0.0 at screen bottom-right

    private String infoText;

    public InfoPage(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout());
        setOpaque(false); // InfoPage JPanel itself is transparent

        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        // Layer 0: Background
        backgroundPanel = new BackgroundImagePanel(INFO_PAGE_BACKGROUND_PATH);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

        // Layer 1: Main Content Block (to be centered)
        mainContentPanel = new JPanel(new BorderLayout(0, 15)); // 0 hgap, 15 vgap
        mainContentPanel.setOpaque(false); // Transparent to see layeredPane's background

        JPanel topInfoSection = createTopInfoSection();
        mainContentPanel.add(topInfoSection, BorderLayout.NORTH);

        JPanel centerTextSection = createCenterTextSection();
        mainContentPanel.add(centerTextSection, BorderLayout.CENTER);

        JPanel bottomContributorsSection = createBottomContributorsSection();
        mainContentPanel.add(bottomContributorsSection, BorderLayout.SOUTH);

        layeredPane.add(mainContentPanel, JLayeredPane.PALETTE_LAYER); // e.g., Layer 100

        // Layer 2: Floating UI Elements (Back Button)
        backButton = createIconButton(BACK_ICON_PATH, "\u2190", "Back to Login");
        backButton.addActionListener(e -> mainFrame.switchToLoginPage());
        layeredPane.add(backButton, Integer.valueOf(JLayeredPane.MODAL_LAYER)); // e.g., Layer 200

        // Optional: If you want a v1.0.0 at the very bottom right corner of the screen
        // bottomScreenVersionLabel = new JLabel("v1.0.0");
        // bottomScreenVersionLabel.setForeground(Color.BLACK);
        // bottomScreenVersionLabel.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(12f) : new Font("SansSerif", Font.PLAIN, 12));
        // layeredPane.add(bottomScreenVersionLabel, Integer.valueOf(JLayeredPane.MODAL_LAYER + 1));


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

        System.out.println("InfoPage structure initialized with separate back button.");
    }

    private void resizeAndPositionComponents() {
        SwingUtilities.invokeLater(() -> {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            if (backgroundPanel != null) backgroundPanel.setBounds(0, 0, w, h);

            if (mainContentPanel != null) {
                Dimension panelPrefSize = mainContentPanel.getPreferredSize();

                int panelW = Math.min(panelPrefSize.width, w - 100); 
                panelW = Math.max(panelW, 500); 

                int panelH = panelPrefSize.height;

                int x = (w - panelW) / 2;
                int y = (h - panelH) / 2;
                y = Math.max(30, y); 

                mainContentPanel.setBounds(x, y, panelW, panelH);
            }

            if (backButton != null) {
                Dimension btnSize = backButton.getPreferredSize();
                backButton.setBounds(30, 30, btnSize.width, btnSize.height); // Position top-left
            }

            layeredPane.revalidate();
            layeredPane.repaint();
        });
    }

    private JPanel createTopInfoSection() {
        JPanel topInfoSectionPanel = new JPanel(new GridBagLayout());
        topInfoSectionPanel.setOpaque(false);
        topInfoSectionPanel.setBorder(BorderFactory.createEmptyBorder(50, 20, 15, 20)); // T,L,B,R

        JPanel contentStack = new JPanel();
        contentStack.setLayout(new BoxLayout(contentStack, BoxLayout.Y_AXIS));
        contentStack.setOpaque(false);

        JLabel topLogo = createLogoLabel(LOGO_IMAGE_PATH, 100, 100); // Height 0 for aspect
        topLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        topLogo.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JPanel midPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        midPanel.setOpaque(false);
        JLabel gitHubLogo = createLogoLabel(GITHUB_IMAGE_PATH, 24, 0); // Height 0 for aspect
        JLabel repositoryLabel = new JLabel("Repository");
        repositoryLabel.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(18f) : new Font("SansSerif", Font.PLAIN, 18));
        repositoryLabel.setForeground(Color.BLACK);
        midPanel.add(gitHubLogo);
        midPanel.add(repositoryLabel);
        midPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        midPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        versionLabel = new JLabel("Version 1.0.0");
        versionLabel.setForeground(Color.DARK_GRAY);
        versionLabel.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(15f) : new Font("SansSerif", Font.PLAIN, 15));
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentStack.add(topLogo);
        contentStack.add(midPanel);
        contentStack.add(versionLabel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        topInfoSectionPanel.add(contentStack, gbc);

        return topInfoSectionPanel;
    }

    private JPanel createCenterTextSection() {
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(10, 70, 10, 50)); // T,L,B,R

        infoText = "AnoChat is a secure end to end encrypted, disposable\n" +
                   "chat application enabling anonymous real-time\n" +
                   "conversations.";

        infoTextArea = new JTextArea(infoText);
        infoTextArea.setColumns(30); // Hint for preferred width
        infoTextArea.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(17f) : new Font("SansSerif", Font.PLAIN, 17));
        infoTextArea.setForeground(Color.BLACK);
        infoTextArea.setLineWrap(true);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setEditable(false);
        infoTextArea.setFocusable(false);
        infoTextArea.setOpaque(false);
        infoTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoTextArea.setAlignmentY(Component.CENTER_ALIGNMENT);

        JScrollPane scrollPane = new JScrollPane(infoTextArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        DefaultCaret caret = (DefaultCaret) infoTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        centerWrapper.add(scrollPane, gbc);

        return centerWrapper;
    }

    private JPanel createBottomContributorsSection() {
        JPanel bottomOuterWrapper = new JPanel(new GridBagLayout());
        bottomOuterWrapper.setOpaque(false);
        bottomOuterWrapper.setBorder(BorderFactory.createEmptyBorder(10, 80, 20, 80)); // T,L,B,R

        JPanel bottomContentColumns = new JPanel(new GridLayout(1, 2, 60, 0)); // 1 row, 2 cols, 60px hgap
        bottomContentColumns.setOpaque(false);

        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);
        leftColumn.add(createContributorLabel("Mohammad Monis Ahmed"));
        leftColumn.add(createContributorLink("GitHub", "https://github.com/Monis-dev")); // REPLACE
        leftColumn.add(createContributorLink("LinkedIn", "https://linkedin.com/in/your-actual-profile1")); // REPLACE

        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);
        rightColumn.add(createContributorLabel("Mohammad Shees Abdulla"));
        rightColumn.add(createContributorLink("GitHub", "https://github.com/Realm07")); // REPLACE
        rightColumn.add(createContributorLink("LinkedIn", "https://linkedin.com/in/your-actual-profile2")); // REPLACE

        bottomContentColumns.add(leftColumn);
        bottomContentColumns.add(rightColumn);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER; // Center the two-column block
        bottomOuterWrapper.add(bottomContentColumns, gbc);
        return bottomOuterWrapper;
    }

    private JLabel createContributorLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(16f) : new Font("SansSerif", Font.BOLD, 16));
        label.setForeground(Color.BLACK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT); // Align within BoxLayout column
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return label;
    }

    private JLabel createContributorLink(String platform, String url) {
        JLabel label = new JLabel(platform);
        label.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(14f) : new Font("SansSerif", Font.PLAIN, 14));
        label.setForeground(Color.DARK_GRAY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT); // Align within BoxLayout column
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI(url));
                    } else {
                        System.err.println("Desktop browse action not supported for: " + url);
                    }
                } catch (Exception ex) {
                    System.err.println("Could not open link: " + url + " - " + ex.getMessage());
                    JOptionPane.showMessageDialog(InfoPage.this,
                            "Could not open link: " + url + "\nError: " + ex.getMessage(),
                            "Link Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            @Override public void mouseEntered(MouseEvent e) { label.setForeground(new Color(0, 102, 204)); /* Link hover blue */ }
            @Override public void mouseExited(MouseEvent e) { label.setForeground(Color.DARK_GRAY); /* Reset color */ }
        });
        return label;
    }

    private JLabel createLogoLabel(String path, int targetWidth, int targetHeight) {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        try {
            URL imgUrl = getClass().getResource(path);
            if (imgUrl == null) throw new IOException("Resource not found: " + path);
    
            ImageIcon icon = new ImageIcon(imgUrl);
            Image originalImage = icon.getImage();
    
            if (originalImage == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                throw new IOException("Image data invalid or dimensions are zero for: " + path);
            }
    
            int finalWidth = targetWidth;
            int finalHeight = targetHeight;
            int imgOrigW = icon.getIconWidth();
            int imgOrigH = icon.getIconHeight();
    
            if (targetWidth > 0 && targetHeight <= 0) { // Scale by width, calculate height
                finalHeight = (int) (((double)imgOrigH / imgOrigW) * targetWidth);
            } else if (targetHeight > 0 && targetWidth <= 0) { // Scale by height, calculate width
                finalWidth = (int) (((double)imgOrigW / imgOrigH) * targetHeight);
            } else if (targetWidth <= 0 && targetHeight <= 0) { // No target specified, use original (or a default)
                finalWidth = imgOrigW;
                finalHeight = imgOrigH;
            }
    
            if (targetWidth > 0) { 
                 finalHeight = (int) (((double)imgOrigH / imgOrigW) * targetWidth);
                 finalWidth = targetWidth;
            } else if (targetHeight > 0) { 
                 finalWidth = (int) (((double)imgOrigW / imgOrigH) * targetHeight);
                 finalHeight = targetHeight;
            } else { // Original size
                finalWidth = imgOrigW; finalHeight = imgOrigH;
            }
            finalWidth = Math.max(1, finalWidth);
            finalHeight = Math.max(1, finalHeight);
    
    
            Image scaledImage = originalImage.getScaledInstance(finalWidth, finalHeight, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            label.setIcon(scaledIcon);
            label.setPreferredSize(new Dimension(scaledIcon.getIconWidth(), scaledIcon.getIconHeight()));
    
        } catch (Exception e) {
            // ... (fallback logic as before) ...
            System.err.println("ERROR loading/scaling logo (" + path + "): " + e.getMessage());
            label.setText("ImgErr"); label.setForeground(Color.RED);
            Font fallbackFont = MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(16f) : new Font("SansSerif", Font.BOLD, 16);
            label.setFont(fallbackFont);
            FontMetrics fm = label.getFontMetrics(fallbackFont);
            int textWidth = fm.stringWidth(label.getText());
            // Use target dimensions for fallback box if provided, else use text size
            int prefW = targetWidth > 0 ? targetWidth : (textWidth + 20);
            int prefH = targetHeight > 0 ? targetHeight : (fm.getHeight() + 10);
            label.setPreferredSize(new Dimension(prefW, prefH));
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
        Dimension iconButtonSize = new Dimension(30, 30); // Clickable area
        button.setPreferredSize(iconButtonSize);

        try {
            URL iconUrl = getClass().getResource(iconPath);
            if (iconUrl == null) throw new IOException("Icon resource not found: " + iconPath);
            ImageIcon icon = new ImageIcon(iconUrl);
            if (icon.getImage() == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                throw new IOException("Icon image data invalid or dimensions are zero for: " + iconPath);
            }
            Image scaledImage = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH); // Visual size
            button.setIcon(new ImageIcon(scaledImage));
        } catch (Exception ex) {
            System.err.println("Warning: Icon load failed '" + iconPath + "'. Fallback text: " + fallbackText + ". Error: " + ex.getMessage());
            button.setText(fallbackText);
            button.setFont(new Font("SansSerif", Font.BOLD, 20));
            button.setForeground(Color.BLACK); // Ensure fallback is visible on light background
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
                if (imgUrl == null) throw new IOException("Resource not found: " + imagePath);
                this.backgroundImage = ImageIO.read(imgUrl);
                if (this.backgroundImage == null) throw new IOException("ImageIO returned null for " + imagePath);
                System.out.println("[BGPanel] Loaded background: " + imagePath);
            } catch (IOException e) {
                this.errorMessage = "Error loading background (" + imagePath + "): " + e.getMessage();
                System.err.println(errorMessage);
                this.backgroundImage = null;
            }
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Clears the panel
            if (backgroundImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                int panelW = getWidth();
                int panelH = getHeight();
                if (panelW <= 0 || panelH <= 0) { g2d.dispose(); return; }

                int imgW = backgroundImage.getWidth(this);
                int imgH = backgroundImage.getHeight(this);
                if (imgW <= 0 || imgH <= 0) { g2d.dispose(); return; }

                double imgAspect = (double) imgW / imgH;
                double panelAspect = (double) panelW / panelH;
                int drawW, drawH, drawX, drawY;

                if (panelAspect > imgAspect) {
                    drawW = panelW; drawH = (int) (panelW / imgAspect); drawX = 0; drawY = (panelH - drawH) / 2;
                } else {
                    drawH = panelH; drawW = (int) (panelH * imgAspect); drawX = (panelW - drawW) / 2; drawY = 0;
                }
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(backgroundImage, drawX, drawY, drawW, drawH, this);
                g2d.dispose();
            } else {
                // Fallback background color if image fails to load
                g.setColor(new Color(218, 223, 229)); // Light grey
                g.fillRect(0, 0, getWidth(), getHeight());
                // Error message display
                g.setColor(Color.RED);
                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g.getFontMetrics();
                String errorLine1 = "Background Image Load Error!";
                String errorLine2 = errorMessage != null ? errorMessage : "Path: " + imagePathUsed;
                int yPos = getHeight() / 2 - fm.getHeight();
                g.drawString(errorLine1, (getWidth() - fm.stringWidth(errorLine1)) / 2, yPos);
                yPos += fm.getHeight() + 2;
                g.drawString(errorLine2, (getWidth() - fm.stringWidth(errorLine2)) / 2, yPos);
            }
        }
    }
}