package com.iceolive.selenium;

import java.io.*;

/**
 * @author wangmianzhe
 */
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
}
