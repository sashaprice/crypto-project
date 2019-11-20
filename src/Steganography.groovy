import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.WritableRaster
import java.security.SecureRandom

class Steganography {
    private static final String PROJECT_PATH = new File("").getAbsolutePath()

    static void main(String[] args) {
        String inputPath = PROJECT_PATH + "\\data\\input\\solid-2.png"
        String outputPath = PROJECT_PATH + "\\data\\output\\solid.png"
        String randomOutputPath = PROJECT_PATH + "\\data\\output\\solidRandom.png"

        byte[] message = "Handwriting is a spiritual designing, even though it appears by means of a material instrument.".getBytes()
        long seed = 104839L

        BufferedImage baseImage = ImageIO.read(new File(inputPath))
        ImageIO.write(encryptLSB(baseImage, message), "png", new File(outputPath))
        ImageIO.write(encryptRLSB(baseImage, message, seed), "png", new File(randomOutputPath))

        BufferedImage encoded = ImageIO.read(new File(outputPath))
        BufferedImage encodedRandom = ImageIO.read(new File(randomOutputPath))
        assert decryptLSB(encoded, message.size()) == decryptRLSB(encodedRandom, message.size(), seed)
    }

    /**
     * Encrypts an image with given data as an array of bytes. Encryption is performed by
     * replacing the first 2 bits of each RGB value with 2 bits from the data sequentially.
     * Bits are encoded left to right from the data into the RGB. Pixel RGB values also
     * contain an channel, but it is not used in LSB.
     *
     * An RGB value is 4 bytes, formatted as such:
     *         1        2    3    4
     * [ALPHA CHANNEL][ R ][ G ][ B ]
     *
     * @param image the base image to encipher the data
     * @param data the data to encipher into the image
     * @return an image enciphered with the data
     */
    private static BufferedImage encryptLSB(BufferedImage image, byte[] data) {
        int height = image.height
        int width = image.width
        assert height * width >= (4 * data.length / 3)
        BufferedImage encryptedImage = deepCopy(image)
        for (int i = 0; i < data.length * 4; ++i) {
            int x = (i.intdiv(3)) % width
            int y = i.intdiv(3 * width)
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
     * Decodes a number of bytes equal to the given length from an image. The bytes are composed
     * of the left-most pairs of bits taken from each R, G, and B value in the image's pixels
     * starting from the pixel at 0, 0 and moving left to right and top to bottom.
     *
     * @param image the image containing the data to decode
     * @param length the number of bytes to pull from the image
     * @return the decoded data as an array of bytes
     */
    private static byte[] decryptLSB(BufferedImage image, long length) {
        int height = image.height
        int width = image.width
        assert height * width >= (4 * length / 3)
        byte[] data = new byte[length]
        for (int i = 0; i < length * 4; ++i) {
            int x = (i.intdiv(3)) % width
            int y = i.intdiv(3 * width)
            int RGB = image.getRGB(x, y)
            byte pair = (byte) (0b11 & RGB >> 8 * (2 - (i % 3)))
            data[i.intdiv(4)] = (byte) (pair << 2 * (3 - (i % 4)) | data[i.intdiv(4)])
        }
        return data
    }

    /**
     * Encodes an image using the LSB algorithm (see {@link Steganography#encryptLSB}). The
     * indeces are selected from a pseudo-random number generator so that the encoded bits are
     * distributed randomly throughout the pixels. However, the continuity is guaranteed by
     * using the same seed for the generator.
     *
     * @param image the image to encode the data into
     * @param data the data to encode
     * @param seed the seed of the PRNG
     * @return an image encoded with the data
     */
    static BufferedImage encryptRLSB(BufferedImage image, byte[] data, long seed) {
        int height = image.height
        int width = image.width
        assert height * width >= (4 * data.length / 3)
        BufferedImage encryptedImage = deepCopy(image)

        // SecureRandom is used to keep the selection pattern undetectable
        SecureRandom random = new SecureRandom(toBytes(seed))

        // Used arrays keep track of pixels that have already been encoded for each color (i.e. R, G, and B)
        Set<Integer>[] usedX = new Set<Integer>[] {new HashSet<Integer>(), new HashSet<Integer>(), new HashSet<>()}
        Set<Integer>[] usedY = new Set<Integer>[] {new HashSet<Integer>(), new HashSet<Integer>(), new HashSet<>()}
        for (int i = 0; i < data.length * 4; ++i) {
            int x = getUsableIndex(random, width, usedX[i % 3])
            int y = getUsableIndex(random, height, usedY[i % 3])
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
     * Decodes bits in the image via the LSB algorithm (see {@link Steganography#encryptLSB}). Pixels
     * are randomly selected based on a given seed, and the pairs of bits are accumulated into a number
     * of bytes equal to the given length.
     *
     * @param image the image to decode the data from
     * @param length the number of bytes to decode
     * @param seed the seed of the PRNG
     * @return the decoded data
     */
    static byte[] decryptRLSB(BufferedImage image, long length, long seed) {
        int height = image.height
        int width = image.width
        assert height * width >= (4 * length / 3)
        byte[] data = new byte[length]

        // SecureRandom is used to keep the selection pattern undetectable
        SecureRandom random = new SecureRandom(toBytes(seed))

        // Used arrays keep track of pixels that have already been encoded for each color (i.e. R, G, and B)
        Set<Integer>[] usedX = new Set<Integer>[] {new HashSet<Integer>(), new HashSet<Integer>(), new HashSet<>()}
        Set<Integer>[] usedY = new Set<Integer>[] {new HashSet<Integer>(), new HashSet<Integer>(), new HashSet<>()}
        for (int i = 0; i < length * 4; ++i) {
            int x = getUsableIndex(random, width, usedX[i % 3])
            int y = getUsableIndex(random, height, usedY[i % 3])
            int RGB = image.getRGB(x, y)

            // Gets the pair of bytes from one of the color channels and shifts it to the right
            byte pair = (byte) (0b11 & RGB >> 8 * (2 - (i % 3)))

            // Places the pair of bytes in the correct position in the data
            data[i.intdiv(4)] = (byte) (pair << 2 * (3 - (i % 4)) | data[i.intdiv(4)])
        }
        return data
    }

    /**
     * Finds an index that has not been used to encode data in an image. Once a suitable
     * index has been found, it is added to the set of used indeces.
     *
     * @param root the source of the random indeces
     * @param size the range of indeces to be generated
     * @param used the set of indeces that have already been used
     * @return an unused index within the range of the given size
     */
    private static int getUsableIndex(SecureRandom root, int size, Set<Integer> used) {
        int index = root.nextInt(size)
        while (used.contains(index)) {
            index = root.nextInt(size)
        }
        used.add(index)
        return index
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
}