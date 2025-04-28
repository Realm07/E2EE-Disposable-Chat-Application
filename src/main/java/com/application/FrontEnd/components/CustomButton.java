package com.application.FrontEnd.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;

public class CustomButton extends JButton {
    private int cornerRadius = 15;
    private Color backgroundColor;

    public CustomButton(String text, int width, int height, Color bgColor) {
        super(text);

        this.backgroundColor = bgColor; 

        setPreferredSize(new Dimension(width, height));

        setAlignmentY(Component.CENTER_ALIGNMENT);

        setContentAreaFilled(false);

        setBorderPainted(false);

        setFocusPainted(false);

        setOpaque(false);

        setRolloverEnabled(false);

        setForeground(Color.WHITE); 
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create(); 

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (this.backgroundColor != null) {
            g2d.setColor(this.backgroundColor);
        } else {
           
            g2d.setColor(new Color(50, 50, 50));
        }
    
        g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

        super.paintComponent(g);

        g2d.dispose(); 
    }

    public void setBackgroundColor(Color bgColor) {
        this.backgroundColor = bgColor;
        repaint(); 
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }
    
}