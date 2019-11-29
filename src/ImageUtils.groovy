import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.WritableRaster

class ImageUtils {
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
    static Number[] toYCbCr(int[] RGB) {
        int R = RGB[1]
        int G = RGB[2]
        int B = RGB[3]
        Number Y = 0.299 * R + 0.587 * G + 0.114 * B
        Number Cb = 128 - 0.168736 * R - 0.331264 * G + 0.5 * B
        Number Cr = 128 + 0.5 * R - 0.418688 * G - 0.081312 * B
        if (Y > 255) {
            Y = 255
        }
        else if (Y < 0) {
            Y = 0
        }
        else if (Cb > 255) {
            Cb = 255
        }
        else if (Cb < 0) {
            Cb = 0
        }
        else if (Cr > 255) {
            Cr = 255
        }
        else if (Cr < 0) {
            Cr = 0
        }
        return [RGB[0], Y, Cb, Cr]
    }

    /**
     * Converts a YCbCr value into the RGB color space. The alpha channel from the
     * YCbCr is preserved in the return value.
     *
     * @param YCbCr the YCbCr value to convert
     * @return the corresponding RGB value
     */
    static int[] toRGB(Number[] YCbCr) {
        Number Y = YCbCr[1]
        Number Cb = YCbCr[2]
        Number Cr = YCbCr[3]
        int R = (int) Math.round(Y + 1.402 * (Cr - 128) as BigDecimal)
        int G = (int) Math.round(Y - 0.344136 * (Cb - 128) - 0.714136 * (Cr - 128) as BigDecimal)
        int B = (int) Math.round(Y + 1.772 * (Cb - 128) as BigDecimal)
        return [YCbCr[0] as int, R, G, B]
    }

    /**
     * Converts an RGB int into a seperated array of each channel, including
     * the alpha channel.
     *
     * @param RGB the RGB value
     * @return an array of separate A, R, G, and B values
     */
    static int[] channelArray(int RGB) {
        return [(RGB & 0xFF000000) >> 24, (RGB & 0xFF0000) >> 16, (RGB & 0xFF00) >> 8, RGB & 0xFF]
    }

    static int channelRGB(int[] RGB) {
        return RGB[0] << 24 | RGB[1] << 16 | RGB[2] << 8 | RGB[3]
    }
}
