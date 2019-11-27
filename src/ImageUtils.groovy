class StegUtils {
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
    
    /**
     * Performs a discrete cosine transform on an 8x8 matrix of integer values.
     * <a href="https://www.geeksforgeeks.org/discrete-cosine-transform-algorithm-program/">Source</a>
     * @param matrix the matrix of integer values
     * @return the resulting matrix of DCT values
     */
    private static Number[][] discreteCosineTransform(int[][] matrix) {
        assert matrix.length == DCT_BLOCK_SIZE && matrix.every { it.length == DCT_BLOCK_SIZE }
        Number[][] dct = new Number[DCT_BLOCK_SIZE][DCT_BLOCK_SIZE]
        
        for (int i = 0; i < DCT_BLOCK_SIZE; ++i) {
            for (int j = 0; j < DCT_BLOCK_SIZE; ++j) {
                Number sum = 0
                for (int k = 0; k < DCT_BLOCK_SIZE; ++k) {
                    for (int l = 0; l < DCT_BLOCK_SIZE; ++l) {
                        sum += matrix[k][l]
                            * Math.cos((2 * k + 1) * i * Math.PI / (2 * DCT_BLOCK_SIZE))
                            * Math.cos((2 * l + 1) * j * Math.PI / (2 * DCT_BLOCK_SIZE))
                    }
                }
                dct[i][j] = (i == 0 ? 1 : Math.sqrt(2)) * (j == 0 ? 1 : Math.sqrt(2)) * sum / DCT_BLOCK_SIZE
            }
        }
        return dct
    }
}
