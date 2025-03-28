package com.iceolive.selenium;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.JavascriptException;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author wangmianzhe
 */
@Slf4j
public class ChromeUtil {
    private static OkHttpClient client = new OkHttpClient();

    private final static String chromeDriverDomain = "http://chromedriver.storage.googleapis.com";

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


    private static boolean downloadAndUnzip() throws IOException {
        String version = getVersion();
        version = version.split("\\.")[0];
        if (version == null) {
            log.error("请先安装chrome浏览器");
            return false;
        } else {
            String downloadPath = downloadDriver(version);

            if (Integer.parseInt(version) <= 114) {
                ZipUtil.unzip(new File(downloadPath), System.getProperty("user.dir"), "");
            } else {
                ZipUtil.unzip(new File(downloadPath), System.getProperty("user.dir"), "chromedriver-win32/");
            }
            return true;
        }
    }

    public static String getFullVersion(String version) throws IOException {
        Request request;
        Response response;
        try {
            if (Integer.parseInt(version) <= 114) {
                String domain = chromeDriverDomain;
                String url = domain + "/LATEST_RELEASE_" + version;
                request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                response = client.newCall(request).execute();
                String fullVersion = response.body().string().trim();
                return fullVersion;
            } else {
                String url = "https://googlechromelabs.github.io/chrome-for-testing/LATEST_RELEASE_" + version;
                request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                response = client.newCall(request).execute();
                String fullVersion = response.body().string().trim();
                return fullVersion;
            }
        } catch (Exception e) {
            request = new Request.Builder()
                    .url("https://registry.npmmirror.com/-/binary/chrome-for-testing")
                    .get()
                    .build();
            response = client.newCall(request).execute();
            String json = response.body().string();
            String fullVersion = null;
            for (JsonNode node : JsonUtil.parse(json)) {
                String name = node.get("name").asText();
                if (name.startsWith(version + ".")) {
                    fullVersion = name.replace("/", "");
                }
            }
            return fullVersion;
        }

    }

    public static String downloadDriver(String version) throws IOException {

        Request request;
        Response response;
        String downloadUrl;
        String fullVersion = getFullVersion(version);
        log.info("开始下载chromedriver...");

        if (Integer.parseInt(version) <= 114) {
            String domain = chromeDriverDomain;
            downloadUrl = domain + "/" + fullVersion + "/chromedriver_win32.zip";
        } else {
//                downloadUrl = "https://storage.googleapis.com/chrome-for-testing-public" + "/" + fullVersion + "/win32/chromedriver-win32.zip";
            downloadUrl = "https://registry.npmmirror.com/-/binary/chrome-for-testing/" + fullVersion + "/win32/chromedriver-win32.zip";
        }
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
        return downloadPath;
    }

    public static String getDriver() throws IOException {
        String chromedriver = System.getProperty("user.dir") + "\\chromedriver.exe";
        String edgedriver = System.getProperty("user.dir") + "\\msedgedriver.exe";
        boolean isChrome = new File(chromedriver).exists();
        boolean isEdge = new File(edgedriver).exists();
        if (!isChrome && !isEdge) {
            isEdge = EdgeUtil.downloadDriver();
            if (!isEdge) {
                isChrome = downloadAndUnzip();
            }
        }
        //edge优先
        if (isEdge) {
            return edgedriver;
        }
        if (isChrome) {
            return chromedriver;
        }
        return null;
    }

    public static void main(String[] args) throws IOException {


        String script = "";
        String driver = getDriver();
        String proxy = null;
        boolean isServer = false;
        boolean host = false;
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
            } else if (arg.equals("-host")) {
                host = true;
            }

        }
        if (isServer) {
            ChromeServer chromeServer = new ChromeServer(port, host);
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
        boolean guest = Arrays.stream(script.split("\n")).filter(m -> m.trim().startsWith("#guest")).count() > 0;

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
                webDriver = new ChromeWebDriver(driver, headless, guest, browserMobProxy);
            } else {
                webDriver = new ChromeWebDriver(driver, headless, guest);
            }
        } catch (Exception e) {
            if (e.getMessage().contains("only supports Chrome version")) {
                log.error("chromedriver版本不匹配！");
                downloadAndUnzip();
                if (enableMob) {
                    webDriver = new ChromeWebDriver(driver, headless, guest, browserMobProxy);
                } else {
                    webDriver = new ChromeWebDriver(driver, headless, guest);
                }
            } else if (e.getMessage().contains("only supports Microsoft Edge version")) {
                log.error("edgedriver版本不匹配！");
                EdgeUtil.downloadDriver();
                if (enableMob) {
                    webDriver = new ChromeWebDriver(driver, headless, guest, browserMobProxy);
                } else {
                    webDriver = new ChromeWebDriver(driver, headless, guest);
                }
            } else {
                throw e;
            }
        }
        ChromeWebDriver finalWebDriver = webDriver;
        boolean finalEnableMob = enableMob;
        webDriver.addWebDriverCloseEvent(() -> {
            int windowCount = 0;
            try {
                windowCount = finalWebDriver.getWindowHandles().size();
            } catch (Exception e) {
                windowCount = 0;
            }
            if (windowCount == 0) {
                if (finalEnableMob) {
                    browserMobProxy.stop();
                }
                finalWebDriver.quit();
            }
        });
        try {
            webDriver.run(script);
            //打印所有的变量
//            System.out.println(webDriver.getVariableMap());
            log.info("执行完毕");

        } catch (JavascriptException e) {
            log.error("执行出错:" + e.getRawMessage(), e);
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
