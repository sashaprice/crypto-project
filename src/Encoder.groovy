import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.security.SecureRandom

class Encoder {
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
        BufferedImage encryptedImage = ImageUtils.deepCopy(image)

        // SecureRandom is used to keep the selection pattern undetectable
        SecureRandom random = new SecureRandom(MathUtils.toBytes(seed))

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

    static void main(String[] args) {
        String path = new File("").getAbsolutePath() + "\\data\\input\\solid-1.png"
        BufferedImage image = ImageIO.read(new File(path))
        encodeDCT(image, null)

    }

    static BufferedImage encodeDCT(BufferedImage image, byte[] data) {
        int width = image.width
        int height = image.height

        // Converts image into a YCbCr color space and ensures image dimensions are multiples of 8
        Number[][][] yCbCrColorSpace = new Number[width + Math.floorMod(8 - width, 8)][height + Math.floorMod(8 - height, 8)]
        for (int i = 0; i < yCbCrColorSpace.length; ++i) {
            for (int j = 0; j < yCbCrColorSpace[i].length; ++j) {
                int[] channels
                if (i < width && j < height) {
                    channels = ImageUtils.channelArray(image.getRGB(i, j))
                }
                else {
                    channels = [0xFF, 0, 0, 0]
                }
                yCbCrColorSpace[i][j] = ImageUtils.toYCbCr(channels)
            }
        }
        JPEGImage jpegImage = new JPEGImage(yCbCrColorSpace)
        BufferedImage compressed = jpegImage.toRGBImage(width, height)
        ImageIO.write(compressed, "png", new File(new File("").getAbsolutePath() + "\\data\\output\\test.png"))
        return null
    }
}
