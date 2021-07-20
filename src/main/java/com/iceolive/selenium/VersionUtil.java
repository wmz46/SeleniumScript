package com.iceolive.selenium;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author wangmianzhe
 */
public class VersionUtil {
    private static String version = null;

    public static String getVersion() {
        if (version == null) {
            try {
                Properties properties = new Properties();
                // 使用ClassLoader加载properties配置文件生成对应的输入流
                InputStream in = VersionUtil.class.getClassLoader().getResourceAsStream("application.properties");
                // 使用properties对象加载输入流
                properties.load(in);
                //获取key对应的value值
                version = properties.getProperty("version");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return version;
    }
}
