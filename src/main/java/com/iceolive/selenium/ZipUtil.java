package com.iceolive.selenium;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
@Slf4j
public class ZipUtil {
    private ZipUtil(){}
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
}
