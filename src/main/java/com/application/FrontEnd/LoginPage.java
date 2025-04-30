package com.application.FrontEnd;

// Assuming Backend classes are in this package now
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout; // Keep this if using GridLayout elsewhere, not needed just for image panel
// Remove unused Image import if only used for ImageIO approach
// import java.awt.Image;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.print.DocFlavor.URL;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
// Remove ImageIcon import if not used anymore
// import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;

import com.application.Backend.ChatController;
import com.application.FrontEnd.components.CustomButton;
import com.application.FrontEnd.components.CustomLabel;
import com.application.FrontEnd.components.CustomTextField;

public class LoginPage extends JPanel {

////////////////////////////////////////////////////////////////////////////////////////////////////////
    private CustomButton publicRoom;
    private CustomButton privateRoom;
    private CustomTextField userNameField;


    private MainFrame mainFrame;
    private ChatController chatController;
////////////////////////////////////////////////////////////////////////////////////////////////////////
    public LoginPage(MainFrame mainFrame, ChatController chatController) {
        this.mainFrame = mainFrame;
        this.chatController = chatController;

        // this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // String imagePath = "/com/application/FrontEnd/images/Login_img.png"; // Classpath resource path
        // CroppedImagePanel imagePanel = new CroppedImagePanel(imagePath);
        // You might want to set a preferred size for the column if GridLayout isn't enough
        // imagePanel.setPreferredSize(new Dimension(350, 0)); // Example: Preferred width
        // add(imagePanel); // Add directly to the GridLayout cell

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();


        ImageIcon imageIcon = null; 
        JLabel labelWithIcon = new JLabel(); 
        try {
            String imagePath = "/com/application/FrontEnd/images/Chat_logo.png";
            java.net.URL imgUrl = getClass().getResource(imagePath);

            if (imgUrl != null) {
                imageIcon = new ImageIcon(imgUrl); // Load from URL
                labelWithIcon.setIcon(imageIcon); // Set icon on the label
                System.out.println("Logo loaded successfully from: " + imagePath);
            } else {
                System.err.println("ERROR: Could not find logo image resource at: " + imagePath);
                labelWithIcon.setText("Logo Missing"); // Placeholder text
                labelWithIcon.setForeground(Color.RED);
            }
        } catch (Exception e) {
            System.err.println("ERROR: Exception while loading logo: " + e.getMessage());
            e.printStackTrace();
            labelWithIcon.setText("Logo Error"); // Placeholder text
            labelWithIcon.setForeground(Color.RED);
        }

        labelWithIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        formPanel.setBackground(new Color(38, 38, 38));

        Dimension boxSize = new Dimension(380, 400);
        formPanel.setPreferredSize(boxSize);
        formPanel.setMaximumSize(boxSize);
        formPanel.setMinimumSize(boxSize);        

        //Application Name
        CustomLabel AppName = new CustomLabel("AnonChat", 100, 50);
        AppName.setForeground(new Color(220, 220, 220));
        AppName.setFont(AppName.getFont().deriveFont(Font.BOLD, 50f));
        AppName.setAlignmentX(Component.CENTER_ALIGNMENT);
        AppName.setHorizontalAlignment(SwingConstants.CENTER);

        //User name
        CustomLabel labelUserName = new CustomLabel("User Name", 100, 30);
        labelUserName.setForeground(Color.WHITE);
        labelUserName.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelUserName.setHorizontalAlignment(SwingConstants.CENTER);

        userNameField = new CustomTextField(300, 30);
        userNameField.setOpaque(true);
        userNameField.setHorizontalAlignment(SwingConstants.CENTER);
        userNameField.setForeground(Color.BLACK);
        userNameField.setCaretColor(Color.BLACK);
        userNameField.setBorder(BorderFactory.createCompoundBorder(
             BorderFactory.createLineBorder(new Color(80, 80, 100, 200), 1), // Slightly lighter border than bg
             BorderFactory.createEmptyBorder(2, 5, 2, 5) // Inner padding
         ));

        //Buttons
        publicRoom = new CustomButton("Public Rooms", 150, 40, new Color(255,255,255));
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
        buttonPanel.setMaximumSize(new Dimension(400, 50));

        formPanel.add(buttonPanel);
        formPanel.add(Box.createVerticalGlue());
        
        
        
        add(formPanel, gbc);
        
        addEventListeners();
    }

    private String CurrentUserName;
    private String CurrentRoomName;
    private String CurrentPassword;

    public void addEventListeners() {
        ActionListener loginAction = e -> {
            
        };

        publicRoom.addActionListener(loginAction);
        privateRoom.addActionListener(loginAction);
    }

    private void addFormField(JPanel panel, JLabel label, JComponent field) {
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(field);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
    }


    // @Override
    protected void paintComponent(Graphics g) {
    super.paintComponent(g); // Let the default painting happen first
    
    // Cast Graphics to Graphics2D for gradient capabilities
    Graphics2D g2d = (Graphics2D) g.create(); // Use create() for safety

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
    int width = getWidth();
    int height = getHeight();
    
    Color color1 = new Color(255, 102, 255); 
    Color color2 = new Color(77, 0, 77);
    Color color3 = new Color(102, 0, 102);

    // Gradient parameters
    Point2D center = new Point2D.Float(width / 2.0f, height / 2.0f);
    float radius = Math.max(width, height) / 1.5f; // Adjust radius as needed
    Point2D focus = new Point2D.Float(width / 2.0f, height / 2.0f); // Focus same as center for circular
    float[] fractions = {0.0f, 0.6f, 1.0f}; // Where colors transition (0.0 to 1.0)
    Color[] colors = {color1, color2, color3}; // Colors corresponding to fractions
    CycleMethod cycleMethod = CycleMethod.NO_CYCLE; // Fill edges with last color
    
    // Create the radial gradient paint
    RadialGradientPaint gradient = new RadialGradientPaint(center, radius, focus, fractions, colors, cycleMethod);
    
    // Set the paint and fill the background
    g2d.setPaint(gradient);
    g2d.fillRect(0, 0, width, height); // Fill the entire panel
    

    g2d.dispose(); // Dispose of the graphics copy
  }
}


//Old variables 
//Room name
// CustomLabel labelRoomName = new CustomLabel("Room Name", 100, 30);
// labelRoomName.setForeground(Color.WHITE);
// roomNameField = new CustomTextField(300, 30);

// //Password
// CustomLabel labelPassword = new CustomLabel("Password", 100, 30);
// labelPassword.setForeground(Color.WHITE);
// passwordField = new JPasswordField();
// passwordField.setPreferredSize(new Dimension(300, 30));
// passwordField.setMaximumSize(new Dimension(300, 30));

