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

    static JPEGImage encodeDCT(JPEGImage image, byte[] data) {
        int width = image.width + Math.floorMod(8 - image.width, 8)
        int height = image.height + Math.floorMod(8 - image.height, 8)

        JPEGImage encodedImage = ImageUtils.deepCopy(image)

        for (int i = 0, bit = 0; bit < data.length * 8; ++i) {
            int x = i % width
            int y = i.intdiv(width)

            // Avoids encoding the direct current
            if (x % 8 == 0 || y % 8 == 0 || encodedImage.getY(x, y) in (-1..1)) {
                continue
            }

            int value = (data[bit.intdiv(8)] >> (7 - bit % 8)) & 1
            encodedImage.setY(x, y, (encodedImage.getY(x, y) & ~1) | value)
            bit += 1
        }
        return encodedImage
    }
}