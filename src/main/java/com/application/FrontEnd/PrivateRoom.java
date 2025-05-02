package com.application.FrontEnd;

import com.application.FrontEnd.components.*;

import java.awt.event.ComponentEvent;
import java.net.URL;
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
 // Need this for loading resources (GIF, logo)

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLayeredPane; // Import JLayeredPane
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class PrivateRoom extends JPanel{

    private CustomButton createButton;
    private CustomButton joinButton;
    private CustomTextField roomName;
    private CustomTextField password;

    private String CurrentRoomName;
    private String CurrentPassword;
    private String CurrentUserName;

    private JLayeredPane layeredPane;
    private BackgroundGifPanel backgroundPanel;

    private MainFrame mainFrame;

    private static final String BACKGROUND_GIF_PATH = "/com/application/FrontEnd/images/Background01.gif";

    private String tempRoomName = "Room123";
    private String tempPassword = "Room123";

    private CustomButton backButton;
    private static final String BACK_ICON_PATH = "/com/application/FrontEnd/images/left-arrow.png";

    public PrivateRoom(MainFrame mainFrame, String userName){
        this.mainFrame = mainFrame;
        this.CurrentUserName = userName;
        
        setLayout(new BorderLayout());

        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);
       
        backgroundPanel = new BackgroundGifPanel(BACKGROUND_GIF_PATH);
        
        backgroundPanel.setBounds(0, 0, 10, 10);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

        JPanel headerWrapperPanel = new JPanel(new BorderLayout()); // Use BorderLayout
        headerWrapperPanel.setOpaque(false);
        ImageIcon backIcon = null;

        try {
            java.net.URL iconURL = getClass().getResource(BACK_ICON_PATH);
            if(iconURL != null){
                backIcon = new ImageIcon(iconURL);
                System.out.println("Back icon loaded successfully.");
            } else {
                System.err.println("ERROR: Could not find back icon resource at: " + BACK_ICON_PATH);
            }
        } catch (Exception e) {
            System.err.println("ERROR: Exception loading back icon: " + e.getMessage());
            e.printStackTrace();
        }

        backButton = new CustomButton("", 40, 40, Color.GRAY);

        if (backIcon != null) {
            backButton.setIcon(backIcon);
        } else {
            backButton.setText("<"); // Fallback text if icon fails
            backButton.setFont(new Font("SansSerif", Font.BOLD, 20));
        }

        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false); // Make background transparent
        backButton.setFocusPainted(false);
        backButton.setOpaque(false);
        // Set fixed size based on icon/desired size
        Dimension btnSize = new Dimension(40, 40); // Match constructor or icon size
        backButton.setPreferredSize(btnSize);
        backButton.setMinimumSize(btnSize);
        backButton.setMaximumSize(btnSize);

        headerWrapperPanel.add(backButton, BorderLayout.WEST);
        backButton.addActionListener(e -> {
            System.out.println("Back button clicked");
            // Call the method on mainFrame to go back, e.g.:
            mainFrame.switchToLoginPage();
            // Or mainFrame.goBack(); or whatever method you have
        });

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///     icon not loading 
        headerWrapperPanel.setAlignmentX(Component.CENTER_ALIGNMENT); // Align header in BoxLayout
        formPanel.add(headerWrapperPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 30))); 
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Room name
        CustomLabel labelRoomName = new CustomLabel("Room Name", 100, 30);
        labelRoomName.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelRoomName.setForeground(new Color(255,255,255));
        roomName = new CustomTextField(300, 30);
        roomName.setForeground(Color.BLACK);

        //Password
        CustomLabel labelPassword = new CustomLabel("Password", 100, 30);
        labelPassword.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelPassword.setForeground(new Color(255,255,255));
        password = new CustomTextField(300, 30);
        // JPasswordField password = new JPasswordField();
        password.setPreferredSize(new Dimension(300, 30));
        password.setForeground(Color.BLACK);

        //Application Name
        CustomLabel AppName = new CustomLabel("AnonChat", 100, 30);
        AppName.setFont(new Font("Segoe UI", Font.BOLD, 30));
        AppName.setForeground(new Color(255,255,255));
        AppName.setAlignmentX(Component.CENTER_ALIGNMENT);
        AppName.setHorizontalAlignment(SwingConstants.CENTER);
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        headerWrapperPanel.add(AppName, BorderLayout.CENTER);
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Buttons
        createButton = new CustomButton("Create Room", 150, 40, new Color(255,255,255));
        createButton.setForeground(new Color(0,0,0));
        createButton.setFont(new Font("Segoe UI", Font.BOLD, 15));

        joinButton = new CustomButton("Join Room", 150, 40, new Color(51, 102, 255));
        joinButton.setFont(new Font("Segoe UI", Font.BOLD, 15));

        formPanel.add(Box.createVerticalGlue());
        formPanel.add(AppName);
        formPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        formPanel.add(labelRoomName);
        formPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        formPanel.add(roomName);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        formPanel.add(labelPassword);
        formPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        formPanel.add(password);
        formPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        // buttonPanel.setOptimizedDrawingEnabled(false);
        buttonPanel.add(createButton);
        buttonPanel.add(joinButton);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(400, 50));
        buttonPanel.setOpaque(false);

        formPanel.add(buttonPanel);
        formPanel.add(Box.createVerticalGlue());
        // formPanel.setBackground(new Color(0, 0, 0, 100));
        formPanel.setOpaque(false);

        formPanel.add(Box.createVerticalGlue()); // Push content up


        // 4b. Add formPanel (Content Layer) to Layered Pane
        layeredPane.add(formPanel, JLayeredPane.PALETTE_LAYER);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = getWidth();
                int height = getHeight();
        
                // Resize background
                backgroundPanel.setBounds(0, 0, width, height);
        
                // Resize & Center content (adjust formW/formH as needed)
                int formW = 400; // Desired width for form
                int formH = 450; // Desired height for form
                int x = (width - formW) / 2;
                int y = (height - formH) / 2;
                formPanel.setBounds(Math.max(0,x), Math.max(0,y), formW, formH); // Keep it centered
        
                // IMPORTANT: Update content layout
                formPanel.revalidate();
                formPanel.repaint();
            }
        });

        add(layeredPane, BorderLayout.CENTER);
        
        addEventListeners();

    }

    public void addEventListeners() {
        createButton.addActionListener(e -> {
            CurrentRoomName = roomName.getText().trim();
            CurrentPassword = password.getText();

            if (CurrentUserName.isEmpty() || CurrentRoomName.isEmpty() || CurrentPassword.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill in all fields.");
                return;
            }
            System.out.println("Room created successfully");
            mainFrame.switchToChatRoom(CurrentUserName, CurrentRoomName);
        });

        joinButton.addActionListener(e -> {
            CurrentRoomName = roomName.getText().trim();
            CurrentPassword = password.getText();

            if (CurrentUserName.isEmpty() || CurrentRoomName.isEmpty() || CurrentPassword.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill in all fields.");
                return;
            }

            if (CurrentRoomName.equals(tempRoomName) && CurrentPassword.equals(tempPassword)) {
                System.out.println("Authentication successfull joining room");
                mainFrame.switchToChatRoom(CurrentUserName, CurrentRoomName);
            } else {
                JOptionPane.showMessageDialog(null, "Room name or password might be incorrect.");
            }
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
}   
