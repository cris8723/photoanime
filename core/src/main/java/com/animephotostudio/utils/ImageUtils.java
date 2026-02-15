package com.animephotostudio.utils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class ImageUtils {
    public static void applyWatermark(BufferedImage img, String text) {
        Graphics2D g = img.createGraphics();
        try {
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f);
            g.setComposite(ac);
            g.setColor(Color.WHITE);
            int size = Math.max(12, img.getWidth() / 30);
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, size);
            g.setFont(font);
            FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
            int textWidth = (int) (font.getStringBounds(text, frc).getWidth());
            int x = img.getWidth() - textWidth - 12;
            int y = img.getHeight() - 12;
            g.drawString(text, Math.max(8, x), y);
        } finally {
            g.dispose();
        }
    }
}