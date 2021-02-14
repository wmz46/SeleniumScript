package com.iceolive.selenium;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author wangmianzhe
 */
@Slf4j
public class ChromeUtil {


    /**
     * 关闭所有chromedriver进程
     */
    public static void killChromeDriver()  {
        try {
            Runtime.getRuntime().exec("taskkill /F  /FI \"IMAGENAME eq chromedriver.exe\"");
        }catch (IOException e){
            log.error(e.toString(),e);
        }

    }


}
