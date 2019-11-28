class MathUtils {
    /**
     * Performs a discrete cosine transform on an 8x8 matrix of integer values.
     * <a href="https://www.math.cuhk.edu.hk/~lmlui/dct.pdf">Source</a>
     *
     * @param p the matrix of integer values
     * @return the resulting matrix of DCT values
     */
    private static Number[][] discreteCosineTransform(int[][] p) {
        assert p.length == 8 && p.every { it.length == 8 }
        Number[][] dct = new Number[8][8]
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                dct[i][j] = D(p, i, j)
            }
        }
        return dct
    }

    private static Number D(int[][] p, int i, int j) {
        Number sum = 0
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                sum += p[x][y]
                        * Math.cos((2 * x + 1) * i * Math.PI / 16)
                        * Math.cos((2 * y + 1) * j * Math.PI / 16)
            }
        }
        return C(i) * C(j) * sum / 4
    }

    private static Number C(int u) {
        return u == 0 ? 1 / Math.sqrt(2) : 1
    }

    /**
     * Converts a long value into an array of bytes. In Java, longs are 8 bytes, so the
     * resulting array should have exactly 8 elements.
     *
     * @param value the long value to convert to bytes
     * @return an array of bytes from the long value
     */
    static byte[] toBytes(long value) {
        final int LONG_LENGTH = 8
        final int BYTE_LENGTH = 8
        byte[] result = new byte[LONG_LENGTH]
        for (int i = 0; i < LONG_LENGTH; ++i) {
            result[i] = (byte) (value & 0xFF)
            value >>= BYTE_LENGTH
        }
        return result
    }
}
