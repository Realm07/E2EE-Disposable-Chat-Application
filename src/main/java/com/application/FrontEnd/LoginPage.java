package com.application.FrontEnd;

// Imports kept for existing form components + Added imports for Layered Pane and GIF handling
import java.awt.BorderLayout; // Use BorderLayout for the main LoginPage panel
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics; // Need Graphics for custom painting
import java.awt.Image;     // Need Image for the GIF
import java.awt.RenderingHints; // For potentially smoother scaling
import java.awt.Graphics2D; // For RenderingHints
import java.awt.event.ComponentAdapter; // For resizing
import java.awt.event.ComponentEvent;   // For resizing
import java.net.URL; // Need this for loading resources (GIF, logo)

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane; // Import JLayeredPane
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.application.Backend.ChatController;
import com.application.FrontEnd.components.CustomButton;
import com.application.FrontEnd.components.CustomLabel;
import com.application.FrontEnd.components.CustomTextField;

public class LoginPage extends JPanel { // Still extends JPanel

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    private CustomButton publicRoom;
    private CustomButton privateRoom;
    private CustomTextField userNameField;

    private MainFrame mainFrame;
    private ChatController chatController;

    // --- Fields for Layered Pane Approach ---
    private JLayeredPane layeredPane;
    // *** Replace backgroundLabel with our custom panel ***
    private BackgroundGifPanel backgroundPanel; // To hold and SCALE the GIF
    private JPanel formPanel;       // To hold the login controls
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Paths
    private static final String BACKGROUND_GIF_PATH = "/com/application/FrontEnd/images/Background01.gif"; // Path to your GIF
    private static final String LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/Chat_logo.png";


    public LoginPage(MainFrame mainFrame, ChatController chatController) {
        this.mainFrame = mainFrame;
        this.chatController = chatController;

        // Set layout for the main LoginPage panel
        setLayout(new BorderLayout()); // Use BorderLayout to make layeredPane fill it

        // Initialize LayeredPane
        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER); // Add layeredPane to fill LoginPage

        // Initialize and add Background Panel (Layer 0)
        // *** Use the new BackgroundGifPanel ***
        backgroundPanel = new BackgroundGifPanel(BACKGROUND_GIF_PATH);
        // Initial bounds will be set by listener
        backgroundPanel.setBounds(0, 0, 10, 10);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER); // Add to bottom layer


        // --- Initialize Form Panel (Layer 1) --- (No changes needed here)
        formPanel = new JPanel(); // Use the class member
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        formPanel.setOpaque(false); // Keep form transparent

        Dimension boxSize = new Dimension(380, 400);
        formPanel.setPreferredSize(boxSize);
        formPanel.setMaximumSize(boxSize);
        formPanel.setMinimumSize(boxSize);

        // --- Create and Add Components to formPanel --- (No changes needed here)
        JLabel labelWithIcon = createLogoLabel();
        labelWithIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        CustomLabel AppName = new CustomLabel("AnonChat", 100, 50);
        AppName.setForeground(new Color(220, 220, 220));
        AppName.setFont(AppName.getFont().deriveFont(Font.BOLD, 50f));
        AppName.setAlignmentX(Component.CENTER_ALIGNMENT);
        AppName.setHorizontalAlignment(SwingConstants.CENTER);

        CustomLabel labelUserName = new CustomLabel("User Name", 100, 30);
        labelUserName.setForeground(Color.WHITE);
        labelUserName.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelUserName.setHorizontalAlignment(SwingConstants.CENTER);

        userNameField = new CustomTextField(300, 30);
        userNameField.setOpaque(false);
        userNameField.setBackground(new Color(240, 240, 240));
        userNameField.setForeground(Color.BLACK);
        userNameField.setCaretColor(Color.BLACK);
        userNameField.setHorizontalAlignment(SwingConstants.CENTER);
        userNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255,255,255), 1),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));

        publicRoom = new CustomButton("Public Rooms", 150, 40, new Color(255, 255, 255));
        publicRoom.setForeground(Color.BLACK);
        publicRoom.setFont(new Font("Segoe UI", Font.BOLD, 15));

        privateRoom = new CustomButton("Private Room", 150, 40, new Color(51, 102, 255));
        privateRoom.setForeground(Color.WHITE);
        privateRoom.setFont(new Font("Segoe UI", Font.BOLD, 15));

        formPanel.add(Box.createVerticalGlue());
        formPanel.add(labelWithIcon);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        formPanel.add(AppName);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        formPanel.add(labelUserName);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(userNameField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(publicRoom);
        buttonPanel.add(privateRoom);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        formPanel.add(buttonPanel);
        formPanel.add(Box.createVerticalGlue());

        // Add formPanel to the Layered Pane (higher layer)
        formPanel.setBounds(0, 0, boxSize.width, boxSize.height); // Initial bounds
        layeredPane.add(formPanel, JLayeredPane.PALETTE_LAYER); // Add form on top


        // Add Listener for Resizing/Centering
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeAndCenterComponents();
            }
            @Override
            public void componentShown(ComponentEvent e) {
                 resizeAndCenterComponents();
            }
        });

        addEventListeners();
    }


    // Helper method to create the logo label (No changes needed)
    private JLabel createLogoLabel() {
        JLabel label = new JLabel();
        try {
            URL imgUrl = getClass().getResource(LOGO_IMAGE_PATH);
            if (imgUrl != null) {
                label.setIcon(new ImageIcon(imgUrl));
                System.out.println("Logo loaded successfully from: " + LOGO_IMAGE_PATH);
            } else {
                System.err.println("ERROR: Could not find logo image resource at: " + LOGO_IMAGE_PATH);
                label.setText("Logo Missing");
                label.setForeground(Color.RED);
                 label.setFont(new Font("SansSerif", Font.BOLD, 12));
            }
        } catch (Exception e) {
            System.err.println("ERROR: Exception while loading logo: " + e.getMessage());
            e.printStackTrace();
            label.setText("Logo Error");
            label.setForeground(Color.RED);
             label.setFont(new Font("SansSerif", Font.BOLD, 12));
        }
        return label;
    }

    // Method to handle resizing and centering logic
    private void resizeAndCenterComponents() {
        int layeredWidth = layeredPane.getWidth();
        int layeredHeight = layeredPane.getHeight();

        // *** Resize background *panel* to fill the layered pane ***
        if (backgroundPanel != null) {
             backgroundPanel.setBounds(0, 0, layeredWidth, layeredHeight);
        }

        // Center the form panel (No changes needed here)
        if (formPanel != null) {
            Dimension formSize = formPanel.getPreferredSize();
            int formW = formSize.width;
            int formH = formSize.height;
            int x = Math.max(0, (layeredWidth - formW) / 2);
            int y = Math.max(0, (layeredHeight - formH) / 2);
            formPanel.setBounds(x, y, formW, formH);
        }
    }


    // --- Event Listeners --- (No changes needed)
    public void addEventListeners() {
        publicRoom.addActionListener(e -> {
            String currentUserName = userNameField.getText().trim();
            if (currentUserName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a User Name.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            System.out.println("Public Room panel initiated for user: " + currentUserName);
            mainFrame.switchToPublicRoom(currentUserName);
        });

        privateRoom.addActionListener(e -> {
            String currentUserName = userNameField.getText().trim();
            if (currentUserName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a User Name.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            System.out.println("Private Room panel initiated for user: " + currentUserName);
            mainFrame.switchToPrivateRoom(currentUserName);
        });
    }

    // --- Inner Class for Background GIF Panel ---
    private class BackgroundGifPanel extends JPanel {
        private Image gifImage;
        private String errorMessage = null;
        private String imagePathUsed; // Store path for error messages

        public BackgroundGifPanel(String imagePath) {
            this.imagePathUsed = imagePath;
            try {
                URL gifUrl = getClass().getResource(imagePath);
                if (gifUrl != null) {
                    ImageIcon icon = new ImageIcon(gifUrl);
                    if (icon.getIconWidth() == -1) { // Basic check if loading failed
                         this.errorMessage = "Failed to load GIF: ImageIcon error";
                         System.err.println("[BackgroundGifPanel] " + this.errorMessage + " (" + imagePath + ")");
                         this.gifImage = null;
                    } else {
                        this.gifImage = icon.getImage(); // Get the Image object
                        System.out.println("[BackgroundGifPanel] GIF Image loaded successfully from: " + imagePath);
                    }
                } else {
                    this.errorMessage = "GIF resource not found";
                    System.err.println("[BackgroundGifPanel] " + this.errorMessage + " (" + imagePath + ")");
                    this.gifImage = null;
                }
            } catch (Exception e) {
                this.errorMessage = "Exception loading GIF: " + e.getMessage();
                System.err.println("[BackgroundGifPanel] " + this.errorMessage + " (" + imagePath + ")");
                e.printStackTrace();
                this.gifImage = null;
            }
             // Important: Set opaque false if you want things behind this panel (in the layered pane)
             // to potentially show through (though likely not needed here as it's the bottom layer)
             // setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Paint background if opaque, handle borders etc.

            if (gifImage != null) {
                // --- Draw the image scaled to fill the panel ---
                Graphics2D g2d = (Graphics2D) g.create();
                // Optional: Add rendering hints for potentially better scaling quality
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                // Draw the image, scaling it to the component's current width and height.
                // Passing 'this' as the ImageObserver is crucial for animated GIFs to repaint correctly.
                g2d.drawImage(gifImage, 0, 0, getWidth(), getHeight(), this);
                g2d.dispose();
                // --- End drawing ---
            } else {
                // Draw fallback background and error message if image failed to load
                g.setColor(new Color(77, 0, 77)); // Dark purple fallback
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.YELLOW);
                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                String text = "BG Load Error: " + (errorMessage != null ? errorMessage : "Unknown");
                FontMetrics fm = g.getFontMetrics();
                int msgWidth = fm.stringWidth(text);
                g.drawString(text, Math.max(5, (getWidth() - msgWidth) / 2) , getHeight() / 2 + fm.getAscent() / 2);
                 String pathText = "(" + imagePathUsed + ")";
                 msgWidth = fm.stringWidth(pathText);
                 g.drawString(pathText, Math.max(5, (getWidth() - msgWidth) / 2) , getHeight() / 2 + fm.getAscent() / 2 + fm.getHeight());

            }
        }
    }
    // --- End Inner Class ---

} // End LoginPage class