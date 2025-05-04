package com.application.FrontEnd;

// Core Swing & AWT Imports
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;

import java.awt.*;
import java.awt.event.*;
import java.awt.GridBagConstraints;

// Image Loading Imports
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;

// Custom Components
import com.application.FrontEnd.components.*;


public class InfoPage extends JPanel {

    // --- References ---
    private MainFrame mainFrame;

    // --- Constants ---
    private static final String BACK_ICON_PATH = "/com/application/FrontEnd/images/ICON_Back.png";
    private static final String BACKGROUND_GIF_PATH = "/com/application/FrontEnd/images/BG_PublicRooms.png";

    // --- UI Component Fields ---
    private JLayeredPane layeredPane;
    private BackgroundImagePanel backgroundPanel;
    private JPanel mainContentPanel;
    private JTextArea infoTextArea;

    private String infoText;


    public InfoPage(MainFrame mainFrame){
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout());
        setOpaque(false);

        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        backgroundPanel = new BackgroundImagePanel(BACKGROUND_GIF_PATH);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

        mainContentPanel = new JPanel(new BorderLayout(0, 0));
        mainContentPanel.setOpaque(false);

        JPanel topPanel = createTopPanel();
        mainContentPanel.add(topPanel, BorderLayout.NORTH);

        JPanel centerContentPanel = createCenterContentPanel();
        mainContentPanel.add(centerContentPanel, BorderLayout.CENTER);

        layeredPane.add(mainContentPanel, JLayeredPane.PALETTE_LAYER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeAndPositionComponents();
            }
             @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(InfoPage.this::resizeAndPositionComponents);
            }
        });

        System.out.println("InfoPage structure initialized.");
    }

    private void resizeAndPositionComponents() {
        SwingUtilities.invokeLater(() -> {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            if (backgroundPanel != null) backgroundPanel.setBounds(0, 0, w, h);
            if (mainContentPanel != null) mainContentPanel.setBounds(0, 0, w, h);

            layeredPane.revalidate();
            layeredPane.repaint();
        });
    }


    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JButton backButton = createIconButton(BACK_ICON_PATH, "\u2190", "Back to Login");
        backButton.addActionListener(e -> {
            System.out.println("InfoPage Back button clicked - returning to Login Page");
            mainFrame.switchToLoginPage();
        });

        JLabel aboutLabel = new JLabel("About AnoChat");
        aboutLabel.setFont(MainFrame.sansationBold != null ? MainFrame.sansationBold.deriveFont(30f) : new Font("SansSerif", Font.BOLD, 30));
        aboutLabel.setForeground(Color.BLACK);
        aboutLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel rightPlaceholder = new JLabel();
        rightPlaceholder.setPreferredSize(backButton.getPreferredSize());

        topPanel.add(backButton, BorderLayout.WEST);
        topPanel.add(aboutLabel, BorderLayout.CENTER);
        topPanel.add(rightPlaceholder, BorderLayout.EAST);
        

        return topPanel;
    }

    private JPanel createCenterContentPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 40, 40));
        centerPanel.setOpaque(false);

        infoText = "-Welcome to the ephemeral world of AnonChat, a communication platform built for spontaneous connection. Dive into real-time conversations without the need for permanent accounts or lengthy sign-up processes. Your identity is simply the username you choose for the current session.\r\n" + //
                        "\r\n" + //
                        "-AnonChat is designed around transient interaction. When you close the application, your session ends. This focus on immediate communication makes it ideal for quick group chats, joining public discussions, or setting up temporary private spaces.\r\n" + //
                        "\r\n" + //
                        "-Your journey begins at the dynamic Login screen, featuring animated visuals. Enter your chosen username for this session, and then decide your path: will you explore the established Public Servers or create/join a Private Room?\r\n" + //
                        "\r\n" + //
                        "-The Public Servers area offers a list of pre-configured chat hubs open to all users. Beside each room name, you can see a basic indicator of current activity. Simply select a room and click 'Join' to enter the shared conversation space instantly.\r\n" + //
                        "\r\n" + //
                        "-For more controlled discussions, navigate to the Private Room section. Here, you hold the keys. To start a new, exclusive chat, enter a unique Room Name and a Password, then click 'Create Room'. This combination becomes the entry requirement for anyone else wanting to join.\r\n" + //
                        "\r\n" + //
                        "-To enter an existing private space, you must know both the exact Room Name and the correct Password used when it was created. Input these credentials and click 'Join Room'. The application verifies access based on this specific name/password pair.\r\n" + //
                        "\r\n" + //
                        "-Conversations within these Private Rooms gain essential confidentiality. AnonChat employs an encryption layer where the security key itself is generated directly from the unique combination of the room's name and its password. Every message sent and received within that specific private session is protected using this derived key.\r\n" + //
                        "\r\n" + //
                        "-AnonChat presents a unique visual identity using the Java Swing framework. Custom-built interface elements provide a distinct look, while key screens utilize animated backgrounds managed through layered panes, adding a layer of visual depth to the user experience. Navigation between sections is designed to be straightforward, often featuring a dedicated 'Back' button with an icon for easy returns.\r\n" + //
                        "\r\n" + //
                        "-Under the hood, the application relies on the Pusher service for its core real-time messaging capabilities, ensuring swift delivery across connected clients. Project structure and dependencies are managed using Apache Maven. The private room security layer is handled by a custom Encryption Service.\r\n" + //
                        "\r\n" + //
                        "-AnonChat serves as a functional demonstration of these combined technologies, showcasing concepts in GUI design, real-time networking, and basic session-based communication security.\r\n" + //
                        "\r\n" + //
                        "-Created By:\r\n" + //
                        "\r\n" + //
                        "-Mohammad Shees Abdulla" + 
                        "\r\n" +
                        "-Mohammad Monis Ahmed";
                        
        infoTextArea = new JTextArea(infoText);
        infoTextArea.setFont(new Font("SansSerif", Font.PLAIN, 18));
        infoTextArea.setForeground(Color.BLACK);
        infoTextArea.setLineWrap(true);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setEditable(false);
        infoTextArea.setFocusable(false);
        infoTextArea.setOpaque(false);
        infoTextArea.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(infoTextArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200, 150), 1));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        DefaultCaret caret = (DefaultCaret) infoTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        // scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());

        centerPanel.add(scrollPane, BorderLayout.CENTER);

        return centerPanel;
    }

    private JButton createIconButton(String iconPath, String fallbackText, String tooltip) {
        JButton button = new JButton();
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension iconButtonSize = new Dimension(30, 30);
        button.setPreferredSize(iconButtonSize);
        button.setMinimumSize(iconButtonSize);
        button.setMaximumSize(iconButtonSize);

        try {
            URL iconUrl = getClass().getResource(iconPath);
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                 if (icon.getImage() != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                    Image scaledImage = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                    button.setIcon(new ImageIcon(scaledImage));
                    System.out.println("[createIconButton] Icon loaded and scaled: " + iconPath);
                } else {
                     throw new IOException("ImageIcon created but image data is invalid or dimensions are zero for: " + iconPath);
                }
            } else {
                throw new IOException("Icon resource not found at path: " + iconPath);
            }
        } catch (Exception ex) {
            System.err.println("Warning: Could not load icon " + iconPath + ". Using text fallback. Error: " + ex.getMessage());
            button.setText(fallbackText);
            button.setFont(new Font("SansSerif", Font.BOLD, 20));
            button.setForeground(Color.WHITE);
        }
        return button;
    }


    private static class BackgroundImagePanel extends JPanel {
        private Image backgroundImage;
        private String errorMessage = null;
        private String imagePathUsed;

        public BackgroundImagePanel(String imagePath) {
            this.imagePathUsed = imagePath;
            try {
                URL imgUrl = getClass().getResource(imagePath);
                if (imgUrl != null) {
                    this.backgroundImage = ImageIO.read(imgUrl);
                    if (this.backgroundImage == null) throw new IOException("ImageIO returned null for " + imagePath);
                    System.out.println("[BGPanel] Loaded: " + imagePath + " for PrivateRoomPage");
                } else { throw new IOException("Resource not found: " + imagePath); }
            } catch (IOException e) {
                this.errorMessage = "Ex loading background: " + e.getMessage();
                System.err.println(errorMessage); this.backgroundImage = null;
            }
            setOpaque(true); // Background panel must be opaque
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                int panelW = getWidth(); int panelH = getHeight();
                int imgW = backgroundImage.getWidth(this); int imgH = backgroundImage.getHeight(this);
                if(imgW <=0 || imgH <= 0) { g2d.dispose(); return; }
                double imgAspect = (double) imgW / imgH; double panelAspect = (double) panelW / panelH;
                int drawW, drawH, drawX, drawY;
                if (panelAspect > imgAspect) { drawW = panelW; drawH = (int)(panelW / imgAspect); drawX = 0; drawY = (panelH - drawH) / 2; }
                else { drawH = panelH; drawW = (int)(panelH * imgAspect); drawX = (panelW - drawW) / 2; drawY = 0; }
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(backgroundImage, drawX, drawY, drawW, drawH, this);
                g2d.dispose();
            } else {
                g.setColor(new Color(30, 50, 70)); // Dark blue fallback
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.YELLOW); g.setFont(new Font("SansSerif", Font.BOLD, 14));
                FontMetrics fm = g.getFontMetrics();
                String text = "BG Load Error: " + (errorMessage != null ? errorMessage : "Unknown");
                int msgWidth = fm.stringWidth(text);
                g.drawString(text, Math.max(5, (getWidth() - msgWidth) / 2), getHeight() / 2 + fm.getAscent() / 2 - fm.getHeight()/2);
                String pathText = "(" + imagePathUsed + ")";
                msgWidth = fm.stringWidth(pathText);
                g.drawString(pathText, Math.max(5, (getWidth() - msgWidth) / 2), getHeight() / 2 + fm.getAscent() / 2 + fm.getHeight()/2);
            }
        }
    } // --- End Inner Class ---

} // End InfoPage class