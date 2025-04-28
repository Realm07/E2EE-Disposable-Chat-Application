package com.application.FrontEnd.components;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JLabel;

public class CustomLabel extends JLabel{
    public CustomLabel(String text, int width, int height){
        super(text);

        setPreferredSize(new Dimension(width, height));

        setMaximumSize(new Dimension(300, 30));

        setFont(new Font("Segoe UI", Font.BOLD, 15));
        
        
    }
}
