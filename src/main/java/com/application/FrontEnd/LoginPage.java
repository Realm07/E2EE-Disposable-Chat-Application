import components.CustomButton;
import components.CustomTextField;
import components.CustomLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.Point2D;



class LoginPage extends JPanel {

    private CustomButton createButton;
    private CustomButton joinButton;
    private CustomTextField userName;
    private CustomTextField roomName;
    private CustomTextField password;

    private String tempRoomName = "Room123";
    private String tempPassword = "Room123";

    private MainFrame mainFrame;

    public LoginPage(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        this.setLayout(new GridLayout(1, 2));
        // this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel imagePanel = new JPanel(new BorderLayout());

        java.net.URL imageURL = getClass().getResource("/images/Login_img.png");
        ImageIcon imageIcon = new ImageIcon(imageURL);
        Image scaledImage = imageIcon.getImage().getScaledInstance(300, 350, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        imagePanel.setBackground(Color.BLACK);

        //Mane panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        //User name
        CustomLabel labelUserName = new CustomLabel("User Name", 100, 30);
        labelUserName.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelUserName.setForeground(new Color(255,255,255));
        userName = new CustomTextField(300, 30);
        userName.setForeground(Color.BLACK);

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
        password.setForeground(Color.BLACK);

        //Application Name
        CustomLabel AppName = new CustomLabel("AnonChat", 100, 30);
        AppName.setFont(new Font("Segoe UI", Font.BOLD, 30));
        AppName.setForeground(new Color(255,255,255));
        AppName.setAlignmentX(Component.CENTER_ALIGNMENT);
        AppName.setHorizontalAlignment(SwingConstants.CENTER);

        //Buttons
        createButton = new CustomButton("Create Room", 150, 40, new Color(255,255,255));
        createButton.setForeground(new Color(0,0,0));
        joinButton = new CustomButton("Join Room", 150, 40, new Color(51, 102, 255));
        joinButton.setOpaque(false);

        formPanel.add(Box.createVerticalGlue());
        formPanel.add(AppName);
        formPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        formPanel.add(labelUserName);
        formPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        formPanel.add(userName);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
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
        formPanel.setBackground(new Color(0, 0, 0, 120));

        this.add(imagePanel);
        this.add(formPanel);

        addEventListeners();
    }

    private String CurrentUserName;
    private String CurrentRoomName;
    private String CurrentPassword;

    public void addEventListeners() {
        createButton.addActionListener(e -> {
            CurrentUserName = userName.getText().trim();
            CurrentRoomName = roomName.getText().trim();
            CurrentPassword = password.getText();

            if (CurrentUserName.isEmpty() || CurrentRoomName.isEmpty() || CurrentPassword.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill in all fields.");
                return;
            }

            mainFrame.switchToChatRoom(CurrentUserName, CurrentRoomName);
        });

        joinButton.addActionListener(e -> {
            CurrentUserName = userName.getText().trim();
            CurrentRoomName = roomName.getText().trim();
            CurrentPassword = password.getText();

            if (CurrentUserName.isEmpty() || CurrentRoomName.isEmpty() || CurrentPassword.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill in all fields.");
                return;
            }

            if (CurrentRoomName.equals(tempRoomName) && CurrentPassword.equals(tempPassword)) {
                mainFrame.switchToChatRoom(CurrentUserName, CurrentRoomName);
            } else {
                JOptionPane.showMessageDialog(null, "Room name or password might be incorrect.");
            }
        });
    }
    @Override
    protected void paintComponent(Graphics g) {
    super.paintComponent(g); // Let the default painting happen first
    
    // Cast Graphics to Graphics2D for gradient capabilities
    Graphics2D g2d = (Graphics2D) g.create(); // Use create() for safety

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
    int width = getWidth();
    int height = getHeight();
    
    Color color1 = new Color(255,255, 255); 
    Color color2 = new Color(102,102,102);
    Color color3 = new Color(0,0,0);

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
