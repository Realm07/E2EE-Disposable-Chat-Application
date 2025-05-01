package com.application.FrontEnd;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import com.application.FrontEnd.components.*;

public class PublicServerRoom extends JPanel {
    private CustomLabel titleLabel;

    private CustomLabel nameLabel;
    private CustomLabel userLabel ;
    private CustomButton joinButton;

    private String CurrentUserName;
    

    private MainFrame mainFrame;

    public PublicServerRoom(MainFrame mainFrame, String userName){
        this.mainFrame = mainFrame;
        this.CurrentUserName = userName;
        
        
        this.setLayout(new BorderLayout(0,15)); 
        this.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));


        //Header Lable panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titleLabel = new CustomLabel("Public Servers", 300, 50);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(titleLabel);
        
        //Index components panel
        JPanel centerContentPanel = new JPanel();
        centerContentPanel.setLayout(new BoxLayout(centerContentPanel, BoxLayout.Y_AXIS));
        
        CustomLabel indexName = new CustomLabel("Room Name", 120, 30);
        CustomLabel indexUsernumber = new CustomLabel("Users Online", 120, 30);
        CustomLabel indexJoin = new CustomLabel("Join Room", 120, 30);

        //Index Label panel
        JPanel indexHoldPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 120, 5)); 

        indexHoldPanel.add(indexName);
        indexHoldPanel.add(indexUsernumber);
        indexHoldPanel.add(indexJoin);
        indexHoldPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerContentPanel.add(indexHoldPanel);
        

        JPanel alphaPanel = createServerRooms("Alpha Room", "5/10", "Alpha", null);
        JPanel bravoPanel = createServerRooms("Bravo Base", "10/10", "Bravo", null);
        JPanel charliePanel = createServerRooms("Charlie Chat", "2/8", "Charlie", null);
        JPanel deltaPanel = createServerRooms("Delta Den", "7/12", "Delta", null);
        JPanel echoPanel = createServerRooms("Echo Chamber", "1/5", "Echo", null);       
        
        centerContentPanel.add(alphaPanel);
        centerContentPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing
        centerContentPanel.add(bravoPanel);
        centerContentPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing
        centerContentPanel.add(charliePanel);
        centerContentPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing
        centerContentPanel.add(deltaPanel);
        centerContentPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing
        centerContentPanel.add(echoPanel);

        centerContentPanel.add(Box.createVerticalGlue());

        
        add(topPanel, BorderLayout.NORTH);
        add(centerContentPanel, BorderLayout.CENTER);

    }

    private JPanel createServerRooms(String roomName, String userCount, String roomButton, String image){
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 100, 5));

        int nameWidth = 150;
        int userWidth = 100;
        int buttonWidth = 100;

        nameLabel = new CustomLabel(roomButton, nameWidth, 30);
        userLabel = new CustomLabel(userCount, userWidth, 30);
        joinButton = new CustomButton(roomButton, buttonWidth, 30, Color.GRAY);

        joinButton.addActionListener(e ->{
            mainFrame.switchToChatRoom(CurrentUserName, roomName);
            System.out.println(CurrentUserName);
        });

        rowPanel.add(nameLabel);
        rowPanel.add(userLabel);
        rowPanel.add(joinButton);
        rowPanel.setBackground(Color.BLUE);

        int preferredHeight = nameLabel.getPreferredSize().height + 15;
        rowPanel.setPreferredSize(new Dimension(100, preferredHeight)); 
        // rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredHeight));
        rowPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        
        rowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return rowPanel;
    }

    

}
