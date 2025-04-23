import components.CustomButton;
import components.CustomLabel;
import components.CustomTextArea;
import components.CustomTextField;

import javax.swing.*;
import javax.swing.border.Border;

import java.awt.*;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


public class ChatRoom extends JPanel {

    private String CurrentRoomName;
    private String CurrentUserName;

    private CustomButton sendButton;
    private CustomButton leaveRoom;
    private CustomTextArea chatArea;
    private CustomTextField chatTextField;
    private CustomLabel roomName;
    private CustomLabel chatHistory;

    private MainFrame mainFrame;

    public ChatRoom(String CurrentUserName, String CurrentRoomName, MainFrame mainFrame) {

        this.mainFrame = mainFrame;
        this.CurrentRoomName = CurrentRoomName;
        this.CurrentUserName = CurrentUserName;

        Color backgroundColor = new Color(45, 45, 45); // A good dark grey
        this.setBackground(backgroundColor);
        this.setOpaque(true);
        
        setLayout(new GridLayout(1, 2, 5, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        //left panel
        JPanel userDisplayPanel = new JPanel();
        userDisplayPanel.setLayout(new BoxLayout(userDisplayPanel, BoxLayout.Y_AXIS));
        userDisplayPanel.setOpaque(false);
        

        //declared components
        CustomTextArea userNameArea = new CustomTextArea("-" + CurrentUserName);
        userNameArea.setBackground(Color.DARK_GRAY);
        userNameArea.setForeground(new Color(255,255,255));

        leaveRoom = new CustomButton("Leave", 100, 30, new Color(255, 77, 77));
        roomName = new CustomLabel(CurrentRoomName, 200, 30);
        roomName.setAlignmentX(Component.CENTER_ALIGNMENT);
        roomName.setHorizontalAlignment(SwingConstants.CENTER);
        roomName.setForeground(new Color(255,255,255));

        JPanel leaveButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        leaveButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0)); 
        leaveButtonPanel.setOpaque(false);
        
        JPanel leftColumnPanel = new JPanel();
        leftColumnPanel.setLayout(new BorderLayout(0, 5));
        leftColumnPanel.setOpaque(false);

        JScrollPane userScrollPane = new JScrollPane(userNameArea);
        userScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        userScrollPane.setPreferredSize(new Dimension(150, 100)); // Consider adjusting size
        userScrollPane.setOpaque(false); // Make scroll pane transparent
        userScrollPane.getViewport().setOpaque(false); // Make viewport transparent
        userScrollPane.setBorder(BorderFactory.createTitledBorder( // Add a border for clarity
                BorderFactory.createEtchedBorder(), "Users",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                null)); 

        //adding the panel
        userDisplayPanel.add(roomName);
        userDisplayPanel.add(userScrollPane);
        userDisplayPanel.add(Box.createGlue());

        leaveButtonPanel.add(leaveRoom); 
        leaveButtonPanel.setOpaque(false);

        leftColumnPanel.add(userDisplayPanel, BorderLayout.CENTER);
        leftColumnPanel.add(leaveButtonPanel, BorderLayout.SOUTH);

        //right panel
        //declared components
        chatArea = new CustomTextArea(null);
        chatArea.setBackground(Color.DARK_GRAY);
        chatArea.setForeground(Color.WHITE);

        sendButton = new CustomButton("Send", 80, 30, new Color(102, 255, 102));

        chatTextField = new CustomTextField(300, 30);

        chatHistory = new CustomLabel("Chat History", 100, 30);
        
        chatHistory.setAlignmentX(RIGHT_ALIGNMENT);

        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setOpaque(false);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout(5, 0));

        inputPanel.add(chatTextField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        Border lineBorder = BorderFactory.createLineBorder(Color.GRAY);
        Border paddingBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(lineBorder, paddingBorder));

        inputPanel.setOpaque(false);

        //chat text area
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout(0,5));

        JPanel chatHeaderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        chatHeaderPanel.setOpaque(false);
        chatHeaderPanel.add(chatHistory);

        chatPanel.add(chatHeaderPanel, BorderLayout.NORTH);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        
        //added to the main panel
        add(leftColumnPanel);
        add(chatPanel);
        
        this.addEventListerners();

    }
    

//     @Override
//     protected void paintComponent(Graphics g) {
//     super.paintComponent(g); // Let the default painting happen first
    
//     // Cast Graphics to Graphics2D for gradient capabilities
//     Graphics2D g2d = (Graphics2D) g.create(); // Use create() for safety
    
//     int width = getWidth();
//     int height = getHeight();
    
//     Color color1 = new Color(255,255, 255); 
//     Color color2 = new Color(51, 0, 51);
//     Color color3 = new Color(51, 0, 51);

//     // Gradient parameters
//     Point2D center = new Point2D.Float(width / 2.0f, height / 2.0f);
//     float radius = Math.max(width, height) / 1.5f; // Adjust radius as needed
//     Point2D focus = new Point2D.Float(width / 2.0f, height / 2.0f); // Focus same as center for circular
//     float[] fractions = {0.0f, 0.6f, 1.0f}; // Where colors transition (0.0 to 1.0)
//     Color[] colors = {color1, color2, color3}; // Colors corresponding to fractions
//     CycleMethod cycleMethod = CycleMethod.NO_CYCLE; // Fill edges with last color
    
//     // Create the radial gradient paint
//     RadialGradientPaint gradient = new RadialGradientPaint(center, radius, focus, fractions, colors, cycleMethod);
    
//     // Set the paint and fill the background
//     g2d.setPaint(gradient);
//     g2d.fillRect(0, 0, width, height); // Fill the entire panel
    

//     g2d.dispose(); // Dispose of the graphics copy
//   }

  //Display text in the textarea 
    public void addEventListerners(){
        sendButton.addActionListener(e ->{
            String result = chatTextField.getText().trim();
            if(!result.isEmpty()){
                chatArea.append(CurrentUserName +": " + result + "\n");
                chatTextField.setText("");
            }
        });
        chatTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String result = chatTextField.getText().trim();
                    if(!result.isEmpty()){
                        chatArea.append(CurrentUserName +": " + result + "\n");
                        chatTextField.setText("");
                    }
                }
            }
        });
        leaveRoom.addActionListener(e ->{
            mainFrame.switchToLoginPage();
        });
    }

    
}