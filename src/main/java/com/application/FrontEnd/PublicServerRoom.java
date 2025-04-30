package com.application.FrontEnd;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

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
    private CustomLabel titlePanel;
    
    private CustomButton alphaButton;
    private CustomButton bravoButton;
    private CustomButton charlieButton;
    private CustomButton deltaButton;
    private CustomButton echoButton;

    private CustomLabel alphaLabel;
    private CustomLabel bravoLabel;
    private CustomLabel charlieLabel;
    private CustomLabel deltaLabel;
    private CustomLabel echoLabel;


    private MainFrame mainFrame;

    public PublicServerRoom(MainFrame mainFrame){
        this.mainFrame = mainFrame;
        
        
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); 
        this.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        //Main Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));


        //Lable panel
        JPanel labelRoomPanel = new JPanel();
        labelRoomPanel.setLayout(new BoxLayout(labelRoomPanel, BoxLayout.Y_AXIS));

        titlePanel = new CustomLabel("Public Servers..", 100, 50);
        titlePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        labelRoomPanel.add(Box.createVerticalGlue());
        labelRoomPanel.add(titlePanel);
        labelRoomPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        labelRoomPanel.setFont(labelRoomPanel.getFont().deriveFont(Font.BOLD));
        labelRoomPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        

        //Index components
        CustomLabel indexName = new CustomLabel("Room Name", 120, 30);
        CustomLabel indexUsernumber = new CustomLabel("Users Online", 120, 30);
        CustomLabel indexJoin = new CustomLabel("Join Room", 120, 30);

        //Index Label panel
        JPanel indexHoldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0)); // Or GridBagLayout

        indexHoldPanel.add(indexName);
        indexHoldPanel.add(Box.createHorizontalStrut(150)); // Add spacing if needed
        indexHoldPanel.add(indexUsernumber);
        indexHoldPanel.add(Box.createHorizontalStrut(100)); // Add spacing if needed
        indexHoldPanel.add(indexJoin);
        indexHoldPanel.setAlignmentX(Component.LEFT_ALIGNMENT);


        mainPanel.add(labelRoomPanel);
        mainPanel.add(indexHoldPanel);

        add(mainPanel);
        
    }

    public void switchToChatRoom(){

    }
}
