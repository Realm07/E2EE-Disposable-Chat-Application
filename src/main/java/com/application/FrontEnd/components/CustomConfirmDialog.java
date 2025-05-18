package com.application.FrontEnd.components; // Or a common dialogs package

import com.application.FrontEnd.MainFrame; // For fonts

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class CustomConfirmDialog extends JDialog {

    public enum UserOption {
        YES, NO, CANCEL // CANCEL could be from closing the dialog window
    }

    private UserOption userChoice = UserOption.CANCEL; // Default if closed

    private JLabel titleLabel;
    private JLabel messageLabel; // Or JTextArea for better wrapping
    private JButton yesButton;
    private JButton noButton;

    // Colors (adjust to match your image precisely)
    private static final Color DIALOG_BACKGROUND = new Color(68, 68, 68); // Dark gray
    private static final Color TEXT_COLOR_LIGHT = new Color(230, 230, 230); // Light gray/white
    private static final Color TEXT_COLOR_TITLE = Color.WHITE;
    private static final Color NO_BUTTON_COLOR = new Color(220, 80, 80);   // Reddish
    private static final Color YES_BUTTON_COLOR = new Color(80, 200, 80);  // Greenish
    private static final Color UNDERLINE_COLOR = new Color(100, 100, 100); // For title underline

    public CustomConfirmDialog(Frame parent, String title, String message, String yesButtonText, String noButtonText) {
        super(parent, true); // Modal dialog

        setTitle(title); // Dialog window title (can be empty if you draw your own)
        setUndecorated(true); // For fully custom look, optional
        // If undecorated, you'll need to handle window dragging or ensure it's not needed.
        // For simplicity here, let's assume it's decorated for now, or you add custom drag.

        // --- Main Panel ---
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Draw rounded rectangle background
                g2.setColor(getBackground()); // DIALOG_BACKGROUND
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); // Adjust arc width/height
                // Optional: Draw a border for the rounded rect
                // g2.setColor(Color.DARK_GRAY);
                // g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
                // super.paintComponent(g); // Don't call if you fully paint background
            }
        };
        mainPanel.setOpaque(false); // Important for custom painting of rounded shape
        mainPanel.setBackground(DIALOG_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1), // Optional outer border
                BorderFactory.createEmptyBorder(20, 25, 20, 25)     // Padding
        ));
        setContentPane(mainPanel);


        // --- Title Panel ---
        titleLabel = new JLabel(title);
        titleLabel.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(24f) : new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_COLOR_TITLE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // Add underline using a MatteBorder
        titleLabel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, UNDERLINE_COLOR), // Bottom underline
                new EmptyBorder(0, 0, 10, 0) // Padding below underline
        ));
        mainPanel.add(titleLabel, BorderLayout.NORTH);


        // --- Message Panel ---
        // Using JTextArea for easier multi-line text and wrapping. Style it like a JLabel.
        JTextArea messageArea = new JTextArea(message);
        messageArea.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(16f) : new Font("SansSerif", Font.PLAIN, 16));
        messageArea.setForeground(TEXT_COLOR_LIGHT);
        messageArea.setBackground(DIALOG_BACKGROUND); // Match dialog background
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setOpaque(false); // If mainPanel provides bg, make this transparent
        // Add some padding around the message
        JPanel messageWrapper = new JPanel(new BorderLayout());
        messageWrapper.setOpaque(false);
        messageWrapper.setBorder(new EmptyBorder(15, 0, 15, 0));
        messageWrapper.add(messageArea, BorderLayout.CENTER);
        mainPanel.add(messageWrapper, BorderLayout.CENTER);


        // --- Button Panel ---
        noButton = createStyledButton(noButtonText, NO_BUTTON_COLOR, e -> {
            userChoice = UserOption.NO;
            dispose();
        });

        yesButton = createStyledButton(yesButtonText, YES_BUTTON_COLOR, e -> {
            userChoice = UserOption.YES;
            dispose();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0)); // Centered buttons with gap
        buttonPanel.setOpaque(false); // Transparent background
        buttonPanel.add(noButton);
        buttonPanel.add(yesButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Allow closing with Escape key (maps to CANCEL/NO based on preference)
        getRootPane().registerKeyboardAction(e -> {
                    userChoice = UserOption.CANCEL; // Or UserOption.NO
                    dispose();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);


        pack(); // Size the dialog to fit its components
        setLocationRelativeTo(parent); // Center on parent
    }

    private JButton createStyledButton(String text, Color textColor, ActionListener actionListener) {
        JButton button = new JButton("<html><u>" + text + "</u></html>"); // HTML for underline
        button.setFont(MainFrame.sansationRegular != null ? MainFrame.sansationRegular.deriveFont(14f) : new Font("SansSerif", Font.PLAIN, 14));
        button.setForeground(textColor);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false); // Remove focus ring
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(actionListener);
        return button;
    }


    public UserOption getUserChoice() {
        return userChoice;
    }

    /**
     * Static method to show the custom confirm dialog.
     * @param parent The parent frame.
     * @param title The title of the dialog.
     * @param message The message to display.
     * @param yesButtonText Text for the "yes" option button.
     * @param noButtonText Text for the "no" option button.
     * @return The UserOption chosen by the user.
     */
    public static UserOption showDialog(Frame parent, String title, String message, String yesButtonText, String noButtonText) {
        CustomConfirmDialog dialog = new CustomConfirmDialog(parent, title, message, yesButtonText, noButtonText);
        dialog.setVisible(true); // This blocks until the dialog is disposed
        return dialog.getUserChoice();
    }

    // Example Usage:
    public static void main(String[] args) {
        // For testing purposes
        SwingUtilities.invokeLater(() -> {
            // Load fonts if MainFrame.sansation... are used
            // MainFrame.loadCustomFonts(); // If this method is public and static

            JFrame frame = new JFrame("Test Parent");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            JButton showDialogButton = new JButton("Show Custom Dialog");
            showDialogButton.addActionListener(e -> {
                UserOption choice = CustomConfirmDialog.showDialog(
                        frame,
                        "Warning!",
                        "Are you sure you want to download this file?",
                        "Yes, I trust the source",
                        "No, take me back"
                );
                System.out.println("User chose: " + choice);
                if (choice == UserOption.YES) {
                    System.out.println("Proceeding with download...");
                } else {
                    System.out.println("Download cancelled or dialog closed.");
                }
            });
            frame.setLayout(new FlowLayout());
            frame.add(showDialogButton);
        });
    }
}