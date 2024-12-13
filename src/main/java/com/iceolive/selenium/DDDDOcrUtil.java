package com.iceolive.selenium;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import com.yl.ddddocr.OCREngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
@Slf4j
public class DDDDOcrUtil {
    private static final OCREngine ocrEngine = OCREngine.instance();

    /**
     * 获取识别的验证码
     *
     * @param base64 验证码base64字符串
     * @return
     */
    public static String getCode(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try {
            BufferedImage bufferedImage = ImageIO.read(bis);
            return ocrEngine.recognize(bufferedImage);
        } catch (IOException e) {
            log.error("识别验证码异常", e);
            return StringUtils.EMPTY;
        }
    }

    /**
     * 获取识别的验证码
     *
     * @param inputStream 验证码图片输入流
     * @return
     */
    public static String getCode(InputStream inputStream) {
        try {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            return ocrEngine.recognize(bufferedImage);
        } catch (IOException e) {
            log.error("识别验证码异常", e);
            return StringUtils.EMPTY;
        }
    }

}
