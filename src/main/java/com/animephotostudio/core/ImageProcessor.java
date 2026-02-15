package com.animephotostudio.core;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageProcessor {

    /** Pipeline that composes the anime-style look */
    public BufferedImage applyAnimeEffect(BufferedImage src) {
        BufferedImage poster = posterize(src, 8);                    // posterize colors
        BufferedImage saturated = adjustSaturation(poster, 1.15f);    // slight saturation boost
        BufferedImage edges = edgeDetect(src);                        // strong edge map
        BufferedImage output = overlayEdges(saturated, edges);        // combine
        return output;
    }

    public BufferedImage edgeDetect(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        int w = gray.getWidth(), h = gray.getHeight();
        BufferedImage edges = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        int[] gx = {-1, 0, 1, -2, 0, 2, -1, 0, 1};
        int[] gy = {-1, -2, -1, 0, 0, 0, 1, 2, 1};

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int sumX = 0;
                int sumY = 0;
                int idx = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int rgb = gray.getRGB(x + kx, y + ky) & 0xFF;
                        sumX += gx[idx] * rgb;
                        sumY += gy[idx] * rgb;
                        idx++;
                    }
                }
                int mag = (int) Math.min(255, Math.sqrt(sumX * sumX + sumY * sumY));
                int val = 0xFF000000 | (mag << 16) | (mag << 8) | mag;
                edges.setRGB(x, y, val);
            }
        }
        return edges;
    }

    public BufferedImage posterize(BufferedImage src, int levels) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int step = Math.max(1, 256 / levels);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                int a = (rgb >> 24) & 0xFF;
                int r = ((rgb >> 16) & 0xFF) / step * step;
                int g = ((rgb >> 8) & 0xFF) / step * step;
                int b = (rgb & 0xFF) / step * step;
                int nrgb = (a << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
                out.setRGB(x, y, nrgb);
            }
        }
        return out;
    }

    public BufferedImage adjustSaturation(BufferedImage src, float factor) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgba = src.getRGB(x, y);
                int a = (rgba >> 24) & 0xFF;
                int r = (rgba >> 16) & 0xFF;
                int g = (rgba >> 8) & 0xFF;
                int b = rgba & 0xFF;
                float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
                hsb[1] = clampFloat(hsb[1] * factor, 0f, 1f);
                int rgb2 = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                rgb2 = (a << 24) | (rgb2 & 0xFFFFFF);
                out.setRGB(x, y, rgb2);
            }
        }
        return out;
    }

    private BufferedImage overlayEdges(BufferedImage base, BufferedImage edges) {
        BufferedImage out = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(base, 0, 0, null);
        // paint black edges where gradient is strong
        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int e = edges.getRGB(x, y) & 0xFF;
                if (e > 48) {
                    int bg = out.getRGB(x, y);
                    int mixed = blendColors(bg, 0xFF000000, 0.9f);
                    out.setRGB(x, y, mixed);
                }
            }
        }
        g.dispose();
        return out;
    }

    private int blendColors(int src, int over, float alpha) {
        int sa = (src >> 24) & 0xFF;
        int sr = (src >> 16) & 0xFF;
        int sg = (src >> 8) & 0xFF;
        int sb = src & 0xFF;

        int oa = (over >> 24) & 0xFF;
        int or = (over >> 16) & 0xFF;
        int og = (over >> 8) & 0xFF;
        int ob = over & 0xFF;

        int nr = (int) (sr * (1 - alpha) + or * alpha);
        int ng = (int) (sg * (1 - alpha) + og * alpha);
        int nb = (int) (sb * (1 - alpha) + ob * alpha);
        int na = sa;
        return (na << 24) | (clamp(nr) << 16) | (clamp(ng) << 8) | clamp(nb);
    }

    private int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    private float clampFloat(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
}