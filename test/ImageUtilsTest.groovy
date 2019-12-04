import org.junit.jupiter.api.Test

import java.awt.image.BufferedImage

import static org.junit.jupiter.api.Assertions.assertNotEquals
import static org.junit.jupiter.api.Assertions.assertEquals

class ImageUtilsTest {
    @Test
    void testDeepCopyBufferedImage() {
        BufferedImage original = new BufferedImage(5, 10, BufferedImage.TYPE_4BYTE_ABGR)
        original.setRGB(3, 6, 0xFFA01C0F as int)
        original.setRGB(2, 5, 0xFFAAAA01 as int)

        BufferedImage copy = ImageUtils.deepCopy(original)
        copy.setRGB(3, 6, 0xFF09D2AF as int)

        // Verifies references are different
        assertNotEquals(original, copy)

        // Verifies copy is deep
        assertNotEquals(original.getRGB(3, 6), copy.getRGB(3, 6))

        // Verifies image is copied
        assertEquals(original.getRGB(2, 5), copy.getRGB(2, 5))
    }

    @Test
    void testDeepCopyJPEGImage() {
        BufferedImage temp = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR)
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                temp.setRGB(i, j, 0xFF007BA7 as int)
            }
        }

        JPEGImage original = new JPEGImage(temp)
        JPEGImage copy = ImageUtils.deepCopy(original)

        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                assertEquals(original.getCb(i, j), copy.getCb(i, j))
            }
        }

        original.setCb(0, 0, 1000)
        assertNotEquals(original.getCb(0, 0), copy.getCb(0, 0))
    }

    @Test
    void testChannelArray() {
        int ARGB = 0xFFABCDEF

        int[] array = ImageUtils.channelArray(ARGB)

        assertEquals(0xFF, array[0])
        assertEquals(0xAB, array[1])
        assertEquals(0xCD, array[2])
        assertEquals(0xEF, array[3])
    }
}