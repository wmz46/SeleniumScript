package com.iceolive.selenium;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * @author wangmianzhe
 */
@Slf4j
public class FileUtil {
    public static String readFromFile(String path, String charset) {

        StringBuffer sb = new StringBuffer();
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            File file = new File(path);
            if (file.isFile() && file.exists()) {
                inputStreamReader = new InputStreamReader(new FileInputStream(file), charset);
                bufferedReader = new BufferedReader(inputStreamReader);
                String txt = null;
                while ((txt = bufferedReader.readLine()) != null) {
                    // UTF-8 byte order mark (EF BB BF)
                    if (txt.startsWith("\uFEFF")) {
                        txt = txt.substring(1);
                    }
                    sb.append(txt + "\n");
                }
                bufferedReader.close();
                inputStreamReader.close();
            } else {
                throw new RuntimeException("文件[" + path + "]不存在");

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                inputStreamReader.close();
            } catch (Exception e) {
            }
        }

        return sb.toString();
    }
    public static boolean writeToFile(String path, String text, String charset) {
        if (text == null) {
            throw new IllegalArgumentException("text不能为null！");
        }
        try {
            byte[] bytes = text.getBytes(charset);
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(path, false))) {
                out.write(bytes);
            } catch (IOException e) {
                log.error("写入文件异常", e);
                throw new RuntimeException("写入文件异常",e);
            }
            return true;
        } catch (UnsupportedEncodingException e) {
            log.error("写入文件异常", e);
            throw new RuntimeException(e);
        }
    }
}
