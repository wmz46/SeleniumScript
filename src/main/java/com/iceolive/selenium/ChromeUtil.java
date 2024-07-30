package com.iceolive.selenium;


import lombok.extern.slf4j.Slf4j;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author wangmianzhe
 */
@Slf4j
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
        if (version != null) {
            return version;
        }
        version = RegUtil.getValue("\"HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Google Chrome\"", "DisplayVersion");
        return version;
    }

    public static void unzip(File file, String destDirPath, String ignorePath) throws IOException {

        log.info("开始解压文件...");
        ZipFile zipFile = new ZipFile(file);
        Enumeration<?> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String name = entry.getName();
            if (name.startsWith(ignorePath)) {
                name = name.substring(ignorePath.length());
            }

            // 如果是文件夹，就创建个文件夹

            if (entry.isDirectory()) {
                String dirPath = destDirPath + "/" + name;

                File dir = new File(dirPath);

                dir.mkdirs();

            } else {
                // 如果是文件，就先创建一个文件，然后用io流把内容copy过去

                File targetFile = new File(destDirPath + "/" + name);

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
            log.error("请先安装chrome浏览器");
        }
        version = version.split("\\.")[0];

        Request request;
        Response response;
        String downloadUrl;
        if (Integer.parseInt(version) <= 114) {
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
            downloadUrl = domain + "/" + fullVersion + "/chromedriver_win32.zip";
        } else {
            String url = "https://googlechromelabs.github.io/chrome-for-testing/LATEST_RELEASE_" + version;
            request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            response = client.newCall(request).execute();
            String fullVersion = response.body().string();
            downloadUrl = "https://storage.googleapis.com/chrome-for-testing-public" + "/" + fullVersion + "/win32/chromedriver-win32.zip";
        }

        log.info("开始下载chromedriver...");
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
        if (Integer.parseInt(version) <= 114) {
            unzip(new File(downloadPath), System.getProperty("user.dir"),"");
        }else{
            unzip(new File(downloadPath), System.getProperty("user.dir"),"chromedriver-win32/");
        }


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
            log.info("当前版本：" + VersionUtil.getVersion());
            String s = FileUtil.readFromFile(script, "utf-8");
            runScript(s, driver, proxy);
        }
    }

    public static void runScript(String script, String driver, String proxy) throws IOException {
        boolean enableMob = Arrays.stream(script.split("\n")).filter(m -> m.trim().startsWith("newHar")).count() > 0;
        boolean headless = Arrays.stream(script.split("\n")).filter(m -> m.trim().startsWith("#headless")).count() > 0;

        BrowserMobProxy browserMobProxy = new BrowserMobProxyServer();
        if (StringUtils.isNotEmpty(proxy)) {
            if (proxy.contains(":")) {
                InetSocketAddress address = new InetSocketAddress(proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]));
                browserMobProxy.setChainedProxy(address);
                enableMob = true;
            }
        }
        if (enableMob) {
            browserMobProxy.start(0);
        }
        ChromeWebDriver webDriver;
        try {
            if (enableMob) {
                webDriver = new ChromeWebDriver(driver, headless, browserMobProxy);
            } else {
                webDriver = new ChromeWebDriver(driver, headless);
            }
        } catch (Exception e) {
            if (e.getMessage().contains("only supports Chrome version")) {
                log.error("chromedriver版本不匹配！");
                downloadAndUnzip();
                if (enableMob) {
                    webDriver = new ChromeWebDriver(driver, headless, browserMobProxy);
                } else {
                    webDriver = new ChromeWebDriver(driver, headless);
                }
            } else {
                throw e;
            }
        }
        ChromeWebDriver finalWebDriver = webDriver;
        boolean finalEnableMob = enableMob;
        webDriver.addWebDriverCloseEvent(() -> {
            if (finalEnableMob) {
                browserMobProxy.stop();
            }
            finalWebDriver.quit();
        });
        try {
            webDriver.run(script);
            //打印所有的变量
//            System.out.println(webDriver.getVariableMap());
            log.info("执行完毕");

        } catch (Exception e) {
            log.error("执行出错:" + e.toString());
        }
        if (headless) {
            //无浏览器窗口模式执行完毕需要关闭对应的webDriver
            if (enableMob) {
                browserMobProxy.stop();
            }
            finalWebDriver.close();
            finalWebDriver.quit();
        }
    }


}
