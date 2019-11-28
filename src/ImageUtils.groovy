import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.WritableRaster

class ImageUtils {
    private static final int DCT_BLOCK_SIZE = 8
    
    /**
     * Creates a deep copy of a buffered image suitable for transformation. This is used
     * to avoid modifying the original image when creating an enciphered image.
     *
     * @param image the image to copy
     * @return a deep copy of the image
     */
    static BufferedImage deepCopy(BufferedImage image) {
        ColorModel model = image.getColorModel()
        WritableRaster raster = image.copyData(image.getRaster().createCompatibleWritableRaster())
        return new BufferedImage(model, raster, model.isAlphaPremultiplied(), null)
    }
    
    /**
     * Converts an RGB value into the YCbCr color space. The alpha channel from
     * the RGB is preserved in the return value.
     *
     * @param RGB the RGB value to convert
     * @return the corresponding YCbCr value
     */
    static int toYCbCr(int RGB) {
        Color color = new Color(RGB)
        int red = color.getRed()
        int green = color.getGreen()
        int blue = color.getBlue()
        int Y = (int) (0.299 * red + 0.587 * green + 0.114 * blue)
        int Cb = (int) (128 - 0.168736 * red - 0.331264 * green + 0.5 * blue)
        int Cr = (int) (128 + 0.5 * red - 0.418688 * green - 0.081312 * blue)
        return RGB & 0xFF000000 | Y << 16 | Cb << 8 | Cr
    }
}
