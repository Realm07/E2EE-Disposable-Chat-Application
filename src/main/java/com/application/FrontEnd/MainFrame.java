package com.application.FrontEnd;

// Assuming Backend classes are in this package now
import com.application.Backend.ChatController;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.*;

public class MainFrame extends JFrame {
    private LoginPage loginPage;
    private ChatRoom chatRoom;
    private PublicServerRoom publicServerRoom;
    private ChatController chatController; // Add reference to the controller
    public static Font sansationRegular; // Make fonts accessible
    public static Font sansationBold;
    public MainFrame() {

        chatController = new ChatController(this);
        setTitle("AnonChat E2EE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

    public void switchToChatRoom(String currentUsername, String currentRoomName) {
        System.out.println("[MainFrame] Switching to ChatRoom for user: " + currentUsername + " in room: " + currentRoomName);
        remove(publicServerRoom);
        chatRoom = new ChatRoom(currentUsername, currentRoomName, this, chatController); // Pass controller
        add(chatRoom);
        chatController.setActiveChatRoomUI(chatRoom); // *** Inform controller about the active UI ***

        // Refresh UI
        revalidate();
        repaint();
    }

    public void switchToLoginPage() {
        System.out.println("[MainFrame] Switching to LoginPage.");
        if (chatRoom != null) {
             remove(chatRoom);
             chatController.setActiveChatRoomUI(null); // *** Inform controller UI is no longer active ***
             chatRoom = null; // Release reference
        }
        loginPage = new LoginPage(this, chatController); // Pass controller
        add(loginPage);

        // Refresh UI
        revalidate();
        repaint();
    }

    public void switchToPublicRoom(String CurrentUserName){
        System.out.println("[MainFrame] Switching to Pulbic server panel");
        if (loginPage != null) {
            remove(loginPage);
            loginPage = null; // Allow garbage collection
        }
        publicServerRoom = new PublicServerRoom(this, CurrentUserName);
        add(publicServerRoom);
        revalidate();
        repaint();
    }

    public void switchToPrivateRoom(){

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

    // --- Main Entry Point (Load fonts before creating UI) ---
    public static void main(String[] args) {
        // Ensure UI creation happens on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            // Load fonts first
            loadCustomFonts();
            // Then create the frame which uses the fonts
            new MainFrame();
        });
    }
}
