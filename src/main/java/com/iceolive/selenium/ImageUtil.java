package com.iceolive.selenium;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author wangmianzhe
 */
public class ImageUtil {

    public static Integer getDistance(String imgPath1, String imgPath2) {
        try {
            BufferedImage img1 = ImageIO.read(new File(imgPath1));
            BufferedImage img2 = ImageIO.read(new File(imgPath2));
            double match = 0.8;
            Integer distance = null;
            while (distance == null) {
                distance = getDistance(img1, img2, match);
                match -= 0.01;
            }
            return distance;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取两张图片的距离
     *
     * @param imgPath1
     * @param imgPath2
     * @param match
     * @return
     */
    public static Integer getDistance(String imgPath1, String imgPath2, double match) {
        try {
            BufferedImage img1 = ImageIO.read(new File(imgPath1));
            BufferedImage img2 = ImageIO.read(new File(imgPath2));
            return getDistance(img1, img2, match);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取两张图片的距离
     *
     * @param img1
     * @param img2
     * @param match
     * @return
     */
    private static Integer getDistance(BufferedImage img1, BufferedImage img2, double match) {
        Rectangle rect = getRectangle(img1);
        BufferedImage image1 = grayImage(img1);
        BufferedImage image2 = grayImage(img2);
        int[] signature = getSignature(image1, rect);
        for (int i = rect.x; i < image2.getWidth() - rect.width; i++) {
            Rectangle rect2 = new Rectangle(i, rect.y, rect.width, rect.height);
            int[] signature2 = getSignature(image2, rect2);
            double signatureMatch = getSignatureMatch(signature, signature2);
            if (signatureMatch > match) {
                return i - rect.x;
            }

        }
        return null;
    }

    /**
     * 是否深色
     *
     * @param color
     * @return
     */
    private static boolean isDarkColor(Color color) {
        int grayLevel = (int) (color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114);
        if (grayLevel >= 192) {
            return false;
        }
        return true;
    }

    /**
     * 灰度化
     *
     * @param image
     * @return
     */
    private static BufferedImage grayImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                grayImage.setRGB(x, y, rgb);
            }
        }
        return grayImage;
    }


    /**
     * 二值化
     *
     * @param image
     * @return
     */
    private static BufferedImage binaryImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                grayImage.setRGB(x, y, rgb);
            }
        }
        return grayImage;
    }

    /**
     * 获取图片区域，即排除白色背景的区域，宽高都比实际大小缩小2/5，排除边缘色差过大的区域。
     *
     * @param image
     * @return
     */
    private static Rectangle getRectangle(BufferedImage image) {
        int top = image.getHeight();
        int bottom = 0;
        int left = image.getWidth();
        int right = 0;

        BufferedImage binaryImage = binaryImage(image);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = binaryImage.getRGB(x, y);
                Color c = new Color(rgb);
                if (isDarkColor(c)) {
                    if (x < left) {
                        left = x;
                    }
                    if (x > right) {
                        right = x;
                    }
                    if (y < top) {
                        top = y;
                    }
                    if (y > bottom) {
                        bottom = y;
                    }
                }
            }
        }
        int padding = (bottom - top) / 5;
        top += padding;
        bottom -= padding;
        left += padding;
        right -= padding;
        Rectangle rectangle = new Rectangle(left, top, right - left, bottom - top);
        return rectangle;
    }

    /**
     * 获取图片特征码,感知哈希算法
     *
     * @param image
     * @param rect
     * @return
     */
    private static int[] getSignature(BufferedImage image, Rectangle rect) {
        int w = rect.width;
        int h = rect.height;
        int[] pixels = new int[w * h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                pixels[x * h + y] = image.getRGB(x + rect.x, y + rect.y);
            }
        }
        double avg = Arrays.stream(pixels).average().getAsDouble();
        int[] signature = Arrays.stream(pixels).map(m -> m >= avg ? 1 : 0).toArray();
        return signature;
    }

    /**
     * 获取每行像素平均值特征码
     * @param image
     * @param rect
     * @return
     */
    private static int[] getRowSignature(BufferedImage image, Rectangle rect) {
        int w = rect.width;
        int h = rect.height;
        int[] rows = new int[h];
        for (int y = 0; y < h; y++) {
            int sum = 0;
            for (int x = 0; x < w; x++) {
                sum += image.getRGB(x + rect.x, y + rect.y);
            }
            int avg =(int) ((double) sum / w);
            rows[y] = avg;
        }
        return rows;
    }
    /**
     * 获取特征码匹配度
     *
     * @param a
     * @param b
     * @return
     */
    private static double getSignatureMatch(int[] a, int[] b) {
        if (a.length != b.length) {
            return 0;
        }
        int length = a.length;
        int sameCount = 0;
        for (int i = 0; i < length; i++) {
            if (a[i] == b[i]) {
                sameCount++;
            }
        }

        return (double) sameCount / length;
    }

    public static void main(String[] args) throws IOException {
        BufferedImage image1 = ImageIO.read(new File("2.png"));
        BufferedImage image2 = ImageIO.read(new File("1.png"));
        Rectangle rect = getRectangle(image1);
        Integer distance = getDistance("2.png", "1.png", 0.71);
        for (int x = 0; x < image2.getWidth(); x++) {
            for (int y = 0; y < image2.getHeight(); y++) {
                if (image1.getRGB(x, y) != -1) {
                    image2.setRGB(x, y, image1.getRGB(x, y));
                }

            }
        }
        Graphics2D graphics = image2.createGraphics();
        graphics.setColor(Color.red);
        graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
        graphics.drawRect(rect.x + distance, rect.y, rect.width, rect.height);
        ImageIO.write(image2, "png", new File("3.png"));
    }

}