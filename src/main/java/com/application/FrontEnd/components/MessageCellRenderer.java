package com.application.FrontEnd.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.application.FrontEnd.MainFrame;

public class MessageCellRenderer extends JPanel implements ListCellRenderer<Object>  {
    private InitialCircle initialCirclePanel;
    private JTextArea messageArea;
    private JLabel senderLabel; 
    private static final String LOGO_IMAGE_PATH = "/com/application/FrontEnd/images/corner-down-right.png";

    public MessageCellRenderer(){
        setLayout(new BorderLayout(5, 2)); 
        setOpaque(true); 

        JLabel logoLabel = createLogoLabel();

        initialCirclePanel = new InitialCircle();

        JPanel circleWrapper = new JPanel();
        circleWrapper.setLayout(new BoxLayout(circleWrapper, BoxLayout.Y_AXIS));
        circleWrapper.setOpaque(false);

        circleWrapper.add(initialCirclePanel);
        circleWrapper.add(logoLabel);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);

        senderLabel = new JLabel();
        senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        senderLabel.setForeground(new Color(210,210,210));
        senderLabel.setOpaque(false);
        senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        messageArea = new JTextArea();
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setEditable(false);
        messageArea.setOpaque(false); 
        messageArea.setForeground(new Color(230, 230, 230));
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        // messageArea.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        messageArea.setBorder(null);
        // messageArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(senderLabel, BorderLayout.NORTH);
        textPanel.add(messageArea, BorderLayout.SOUTH);        

        // Add components to the panel
        add(circleWrapper, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        
        
    }

    private JLabel createLogoLabel() {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        try {
            URL imgUrl = getClass().getResource(LOGO_IMAGE_PATH);
            if (imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                if (icon.getIconWidth() > 0) {
                    // Scale logo based on width - CHANGE SCALING HINT HERE
                    Image scaledImage = icon.getImage().getScaledInstance(
                            20,                         // target width (-1 for height maintains aspect ratio)
                            -1,                          // target height
                            Image.SCALE_SMOOTH     // <<--- Use REPLICATE for sharpness
                    );
                    label.setIcon(new ImageIcon(scaledImage));
                    // Set preferred size based on scaled icon to help layout
                    label.setPreferredSize(new Dimension(((ImageIcon)label.getIcon()).getIconWidth(), ((ImageIcon)label.getIcon()).getIconHeight()));
                } else throw new Exception("Logo ImageIcon invalid");
            } else { throw new Exception("Logo resource not found"); }
        } catch (Exception e) {
            System.err.println("ERROR loading logo (" + LOGO_IMAGE_PATH + "): " + e.getMessage());
            label.setText("AnonChat"); label.setForeground(Color.DARK_GRAY);
            label.setFont(MainFrame.sansationBold.deriveFont(10f));
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
            
        }   else {
            // Handle non-ChatMessage values if necessary
            messageText = value.toString(); // Keep only message if sender unknown
            sender = "?"; // Set a default sender display
        }


        initialCirclePanel.setData(initial, userColor);
        senderLabel.setText(sender );
        messageArea.setText(messageText);

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
            messageArea.setForeground(UIManager.getColor("List.selectionForeground"));
        } else {
            
            setBackground(list.getBackground()); 
            messageArea.setForeground(new Color(230, 230, 230));
        }

        setEnabled(list.isEnabled()); 
        setFont(list.getFont()); 

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

        r = 50 + (r % 156); 
        g = 50 + (g % 156);
        b = 50 + (b % 156);

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

        // Optional: Override toString for simple debugging if needed
        @Override
        public String toString() {
            return sender + ": " + message;
        }
    }
}
