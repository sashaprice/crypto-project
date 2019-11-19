import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.WritableRaster

class Steganography {
    private static final String PROJECT_PATH = new File("").getAbsolutePath()

    static void main(String[] args) {
        // Run this to test if your configuration works properly
        byte[] data = [0b10100101 as byte]
        String path = PROJECT_PATH + "\\data\\input\\test-1.png"
        BufferedImage input = ImageIO.read(new File(path))
        BufferedImage output = encryptLSB(input, data)
    }

    /**
     * Encrypts an image with given data as an array of bytes. Encryption is performed by
     * replacing the first 2 bits of each RGB value with 2 bits from the data sequentially.
     * Bits are encoded left to right from the data into the RGB. Pixel RGB values also
     * contain an alpha channel, but this can be ignored because LSB only uses the RGB
     * values.
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
            byte pair = (byte) ((data[i.intdiv(4)] & 0b11 << 6 - 2 * (i % 4)) >> 2 * (3 - (i % 4)))
            int encodedRGB = RGB & ~(0b11 << 8 * (2 - (i % 3))) | pair << (8 * (2 - (i % 3)))
            println(Integer.toBinaryString(encodedRGB))
            encryptedImage.setRGB(x, y, encodedRGB)
        }
        return encryptedImage
    }

    private static byte[] decryptLSB(BufferedImage image, long length) {
        int height = image.height
        int width = image.width
        assert height * width >= (4 * length / 3)
        byte[] data = new byte[length]
        for (int i = 0; i < length * 4; ++i) {
            int x = (i.intdiv(3)) % width
            int y = i.intdiv(3 * width)
            int RGB = image.getRGB(x, y)
            println(Integer.toBinaryString(RGB))
            //data[i.intdiv(4)] = (byte) (data[i.intdiv(4)] << 2 | 0b11 & RGB >> 8 * (2 - (i % 3)))
        }
        return null
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