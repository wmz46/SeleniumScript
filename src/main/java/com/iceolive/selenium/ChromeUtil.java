package com.iceolive.selenium;


import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author wangmianzhe
 */
public class ChromeUtil {
    private static OkHttpClient client = new OkHttpClient();

    private final static String chromeDriverDomain = "http://chromedriver.storage.googleapis.com";
    private final static String chromeDriverDomain2 = "https://npm.taobao.org/mirrors/chromedriver";

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

    public static String getVersion() {
        String version = RegUtil.getValue("\"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Google Chrome\"", "DisplayVersion");
        return version;
    }

    public static void unzip(File file, String destDirPath) throws IOException {

        System.out.println("开始解压文件...");
        ZipFile zipFile = new ZipFile(file);
        Enumeration<?> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();


            // 如果是文件夹，就创建个文件夹

            if (entry.isDirectory()) {
                String dirPath = destDirPath + "/" + entry.getName();

                File dir = new File(dirPath);

                dir.mkdirs();

            } else {
                // 如果是文件，就先创建一个文件，然后用io流把内容copy过去

                File targetFile = new File(destDirPath + "/" + entry.getName());

                // 保证这个文件的父文件夹必须要存在

                if (!targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdirs();

                }

                targetFile.createNewFile();

                // 将压缩文件内容写入到这个文件中

                InputStream is = zipFile.getInputStream(entry);

                FileOutputStream fos = new FileOutputStream(targetFile);

                int len;

                byte[] buf = new byte[1024];

                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);

                }

                // 关流顺序，先打开的后关闭

                fos.close();

                is.close();

            }

        }
    }

    private static void downloadAndUnzip() throws IOException {
        String domain = chromeDriverDomain2;
        String version = getVersion();
        if (version == null) {
            System.out.println("请先安装chrome浏览器");
        }
        version = version.split("\\.")[0];
        Request request;
        Response response;
        try {
            String url = domain + "/LATEST_RELEASE_" + version;
            request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            response = client.newCall(request).execute();
        } catch (Exception e) {
            domain = chromeDriverDomain;
            String url = domain + "/LATEST_RELEASE_" + version;
            request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            response = client.newCall(request).execute();

        }
        String fullVersion = response.body().string();
        String downloadUrl = domain + "/" + fullVersion + "/chromedriver_win32.zip";
        System.out.println("开始下载chromedriver...");
        request = new Request.Builder().url(downloadUrl).build();
        response = client.newCall(request).execute();
        InputStream is;
        is = response.body().byteStream();
        FileOutputStream fos = null;
        String downloadPath = System.getProperty("user.dir") + "\\chromedriver_win32.zip";
        fos = new FileOutputStream(downloadPath);
        int len;
        byte[] bytes = new byte[4096];
        while ((len = is.read(bytes)) != -1) {
            fos.write(bytes, 0, len);
        }
        fos.flush();
        is.close();
        fos.close();

        unzip(new File(downloadPath), System.getProperty("user.dir"));

    }

    public static void main(String[] args) throws IOException {
        String script = "";
        String driver = System.getProperty("user.dir") + "\\chromedriver.exe";
        if (!new File(driver).exists()) {
            downloadAndUnzip();
        }
        String proxy = null;
        boolean isServer = false;
        int port = 10042;
        for (int i = 0; args != null && i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-s")) {
                script = args[++i];
            } else if (arg.equals("-script")) {
                script = args[++i];
            } else if (arg.equals("-driver")) {
                driver = args[++i];
            } else if (arg.equals("-proxy")) {
                proxy = args[++i];
            } else if (arg.equals("-ws")) {
                isServer = true;
            } else if (arg.equals("-p")) {
                port = Integer.parseInt(args[++i]);
            }
        }
        if (isServer) {
            ChromeServer chromeServer = new ChromeServer(port);
            chromeServer.start();
        } else {
            String s = FileUtil.readFromFile(script, "utf-8");
            runScript(s, driver, proxy);
        }
    }

    public static void runScript(String script, String driver, String proxy) throws IOException {
        boolean enableMob = Arrays.stream(script.split("\n")).filter(m -> m.trim().startsWith("newHar")).count() > 0;

        String finalProxy = proxy;
        BrowserMobProxy browserMobProxy = new BrowserMobProxyServer();
        if (proxy != null) {
            InetSocketAddress address = new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]));
            browserMobProxy.setChainedProxy(address);
        }
        if(enableMob) {
            browserMobProxy.start(0);
        }
        ChromeWebDriver webDriver;
        try {
            if (enableMob) {
                webDriver = new ChromeWebDriver(driver, browserMobProxy);
            } else {
                webDriver = new ChromeWebDriver(driver);
            }
        } catch (Exception e) {
            if (e.getMessage().contains("only supports Chrome version")) {
                System.out.println("chromedriver版本不匹配！");
                downloadAndUnzip();
                if (enableMob) {
                    webDriver = new ChromeWebDriver(driver, browserMobProxy);
                } else {
                    webDriver = new ChromeWebDriver(driver);
                }
            } else {
                throw e;
            }
        }
        ChromeWebDriver finalWebDriver = webDriver;
        webDriver.addWebDriverCloseEvent(() -> {
            if (finalProxy != null) {
                browserMobProxy.stop();
            }
            finalWebDriver.quit();
        });
        try {
            webDriver.run(script);
            //打印所有的变量
            System.out.println(webDriver.getVariableMap());
            System.out.println("执行完毕");
        } catch (Exception e) {
            System.out.println("执行出错:" + e.toString());
        }
    }


}
