package com.application.FrontEnd.components;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTextField;

public class CustomTextField extends JTextField{

    public CustomTextField( int width, int height){
        super();

        setPreferredSize(new Dimension(width, height));

        setAlignmentX(Component.CENTER_ALIGNMENT);

        setMaximumSize(new Dimension(300, 30)); // You might reconsider if setMaximumSize is needed with GridBagLayout

        // Remove these lines to allow external control:
        // setBackground(new Color(255,255,255,250));
        // setBorder(BorderFactory.createEmptyBorder());
    }
}