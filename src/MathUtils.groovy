import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix

class MathUtils {

    private static final RealMatrix DCT_BASIS
    static {
        double[][] temp = new double[8][8]
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                temp[i][j] = i == 0 ? 1 / Math.sqrt(8) : Math.cos((2 * j + 1) * i * Math.PI / 16) / 2
            }
        }
        DCT_BASIS = MatrixUtils.createRealMatrix(temp)
    }

    /**
     * Performs a discrete cosine transform on an 8x8 matrix of integer values.
     *
     * @param h the matrix of integer values
     * @return the resulting matrix of DCT values
     */
    static double[][] DCT(double[][] h) {
        assert h.length == 8 && h.every { it.length == 8 }
        return (DCT_BASIS * MatrixUtils.createRealMatrix(h) * MatrixUtils.inverse(DCT_BASIS)).getData()
    }

    static double[][] IDCT(double[][] H) {
        assert H.length == 8 && H.every { it.length == 8 }
        return (MatrixUtils.inverse(DCT_BASIS) * MatrixUtils.createRealMatrix(H) * DCT_BASIS).getData()
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