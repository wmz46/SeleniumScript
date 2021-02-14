package com.iceolive.selenium;

import org.junit.Test;

public class Tests {
    @Test
    public void test() {
        ChromeWebDriver webDriver = new ChromeWebDriver(System.getProperty("user.dir") + "\\webdriver\\chromedriver.exe");

        boolean result = webDriver.runFromFile(System.getProperty("user.dir") +"\\tests\\aliexpress.txt");
        if(result){
            System.out.println("执行完毕");
        }else{
            System.out.println("执行出错");
        }
        //打印所有的变量
        System.out.println(webDriver.getVariableMap());
        webDriver.close();
        webDriver.quit();
    }
}
