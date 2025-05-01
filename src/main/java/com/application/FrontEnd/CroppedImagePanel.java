// src/main/java/com/application/FrontEnd/CroppedImagePanel.java
package com.application.FrontEnd; // Or com.application.FrontEnd.components

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
// Need java.net.URL for getResource
import java.net.URL;

/**
 * A JPanel that displays an image cropped from the center to fill the panel bounds
 * while maintaining the image's aspect ratio (zooming in if necessary).
 * NOTE: This is NOT used by LoginPage for the GIF background in the modified code above.
 */
public class CroppedImagePanel extends JPanel {

    private Image originalImage;
    private String imageLoadError = null;
    private String imagePathUsed = "Not Loaded"; // Added for better error messages

    public CroppedImagePanel(String imagePath) {
        this.imagePathUsed = imagePath; // Store path
        // Load the image during construction
        try {
            System.out.println("[CroppedImagePanel] Attempting to load image: " + imagePath);
            // Use URL for reliable resource loading
            URL imgUrl = getClass().getResource(imagePath);
            if (imgUrl != null) {
                this.originalImage = ImageIO.read(imgUrl);
                if (this.originalImage == null) {
                    // More specific error if ImageIO returns null
                    imageLoadError = "ImageIO.read returned null for: " + imagePath + " (invalid format or empty file?)";
                    System.err.println(imageLoadError);
                    // No throw needed here, null check handles it
                } else {
                    System.out.println("[CroppedImagePanel] Image loaded successfully ("
                            + originalImage.getWidth(null) + "x" + originalImage.getHeight(null) + ") from " + imgUrl);
                }
            } else {
                imageLoadError = "Image not found in classpath: " + imagePath;
                System.err.println(imageLoadError);
            }
        } catch (IOException e) {
            imageLoadError = "I/O Error loading image '" + imagePath + "': " + e.getMessage();
            System.err.println(imageLoadError);
            e.printStackTrace();
            this.originalImage = null; // Ensure image is null on error
        } catch (Exception e) { // Catch other potential errors during loading
            imageLoadError = "Unexpected Error loading image '" + imagePath + "': " + e.getMessage();
            System.err.println(imageLoadError);
            e.printStackTrace();
             this.originalImage = null;
        }

        // Keep opaque true to paint fallback background/error message
        setOpaque(true);
        // A slightly different default background for distinction
        setBackground(new Color(50, 50, 50));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Paint background (e.g., the grey color), border etc.

        if (originalImage == null) {
            // Enhanced error message drawing
             g.setColor(Color.DARK_GRAY);
             g.fillRect(0,0, getWidth(), getHeight()); // Dark BG for error message
             g.setColor(Color.YELLOW);
             g.setFont(new Font("SansSerif", Font.BOLD, 12));
             String errorText = imageLoadError != null ? imageLoadError : "Image is null";
             FontMetrics fm = g.getFontMetrics();
             int msgWidth = fm.stringWidth(errorText);
             int x = (getWidth() - msgWidth) / 2;
             int y = (getHeight() / 2) + fm.getAscent() / 2;
             g.drawString(errorText, Math.max(5, x), Math.max(20, y)); // Ensure visible
             return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try { // Use try-finally for dispose
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int panelW = getWidth();
            int panelH = getHeight();
            int imgW = originalImage.getWidth(this);
            int imgH = originalImage.getHeight(this);

            if (panelW <= 0 || panelH <= 0 || imgW <= 0 || imgH <= 0) {
                return; // Nothing to draw
            }

            double panelAspect = (double) panelW / panelH;
            double imgAspect = (double) imgW / imgH;

            int sx1 = 0, sy1 = 0, sx2 = imgW, sy2 = imgH;
            int cropW = imgW, cropH = imgH; // Dimensions of the crop area in image coordinates

            if (panelAspect > imgAspect) {
                // Panel is wider than image aspect -> Crop width
                cropW = (int) Math.round(imgH * panelAspect); // Use Math.round for precision
                sx1 = (imgW - cropW) / 2;
                sx2 = sx1 + cropW;
                 // Reset height crop vars
                 sy1 = 0;
                 sy2 = imgH;

            } else if (panelAspect < imgAspect) {
                 // Panel is taller than image aspect -> Crop height
                cropH = (int) Math.round(imgW / panelAspect); // Use Math.round
                sy1 = (imgH - cropH) / 2;
                sy2 = sy1 + cropH;
                 // Reset width crop vars
                 sx1 = 0;
                 sx2 = imgW;
            }
            // else: aspect ratios match, use full image (sx1=0, sy1=0, sx2=imgW, sy2=imgH)

            // Destination rectangle is always the full panel bounds
            int dx1 = 0;
            int dy1 = 0;
            int dx2 = panelW;
            int dy2 = panelH;

            // Draw the calculated source portion to the destination bounds
            g2d.drawImage(originalImage, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, this);

        } finally {
            g2d.dispose(); // Ensure graphics context is disposed
        }
    }

    // Optional: Override getPreferredSize if needed for layout managers
    // that respect it when this panel is used directly.
    // @Override
    // public Dimension getPreferredSize() {
    //     if (originalImage != null) {
    //         // Example: Base preferred size on image, maybe capped
    //         return new Dimension(Math.min(800, originalImage.getWidth(this)),
    //                              Math.min(600, originalImage.getHeight(this)));
    //     } else {
    //         return new Dimension(300, 200); // Default size if image fails
    //     }
    // }
}