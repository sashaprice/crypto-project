import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class JPEGImage {
    private static final int[][] Q50_MATRIX = {{16, 11, 10, 16, 24,  40,  51,  61},
                                               {12, 12, 14, 19, 26,  58,  60,  55},
                                               {14, 13, 16, 24, 40,  57,  69,  56},
                                               {14, 17, 22, 29, 51,  87,  80,  62},
                                               {18, 22, 37, 56, 68,  109, 103, 77},
                                               {24, 35, 55, 64, 81,  104, 113, 92},
                                               {49, 64, 78, 87, 103, 121, 120, 101},
                                               {72, 92, 95, 98, 112, 110, 103, 99}};

    private int[][] alphaChannel;
    private int[][] yChannel;
    private int[][] cBChannel;
    private int[][] cRChannel;

    private int width;
    private int height;

    public JPEGImage(BufferedImage image) {
        double[][][] yCbCrData = new double[image.getHeight()][image.getWidth()][];
        for (int x = 0; x < image.getWidth(); ++x) {
            for (int y = 0; y < image.getHeight(); ++y) {
                yCbCrData[y][x] = ImageUtils.toYCbCr(ImageUtils.channelArray(image.getRGB(x, y)));
            }
        }
        read(yCbCrData);
    }

    /**
     * Reads data and converts into the correct values by compressing
     * the y, cB, and cR channels. The chrominance channels are also
     * downsampled to save space.
     *
     * @param data the data of the image
     */
    private void read(double[][][] data) {
        // Verifies consistency of row lengths
        for (int i = 1; i < data.length; ++i) {
            if (data[i].length != data[i - 1].length) {
                throw new IllegalArgumentException("rows of data must be of same length");
            }
        }

        width = data[0].length;
        height = data.length;
        // Extends width/height to the lowest multiple of 8 greater than width/height
        int extWidth = width + Math.floorMod(8 - width, 8);
        int extHeight = height + Math.floorMod(8 - height, 8);

        alphaChannel = new int[extHeight][extWidth];
        double[][] yPlane = new double[extHeight][extWidth];
        double[][] cBPlane = new double[extHeight][extWidth];
        double[][] cRPlane = new double[extHeight][extWidth];

        // Fills in channels with values for data or values for a black pixel in yCbCr
        for (int x = 0; x < extHeight; ++x) {
            for (int y = 0; y < extWidth; ++y) {
                if (x < height && y < width) {
                    alphaChannel[x][y] = Math.min((int) data[x][y][0], 255);
                    yPlane[x][y] = data[x][y][1];
                    cBPlane[x][y] = data[x][y][2];
                    cRPlane[x][y] = data[x][y][3];
                }
                else {
                    // Default values if x and y are not in bounds of data
                    alphaChannel[x][y] = 0xFF;
                    yPlane[x][y] = 0x10;
                    cBPlane[x][y] = 0x80;
                    cRPlane[x][y] = 0x80;
                }
            }
        }

        // Downsamples chrominance channels and compresses both chrominance and luminance
        yChannel = compress(yPlane);
        cBChannel = compress(downsample(cBPlane));
        cRChannel = compress(downsample(cRPlane));
    }

    /**
     * Reduces the dimensions of an N x N matrix into an
     * N/2 x N/2 matrix. Data is reduced to 2x2 blocks, and
     * the average value of the blocks becomes the new
     * pixel value.
     *
     * @param data the data of an image to downsample
     * @return a matrix of only half the original dimensions
     */
    private static double[][] downsample(double[][] data) {
        double[][] plane = new double[data.length / 2][data[0].length / 2];
        for (int i = 0; i < data.length; i += 2) {
            for (int j = 0; j < data[i].length; j += 2) {
                plane[i / 2][j / 2] = (data[i][j] + data[i + 1][j] + data[i][j + 1] + data[i + 1][j + 1]) / 4;
            }
        }
        return plane;
    }

    /**
     * Increases the dimensions of an N x N matrix into a
     * 2N x 2N matrix. Individual elements are converted into
     * 2x2 blocks of the same element value.
     *
     * @param channel the channel to upsample
     * @return a matrix of twice the original dimensions
     */
    private static double[][] upsample(double[][] channel) {
        double[][] plane = new double[channel.length * 2][channel[0].length * 2];
        for (int i = 0; i < plane.length; ++i) {
            for (int j = 0; j < plane[i].length; ++j) {
                plane[i][j] = channel[i / 2][j / 2];
            }
        }
        return plane;
    }

    /**
     * Compresses a matrix of values by the JPEG compression algorithm.
     * The matrix is converted into blocks of 8x8 elements; each block
     * subtracts 128 from the values to center the data around 0 and then
     * is transformed by the DCT algorithm.
     *
     * The resulting matrix is then divided a set of predefined values
     * (see {@link JPEGImage#Q50_MATRIX}) and rounded to become the
     * quantized, compressed data. JPEG compresses further, but this
     * is not necessary for the project.
     *
     * @param data the data to compress
     * @return a matrix of 8x8 blocks of compressed data
     */
    private static int[][] compress(double[][] data) {
        int[][] channel = new int[data.length][data[0].length];
        for (int x = 0; x < data.length; x += 8) {
            for (int y = 0; y < data[x].length; y += 8) {
                double[][] block = new double[8][8];
                for (int i = 0; i < 8; ++i) {
                    for (int j = 0; j < 8; ++j) {
                        block[i][j] = data[x + i][y + j] - 128;
                    }
                }
                block = MathUtils.DCT(block);
                for (int i = 0; i < 8; ++i) {
                    for (int j = 0; j < 8; ++j) {
                        channel[x + i][y + j] = (int) Math.round(block[i][j] / Q50_MATRIX[i][j]);
                    }
                }
            }
        }
        return channel;
    }

    /**
     * Decompresses an image encoded in the JPEG compression format. A matrix
     * is divided up into 8x8 blocks, and each block is multiplied by a
     * corresponding element in {@link JPEGImage#Q50_MATRIX}. A DCT is applied
     * to this block, and the resulting value is then shifted by 128 to return
     * the values to the interval [0, 255].
     *
     * @param channel the channel to decompress
     * @return a decompressed matrix of the original data.
     */
    private static double[][] decompress(int[][] channel) {
        double[][] data = new double[channel.length][channel[0].length];
        for (int x = 0; x < channel.length; x += 8) {
            for (int y = 0; y < channel[x].length; y += 8) {
                double[][] block = new double[8][8];
                for (int i = 0; i < 8; ++i) {
                    for (int j = 0; j < 8; ++j) {
                        block[i][j] = channel[x + i][y + j] * Q50_MATRIX[i][j];
                    }
                }
                block = MathUtils.IDCT(block);
                for (int i = 0; i < 8; ++i) {
                    for (int j = 0; j < 8; ++j) {
                        data[x + i][y + j] = block[i][j] + 128;
                    }
                }
            }
        }
        return data;
    }

    /**
     * Converts a compressed JPEG image into the yCbCr color space which is
     * then translated into ARGB and returned as a BufferedImage.
     *
     * @return an ARGB-encoded BufferedImage
     */
    public BufferedImage toBufferedImage() {
        double[][] yData = decompress(yChannel);
        double[][] cBData = upsample(decompress(cBChannel));
        double[][] cRData = upsample(decompress(cRChannel));

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                double[] aYCbCr = {alphaChannel[y][x], yData[y][x], cBData[y][x], cRData[y][x]};
                int aRGB = ImageUtils.channelInt(ImageUtils.toRGB(aYCbCr));
                image.setRGB(x, y, aRGB);
            }
        }
        return image;
    }

    public int getAlpha(int x, int y) {
        return alphaChannel[y][x];
    }

    public int getY(int x, int y) {
        return yChannel[y][x];
    }

    public int getCb(int x, int y) {
        return cBChannel[y][x];
    }

    public int getCr(int x, int y) {
        return cRChannel[y][x];
    }

    public void setAlpha(int x, int y, int value) {
        alphaChannel[y][x] = Math.min(Math.max(0, value), 255);
    }

    public void setY(int x, int y, int value) {
        yChannel[y][x] = Math.min(Math.max(0, value), 255);
    }

    public void setCb(int x, int y, int value) {
        cBChannel[y][x] = Math.min(Math.max(0, value), 255);
    }

    public void setCr(int x, int y, int value) {
        cRChannel[y][x] = Math.min(Math.max(0, value), 255);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}