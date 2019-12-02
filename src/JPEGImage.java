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
        for (int x = 0; x < image.getHeight(); ++x) {
            for (int y = 0; y < image.getWidth(); ++y) {
                yCbCrData[x][y] = ImageUtils.toYCbCr(ImageUtils.channelArray(image.getRGB(y, x)));
            }
        }
        read(yCbCrData);
    }

    private void read(double[][][] data) {
        for (int i = 1; i < data.length; ++i) {
            if (data[i].length != data[i - 1].length) {
                throw new IllegalArgumentException("rows of data must be of same length");
            }
        }

        width = data[0].length;
        height = data.length;
        int extWidth = width + Math.floorMod(8 - width, 8);
        int extHeight = height + Math.floorMod(8 - height, 8);

        alphaChannel = new int[extHeight][extWidth];
        double[][] yPlane = new double[extHeight][extWidth];
        double[][] cBPlane = new double[extHeight][extWidth];
        double[][] cRPlane = new double[extHeight][extWidth];

        for (int x = 0; x < extHeight; ++x) {
            for (int y = 0; y < extWidth; ++y) {
                if (x < height && y < width) {
                    alphaChannel[x][y] = Math.min((int) data[x][y][0], 255);
                    yPlane[x][y] = data[x][y][1];
                    cBPlane[x][y] = data[x][y][2];
                    cRPlane[x][y] = data[x][y][3];
                }
                else {
                    alphaChannel[x][y] = 0xFF;
                    yPlane[x][y] = 0x10;
                    cBPlane[x][y] = 0x80;
                    cRPlane[x][y] = 0x80;
                }
            }
        }

        cBPlane = downsample(cBPlane);
        cRPlane = downsample(cRPlane);

        yChannel = compress(yPlane);
        cBChannel = compress(cBPlane);
        cRChannel = compress(cRPlane);
    }

    public static void main(String[] args) throws IOException {
        // TODO check that conversion from yCbCr to RGB works correctly
        String projectPath = new File("").getAbsolutePath();
        BufferedImage input = ImageIO.read(new File(projectPath + "\\data\\input\\nature-2.jpg"));
        JPEGImage image = new JPEGImage(input);
        ImageIO.write(image.toBufferedImage(), "png", new File(projectPath + "\\data\\output\\nature-2.jpg"));
    }

    private static double[][] downsample(double[][] data) {
        double[][] plane = new double[data.length / 2][data[0].length / 2];
        for (int i = 0; i < data.length; i += 2) {
            for (int j = 0; j < data[i].length; j += 2) {
                plane[i / 2][j / 2] = (data[i][j] + data[i + 1][j] + data[i][j + 1] + data[i + 1][j + 1]) / 4;
            }
        }
        return plane;
    }

    private static int[][] upsample(int[][] channel) {
        int[][] plane = new int[channel.length * 2][channel[0].length * 2];
        for (int i = 0; i < plane.length; ++i) {
            for (int j = 0; j < plane[i].length; ++j) {
                plane[i][j] = channel[i / 2][j / 2];
            }
        }
        return plane;
    }

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

    public BufferedImage toBufferedImage() {
        int[][] cBChannel = upsample(this.cBChannel);
        int[][] cRChannel = upsample(this.cRChannel);

        double[][] yData = decompress(yChannel);
        double[][] cBData = decompress(cBChannel);
        double[][] cRData = decompress(cRChannel);

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