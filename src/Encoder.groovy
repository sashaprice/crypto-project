import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix

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
        int[][] M = [[26, -5, -5, -5, -5, -5, -5, 8],
        [64, 52, 8, 26, 26, 26, 8, -18],
        [126, 70, 26, 26, 52, 26, -5, -5],
        [111, 52, 8, 52, 52, 38, -5, -5],
        [52, 26, 8, 39, 38, 21, 8, 8],
        [0, 8, -5, 8, 26, 52, 70, 26],
        [-5, -23, -18, 21, 8, 8, 52, 38],
        [-18, 8, -5, -5, -5, 8, 26, 8]]
    }

    static BufferedImage encodeDCT(BufferedImage image, byte[] data) {
        BufferedImage encodedImage = ImageUtils.deepCopy(image)
        int width = encodedImage.width
        int height = encodedImage.height

        // First component: luminance
        for (int x = 0; x < width; x += 8) {
            for (int y = 0; y < height; y += 8) {
                int[][] block = getBlock(image, x, y)
                for (int i = 0; i < width; ++i) {
                    for (int j = 0; j < height; ++j) {
                        block[i][j] = (block[i][j] >> 16) & 0xFF
                    }
                }
                int[][] C = quantizeBlock(block)
            }
        }
        return encodedImage
    }

    private static int[][] quantizeBlock(int[][] block) {
        Number[][] D = MathUtils.discreteCosineTransform(block)
        int[][] C = new int[8][8]
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                C[i][j] = Math.round(D[i][j] / MathUtils.Q50_MATRIX[i][j]) as int
            }
        }
        return C
    }

    private static int[][] getBlock(BufferedImage image, int i, int j) {
        int[][] pixels = new int[8][8]
        for (int x = i; x < i + 8; ++x) {
            for (int y = j; y < j + 8; ++y) {
                int RGB = x < image.width && y < image.height ? image.getRGB(x, y) : 0xFF000000
                pixels[x][y] = ImageUtils.toYCbCr(RGB) - 128
            }
        }
        return pixels
    }
}
