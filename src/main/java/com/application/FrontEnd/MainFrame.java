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
    private CardLayout cardLayout;
    private JPanel mainViewPanel;


    private static final String LOGIN_PAGE_CARD = "LoginPage";
    private static final String CHAT_ROOM_CARD = "ChatRoom";
    private static final String PUBLIC_SERVER_ROOM_CARD = "PublicServerRoom";
    private static final String PRIVATE_ROOM_PAGE_CARD = "PrivateRoomPage";
    private static final String INFO_PAGE_CARD = "InfoPage";
    private static final String SETTING_ROOM_CARD = "SettingRoom";


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

        cardLayout = new CardLayout();
        mainViewPanel = new JPanel(cardLayout);
        add(mainViewPanel, BorderLayout.CENTER);

        loginPage = new LoginPage(this, chatController);
        mainViewPanel.add(loginPage, LOGIN_PAGE_CARD);

        settingRoom = new SettingRoom(this);
        mainViewPanel.add(settingRoom, SETTING_ROOM_CARD);

        cardLayout.show(mainViewPanel, LOGIN_PAGE_CARD);

        pack();

        setSize(1024, 768); 

        setLocationRelativeTo(null);

        setVisible(true);

        System.out.println("MainFrame Initialized. Size: " + getSize());
    }

    public ChatController getChatController() {
        return chatController;
    }

    public void switchToChatRoom(String currentUsername, String currentRoomName) {
        System.out.println("[MainFrame] Switching to ChatRoom for user: " + currentUsername + " in room: " + currentRoomName);
        
        if(chatRoom == null){
            System.err.println("ChatRoom instance is null in switchToChatRoom. Recreating.");
            chatRoom = new ChatRoom(currentUsername, currentRoomName, this, chatController);
            mainViewPanel.add(chatRoom, CHAT_ROOM_CARD);
        }
         chatRoom.reInitializeForNewSession(currentUsername, currentRoomName);

         if(chatController != null){
            chatController.setActiveChatRoomUI(chatRoom);
         }
         cardLayout.show(mainViewPanel, CHAT_ROOM_CARD);
         mainViewPanel.revalidate();
         mainViewPanel.repaint();
    }

    public void switchToLoginPage() {
        System.out.println("[MainFrame] Switching to LoginPage.");

        if(chatController != null){
            chatController.setActiveChatRoomUI(null);
        }

        if(loginPage == null){
            System.err.println("Error: LoginPage instance is null. Recreating.");
            loginPage = new LoginPage(this, chatController);
            mainViewPanel.add(loginPage, LOGIN_PAGE_CARD);
        }

        cardLayout.show(mainViewPanel, LOGIN_PAGE_CARD);
        mainViewPanel.revalidate();
        mainViewPanel.repaint();

    }


    public void switchToPublicRoom(String currentUserName){
        System.out.println("[MainFrame] Switching to Public server panel for user: " + currentUserName);
        
        if(chatController != null){
            chatController.setActiveChatRoomUI(null);
        }

        if(publicServerRoom == null){
            publicServerRoom = new PublicServerRoom(this, currentUserName, chatController);
            mainViewPanel.add(publicServerRoom, PUBLIC_SERVER_ROOM_CARD);
        }

        cardLayout.show(mainViewPanel, PUBLIC_SERVER_ROOM_CARD);
        mainViewPanel.revalidate();
        mainViewPanel.repaint();
    }

    public void switchToPrivateRoom(String currentUserName){
        System.out.println("[MainFrame] Switching to Private Room Page for user: " + currentUserName);
        
        if(chatController != null){
            chatController.setActiveChatRoomUI(null);
        }

        if(privateRoomPage == null){
            privateRoomPage = new PrivateRoomPage(this, currentUserName);
            mainViewPanel.add(privateRoomPage, PRIVATE_ROOM_PAGE_CARD);
        }

        cardLayout.show(mainViewPanel, PRIVATE_ROOM_PAGE_CARD);
        mainViewPanel.revalidate();
        mainViewPanel.repaint();
    }

    public void switchToInfoPage(){
        System.out.println("[MainFrame] Switching to Information panel.");
        if(loginPage != null){
            infoPage = new InfoPage(this);
            mainViewPanel.add(infoPage, INFO_PAGE_CARD);
        }

        cardLayout.show(mainViewPanel, INFO_PAGE_CARD);
        mainViewPanel.revalidate();
        mainViewPanel.repaint();
    }

     public void switchToSettingRoom() {
        System.out.println("[MainFrame] Switching to Setting panel.");
        if (settingRoom == null) { 
             System.err.println("Error: SettingRoom instance is null. Recreating.");
            settingRoom = new SettingRoom(this);
            mainViewPanel.add(settingRoom, SETTING_ROOM_CARD);
        }
        cardLayout.show(mainViewPanel, SETTING_ROOM_CARD);
        mainViewPanel.revalidate();
        mainViewPanel.repaint();
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 

    public void showChatRoomCard() { 
        System.out.println("[MainFrame] Showing ChatRoom card.");
        if (chatRoom == null) { 
            System.err.println("Error: ChatRoom instance is null when trying to show its card!");
            switchToLoginPage(); 
            return;
        }
        // if (chatController != null && chatController.getActiveChatRoomUI() == null) {
        //     chatController.setActiveChatRoomUI(chatRoom);
        // }
        
        cardLayout.show(mainViewPanel, CHAT_ROOM_CARD);
        mainViewPanel.revalidate();
        mainViewPanel.repaint();
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 

    public void applyChatRoomBackground(String imagePath) {
        if (chatRoom != null) {
            chatRoom.setBackgroundImage(imagePath); // Assuming setBackgroundImage exists in ChatRoom
        } else {
            System.err.println("[MainFrame] Attempted to set background but ChatRoom instance is null.");
        }
    }

    private static void loadCustomFonts() {
        try {
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

   

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            loadCustomFonts();
            new MainFrame();
        });
    }
}
