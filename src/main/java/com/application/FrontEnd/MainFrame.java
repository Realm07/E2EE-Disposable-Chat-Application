package com.application.FrontEnd;

// Assuming Backend classes are in this package now
import com.application.Backend.ChatController;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.*;
import java.awt.event.WindowAdapter; // Import WindowAdapter
import java.awt.event.WindowEvent;   // Import WindowEvent

public class MainFrame extends JFrame {
    private LoginPage loginPage;
    private ChatRoom chatRoom;
    private PublicServerRoom publicServerRoom;
    private PrivateRoomPage privateRoomPage; // Added
    private ChatController chatController; // Add reference to the controller
    private InfoPage infoPage;
    private SettingRoom settingRoom;


    public static Font sansationRegular; // Make fonts accessible
    public static Font sansationBold;

    public MainFrame() {

        chatController = new ChatController(this);
        setTitle("AnoChat");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("[MainFrame] Window closing event detected.");
                // Perform cleanup through the controller
                if (chatController != null) {
                    chatController.handleApplicationShutdown();
                } else {
                    System.err.println("[MainFrame] ChatController is null during window closing!");
                }
                // Close the window
                dispose();
                // Ensure the application exits
                System.exit(0);
            }
        });
        try {
            java.net.URL iconUrl = getClass().getResource("/com/application/FrontEnd/images/Chat_logo.png");
            if (iconUrl != null) {
                Image icon = Toolkit.getDefaultToolkit().getImage(iconUrl);
                setIconImage(icon);
            } else {
                System.err.println("Warning: App icon not found in classpath.");
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load application icon: " + e.getMessage());
        }
        // --- Window Initialization Sequence ---

        // 1. Add the INITIAL content panel (loginPage)
        loginPage = new LoginPage(this, chatController);
        add(loginPage);

        // 2. Pack the frame to calculate initial component layout sizes
        pack();

        // 3. Set the DESIRED SIZE after packing
        setSize(1024, 768); // Set your desired dimensions

        // 4. Center the frame AFTER setting the size
        setLocationRelativeTo(null);

        // 5. Make visible LAST
        setVisible(true);

        System.out.println("MainFrame Initialized. Size: " + getSize());
    }
    public ChatController getChatController() {
        return chatController;
    }
    public void switchToChatRoom(String currentUsername, String currentRoomName) {
        System.out.println("[MainFrame] Switching to ChatRoom for user: " + currentUsername + " in room: " + currentRoomName);
        // Remove whatever panel is currently showing
        getContentPane().removeAll(); // Remove all components from content pane

        chatRoom = new ChatRoom(currentUsername, currentRoomName, this, chatController); // Pass controller
        add(chatRoom);
        chatController.setActiveChatRoomUI(chatRoom); // Inform controller about the active UI

        // Refresh UI
        revalidate();
        repaint();
    }

    public void switchToLoginPage() {
        System.out.println("[MainFrame] Switching to LoginPage.");
        // Remove whatever panel is currently showing
        getContentPane().removeAll(); // Remove all components from content pane

        // Deactivate chat UI if it was active
        if (chatRoom != null) {
            chatController.setActiveChatRoomUI(null);
            chatRoom = null;
        }
        // Release references to other pages too
        publicServerRoom = null;
        privateRoomPage = null;


        loginPage = new LoginPage(this, chatController); // Pass controller
        add(loginPage);

        // Refresh UI
        revalidate();
        repaint();
    }


    public void switchToPublicRoom(String currentUserName){
        System.out.println("[MainFrame] Switching to Public server panel for user: " + currentUserName);
        // Remove whatever panel is currently showing
        getContentPane().removeAll();

        // Release references to other pages
        loginPage = null;
        chatRoom = null;
        privateRoomPage = null;
        if (chatRoom != null) chatController.setActiveChatRoomUI(null); // Deactivate chat UI if switching from it

        // <<< Pass the controller instance >>>
        publicServerRoom = new PublicServerRoom(this, currentUserName, this.chatController);
        add(publicServerRoom);
        revalidate();
        repaint();
    }

    public void switchToPrivateRoom(String currentUserName){ // Pass the username!
        System.out.println("[MainFrame] Switching to Private Room Page for user: " + currentUserName);
        // Remove whatever panel is currently showing
        getContentPane().removeAll();

        // Release references to other pages
        loginPage = null;
        chatRoom = null;
        publicServerRoom = null;
        if (chatRoom != null) chatController.setActiveChatRoomUI(null); // Deactivate chat UI if switching from it


        privateRoomPage = new PrivateRoomPage(this, currentUserName); // Pass username
        add(privateRoomPage);
        revalidate();
        repaint();
    }

    public void switchToInfoPage(){
        System.out.println("[MainFrame] Switching to Information panel.");
        if(loginPage != null){
            remove(loginPage);
            loginPage = null;
        }

        infoPage = new InfoPage(this);
        add(infoPage);

        revalidate();
        repaint(); 
    }

    private static void loadCustomFonts() {
        try {
            // Load Regular font
            InputStream regularStream = MainFrame.class.getResourceAsStream("/com/application/FrontEnd/fonts/Sansation_Regular.ttf");
            if (regularStream != null) {
                sansationRegular = Font.createFont(Font.TRUETYPE_FONT, regularStream).deriveFont(12f); // Default size 12pt
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(sansationRegular);
                System.out.println("Sansation Regular font loaded and registered.");
            } else {
                System.err.println("ERROR: Could not find Sansation_Regular.ttf resource. Using fallback.");
                sansationRegular = new Font("SansSerif", Font.PLAIN, 12); // Fallback
            }

            // Load Bold font
            InputStream boldStream = MainFrame.class.getResourceAsStream("/com/application/FrontEnd/fonts/Sansation_Bold.ttf");
            if (boldStream != null) {
                sansationBold = Font.createFont(Font.TRUETYPE_FONT, boldStream).deriveFont(12f); // Default size 12pt
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(sansationBold);
                System.out.println("Sansation Bold font loaded and registered.");
            } else {
                System.err.println("ERROR: Could not find Sansation_Bold.ttf resource. Using fallback.");
                sansationBold = new Font("SansSerif", Font.BOLD, 12); // Fallback
            }

        } catch (FontFormatException | IOException e) {
            System.err.println("ERROR loading custom fonts: " + e.getMessage());
            e.printStackTrace();
            // Set fallbacks if any exception occurred
            if (sansationRegular == null) sansationRegular = new Font("SansSerif", Font.PLAIN, 12);
            if (sansationBold == null) sansationBold = new Font("SansSerif", Font.BOLD, 12);
        }
    }

    public void switchToSettingRoom(){
        System.out.println("[MainFrame] Switching to Setting panel.");
        remove(chatRoom);
        settingRoom = new SettingRoom(this);
        add(settingRoom);
        revalidate();
        repaint();

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            loadCustomFonts();
            new MainFrame();
        });
    }
}
