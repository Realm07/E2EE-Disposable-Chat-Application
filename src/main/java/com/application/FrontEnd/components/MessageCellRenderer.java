package com.application.FrontEnd.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import com.application.FrontEnd.MainFrame;

public class MessageCellRenderer extends JPanel implements ListCellRenderer<Object>  {
    private InitialCircle initialCirclePanel;
    private JTextArea messageArea;
    private JLabel senderLabel;
    private static final String LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/corner-down-right.png";

    private final Color defaultMessageColor = new Color(230, 230, 230);
    private final Color defaultSenderColor = new Color(210,210,210);


    public MessageCellRenderer(){
        setLayout(new BorderLayout(5, 2));
        
        setOpaque(false); 

        JLabel logoLabel = createLogoLabel();
        logoLabel.setOpaque(false);

        initialCirclePanel = new InitialCircle();
        initialCirclePanel.setOpaque(false); 

        JPanel circleWrapper = new JPanel();
        circleWrapper.setLayout(new BoxLayout(circleWrapper, BoxLayout.Y_AXIS));
        circleWrapper.setOpaque(false);

        circleWrapper.add(initialCirclePanel);
        circleWrapper.add(logoLabel);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);

        senderLabel = new JLabel();
        senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        senderLabel.setForeground(defaultSenderColor);
        senderLabel.setOpaque(false);
        senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        messageArea = new JTextArea();
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setForeground(defaultMessageColor);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        messageArea.setBorder(null);

        textPanel.add(senderLabel, BorderLayout.NORTH);
        textPanel.add(messageArea, BorderLayout.SOUTH);

        add(circleWrapper, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    }

    private JLabel createLogoLabel() {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setOpaque(false);
        try {
            URL imgUrl = getClass().getResource(LOGO_IMAGE_PATH);
            if (imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                if (icon.getIconWidth() > 0) {
                    Image scaledImage = icon.getImage().getScaledInstance(
                            20, -1, Image.SCALE_SMOOTH);
                    label.setIcon(new ImageIcon(scaledImage));
                    label.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
                } else throw new Exception("Logo ImageIcon invalid");
            } else { throw new Exception("Logo resource not found"); }
        } catch (Exception e) {
            System.err.println("ERROR loading logo (" + LOGO_IMAGE_PATH + "): " + e.getMessage());
            label.setText(">"); // Simpler fallback
            label.setForeground(Color.GRAY);
            if (MainFrame.sansationBold != null) {
                label.setFont(MainFrame.sansationBold.deriveFont(10f));
            } else {
                label.setFont(new Font("SansSerif", Font.BOLD, 10));
            }
            label.setPreferredSize(new Dimension(20, 10));
        }
        return label;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        String sender = "???";
        String messageText = value.toString();
        char initial = '?';
        Color userColor = Color.DARK_GRAY;

        if (value instanceof ChatMessage) {
            ChatMessage chatMessage = (ChatMessage) value;
            sender = chatMessage.getSender();
            messageText = chatMessage.getMessage();
            if (sender != null && !sender.isEmpty()) {
                initial = sender.charAt(0);
                userColor = getUserColor(sender);
            }
        } else {
            messageText = value.toString();
            sender = "?";
        }

        initialCirclePanel.setData(initial, userColor);
        senderLabel.setText(sender );
        messageArea.setText(messageText);

        this.setBackground(list.getBackground());

        senderLabel.setForeground(defaultSenderColor); 
        messageArea.setForeground(defaultMessageColor);


        setEnabled(list.isEnabled());

        return this;
    }

    private Color getUserColor(String username) {
        if (username == null || username.isEmpty()) {
            return Color.GRAY;
        }
        int hash = username.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;

        r = 100 + (Math.abs(r) % 128); 
        g = 100 + (Math.abs(g) % 128);
        b = 100 + (Math.abs(b) % 128);

        return new Color(r, g, b);
    }

    public static class ChatMessage {
        private String sender;
        private String message;

        public ChatMessage(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }
        public String getSender() { return sender; }
        public String getMessage() { return message; }
        @Override
        public String toString() {
            return sender + ": " + message;
        }
    }
}