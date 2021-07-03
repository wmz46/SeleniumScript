package com.iceolive.selenium;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.MessageFormat;

public class RegUtil {
    public static String getValue(String keyName,String valueName){
        try {
            String cmd = MessageFormat.format( "reg query {0} -v {1}",keyName,valueName);
            Process ps = null;
            ps = Runtime.getRuntime().exec(cmd);
            ps.getOutputStream().close();
            InputStreamReader i = new InputStreamReader(ps.getInputStream());
            String line;
            BufferedReader ir = new BufferedReader(i);
            while ((line = ir.readLine()) != null) {
                if(line.contains("    ")){
                    return line.split("    ")[3];
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
