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
import java.awt.GridLayout; // Keep this if using GridLayout elsewhere, not needed just for image panel
// Remove unused Image import if only used for ImageIO approach
// import java.awt.Image;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
    private CustomButton createButton;
    private CustomButton joinButton;
    private CustomTextField userNameField;
    private CustomTextField roomNameField;
    private JPasswordField passwordField;

    private String tempRoomName = "Room123";
    private String tempPassword = "Room123";

    private MainFrame mainFrame;
    private ChatController chatController;
////////////////////////////////////////////////////////////////////////////////////////////////////////
    public LoginPage(MainFrame mainFrame, ChatController chatController) {
        this.mainFrame = mainFrame;
        this.chatController = chatController;

        this.setLayout(new GridLayout(1, 2));
        // this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String imagePath = "/com/application/FrontEnd/images/Login_img.png"; // Classpath resource path
        CroppedImagePanel imagePanel = new CroppedImagePanel(imagePath);
        // You might want to set a preferred size for the column if GridLayout isn't enough
        // imagePanel.setPreferredSize(new Dimension(350, 0)); // Example: Preferred width
        add(imagePanel); // Add directly to the GridLayout cell


        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        formPanel.setOpaque(false);

        //User name
        CustomLabel labelUserName = new CustomLabel("User Name", 100, 30);
        labelUserName.setForeground(Color.WHITE);
        userNameField = new CustomTextField(300, 30);

        //Room name
        CustomLabel labelRoomName = new CustomLabel("Room Name", 100, 30);
        labelRoomName.setForeground(Color.WHITE);
        roomNameField = new CustomTextField(300, 30);

        //Password
        CustomLabel labelPassword = new CustomLabel("Password", 100, 30);
        labelPassword.setForeground(Color.WHITE);
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(300, 30));
        passwordField.setMaximumSize(new Dimension(300, 30));

        //Application Name
        CustomLabel AppName = new CustomLabel("AnonChat", 100, 30);
        AppName.setForeground(new Color(220, 220, 220));
        AppName.setFont(AppName.getFont().deriveFont(Font.BOLD, 24f));
        AppName.setAlignmentX(Component.CENTER_ALIGNMENT);
        AppName.setHorizontalAlignment(SwingConstants.CENTER);

        //Buttons
        createButton = new CustomButton("Create Room", 150, 40, new Color(255,255,255));
        createButton.setForeground(Color.BLACK);
        createButton.setFont(new Font("Segoe UI", Font.BOLD, 15));

        joinButton = new CustomButton("Join Room", 150, 40, new Color(51, 102, 255));
        joinButton.setForeground(Color.WHITE);
        joinButton.setFont(new Font("Segoe UI", Font.BOLD, 15));

        formPanel.add(Box.createVerticalGlue());
        formPanel.add(AppName);
        formPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        addFormField(formPanel, labelUserName, userNameField);
        addFormField(formPanel, labelRoomName, roomNameField);
        addFormField(formPanel, labelPassword, passwordField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(createButton);
        buttonPanel.add(joinButton);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(400, 50));

        formPanel.add(buttonPanel);
        formPanel.add(Box.createVerticalGlue());
        formPanel.setOpaque(false);
    

        add(imagePanel);
        add(formPanel);

        addEventListeners();
    }

    private String CurrentUserName;
    private String CurrentRoomName;
    private String CurrentPassword;

    public void addEventListeners() {
        ActionListener loginAction = e -> {
            String username = userNameField.getText().trim();
            String roomName = roomNameField.getText().trim();
            String password = new String(passwordField.getPassword());
            java.util.Arrays.fill(passwordField.getPassword(), ' '); // Clear password

            if (username.isEmpty() || roomName.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username, Room Name, and Password cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // *** Delegate login attempt to the Controller using new method ***
            // Controller now handles both join/create logic internally based on room/key status
            chatController.joinInitialRoom(username, roomName, password);
        };

        createButton.addActionListener(loginAction);
        joinButton.addActionListener(loginAction);
    }

    private void addFormField(JPanel panel, JLabel label, JComponent field) {
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(field);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
    }

    @Override
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
