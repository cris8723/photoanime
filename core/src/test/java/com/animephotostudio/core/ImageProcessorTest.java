package com.animephotostudio.core;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.animephotostudio.utils.ImageUtils;

class ImageProcessorTest {
    @BeforeAll
    static void beforeAll() {
        // ensure AWT works in CI/headless
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void applyAnimeEffect_preservesDimensionsAndAltersPixels() {
        int w = 20, h = 10;
        BufferedImage src = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        // left = red, right = green
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                src.setRGB(x, y, x < w / 2 ? 0xFFFF0000 : 0xFF00FF00);
            }
        }

        ImageProcessor p = new ImageProcessor();
        BufferedImage out = p.applyAnimeEffect(src);

        assertNotNull(out);
        assertEquals(w, out.getWidth());
        assertEquals(h, out.getHeight());

        boolean anyDifferent = false;
        for (int y = 0; y < h && !anyDifferent; y++) {
            for (int x = 0; x < w; x++) {
                if (src.getRGB(x, y) != out.getRGB(x, y)) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "Processed image must differ from source");
    }

    @Test
    void edgeDetect_detectsVerticalEdge() {
        int w = 10, h = 6;
        BufferedImage src = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        // left black, right white
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                src.setRGB(x, y, x < w / 2 ? 0xFF000000 : 0xFFFFFFFF);
            }
        }

        ImageProcessor p = new ImageProcessor();
        BufferedImage edges = p.edgeDetect(src);

        assertEquals(w, edges.getWidth());
        assertEquals(h, edges.getHeight());

        // Expect strong edges near the seam in each row
        int seamX = w / 2 - 1;
        boolean seamFound = false;
        for (int y = 1; y < h - 1; y++) {
            int val = edges.getRGB(seamX, y) & 0xFF;
            if (val > 48) seamFound = true;
        }
        assertTrue(seamFound, "Edge detector should find the vertical seam");
    }

    @Test
    void posterize_channelValuesAreMultiplesOfStep() {
        int w = 4, h = 1;
        BufferedImage src = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        src.setRGB(0, 0, new Color(10, 20, 30).getRGB());
        src.setRGB(1, 0, new Color(60, 100, 140).getRGB());
        src.setRGB(2, 0, new Color(130, 170, 200).getRGB());
        src.setRGB(3, 0, new Color(220, 230, 240).getRGB());

        ImageProcessor p = new ImageProcessor();
        int levels = 4;
        BufferedImage out = p.posterize(src, levels);

        int step = Math.max(1, 256 / levels);
        for (int x = 0; x < w; x++) {
            int rgb = out.getRGB(x, 0);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            assertEquals(0, r % step, "R must be multiple of step");
            assertEquals(0, g % step, "G must be multiple of step");
            assertEquals(0, b % step, "B must be multiple of step");
        }
    }

    @Test
    void adjustSaturation_increasesSaturation() {
        BufferedImage src = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        float hue = 0.3f, sat = 0.2f, bri = 0.9f;
        int rgb = Color.HSBtoRGB(hue, sat, bri);
        src.setRGB(0, 0, (0xFF << 24) | (rgb & 0xFFFFFF));

        ImageProcessor p = new ImageProcessor();
        BufferedImage out = p.adjustSaturation(src, 1.5f);

        int outRgb = out.getRGB(0, 0);
        int or = (outRgb >> 16) & 0xFF;
        int og = (outRgb >> 8) & 0xFF;
        int ob = outRgb & 0xFF;
        float[] hsbOut = Color.RGBtoHSB(or, og, ob, null);
        assertTrue(hsbOut[1] >= sat - 1e-6, "Saturation should not decrease");
    }

    @Test
    void applyWatermark_modifiesImage() {
        BufferedImage img = new BufferedImage(200, 80, BufferedImage.TYPE_INT_ARGB);
        int[] before = new int[200 * 80];
        img.getRGB(0, 0, 200, 80, before, 0, 200);

        ImageUtils.applyWatermark(img, "TEST-WM");

        int[] after = new int[200 * 80];
        img.getRGB(0, 0, 200, 80, after, 0, 200);

        Set<Integer> diffs = new HashSet<>();
        for (int i = 0; i < before.length; i++) {
            if (before[i] != after[i]) diffs.add(after[i]);
        }
        assertTrue(diffs.size() > 0, "Watermark should change pixels in the image");
    }

    @Test
    void posterize_levelsOneAndMax() {
        BufferedImage src = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        src.setRGB(0, 0, new Color(12, 34, 56).getRGB());
        src.setRGB(1, 0, new Color(128, 200, 77).getRGB());
        src.setRGB(2, 0, new Color(250, 10, 180).getRGB());

        ImageProcessor p = new ImageProcessor();
        BufferedImage one = p.posterize(src, 1);
        // levels=1 -> step = 256 -> channels become 0
        for (int x = 0; x < 3; x++) {
            int rgb = one.getRGB(x, 0);
            assertEquals(0, (rgb >> 16) & 0xFF);
            assertEquals(0, (rgb >> 8) & 0xFF);
            assertEquals(0, rgb & 0xFF);
        }

        BufferedImage exact = p.posterize(src, 256);
        // levels=256 -> step=1 -> identical to original (ignoring alpha)
        for (int x = 0; x < 3; x++) {
            int in = src.getRGB(x, 0) & 0xFFFFFF;
            int out = exact.getRGB(x, 0) & 0xFFFFFF;
            assertEquals(in, out);
        }
    }

    @Test
    void edgeDetect_handlesTinyImages() {
        ImageProcessor p = new ImageProcessor();
        BufferedImage a = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        a.setRGB(0, 0, 0xFF112233);
        BufferedImage r1 = p.edgeDetect(a);
        assertNotNull(r1);
        assertEquals(1, r1.getWidth());
        assertEquals(1, r1.getHeight());

        BufferedImage b = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        b.setRGB(0, 0, 0xFF000000);
        b.setRGB(1, 0, 0xFFFFFFFF);
        BufferedImage r2 = p.edgeDetect(b);
        assertNotNull(r2);
        assertEquals(2, r2.getWidth());
        assertEquals(2, r2.getHeight());
    }

    @Test
    void adjustSaturation_clampsToOne() {
        BufferedImage src = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        float hue = 0.7f, sat = 0.8f, bri = 0.6f;
        int rgb = Color.HSBtoRGB(hue, sat, bri);
        src.setRGB(0, 0, (0xFF << 24) | (rgb & 0xFFFFFF));

        ImageProcessor p = new ImageProcessor();
        BufferedImage out = p.adjustSaturation(src, 10f);
        int or = (out.getRGB(0, 0) >> 16) & 0xFF;
        int og = (out.getRGB(0, 0) >> 8) & 0xFF;
        int ob = out.getRGB(0, 0) & 0xFF;
        float[] hsbOut = Color.RGBtoHSB(or, og, ob, null);
        assertTrue(hsbOut[1] <= 1.0f);
        assertTrue(hsbOut[1] >= sat - 1e-6);
    }

    @Test
    void applyAnimeEffect_darkensEdges() {
        int w = 20, h = 8;
        BufferedImage src = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                src.setRGB(x, y, x < w / 2 ? 0xFF88CCFF : 0xFF88CCFF);
            }
        }
        // add a white vertical seam to force an edge
        for (int y = 0; y < h; y++) src.setRGB(w / 2, y, 0xFFFFFFFF);

        ImageProcessor p = new ImageProcessor();
        BufferedImage out = p.applyAnimeEffect(src);

        int seamX = w / 2;
        int innerX = seamX - 3;
        boolean foundDarker = false;
        for (int y = 2; y < h - 2; y++) {
            float innerLum = luminance(out.getRGB(innerX, y));
            // look for any darker pixel within +/-2 of seam
            for (int dx = -2; dx <= 2; dx++) {
                int sx = Math.max(0, Math.min(w - 1, seamX + dx));
                float lum = luminance(out.getRGB(sx, y));
                if (lum < innerLum - 1e-3) {
                    foundDarker = true;
                    break;
                }
            }
            if (foundDarker) break;
        }
        assertTrue(foundDarker, "At least one pixel near the seam should be darker than inner area");
    }

    @Test
    void posterize_invalidLevels_throws() {
        BufferedImage src = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        ImageProcessor p = new ImageProcessor();
        assertThrows(IllegalArgumentException.class, () -> p.posterize(src, 0));
        assertThrows(IllegalArgumentException.class, () -> p.posterize(src, -3));
    }

    @Test
    void posterize_preservesAlpha() {
        BufferedImage src = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        src.setRGB(0, 0, (0x10 << 24) | (10 << 16) | (20 << 8) | 30);
        src.setRGB(1, 0, (0x80 << 24) | (60 << 16) | (100 << 8) | 140);
        src.setRGB(2, 0, (0xFF << 24) | (220 << 16) | (230 << 8) | 240);

        ImageProcessor p = new ImageProcessor();
        BufferedImage out = p.posterize(src, 4);

        for (int x = 0; x < 3; x++) {
            int ia = (src.getRGB(x, 0) >> 24) & 0xFF;
            int oa = (out.getRGB(x, 0) >> 24) & 0xFF;
            assertEquals(ia, oa, "Alpha must be preserved by posterize");
        }
    }

    @Test
    void adjustSaturation_preservesAlpha() {
        BufferedImage src = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        src.setRGB(0, 0, (0x20 << 24) | Color.HSBtoRGB(0.2f, 0.1f, 0.9f));
        src.setRGB(1, 0, (0xE0 << 24) | Color.HSBtoRGB(0.6f, 0.7f, 0.5f));

        ImageProcessor p = new ImageProcessor();
        BufferedImage out = p.adjustSaturation(src, 1.5f);

        for (int x = 0; x < 2; x++) {
            int ia = (src.getRGB(x, 0) >> 24) & 0xFF;
            int oa = (out.getRGB(x, 0) >> 24) & 0xFF;
            assertEquals(ia, oa, "Alpha must be preserved by adjustSaturation");
        }
    }

    @Test
    void applyAnimeEffect_concurrent() throws Exception {
        int threads = 8;
        BufferedImage src = new BufferedImage(300, 120, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int px = ((x * 37) & 0xFF);
                int py = ((y * 61) & 0xFF);
                src.setRGB(x, y, (0xFF << 24) | (px << 16) | (py << 8) | ((px + py) & 0xFF));
            }
        }

        ImageProcessor p = new ImageProcessor();
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        try {
            List<Future<BufferedImage>> fut = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                fut.add(ex.submit(() -> p.applyAnimeEffect(src)));
            }
            for (Future<BufferedImage> f : fut) {
                BufferedImage out = f.get();
                assertNotNull(out);
                assertEquals(src.getWidth(), out.getWidth());
                assertEquals(src.getHeight(), out.getHeight());
            }
        } finally {
            ex.shutdownNow();
        }
    }

    private static float luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }
}