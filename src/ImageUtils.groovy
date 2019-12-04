import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix

import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.WritableRaster

class ImageUtils {
    private static final RealMatrix CONVERSION_MATRIX = MatrixUtils.createRealMatrix(
            [[    0.299,     0.587,     0.114],
             [-0.168736, -0.331264,       0.5],
             [      0.5, -0.418688, -0.081312]] as double[][]
    )

    private static final RealMatrix SHIFT = MatrixUtils.createRealMatrix(
            [[0], [128], [128]] as double[][]
    )

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
     * Creates a deep copy of a JPEGImage. Dimensions are maintained,
     * and copies the data for alpha, y, Cb, and Cr channels.
     *
     * @param image the image to copy
     * @return a deep copy of the image
     */
    static JPEGImage deepCopy(JPEGImage image) {
        JPEGImage copy = new JPEGImage(new double[image.height][image.width][4])
        int width = image.width + Math.floorMod(8 - image.width, 8)
        int height = image.height + Math.floorMod(8 - image.height, 8)
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                copy.setAlpha(i, j, image.getAlpha(i, j))
                copy.setY(i, j, image.getY(i, j))
            }
        }
        for (int i = 0; i < width.intdiv(2); ++i) {
            for (int j = 0; j < height.intdiv(2); ++j) {
                copy.setCb(i, j, image.getCb(i, j))
                copy.setCr(i, j, image.getCr(i, j))
            }
        }
        return copy
    }
    
    /**
     * Converts an RGB value into the YCbCr color space. The alpha channel from
     * the RGB is preserved in the return value.
     *
     * @param RGB the RGB value to convert
     * @return the corresponding YCbCr value
     */
    static double[] toYCbCr(int[] RGB) {
        double[][] input = [[RGB[1]], [RGB[2]], [RGB[3]]]
        RealMatrix output = (CONVERSION_MATRIX * MatrixUtils.createRealMatrix(input)).add(SHIFT)
        return [RGB[0]] + output.getData().collect(x -> x[0]) as double[]
    }

    /**
     * Converts a YCbCr value into the RGB color space. The alpha channel from the
     * YCbCr is preserved in the return value.
     *
     * @param YCbCr the YCbCr value to convert
     * @return the corresponding RGB value
     */
    static int[] toRGB(double[] YCbCr) {
        double[][] input = [[YCbCr[1]], [YCbCr[2]], [YCbCr[3]]]
        RealMatrix output = MatrixUtils.inverse(CONVERSION_MATRIX) * MatrixUtils.createRealMatrix(input).subtract(SHIFT)
        return [YCbCr[0] as int] + output.getData().collect(x -> Math.round(x[0])) as int[]
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

    static int channelInt(int[] RGB) {
        return RGB[0] << 24 | RGB[1] << 16 | RGB[2] << 8 | RGB[3]
    }
}