import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.WritableRaster
import java.security.SecureRandom

class Steganography {
    private static final String PROJECT_PATH = new File("").getAbsolutePath()
    private static final int DCT_BLOCK_SIZE = 8

    static void main(String[] args) {
        String inputPath = PROJECT_PATH + "\\data\\input\\nature-7.png"
        String outputPath = PROJECT_PATH + "\\data\\output\\nature-7-encoded.png"

        BufferedImage image = ImageIO.read(new File(inputPath))
        // TODO For DCT steganography, see Jsteg
    }

    /**
     * Encodes an image using the LSB algorithm. The
     * indeces are selected from a pseudo-random number generator so that the encoded bits are
     * distributed randomly throughout the pixels. However, the continuity is guaranteed by
     * using the same seed for the generator.
     *
     * @param image the image to encode the data into
     * @param data the data to encode
     * @param seed the seed of the PRNG
     * @return an image encoded with the data
     */
    static BufferedImage encodeLSB(BufferedImage image, byte[] data, long seed) {
        int height = image.height
        int width = image.width
        assert height * width >= (4 * data.length / 3)
        BufferedImage encryptedImage = deepCopy(image)

        // SecureRandom is used to keep the selection pattern undetectable
        SecureRandom random = new SecureRandom(toBytes(seed))

        // Used arrays keep track of pixels that have already been encoded for each color (i.e. R, G, and B)
        List<Integer>[] usedX = new List<Integer>[] {new ArrayList<>(0..width - 1), new ArrayList<>(0..width - 1), new ArrayList<>(0..width - 1)}
        List<Integer>[] usedY = new List<Integer>[] {new ArrayList<>(0..height - 1), new ArrayList<>(0..height - 1), new ArrayList<>(0..height - 1)}
        for (int i = 0; i < data.length * 4; ++i) {
            int x = usedX[i % 3].remove(random.nextInt(usedX[i % 3].size()))
            int y = usedY[i % 3].remove(random.nextInt(usedY[i % 3].size()))
            int RGB = encryptedImage.getRGB(x, y)

            // Takes a pair of bits from a byte in the data and shifts them to the right end
            byte pair = (byte) ((data[i.intdiv(4)] & 0b11 << 6 - 2 * (i % 4)) >> 2 * (3 - (i % 4)))

            // Creates a 2-bit-long hole in the RGB value and fills it with the pair of bits
            int encodedRGB = RGB & ~(0b11 << 8 * (2 - (i % 3))) | pair << (8 * (2 - (i % 3)))
            encryptedImage.setRGB(x, y, encodedRGB)
        }
        return encryptedImage
    }

    /**
     * Decodes bits in the image via the LSB algorithm. Pixels
     * are randomly selected based on a given seed, and the pairs of bits are accumulated into a number
     * of bytes equal to the given length.
     *
     * @param image the image to decode the data from
     * @param length the number of bytes to decode
     * @param seed the seed of the PRNG
     * @return the decoded data
     */
    static byte[] decodeLSB(BufferedImage image, long length, long seed) {
        int height = image.height
        int width = image.width
        assert height * width >= (4 * length / 3)
        byte[] data = new byte[length]

        // SecureRandom is used to keep the selection pattern undetectable
        SecureRandom random = new SecureRandom(toBytes(seed))

        // Used arrays keep track of pixels that have already been encoded for each color (i.e. R, G, and B)
        List<Integer>[] usedX = new List<Integer>[] {new ArrayList<>(0..width - 1), new ArrayList<>(0..width - 1), new ArrayList<>(0..width - 1)}
        List<Integer>[] usedY = new List<Integer>[] {new ArrayList<>(0..height - 1), new ArrayList<>(0..height - 1), new ArrayList<>(0..height - 1)}
        for (int i = 0; i < length * 4; ++i) {
            int x = usedX[i % 3].remove(random.nextInt(usedX[i % 3].size()))
            int y = usedY[i % 3].remove(random.nextInt(usedY[i % 3].size()))
            int RGB = image.getRGB(x, y)

            // Gets the pair of bytes from one of the color channels and shifts it to the right
            byte pair = (byte) (0b11 & RGB >> 8 * (2 - (i % 3)))

            // Places the pair of bytes in the correct position in the data
            data[i.intdiv(4)] |= (byte) (pair << 2 * (3 - (i % 4)))
        }
        return data
    }

    /**
     * Converts a long value into an array of bytes. In Java, longs are 8 bytes, so the
     * resulting array should have exactly 8 elements.
     *
     * @param value the long value to convert to bytes
     * @return an array of bytes from the long value
     */
    private static byte[] toBytes(long value) {
        final int LONG_LENGTH = 8
        final int BYTE_LENGTH = 8
        byte[] result = new byte[LONG_LENGTH]
        for (int i = 0; i < LONG_LENGTH; ++i) {
            result[i] = (byte) (value & 0xFF)
            value >>= BYTE_LENGTH
        }
        return result
    }

    /**
     * Creates a deep copy of a buffered image suitable for transformation. This is used
     * to avoid modifying the original image when creating an enciphered image.
     *
     * @param image the image to copy
     * @return a deep copy of the image
     */
    private static BufferedImage deepCopy(BufferedImage image) {
        ColorModel model = image.getColorModel()
        WritableRaster raster = image.copyData(image.getRaster().createCompatibleWritableRaster())
        return new BufferedImage(model, raster, model.isAlphaPremultiplied(), null)
    }

    /**
     * Converts an RGB value into a YCbCr value. Conversion is done by applying a Matrix transformation
     * onto an RGB vector. The YCbCr color space separates the luminance (lightness/darkness) from the
     * chrominance (color) of a pixel; this makes the DCT transformation of an image in this color space
     * possible.
     * <a href="https://en.wikipedia.org/wiki/YCbCr#ITU-R_BT.709_conversion#JPEG_conversion">Source</a>
     * @param RGB the encoded RGB value as an int
     * @return an array of Number s representing the YCbCr encoding of the RGB value
     */
    private static Number[] getYCbCr(int RGB) {
        Color color = new Color(RGB)
        int red = color.getRed()
        int green = color.getGreen()
        int blue = color.getBlue()
        int Y = (int) (0.299 * red + 0.587 * green + 0.114 * blue)
        int Cb = (int) (128 - 0.168736 * red - 0.331264 * green + 0.5 * blue)
        int Cr = (int) (128 + 0.5 * red - 0.418688 * green - 0.081312 * blue)
        return Y << 16 | Cb << 8 | Cr
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
                Number ci = (i == 0 ? 1 : Math.sqrt(2)) / Math.sqrt(DCT_BLOCK_SIZE)
                Number cj = (j == 0 ? 1 : Math.sqrt(2)) / Math.sqrt(DCT_BLOCK_SIZE)
                
                Number sum = 0
                for (int k = 0; k < DCT_BLOCK_SIZE; ++k) {
                    for (int l = 0; l < DCT_BLOCK_SIZE; ++l) {
                        sum += matrix[k][l]
                            * Math.cos((2 * k + 1) * i * Math.PI / (2 * DCT_BLOCK_SIZE))
                            * Math.cos((2 * l + 1) * j * Math.PI / (2 * DCT_BLOCK_SIZE))
                    }
                }
                dct[i][j] = ci * cj * sum
            }
        }
        return dct
    }
}
