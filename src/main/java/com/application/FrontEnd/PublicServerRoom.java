package com.application.FrontEnd;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

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

import com.application.FrontEnd.components.*;

public class PublicServerRoom extends JPanel {
    private CustomLabel titleLabel;

    private CustomLabel nameLabel;
    private CustomLabel userLabel ;
    private CustomButton joinButton;

    private String CurrentUserName;
    
    private MainFrame mainFrame;

    private JLayeredPane layeredPane;
    private BackgroundGifPanel backgroundPanel;

    private CustomButton backButton;

    private static final String BACKGROUND_GIF_PATH = "/com/application/FrontEnd/images/Background01.gif";
/////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String BACK_ICON_PATH = "/com/application/FrontEnd/images/left-arrow.png";
/////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public PublicServerRoom(MainFrame mainFrame, String userName){
        this.mainFrame = mainFrame;
        this.CurrentUserName = userName;
        
        
        this.setLayout(new BorderLayout(0,50)); 
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
       //background image panel 
        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);
       
        backgroundPanel = new BackgroundGifPanel(BACKGROUND_GIF_PATH);
        
        backgroundPanel.setBounds(0, 0, 10, 10);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///     backspace icon not working..
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


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        JPanel mainContentPanel = new JPanel(new BorderLayout(0, 15));
        mainContentPanel.setOpaque(false);
        
        //Header Lable panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titleLabel = new CustomLabel("Public Servers", 300, 50);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(titleLabel);
        topPanel.setOpaque(false);

        headerWrapperPanel.add(topPanel, BorderLayout.CENTER);
        
        //Index components panel
        JPanel centerContentPanel = new JPanel();
        centerContentPanel.setLayout(new BoxLayout(centerContentPanel, BoxLayout.Y_AXIS));
        
        CustomLabel indexName = new CustomLabel("Room Name", 120, 30);
        CustomLabel indexUsernumber = new CustomLabel("Users Online", 120, 30);
        CustomLabel indexJoin = new CustomLabel("Join Room", 120, 30);

        //Index Label panel
        JPanel indexHoldPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 120, 5)); 

        indexHoldPanel.add(indexName);
        indexHoldPanel.add(indexUsernumber);
        indexHoldPanel.add(indexJoin);
        indexHoldPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        indexHoldPanel.setOpaque(false);

        centerContentPanel.add(indexHoldPanel);

        
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///     change the image path here, default set to null
        JPanel alphaPanel = createServerRooms("Alpha Room", "5/10", "Alpha", null);
        JPanel bravoPanel = createServerRooms("Bravo Base", "10/10", "Bravo", null);
        JPanel charliePanel = createServerRooms("Charlie Chat", "2/8", "Charlie", null);
        JPanel deltaPanel = createServerRooms("Delta Den", "7/12", "Delta", null);
        JPanel echoPanel = createServerRooms("Echo Chamber", "1/5", "Echo", null);       
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        centerContentPanel.add(alphaPanel);
        centerContentPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing
        centerContentPanel.add(bravoPanel);
        centerContentPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing
        centerContentPanel.add(charliePanel);
        centerContentPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing
        centerContentPanel.add(deltaPanel);
        centerContentPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing
        centerContentPanel.add(echoPanel);

        centerContentPanel.add(Box.createVerticalGlue());
        centerContentPanel.setOpaque(false);

        mainContentPanel.add(headerWrapperPanel, BorderLayout.NORTH);
        mainContentPanel.add(centerContentPanel, BorderLayout.CENTER);
      

        layeredPane.add(mainContentPanel, JLayeredPane.PALETTE_LAYER);

        
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //adjusting the size of the image and window
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
                mainContentPanel.setBounds(0, 0, width, height); // Keep it centered // Keep it centered
        
                // IMPORTANT: Update content layout
                mainContentPanel.revalidate();
                mainContentPanel.repaint();
            }
        });

    }

    private JLabel createLogoLabel() {
        JLabel label = new JLabel();
        try {
            URL imgUrl = getClass().getResource(BACK_ICON_PATH);
            if (imgUrl != null) {
                label.setIcon(new ImageIcon(imgUrl));
                System.out.println("Logo loaded successfully from: " + BACK_ICON_PATH);
            } else {
                System.err.println("ERROR: Could not find logo image resource at: " + BACK_ICON_PATH);
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

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//     creating custome server room
    private JPanel createServerRooms(String roomName, String userCount, String roomButton, String image){
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 100, 5));

        int nameWidth = 150;
        int userWidth = 100;
        int buttonWidth = 100;

        nameLabel = new CustomLabel(roomButton, nameWidth, 30);
        userLabel = new CustomLabel(userCount, userWidth, 30);
        joinButton = new CustomButton(roomButton, buttonWidth, 30, Color.GRAY);

        joinButton.addActionListener(e ->{
            mainFrame.switchToChatRoom(CurrentUserName, roomName);
            System.out.println(CurrentUserName);
        });

        rowPanel.add(nameLabel);
        rowPanel.add(userLabel);
        rowPanel.add(joinButton);
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //comment this out and add image panel and set it as background
        rowPanel.setBackground(Color.BLUE);
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        int preferredHeight = nameLabel.getPreferredSize().height + 15;
        rowPanel.setPreferredSize(new Dimension(100, preferredHeight)); 
        rowPanel.setMaximumSize(new Dimension(700, preferredHeight));
        rowPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        rowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return rowPanel;
    }

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
