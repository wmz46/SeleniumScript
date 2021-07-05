package com.iceolive.selenium;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author wangmianzhe
 */
public class ImageUtil {
    public static void binaryImage(String filepath, String destPath) throws IOException {
        File file = new File(filepath);
        BufferedImage image = ImageIO.read(file);

        int width = image.getWidth();
        int height = image.getHeight();
//重点，技巧在这个参数BufferedImage.TYPE_BYTE_BINARY
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int rgb = image.getRGB(i, j);
                grayImage.setRGB(i, j, rgb);
            }
        }

        File newFile = new File(destPath);
        ImageIO.write(grayImage, "png", newFile);
    }

    public static boolean isDarkColor(Color color) {
        int grayLevel = (int) (color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114);
        if (grayLevel >= 192) {
            return false;
        }
        return true;
    }

    public static void grayImage(String filepath, String destPath) throws IOException {
        File file = new File(filepath);
        BufferedImage image = ImageIO.read(file);

        int width = image.getWidth();
        int height = image.getHeight();
        //重点，技巧在这个参数BufferedImage.TYPE_BYTE_GRAY
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int rgb = image.getRGB(i, j);
                grayImage.setRGB(i, j, rgb);
            }
        }

        File newFile = new File(destPath);
        ImageIO.write(grayImage, "png", newFile);
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
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int rgb = image.getRGB(i, j);
                grayImage.setRGB(i, j, rgb);
            }
        }
        return grayImage;
    }

    private static Rectangle getRectangle(BufferedImage image) {
        int top = image.getHeight();
        int bottom = 0;
        int left = image.getWidth();
        int right = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
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

    private static Integer getDistance(BufferedImage image1, BufferedImage image2) {
        Rectangle rectangle = getRectangle(image1);

        for (int i = rectangle.x; i < image2.getWidth() - rectangle.width; i++) {
            boolean flag = true;
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                    Color color = new Color(image1.getRGB(x, y));
                    Color color1 = new Color(image2.getRGB(i + x, y));
                    double v = Math.sqrt(Math.pow(color.getRed() - color1.getRed(), 2) + Math.pow(color.getGreen() - color1.getGreen(), 2) + Math.pow(color.getBlue() - color1.getBlue(), 2));
                    if (v<25) {
                        flag = false;
                    }else{
                        flag = true;
                    }
                    if (!flag) {
                        break;
                    }
                }
                if (!flag) {
                    break;
                }
            }
            if (flag) {
                return i;
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        String filepath1 = System.getProperty("user.dir") + "/2.png";
        String filepath2 = System.getProperty("user.dir") + "/1.png";
        String filepath3 = System.getProperty("user.dir") + "/3.png";
        BufferedImage image1 = ImageIO.read(new File(filepath1));
        BufferedImage image2 = ImageIO.read(new File(filepath2));

        int distance = getDistance(image1, image2);
        System.out.println(distance);
        for (int x = distance; x < image2.getWidth(); x++) {
            for (int y = 0; y < image2.getHeight(); y++) {
                int rgb = image1.getRGB(x - distance, y);
                if (rgb != -1) {
                    image2.setRGB(x, y, rgb);
                }
            }
        }
        ImageIO.write(image2, "png", new File(filepath3));


    }

}