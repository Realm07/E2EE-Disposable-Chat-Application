package com.application.FrontEnd;

// Assuming Backend classes are in this package now
import com.application.Backend.ChatController;

import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.*;

public class MainFrame extends JFrame {
    private LoginPage loginPage;
    private ChatRoom chatRoom;
    private ChatController chatController; // Add reference to the controller

    public MainFrame() {
        // --- Instantiate Controller ---
        chatController = new ChatController(this); // Pass reference to this frame

        setTitle("AnonChat E2EE"); // Update title maybe
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        try { // Basic error handling for image loading
             Image icon = Toolkit.getDefaultToolkit().getImage("src/main/java/com/application/FrontEnd/images/Chat_logo.png"); // More specific path
             setIconImage(icon);
        } catch (Exception e) {
             System.err.println("Warning: Could not load application icon.");
        }
        setBounds(100, 100, 850, 550); // Set initial size and position

        // --- Show Login Page ---
        loginPage = new LoginPage(this, chatController); // Pass controller to LoginPage
        add(loginPage);

        setLocationRelativeTo(null); // Center on screen
        setVisible(true);
        // No need for revalidate/repaint immediately after adding before first visible
    }

    public void switchToChatRoom(String currentUsername, String currentRoomName) {
        System.out.println("[MainFrame] Switching to ChatRoom for user: " + currentUsername + " in room: " + currentRoomName);
        if (loginPage != null) {
             remove(loginPage);
             loginPage = null; // Release reference
        }
         if (chatRoom != null) { // Clean up old chat room if switching directly between rooms (future)
             remove(chatRoom);
         }

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

    // Main entry point for the application - Creates the MainFrame
    public static void main(String[] args) {
        // Ensure UI creation happens on the Event Dispatch Thread (EDT) - Standard Swing practice
        SwingUtilities.invokeLater(() -> {
            new MainFrame();
        });
    }
}
