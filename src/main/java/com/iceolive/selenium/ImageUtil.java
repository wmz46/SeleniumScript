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
        ImageIO.write(grayImage, "jpg", newFile);
    }

    public static boolean isDarkColor(Color color){
        int grayLevel = (int) (color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114);
        if(grayLevel>=192){
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
        ImageIO.write(grayImage, "jpg", newFile);
    }


    public static void main(String[] args) throws IOException {
        String filepath1 = System.getProperty("user.dir") + "/1.png";
        String filepath2 = System.getProperty("user.dir") + "/2.png";
        String distPath2 = System.getProperty("user.dir") + "/2.jpg";
        ImageUtil.binaryImage(filepath2, distPath2);
        BufferedImage image = ImageIO.read(new File(distPath2));
        int top =image.getHeight();
        int bottom = 0;
        int left = image.getWidth();
        int right = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                Color c=new Color(rgb);
                if(isDarkColor(c)){
                    if(x<left){
                        left = x;
                    }
                    if(x>right){
                        right = x;
                    }
                    if(y<top){
                        top = y;
                    }
                    if(y>bottom){
                        bottom = y;
                    }
                }
            }
        }
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if(x)
            }
        }
        System.out.println(0+","+image.getHeight()+","+0+","+image.getWidth());
        System.out.println(top+","+bottom+","+left+","+right);
    }

}