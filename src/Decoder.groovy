class Decoder {
    /**
     * Decodes bits in the image via the LSB algorithm. Pixels are randomly selected based on
     * a given seed, and the pairs of bits are accumulated into a number of bytes equal to the
     * given length.
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
}