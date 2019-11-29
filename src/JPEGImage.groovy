import java.awt.image.BufferedImage

class JPEGImage {
    private Number[][][] pixels
    private int width
    private int height

    /**
     * Takes in a matrix of YCbCr pixels (stored as individual arrays) and compresses
     * data at a quantization quality of 50.
     *
     * @param yCbCrData the data of the pixels
     */
    JPEGImage(Number[][][] yCbCrData) {
        int length = yCbCrData[0].length
        assert yCbCrData.every { it.length == length }

        // Luminance is compresed, but not chrominance
        width = length
        height = yCbCrData.length
        pixels = new Number[height][width][4]
        for (int x = 0; x < width; x += 8) {
            for (int y = 0; y < height; y += 8) {
                Number[][] block = new Number[8][8]
                for (int i = 0; i < 8; ++i) {
                    for (int j = 0; j < 8; ++j) {
                        block[i][j] = yCbCrData[x + i][y + j][1] - 128
                    }
                }
                block = MathUtils.discreteCosineTransform(block)
                for (int i = 0; i < 8; ++i) {
                    for (int j = 0; j < 8; ++j) {
                        pixels[x + i][y + j][0] = yCbCrData[x + i][y + j][0]
                        pixels[x + i][y + j][1] = Math.round(block[i][j] / MathUtils.Q50_MATRIX[i][j]) as int
                        pixels[x + i][y + j][2] = yCbCrData[x + i][y + j][2]
                        pixels[x + i][y + j][3] = yCbCrData[x + i][y + j][3]
                    }
                }
            }
        }
    }

    Number[] getAt(List<Integer> coordinate) {
        return pixels[coordinate[0]][coordinate[1]]
    }

    Number[] putAt(List<Integer> coordinate, Number[] value) {
        value.every {
            if (it < 0) {
                it = 0
            }
            else if (it > 255) {
                it = 255
            }
        }
        pixels[coordinate[0]][coordinate[1]] = value.clone()
    }

    int getWidth() {
        return width
    }

    int getHeight() {
        return height
    }

    BufferedImage toRGBImage(int w, int h) {
        assert w <= width && h <= height
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR)

        Number[][][] temp = pixels.clone()
        for (int x = 0; x < width; x += 8) {
            for (int y = 0; y < height; y += 8) {
                Number[][] block = new Number[8][8]
                for (int i = 0; i < 8; ++i) {
                    for (int j = 0; j < 8; ++j) {
                        block[i][j] = temp[x + i][y + j][1] * MathUtils.Q50_MATRIX[i][j]
                    }
                }
                // TODO verify this is correct IDCT transform
                block = MathUtils.discreteCosineTransform(block)
                for (int i = 0; i < 8; ++i) {
                    for (int j = 0; j < 8; ++j) {
                        temp[x + i][y + j][1] = block[i][j]
                    }
                }
            }
        }

        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                image.setRGB(x, y, ImageUtils.channelRGB(ImageUtils.toRGB(temp[x][y])))
            }
        }
        return image
    }
}