package com.iceolive.selenium;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class EdgeUtil {
    private EdgeUtil(){}
    private static OkHttpClient client = new OkHttpClient();
    public static String getVersion() {
        String version = RegUtil.getValue("\"HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Edge\\BLBeacon\"", "version");
        return version;
    }
    public static boolean downloadDriver() throws IOException {
        String version = getVersion();
        if(version == null){
            log.error("请先安装edge浏览器");
            return false;
        }
        version = version.split("\\.")[0];
        Request request;
        Response response;
        String downloadUrl; 
        String url =  "https://msedgedriver.azureedge.net/LATEST_RELEASE_" + version;
        request = new Request.Builder()
                .url(url)
                .get()
                .build();
        response = client.newCall(request).execute();
        String fullVersion = response.body().string();
        downloadUrl =  "https://msedgedriver.azureedge.net/" + fullVersion + "/edgedriver_win64.zip";
        log.info("开始下载edgedriver...");
        request = new Request.Builder().url(downloadUrl).build();
        response = client.newCall(request).execute();
        InputStream is;
        is = response.body().byteStream();
        FileOutputStream fos = null;
        String downloadPath = System.getProperty("user.dir") + "\\edgedriver_win64.zip";
        fos = new FileOutputStream(downloadPath);
        int len;
        byte[] bytes = new byte[4096];
        while ((len = is.read(bytes)) != -1) {
            fos.write(bytes, 0, len);
        }
        fos.flush();
        is.close();
        fos.close();
        ZipUtil.unzip(new File(downloadPath), System.getProperty("user.dir"), "");
        return true;
    }
}
