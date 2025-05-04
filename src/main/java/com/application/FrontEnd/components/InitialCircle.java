package com.application.FrontEnd.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font; 
import java.awt.FontMetrics; 
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

public class InitialCircle extends JPanel {

    private char initial; 
    private Color backgroundColor;
    private final Dimension preferredSize = new Dimension(24, 24); 

    public InitialCircle(char initial, Color backgroundColor) {
        this.initial = Character.toUpperCase(initial); 
        this.backgroundColor = (backgroundColor != null) ? backgroundColor : Color.GRAY; 

        setPreferredSize(preferredSize);
        setMinimumSize(preferredSize);   
        setMaximumSize(preferredSize);   

        setOpaque(false); 
    }

    public InitialCircle() {
        this('?', Color.GRAY); 
    }


    
    public void setData(char initial, Color backgroundColor) {
        this.initial = Character.toUpperCase(initial);
        this.backgroundColor = (backgroundColor != null) ? backgroundColor : Color.GRAY;
        repaint(); 
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();

        try { 
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(backgroundColor);
            g2d.fillOval(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.WHITE); 

            Font initialFont = new Font("SansSerif", Font.BOLD, getHeight() / 2 + 2); 
            g2d.setFont(initialFont);

            FontMetrics fm = g2d.getFontMetrics();
            String initialStr = String.valueOf(initial);
            int stringWidth = fm.stringWidth(initialStr);
            int stringAscent = fm.getAscent(); 

            int x = (getWidth() - stringWidth) / 2;

            int y = (getHeight() - fm.getHeight()) / 2 + stringAscent;

            g2d.drawString(initialStr, x, y); 

        } finally {
            g2d.dispose(); 
        }
    }
}