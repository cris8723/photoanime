package com.animephotostudio.utils;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import org.junit.jupiter.api.Test;

class ImageUtilsTest {

    @Test
    void copyCreatesIndependentBufferedImage() {
        BufferedImage src = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        src.setRGB(0, 0, 0xFF112233);
        BufferedImage c = ImageUtils.copy(src);
        assertNotSame(src, c);
        assertEquals(src.getRGB(0, 0), c.getRGB(0, 0));
        // modify copy and ensure source unchanged
        c.setRGB(0, 0, 0xFF445566);
        assertNotEquals(src.getRGB(0, 0), c.getRGB(0, 0));
    }
}
