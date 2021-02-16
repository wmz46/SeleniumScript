package com.iceolive.selenium;


import org.openqa.selenium.chrome.ChromeDriver;

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

    public static void main(String[] args) {
        String script = "";
        for (int i = 0; args != null && i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-s")) {
                script = args[++i];
            } else if (arg.equals("-script")) {
                script = args[++i];
            }
        }
        ChromeWebDriver webDriver = new ChromeWebDriver(System.getProperty("user.dir") + "\\chromedriver.exe");
        webDriver.addWebDriverCloseEvent(()->{
           webDriver.quit();
        });
        try {
            webDriver.runFromFile(script);
            //打印所有的变量
            System.out.println(webDriver.getVariableMap());
            System.out.println("执行完毕");
        } catch (Exception e) {
            System.out.println("执行出错:" + e.toString());
        }
    }


}
