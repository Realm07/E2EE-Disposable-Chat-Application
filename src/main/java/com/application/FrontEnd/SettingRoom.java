package com.application.FrontEnd;

/////////////////////////////////////////////////
/// to display initials or images call the InitialCircle here and put a condition if setDefault send Initial else render image 

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.net.URL;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.awt.event.*;
import java.net.URI;

import com.application.FrontEnd.components.CustomButton;
// Assuming CustomLabel and CustomTextField are not strictly needed if we use standard components + styling
// import com.application.FrontEnd.components.CustomLabel;
// import com.application.FrontEnd.components.CustomTextField;


public class SettingRoom extends JPanel {

    // --- UI Components ---
    private JCheckBox saveUsernameCheckBox;
    private JCheckBox trustLinksCheckBox;
    private JButton backButton;
    private String selectedAvatarPath = null;
    private Object selectedChatBackgroundSource = new Color(45,45,45); // Default BG color

    // --- Layout Panels ---
    private JPanel mainContentPanel;
    private JLayeredPane layeredPane;
    private BackgroundImagePanel backgroundPanel;

    // --- Constants ---
    private static final String OVERALL_BACKGROUND_IMAGE_PATH = "/com/application/FrontEnd/images/BG_LoginPage.jpg";
    private static final String BACK_ICON_PATH = "/com/application/FrontEnd/images/ICON_Back.png";
    // Remove CHECK_MARK_ICON_PATH as currentProfilePicDisplayLabel is removed
    // private static final String CHECK_MARK_ICON_PATH = "/com/application/FrontEnd/images/ICON_Checkmark_Large.png";

    private static final String DEER_ICON_PATH = "/com/application/FrontEnd/images/Animal/deer.png";
    private static final String BUTTERFLY_ICON_PATH = "/com/application/FrontEnd/images/Animal/butterfly.png";
    private static final String CAT_ICON_PATH = "/com/application/FrontEnd/images/Animal/cat.png";
    private static final String SHARK_ICON_PATH = "/com/application/FrontEnd/images/Animal/shark.png";
    private static final String JAGUAR_ICON_PATH = "/com/application/FrontEnd/images/Animal/jaguar.png";
    private static final String TURTLE_ICON_PATH = "/com/application/FrontEnd/images/Animal/turtle.png";
    private static final String KOI_ICON_PATH = "/com/application/FrontEnd/images/Animal/koi.png";
    private static final String MACAW_ICON_PATH = "/com/application/FrontEnd/images/Animal/macaw.png";
    private static final String PELICAN_ICON_PATH = "/com/application/FrontEnd/images/Animal/pelican.png";

    private String[] animalIconPaths = {DEER_ICON_PATH, BUTTERFLY_ICON_PATH, CAT_ICON_PATH, KOI_ICON_PATH, SHARK_ICON_PATH, MACAW_ICON_PATH, JAGUAR_ICON_PATH, TURTLE_ICON_PATH, PELICAN_ICON_PATH};
    // Remove currentProfilePicDisplayLabel as per request
    // private JLabel currentProfilePicDisplayLabel;
    private MainFrame mainFrame;


    public SettingRoom(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setOpaque(false);

        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        backgroundPanel = new BackgroundImagePanel(OVERALL_BACKGROUND_IMAGE_PATH);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

        mainContentPanel = createMainContentPanel();
        layeredPane.add(mainContentPanel, JLayeredPane.PALETTE_LAYER);

        // this.backButton = createIconButton(BACK_ICON_PATH, "\u2190", "Back");
        // this.backButton.addActionListener(e -> {
        //     if (this.mainFrame != null) {
        //         mainFrame.switch();
        //     }
        // });
        // layeredPane.add(this.backButton, Integer.valueOf(JLayeredPane.MODAL_LAYER));

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { resizeAndLayoutLayeredComponents(); }
            @Override public void componentShown(ComponentEvent e) { SwingUtilities.invokeLater(SettingRoom.this::resizeAndLayoutLayeredComponents); }
        });
    }

    private JLabel createSectionTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(20f) : new Font("SansSerif", Font.BOLD, 20));
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 0)); // Added left padding of 5
        label.setAlignmentX(Component.LEFT_ALIGNMENT); // Ensure it aligns left in BoxLayout
        return label;
    }

    private JPanel createCheckBoxSettingItem(String labelText, boolean isSelectedInitially, String propertyName) {
        JPanel itemPanel = new JPanel(new BorderLayout(15, 0));
        itemPanel.setOpaque(true);
        itemPanel.setBackground(new Color(55, 55, 55));
        itemPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel label = new JLabel(labelText);
        label.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(16f) : new Font("SansSerif", Font.PLAIN, 16));
        label.setForeground(Color.LIGHT_GRAY);

        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(isSelectedInitially);
        checkBox.setOpaque(false);
        checkBox.setPreferredSize(new Dimension(25, 25));
        checkBox.setHorizontalAlignment(SwingConstants.CENTER);

        itemPanel.add(label, BorderLayout.CENTER);
        itemPanel.add(checkBox, BorderLayout.EAST);

        if ("Save Username for Later".equals(labelText)) saveUsernameCheckBox = checkBox;
        else if ("Always trust download links".equals(labelText)) trustLinksCheckBox = checkBox;
        return itemPanel;
    }

    private JPanel createMainContentPanel() {
        JPanel settingsAreaPanel = new JPanel(new BorderLayout(0, 30));
        settingsAreaPanel.setOpaque(true);
        settingsAreaPanel.setBackground(new Color(25, 25, 25));
        settingsAreaPanel.setBorder(BorderFactory.createEmptyBorder(35, 50, 35, 50));

        JLabel titleLabel = new JLabel("Settings");
        titleLabel.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(32f) : new Font("SansSerif", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0,0,15,0));
        settingsAreaPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel contentStack = new JPanel();
        contentStack.setLayout(new BoxLayout(contentStack, BoxLayout.Y_AXIS));
        contentStack.setOpaque(false);
        // contentStack.setAlignmentX(Component.LEFT_ALIGNMENT); // This won't directly center contentStack if its parent is BorderLayout.CENTER

        // General Section
        JPanel generalSection = new JPanel();
        generalSection.setLayout(new BoxLayout(generalSection, BoxLayout.Y_AXIS));
        generalSection.setOpaque(false);
        generalSection.setAlignmentX(Component.LEFT_ALIGNMENT); // For content within generalSection

        generalSection.add(createSectionTitleLabel("General")); // Label will be left-aligned due to its own setAlignmentX and border
        generalSection.add(Box.createRigidArea(new Dimension(0, 8)));
        JPanel saveUserItem = createCheckBoxSettingItem("Save Username for Later", true, "saveUsername");
        saveUserItem.setAlignmentX(Component.LEFT_ALIGNMENT); // Align the whole item bar left
        generalSection.add(saveUserItem);
        generalSection.add(Box.createRigidArea(new Dimension(0, 12)));
        JPanel trustLinksItem = createCheckBoxSettingItem("Always trust download links", false, "trustLinks");
        trustLinksItem.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 120, 255), 2),
                BorderFactory.createEmptyBorder(13, 18, 13, 18)
        ));
        trustLinksItem.setAlignmentX(Component.LEFT_ALIGNMENT); // Align the whole item bar left
        generalSection.add(trustLinksItem);
        contentStack.add(generalSection);

        contentStack.add(Box.createRigidArea(new Dimension(0, 35)));

        // User Section
        JPanel userSection = new JPanel();
        userSection.setLayout(new BoxLayout(userSection, BoxLayout.Y_AXIS));
        userSection.setOpaque(false);
        userSection.setAlignmentX(Component.LEFT_ALIGNMENT); // For content within userSection

        userSection.add(createSectionTitleLabel("User")); // Label will be left-aligned
        userSection.add(Box.createRigidArea(new Dimension(0, 8)));
        JPanel userCustomization = createUserCustomizationPanel();
        userCustomization.setAlignmentX(Component.LEFT_ALIGNMENT); // Align the customization panel group left
        userSection.add(userCustomization);
        contentStack.add(userSection);
        
        contentStack.add(Box.createVerticalGlue());

        settingsAreaPanel.add(contentStack, BorderLayout.CENTER);
        return settingsAreaPanel;
    }

    private JPanel createUserCustomizationPanel() {
        JPanel customizationPanel = new JPanel(new GridBagLayout());
        customizationPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.33;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST; // Align this card to the top-left of its cell
        gbc.insets = new Insets(0, 0, 0, 10);
        customizationPanel.add(createProfilePicturePanel(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.67;
        gbc.insets = new Insets(0, 10, 0, 0);
        // gbc.anchor will still be NORTHWEST from previous setting for this column as well
        customizationPanel.add(createChatBackgroundPanel(), gbc);

        return customizationPanel;
    }

    private JPanel createProfilePicturePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setOpaque(true);
        panel.setBackground(new Color(45, 45, 45));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Profile Picture");
        title.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(16f) : new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(Color.LIGHT_GRAY);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);

        // Removed currentProfilePicDisplayLabel from here
        // The avatarGrid will be in the CENTER now

        JPanel bottomSection = new JPanel(new BorderLayout(0,10));
        bottomSection.setOpaque(false);

        JPanel avatarGrid = new JPanel(new GridLayout(0, 3, 8, 8));
        avatarGrid.setOpaque(false);
        avatarGrid.setBorder(BorderFactory.createEmptyBorder(10,0,10,0));
        for (String path : animalIconPaths) {
            JLabel avatarLabel = createSelectionImageLabel(path, 45, 45, avatarGrid, true, true);
            avatarGrid.add(avatarLabel);
        }
        // Instead of adding avatarGrid to bottomSection's CENTER, we add it directly to panel's CENTER
        // This makes avatarGrid the main central component of the profile picture panel
        panel.add(avatarGrid, BorderLayout.CENTER);


        JButton setDefaultButton = new CustomButton("Set Default", 0, 35, new Color(180, 80, 80));
        if (MainFrame.sansationRegular != null) setDefaultButton.setFont(MainFrame.sansationRegular.deriveFont(14f));
        setDefaultButton.setForeground(Color.WHITE);
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonWrapper.setOpaque(false);
        buttonWrapper.add(setDefaultButton);
        setDefaultButton.setPreferredSize(new Dimension(120,35));

        // Add the button wrapper directly to the panel's SOUTH
        panel.add(buttonWrapper, BorderLayout.SOUTH);
        // bottomControls is no longer needed as its children are now directly in 'panel'
        return panel;
    }

    // updateCurrentProfilePicDisplay is no longer needed as currentProfilePicDisplayLabel is removed
    // private void updateCurrentProfilePicDisplay(String imagePath) { ... }


    private JLabel createSelectionImageLabel(String path, int w, int h, JPanel parentGroup, boolean isAvatar, boolean noDefaultBorder) {
        JLabel label = createGenericImageLabel(path, w, h);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (noDefaultBorder) {
            label.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        } else {
            label.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        }
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (Component c : parentGroup.getComponents()) {
                    if (c instanceof JLabel) {
                         if (noDefaultBorder) ((JLabel)c).setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
                         else ((JLabel)c).setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                    }
                }
                label.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
                if (isAvatar) {
                    selectedAvatarPath = path;
                    // Removed call to updateCurrentProfilePicDisplay
                    System.out.println("Avatar selected: " + path);
                } else { // This 'else' branch is for background selection images
                    selectedChatBackgroundSource = path;
                    System.out.println("Background selected (image path): " + path);
                }
            }
        });
        return label;
    }

    private JPanel createChatBackgroundPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(true);
        panel.setBackground(new Color(45, 45, 45));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Chat Background");
        title.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(16f) : new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(Color.LIGHT_GRAY);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);

        JPanel backgroundGrid = new JPanel(new GridLayout(0, 3, 10, 10));
        backgroundGrid.setOpaque(false);

        JPanel defaultBgPanel = createBackgroundPreviewPanel("Default", new Color(30,30,30), backgroundGrid, true);
        defaultBgPanel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
        backgroundGrid.add(defaultBgPanel);

        backgroundGrid.add(createBackgroundPreviewPanel("Ocean", new Color(60, 120, 180), backgroundGrid, true));
        backgroundGrid.add(createBackgroundPreviewPanel("Forest", new Color(50, 100, 50), backgroundGrid, true));
        backgroundGrid.add(createBackgroundPreviewPanel("Sunset", new Color(200, 100, 50), backgroundGrid, true));
        backgroundGrid.add(createBackgroundPreviewPanel("Waves", OVERALL_BACKGROUND_IMAGE_PATH, backgroundGrid, true));
        backgroundGrid.add(createBackgroundPreviewPanel("Dark Tech", "/com/application/FrontEnd/images/BG_PublicRooms.png", backgroundGrid, true)); // Corrected path typo from image

        panel.add(backgroundGrid, BorderLayout.CENTER); // Directly add the grid
        return panel;
    }

    private JPanel createBackgroundPreviewPanel(String name, Object bgSource, JPanel parentGroup, boolean noDefaultBorder) {
        JPanel previewPanel = new JPanel(new BorderLayout(0,3));
        previewPanel.setPreferredSize(new Dimension(100, 75));
        if (noDefaultBorder) {
            previewPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2)); // Slightly more empty border
        } else {
            previewPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }
        previewPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(11f) : new Font("SansSerif", Font.PLAIN, 11));

        if (bgSource instanceof Color) {
            previewPanel.setBackground((Color) bgSource);
            previewPanel.setOpaque(true);
            if (isColorLight((Color)bgSource)) nameLabel.setForeground(Color.BLACK); else nameLabel.setForeground(Color.WHITE);
        } else if (bgSource instanceof String) {
            JLabel imgLabel = createGenericImageLabel((String)bgSource, 96, 56); // Adjusted for border
            previewPanel.add(imgLabel, BorderLayout.CENTER);
            previewPanel.setOpaque(false);
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setBackground(new Color(0,0,0,120));
            nameLabel.setOpaque(true);
        }
        previewPanel.add(nameLabel, BorderLayout.SOUTH);

        previewPanel.addMouseListener(new MouseAdapter() {
             @Override
            public void mouseClicked(MouseEvent e) {
                for (Component c : parentGroup.getComponents()) {
                    if (c instanceof JPanel) {
                        if (noDefaultBorder) ((JPanel)c).setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
                        else ((JPanel)c).setBorder(BorderFactory.createLineBorder(Color.GRAY));
                    }
                }
                previewPanel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
                selectedChatBackgroundSource = bgSource;
                System.out.println("Chat background selected: " + name);
            }
        });
        return previewPanel;
    }

    private boolean isColorLight(Color color) {
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        return luminance > 0.5;
    }

    private JLabel createGenericImageLabel(String path, int targetWidth, int targetHeight) {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        try {
            URL imgUrl = getClass().getResource(path);
            if (imgUrl == null) throw new IOException("Resource not found: " + path);
            ImageIcon icon = new ImageIcon(imgUrl);
            Image originalImage = icon.getImage();
            if (originalImage == null || icon.getIconWidth() <= 0) {
                throw new IOException("Image data invalid or dimensions are zero for: " + path);
            }
            int scaledW = targetWidth;
            int scaledH = targetHeight;
            if (targetHeight <= 0 && icon.getIconWidth() > 0) {
                scaledH = (int) (((double) icon.getIconHeight() / icon.getIconWidth()) * targetWidth);
            } else if (targetWidth <= 0 && icon.getIconHeight() > 0) {
                scaledW = (int) (((double) icon.getIconWidth() / icon.getIconHeight()) * targetHeight);
            }
            scaledW = Math.max(1, scaledW);
            scaledH = Math.max(1, scaledH);
            Image scaledImage = originalImage.getScaledInstance(scaledW, scaledH, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(scaledImage));
            label.setPreferredSize(new Dimension(scaledW, scaledH));
        } catch (Exception e) {
            System.err.println("ERROR loading image for label (" + path + "): " + e.getMessage());
            label.setText("X");
            label.setForeground(Color.RED);
            label.setFont(new Font("SansSerif", Font.BOLD, Math.min(targetWidth, targetHeight) / 2 + 2));
            label.setPreferredSize(new Dimension(targetWidth > 0 ? targetWidth : 30, targetHeight > 0 ? targetHeight : 30));
            // Removed border on error to comply with "no border on images"
        }
        return label;
    }

    private void saveSettings() {
        boolean saveUser = false; if(saveUsernameCheckBox!=null) saveUser = saveUsernameCheckBox.isSelected();
        boolean trustLinks = false; if(trustLinksCheckBox!=null) trustLinks = trustLinksCheckBox.isSelected();
        System.out.println("Saving Settings:");
        System.out.println("  Save Username: " + saveUser);
        System.out.println("  Trust Links: " + trustLinks);
        System.out.println("  Avatar: " + selectedAvatarPath);
        System.out.println("  Chat Background: " + selectedChatBackgroundSource);
        JOptionPane.showMessageDialog(this, "Settings applied (simulation).", "Settings Update", JOptionPane.INFORMATION_MESSAGE);
    }

    private void resizeAndLayoutLayeredComponents() {
        SwingUtilities.invokeLater(() -> {
            int layeredWidth = getWidth(); int layeredHeight = getHeight(); if (layeredWidth <= 0 || layeredHeight <= 0) return;
            if (backgroundPanel != null) backgroundPanel.setBounds(0, 0, layeredWidth, layeredHeight);
            if (mainContentPanel != null) {
                Dimension panelPrefSize = mainContentPanel.getPreferredSize();
                int panelW = Math.min(panelPrefSize.width + 150, layeredWidth - 40); // Increased allowance
                panelW = Math.max(800, panelW); // Increased min width
                int panelH = Math.min(panelPrefSize.height + 150, layeredHeight - 40); // Increased allowance
                panelH = Math.max(700, panelH); // Increased min height
                int x = (layeredWidth - panelW) / 2; int y = (layeredHeight - panelH) / 2; y = Math.max(y, 20);
                mainContentPanel.setBounds(x, y, panelW, panelH);
            }
            if (backButton != null) { Dimension btnSize = backButton.getPreferredSize(); backButton.setBounds(25, 25, btnSize.width, btnSize.height); }
            layeredPane.revalidate(); layeredPane.repaint();
        });
    }

    private JButton createIconButton(String iconPath, String fallbackText, String tooltip) {
        JButton button = new JButton(); button.setToolTipText(tooltip); button.setPreferredSize(new Dimension(40, 40)); button.setFocusPainted(false); button.setBorderPainted(false); button.setContentAreaFilled(false); button.setOpaque(false); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        try {
            URL iconUrl = getClass().getResource(iconPath); if (iconUrl == null) throw new IOException("Icon resource not found: " + iconPath);
            ImageIcon icon = new ImageIcon(iconUrl); if (icon.getImage() == null || icon.getIconWidth() <= 0) throw new IOException("Icon image data invalid for: " + iconPath);
            Image scaledImage = icon.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH); button.setIcon(new ImageIcon(scaledImage));
        } catch (Exception ex) {
            System.err.println("Warning: Icon load failed for '" + iconPath + "'. Using text. Error: " + ex.getMessage()); button.setText(fallbackText); button.setFont(new Font("Arial", Font.BOLD, 22)); button.setForeground(new Color(220,50,50));
        } return button;
    }

    private static class BackgroundImagePanel extends JPanel {
        private Image backgroundImage; private String errorMessage = null; private String imagePathUsed;
        public BackgroundImagePanel(String imagePath) { this.imagePathUsed = imagePath; try { URL imgUrl = getClass().getResource(imagePath); if (imgUrl != null) { this.backgroundImage = ImageIO.read(imgUrl); if (this.backgroundImage == null) throw new IOException("ImageIO returned null for " + imagePath); } else { throw new IOException("Resource not found: " + imagePath); } } catch (IOException e) { this.errorMessage = "Ex loading background ("+imagePath+"): " + e.getMessage(); System.err.println(errorMessage); this.backgroundImage = null; } setOpaque(true); }
        @Override protected void paintComponent(Graphics g) { super.paintComponent(g); if (backgroundImage != null) { Graphics2D g2d = (Graphics2D) g.create(); int panelW = getWidth(); int panelH = getHeight(); if (panelW <=0 || panelH <= 0) { g2d.dispose(); return; } int imgW = backgroundImage.getWidth(this); int imgH = backgroundImage.getHeight(this); if(imgW <=0 || imgH <= 0) { g2d.dispose(); return; } double imgAspect = (double) imgW / imgH; double panelAspect = (double) panelW / panelH; int drawW, drawH, drawX, drawY; if (panelAspect > imgAspect) { drawW = panelW; drawH = (int)(panelW / imgAspect); drawX = 0; drawY = (panelH - drawH) / 2; } else { drawH = panelH; drawW = (int)(panelH * imgAspect); drawX = (panelW - drawW) / 2; drawY = 0; } g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); g2d.drawImage(backgroundImage, drawX, drawY, drawW, drawH, this); g2d.dispose();
            } else { g.setColor(new Color(25, 25, 25)); g.fillRect(0, 0, getWidth(), getHeight()); g.setColor(Color.YELLOW); g.setFont(new Font("SansSerif", Font.BOLD, 14)); FontMetrics fm = g.getFontMetrics(); String text = "BG Load Error: " + (errorMessage != null ? errorMessage : "Unknown"); int msgWidth = fm.stringWidth(text); g.drawString(text, Math.max(5, (getWidth() - msgWidth) / 2), getHeight() / 2 + fm.getAscent() / 2 - fm.getHeight()/2); String pathText = "(" + imagePathUsed + ")"; msgWidth = fm.stringWidth(pathText); g.drawString(pathText, Math.max(5, (getWidth() - msgWidth) / 2), getHeight() / 2 + fm.getAscent() / 2 + fm.getHeight()/2); }
        }
    }
}