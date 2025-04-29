// src/main/java/com/application/FrontEnd/CroppedImagePanel.java
package com.application.FrontEnd; // Or com.application.FrontEnd.components

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * A JPanel that displays an image cropped from the center to fill the panel bounds
 * while maintaining the image's aspect ratio (zooming in if necessary).
 */
public class CroppedImagePanel extends JPanel {

    private Image originalImage;
    private String imageLoadError = null;

    public CroppedImagePanel(String imagePath) {
        // Load the image during construction
        try {
            System.out.println("[CroppedImagePanel] Attempting to load image: " + imagePath);
            java.net.URL imgUrl = getClass().getResource(imagePath);
            if (imgUrl != null) {
                this.originalImage = ImageIO.read(imgUrl);
                if (this.originalImage == null) {
                    throw new IOException("ImageIO.read returned null - invalid image format or file?");
                }
                System.out.println("[CroppedImagePanel] Image loaded successfully ("
                        + originalImage.getWidth(null) + "x" + originalImage.getHeight(null) + ")");
            } else {
                imageLoadError = "Image not found in classpath: " + imagePath;
                System.err.println(imageLoadError);
            }
        } catch (IOException e) {
            imageLoadError = "Error loading image: " + e.getMessage();
            System.err.println(imageLoadError);
            e.printStackTrace();
            this.originalImage = null; // Ensure image is null on error
        }
        // Set opaque to true if this panel should paint its own background when needed
        setOpaque(true);
        setBackground(new Color(64, 64, 64)); // Set a default background
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Paint background, border etc.

        if (originalImage == null) {
            // Handle image loading error - draw error message
            if (imageLoadError != null) {
                g.setColor(Color.RED);
                g.drawString(imageLoadError, 10, 20);
            } else {
                g.setColor(Color.YELLOW);
                g.drawString("Image is null", 10, 20);
            }
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create(); // Work on a copy
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); // Smoother drawing when cropped area is scaled

        int panelW = getWidth();
        int panelH = getHeight();
        int imgW = originalImage.getWidth(this); // Pass observer
        int imgH = originalImage.getHeight(this);

        if (panelW <= 0 || panelH <= 0 || imgW <= 0 || imgH <= 0) {
            g2d.dispose();
            return; // Nothing to draw if dimensions are invalid
        }

        // Calculate aspect ratios
        double panelAspect = (double) panelW / panelH;
        double imgAspect = (double) imgW / imgH;

        // Determine source rectangle (sx1, sy1, sx2, sy2) - the portion of the image to draw
        int sx1, sy1, sx2, sy2;
        int cropW, cropH; // Dimensions of the crop area in image coordinates

        if (panelAspect > imgAspect) {
            // Panel is wider than image (relative to height) -> Fit height, crop width
            cropH = imgH;
            cropW = (int) (imgH * panelAspect); // Calculate width needed to match panel aspect
            sx1 = (imgW - cropW) / 2; // Center the crop horizontally
            sy1 = 0;
        } else {
            // Panel is taller than or same aspect as image -> Fit width, crop height
            cropW = imgW;
            cropH = (int) (imgW / panelAspect); // Calculate height needed to match panel aspect
            sx1 = 0;
            sy1 = (imgH - cropH) / 2; // Center the crop vertically
        }
        sx2 = sx1 + cropW;
        sy2 = sy1 + cropH;


        // Destination rectangle (dx1, dy1, dx2, dy2) - where to draw on the panel (entire panel)
        int dx1 = 0;
        int dy1 = 0;
        int dx2 = panelW;
        int dy2 = panelH;

        // Draw the specified part of the image onto the panel bounds
        // drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer)
        g2d.drawImage(originalImage, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, this);

        g2d.dispose(); // Release graphics copy resources
    }

    // Override getPreferredSize if you want the panel to request a specific size
    // based on the image, although BorderLayout might override this.
    // @Override
    // public Dimension getPreferredSize() {
    //     // Example: return new Dimension(300, 400); // Or based on image ratio
    // }
}