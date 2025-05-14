package com.application.FrontEnd;

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

import com.application.FrontEnd.components.CustomButton;

public class SettingRoom extends JPanel {

    private JCheckBox saveUsernameCheckBox;
    private JCheckBox trustLinksCheckBox;
    private JButton backButton;
    private String selectedAvatarPath = null;
    private JButton setDefaultButton;
    private JPanel avatarGrid;
    

    private JPanel mainContentPanel;
    private JLayeredPane layeredPane;
    private BackgroundImagePanel overallBackgroundPanel; // Renamed for clarity

    private static final String OVERALL_BACKGROUND_IMAGE_PATH = "/com/application/FrontEnd/images/BG_LoginPage.jpg";
    private static final String BACK_ICON_PATH = "/com/application/FrontEnd/images/ICON_Back.png";

    // This will store the path or Color object for the CHAT background
    private Object selectedChatBackgroundSource = OVERALL_BACKGROUND_IMAGE_PATH; // Initialize with a default

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
    private MainFrame mainFrame;
    private JPanel currentlySelectedChatBgPreview = null;
    

    private String currentImage = null;

    public SettingRoom(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        this.currentImage = OVERALL_BACKGROUND_IMAGE_PATH;

        setLayout(new BorderLayout());
        setOpaque(false);

        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        overallBackgroundPanel = new BackgroundImagePanel(currentImage);
        layeredPane.add(overallBackgroundPanel, JLayeredPane.DEFAULT_LAYER);

        mainContentPanel = createMainContentPanel();
        layeredPane.add(mainContentPanel, JLayeredPane.PALETTE_LAYER);

        this.backButton = createIconButton(BACK_ICON_PATH, "\u2190", "Back to Chat");
        this.backButton.addActionListener(e -> {
            if (this.mainFrame != null) {
                if (selectedChatBackgroundSource instanceof String) {
                    mainFrame.applyChatRoomBackground((String) selectedChatBackgroundSource);
                } else if (selectedChatBackgroundSource instanceof Color) {
                    System.out.println("Color background selected, but ChatRoom background change might only support images currently.");
                }
                mainFrame.showChatRoomCard(); // Use the method that doesn't take a path if path is handled internally by applyChatRoomBackground
            }
        });
        layeredPane.add(this.backButton, Integer.valueOf(JLayeredPane.MODAL_LAYER));

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { resizeAndLayoutLayeredComponents(); }
            @Override public void componentShown(ComponentEvent e) { SwingUtilities.invokeLater(SettingRoom.this::resizeAndLayoutLayeredComponents); }
        });
    }

    private JLabel createSectionTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(20f) : new Font("SansSerif", Font.BOLD, 20));
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
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

        JPanel generalSection = new JPanel();
        generalSection.setLayout(new BoxLayout(generalSection, BoxLayout.Y_AXIS));
        generalSection.setOpaque(false);
        generalSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        generalSection.add(createSectionTitleLabel("General"));
        generalSection.add(Box.createRigidArea(new Dimension(0, 8)));
        JPanel saveUserItem = createCheckBoxSettingItem("Save Username for Later", true, "saveUsername");
        saveUserItem.setAlignmentX(Component.LEFT_ALIGNMENT);
        generalSection.add(saveUserItem);
        generalSection.add(Box.createRigidArea(new Dimension(0, 12)));
        JPanel trustLinksItem = createCheckBoxSettingItem("Always trust download links", false, "trustLinks");
        trustLinksItem.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 120, 255), 2),
                BorderFactory.createEmptyBorder(13, 18, 13, 18)
        ));
        trustLinksItem.setAlignmentX(Component.LEFT_ALIGNMENT);
        generalSection.add(trustLinksItem);
        contentStack.add(generalSection);

        contentStack.add(Box.createRigidArea(new Dimension(0, 35)));

        JPanel userSection = new JPanel();
        userSection.setLayout(new BoxLayout(userSection, BoxLayout.Y_AXIS));
        userSection.setOpaque(false);
        userSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        userSection.add(createSectionTitleLabel("User"));
        userSection.add(Box.createRigidArea(new Dimension(0, 8)));
        JPanel userCustomization = createUserCustomizationPanel();
        userCustomization.setAlignmentX(Component.LEFT_ALIGNMENT);
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
        gbc.weightx = 0.33; // Adjusted weight
        gbc.weighty = 1.0; // Allow vertical expansion
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 0, 15); // Increased right inset
        customizationPanel.add(createProfilePicturePanel(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.67; // Adjusted weight
        gbc.insets = new Insets(0, 0, 0, 0); // Removed left inset, relying on previous right inset
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

        avatarGrid = new JPanel(new GridLayout(0, 3, 8, 8));
        avatarGrid.setOpaque(false);
        avatarGrid.setBorder(BorderFactory.createEmptyBorder(10,0,10,0));
        for (String path : animalIconPaths) {
            JLabel avatarLabel = createSelectionImageLabel(path, 45, 45, avatarGrid, true, true, this.selectedAvatarPath);
            avatarGrid.add(avatarLabel);
        }
        panel.add(avatarGrid, BorderLayout.CENTER);

        setDefaultButton = new CustomButton("Set Default", 0, 35, new Color(180, 80, 80));
        if (MainFrame.sansationRegular != null) setDefaultButton.setFont(MainFrame.sansationRegular.deriveFont(14f));
        setDefaultButton.setForeground(Color.WHITE);
        setDefaultButton.setPreferredSize(new Dimension(120,35));
        setDefaultButton.addActionListener(e -> {
            selectedAvatarPath = null; // Or a path to a default avatar image
            // You might want to update some visual indicator for avatar selection here
            System.out.println("Avatar set to default (null path).");
        });

        setDefaultButton.addActionListener(e -> {
            this.selectedAvatarPath = null; // Update SettingRoom's internal state
            System.out.println("SettingRoom: Avatar set to default (initials).");

            // Tell MainFrame to use no specific avatar
            if (mainFrame != null) {
                mainFrame.setCurrentUserAvatarPath(null);
            }

            // Also, visually un-highlight all avatars in the SettingRoom
            if (avatarGrid != null) {
                for (Component c : avatarGrid.getComponents()) {
                    if (c instanceof JLabel) {
                        // Revert border to non-selected state.
                        // Assuming createSelectionImageLabel sets an empty border for noDefaultBorder = true
                        // or a gray line border otherwise for non-selected items.
                        // We just need to ensure it's not the CYAN highlight.
                        // The actual border depends on noDefaultBorder, check createSelectionImageLabel logic.
                         Object noDefBorderProp = ((JComponent) c).getClientProperty("noDefaultBorder");
                         boolean noDefBorder = (noDefBorderProp instanceof Boolean) && (Boolean)noDefBorderProp;

                         if (noDefBorder) {
                            ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
                         } else {
                            ((JLabel) c).setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                         }
                    }
                }
            }
        });

        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonWrapper.setOpaque(false);
        buttonWrapper.add(setDefaultButton);
        panel.add(buttonWrapper, BorderLayout.SOUTH);
        return panel;
    }

    private JLabel createSelectionImageLabel(String path, int w, int h, JPanel parentGroup,
                                             boolean isAvatar, boolean noDefaultBorder, String currentOverallSelectionPath) {
        JLabel label = createGenericImageLabel(path, w, h); 
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.putClientProperty("imagePath", path); 
        label.putClientProperty("noDefaultBorder", noDefaultBorder); 


        if (isAvatar) {
            if (path.equals(currentOverallSelectionPath)) { // Check if this one is the selected one
                label.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
            } else {
                if (noDefaultBorder) {
                    label.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
                } else {
                    label.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                }
            }
        } else { // For non-avatar items, just apply default or empty border
            if (noDefaultBorder) {
                label.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
            } else {
                label.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            }
        }


        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Deselect all other items in the parentGroup
                for (Component c : parentGroup.getComponents()) {
                    if (c instanceof JLabel) {
                        // Get the 'noDefaultBorder' property stored earlier
                        Object noDefBorderProp = ((JComponent) c).getClientProperty("noDefaultBorder");
                        boolean componentNoDefBorder = (noDefBorderProp instanceof Boolean) && (Boolean)noDefBorderProp;

                        if (componentNoDefBorder) {
                            ((JLabel)c).setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
                        } else {
                            ((JLabel)c).setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                        }
                    }
                }
                // Select the clicked item
                label.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));

                if (isAvatar) {
                    // 'path' is the image path of the clicked avatar, captured by the lambda
                    SettingRoom.this.selectedAvatarPath = path; // Update SettingRoom's internal state
                    System.out.println("SettingRoom: Avatar selected: " + path);

                    // Tell MainFrame about the new choice
                    if (mainFrame != null) {
                        mainFrame.setCurrentUserAvatarPath(path);
                    }
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

        String wavesPath = "/com/application/FrontEnd/images/BG_LoginPage.jpg"; // Define your paths
        String darkTechPath = "/com/application/FrontEnd/images/BG_PublicRooms.png";
        String darkBallPath = "/com/application/FrontEnd/images/BG_1.jpg";
        String diskPath = "/com/application/FrontEnd/images/BG_2.jpg";
        String darkCubePath = "/com/application/FrontEnd/images/BG_3.jpg";
        

        backgroundGrid.add(createBackgroundPreviewPanel("Waves", wavesPath, wavesPath.equals(selectedChatBackgroundSource)));
        backgroundGrid.add(createBackgroundPreviewPanel("Dark Tech", darkTechPath, darkTechPath.equals(selectedChatBackgroundSource)));
        backgroundGrid.add(createBackgroundPreviewPanel("Ball", darkBallPath, darkBallPath.equals(selectedChatBackgroundSource)));
        backgroundGrid.add(createBackgroundPreviewPanel("Disk", diskPath, diskPath.equals(selectedChatBackgroundSource)));
        backgroundGrid.add(createBackgroundPreviewPanel("Cube", darkCubePath, darkCubePath.equals(selectedChatBackgroundSource)));
        
        JScrollPane scrollPane = new JScrollPane(backgroundGrid);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }


    private JPanel createBackgroundPreviewPanel(String name, Object bgSource, boolean initiallySelected) {
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        previewPanel.setPreferredSize(new Dimension(100, 75)); // Smaller previews
        previewPanel.setOpaque(true); // Panel itself is opaque to show its border and potentially a base color
        previewPanel.setBackground(new Color(60,60,60)); // A base color for the preview panel

        previewPanel.putClientProperty("bgSource", bgSource); // Store the actual source (String path or Color)
        previewPanel.putClientProperty("bgName", name);

        JPanel visualDisplayArea = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (bgSource instanceof Color) {
                    g2d.setColor((Color) bgSource);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                } else if (bgSource instanceof String) {
                    try {
                        URL imgUrl = SettingRoom.class.getResource((String) bgSource);
                        if (imgUrl != null) {
                            Image img = ImageIO.read(imgUrl);
                            if (img != null) {
                                g2d.drawImage(img, 0, 0, getWidth(), getHeight(), this); // Scale to fill
                            } else { drawPlaceholder(g2d, "No Img"); }
                        } else { drawPlaceholder(g2d, "Path Err"); }
                    } catch (IOException ex) {
                        drawPlaceholder(g2d, "Load Err");
                        System.err.println("Error loading preview " + name + ": " + ex.getMessage());
                    }
                }
                g2d.dispose();
            }
            private void drawPlaceholder(Graphics2D g2d, String text) {
                g2d.setColor(Color.DARK_GRAY);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(Color.LIGHT_GRAY);
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, getHeight() / 2 + fm.getAscent() / 2);
            }
        };
        previewPanel.add(visualDisplayArea, BorderLayout.CENTER);

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(10f) : new Font("SansSerif", Font.PLAIN, 10));
        nameLabel.setForeground(Color.LIGHT_GRAY);
        nameLabel.setOpaque(true);
        nameLabel.setBackground(new Color(0,0,0,100)); // Semi-transparent black for better readability
        previewPanel.add(nameLabel, BorderLayout.SOUTH);

        if (initiallySelected) {
            previewPanel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
            this.currentlySelectedChatBgPreview = previewPanel;
        } else {
            previewPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        }

        previewPanel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            JPanel clickedPanel = (JPanel) e.getSource();
            Object newBgSource = clickedPanel.getClientProperty("bgSource"); // This is for ChatRoom (String or Color)
            String newBgName = (String) clickedPanel.getClientProperty("bgName");

            System.out.println("[SettingRoom] Chat BG Preview '" + newBgName + "' clicked. Source for ChatRoom: " + newBgSource);

            // Update visual selection for chat background previews
            if (currentlySelectedChatBgPreview != null) {
                currentlySelectedChatBgPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            }
            clickedPanel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
            currentlySelectedChatBgPreview = clickedPanel;

            selectedChatBackgroundSource = newBgSource; // This variable is for the ChatRoom
            System.out.println("[SettingRoom] New selectedChatBackgroundSource (for ChatRoom): " + selectedChatBackgroundSource);

            if (newBgSource instanceof String) {
                String imagePathForSettingRoom = (String) newBgSource;
                currentImage = imagePathForSettingRoom; // Update the SettingRoom's currentImage field

                if (overallBackgroundPanel != null) {
                    System.out.println("[SettingRoom] Applying new background to SettingRoom itself: " + imagePathForSettingRoom);
                    overallBackgroundPanel.setImage(imagePathForSettingRoom); // Change SettingRoom's BG
                } else {
                    System.err.println("[SettingRoom] overallBackgroundPanel is null. Cannot change SettingRoom's own background.");
                }
            } else if (newBgSource instanceof Color) {
                currentImage = null; // Or some default image path if SettingRoom shouldn't have solid color BGs
                System.out.println("[SettingRoom] Color selected for ChatRoom. SettingRoom's 'currentImage' set to null.");
                    if (overallBackgroundPanel != null) {
                    }
            }
        }
        });
        return previewPanel;
    }

    private void refreshAvatarSelectionDisplay() {
        if (avatarGrid == null || mainFrame == null) {
            return;
        }
        String currentAvatarPreference = mainFrame.getSelectedUserAvatarPathForRenderer();
        this.selectedAvatarPath = currentAvatarPreference; // Sync SettingRoom's internal state

        for (Component comp : avatarGrid.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel avatarLabel = (JLabel) comp;
                String labelPath = (String) avatarLabel.getClientProperty("imagePath");
                Object noDefBorderProp = avatarLabel.getClientProperty("noDefaultBorder");
                boolean noDefBorder = (noDefBorderProp instanceof Boolean) && (Boolean)noDefBorderProp;

                if (labelPath != null && labelPath.equals(currentAvatarPreference)) {
                    avatarLabel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
                } else {
                    if (noDefBorder) {
                        avatarLabel.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
                    } else {
                        avatarLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                    }
                }
            }
        }
    }

    private JLabel createGenericImageLabel(String path, int targetWidth, int targetHeight) {
        JLabel label = new JLabel();
        label.putClientProperty("imagePath", path); // IMPORTANT: Store the path
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
        }
        return label;
    }

    private void saveSettings() {
        // ... (This method can remain for other settings) ...
        boolean saveUser = false; if(saveUsernameCheckBox!=null) saveUser = saveUsernameCheckBox.isSelected();
        boolean trustLinks = false; if(trustLinksCheckBox!=null) trustLinks = trustLinksCheckBox.isSelected();
        System.out.println("Saving Settings:");
        System.out.println("  Save Username: " + saveUser);
        System.out.println("  Trust Links: " + trustLinks);
        System.out.println("  Avatar: " + selectedAvatarPath);
        System.out.println("  Chat Background Source: " + selectedChatBackgroundSource); // Log the Object
        JOptionPane.showMessageDialog(this, "Settings applied (simulation).", "Settings Update", JOptionPane.INFORMATION_MESSAGE);
    }

    private void resizeAndLayoutLayeredComponents() {
        // ... (This method can remain as is) ...
        SwingUtilities.invokeLater(() -> {
            int layeredWidth = getWidth(); int layeredHeight = getHeight(); if (layeredWidth <= 0 || layeredHeight <= 0) return;
            if (overallBackgroundPanel != null) overallBackgroundPanel.setBounds(0, 0, layeredWidth, layeredHeight);
            if (mainContentPanel != null) {
                Dimension panelPrefSize = mainContentPanel.getPreferredSize();
                int panelW = Math.min(panelPrefSize.width + 150, layeredWidth - 40); 
                panelW = Math.max(800, panelW); 
                int panelH = Math.min(panelPrefSize.height + 150, layeredHeight - 40); 
                panelH = Math.max(700, panelH); 
                int x = (layeredWidth - panelW) / 2; int y = (layeredHeight - panelH) / 2; y = Math.max(y, 20);
                mainContentPanel.setBounds(x, y, panelW, panelH);
            }
            if (backButton != null) { Dimension btnSize = backButton.getPreferredSize(); backButton.setBounds(25, 25, btnSize.width, btnSize.height); }
            layeredPane.revalidate(); layeredPane.repaint();
        });
    }

    private JButton createIconButton(String iconPath, String fallbackText, String tooltip) {
        // ... (This method can remain as is) ...
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
        private Image backgroundImage;
        private String errorMessage = null; // To store errors from loadImage
        private String imagePathUsed;   // To display in error messages

        public BackgroundImagePanel(String imagePath) {
            this.imagePathUsed = imagePath; // Store the initial path
            setImage(imagePath);          // Call the public setImage to load it
            setOpaque(true);              // This panel should be opaque
        }

        // Public method to change the image of THIS panel
        public void setImage(String newImagePath) {
            System.out.println("[SettingRoom.BGPanel.setImage] Attempting to set new image to: " + newImagePath);
            this.imagePathUsed = newImagePath; // Update the path being used
            loadImage(newImagePath);         // Load the new image (this will set backgroundImage and errorMessage)
            repaint();                       // Trigger a repaint of THIS panel
        }

        private void loadImage(String imagePath) {
            if (imagePath == null || imagePath.trim().isEmpty()) {
                this.errorMessage = "Image path is null or empty.";
                this.backgroundImage = null;
                System.err.println("[SettingRoom.BGPanel.loadImage] " + this.errorMessage);
                return;
            }
            try {
                URL imgUrl = SettingRoom.class.getResource(imagePath); // Use SettingRoom.class for static inner class context
                if (imgUrl != null) {
                    this.backgroundImage = ImageIO.read(imgUrl);
                    if (this.backgroundImage == null) {
                        throw new IOException("ImageIO.read returned null for path: " + imagePath + " (URL: " + imgUrl.toExternalForm() + ")");
                    }
                    this.errorMessage = null; // Successfully loaded, clear any previous error
                    System.out.println("[SettingRoom.BGPanel.loadImage] Loaded image for self: " + imagePath);
                } else {
                    throw new IOException("Resource not found (URL was null) for path: " + imagePath + ". Check classpath and path string.");
                }
            } catch (IOException e) {
                this.errorMessage = "IOException: " + e.getMessage(); // Store the specific error
                System.err.println("[SettingRoom.BGPanel.loadImage] Error loading SettingRoom BG (" + imagePath + "): " + this.errorMessage);
                this.backgroundImage = null; // Ensure image is null on error
            } catch (Exception e) {
                this.errorMessage = "Unexpected Exception: " + e.getMessage();
                System.err.println("[SettingRoom.BGPanel.loadImage] Unexpected error loading SettingRoom BG (" + imagePath + "): " + this.errorMessage);
                e.printStackTrace();
                this.backgroundImage = null;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Crucial for opaque panels to clear their background

            if (backgroundImage != null) {
                // Your existing, correct image drawing logic
                Graphics2D g2d = (Graphics2D) g.create();
                int panelW = getWidth();
                int panelH = getHeight();
                if (panelW <= 0 || panelH <= 0) {
                    g2d.dispose(); return;
                }
                int imgW = backgroundImage.getWidth(this);
                int imgH = backgroundImage.getHeight(this);
                if (imgW <= 0 || imgH <= 0) {
                    g2d.dispose();
                     // Optionally draw a small error directly on the image area if dimensions are bad
                    g.setColor(Color.RED);
                    g.drawString("Bad Img Dims", 5, 15);
                    return;
                }
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
                // Improved Error Drawing
                g.setColor(new Color(25, 25, 25)); // Default dark background for SettingRoom on error
                g.fillRect(0, 0, getWidth(), getHeight());

                g.setColor(Color.ORANGE); // More visible error text color
                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                FontMetrics fm = g.getFontMetrics();

                String errTitle = "SettingRoom Background Error:";
                String errMsg = (this.errorMessage != null ? this.errorMessage : "Image not available or load failed.");
                String pathMsg = "Attempted Path: " + (this.imagePathUsed != null ? this.imagePathUsed : "unknown");

                int y = getHeight() / 2 - fm.getHeight(); // Adjusted for multi-line
                g.drawString(errTitle, (getWidth() - fm.stringWidth(errTitle)) / 2, y);
                y += fm.getHeight() + 2;
                // Simple wrap for error message
                for(String line : wordWrap(errMsg, fm, getWidth()-20)) {
                    g.drawString(line, (getWidth() - fm.stringWidth(line)) / 2, y);
                    y+=fm.getHeight();
                }
                y += 2;
                g.drawString(pathMsg, (getWidth() - fm.stringWidth(pathMsg)) / 2, y);
            }
        }

        // Helper for word wrap (add this if not already present or use a library)
        private java.util.List<String> wordWrap(String text, FontMetrics fm, int maxWidth) {
            java.util.List<String> lines = new java.util.ArrayList<>();
            if (text == null || text.isEmpty()) {
                lines.add(""); return lines;
            }
            String[] words = text.split(" ");
            StringBuilder currentLine = new StringBuilder();
            for (String word : words) {
                if (fm.stringWidth(currentLine.toString() + word) < maxWidth || currentLine.length() == 0) {
                    if (currentLine.length() > 0) currentLine.append(" ");
                    currentLine.append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
            return lines;
        }
    }

}