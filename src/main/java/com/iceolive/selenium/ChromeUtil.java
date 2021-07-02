package com.iceolive.selenium;


import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.net.InetSocketAddress;

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
        String driver = System.getProperty("user.dir") + "\\chromedriver.exe";
        for (int i = 0; args != null && i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-s")) {
                script = args[++i];
            } else if (arg.equals("-script")) {
                script = args[++i];
            } else if (arg.equals("-driver")) {
                driver = args[++i];

            }
        }
        BrowserMobProxy proxy = new BrowserMobProxyServer();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1",25378);
        proxy.setChainedProxy(address);
        proxy.start(0);
        ChromeWebDriver webDriver = new ChromeWebDriver(driver,proxy);
        webDriver.addWebDriverCloseEvent(() -> {
            proxy.stop();
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
