package com.iceolive.selenium;


import java.io.IOException;

/**
 * @author wangmianzhe
 */
public class ChromeUtil {


    /**
     * 关闭所有chromedriver进程
     */
    public static void killChromeDriver() {
        try {
            Runtime.getRuntime().exec("taskkill /F  /FI \"IMAGENAME eq chromedriver.exe\"");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

   

}
