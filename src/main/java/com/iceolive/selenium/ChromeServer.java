package com.iceolive.selenium;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lowagie.text.pdf.codec.Base64;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author wangmianzhe
 */
public class ChromeServer extends WebSocketServer {
    private Integer port;

    public ChromeServer(int port) {
        super(new InetSocketAddress(port));
        this.port = port;
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        System.out.println(webSocket.getRemoteSocketAddress() + "建立连接");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        System.out.println(webSocket.getRemoteSocketAddress() + "断开连接");
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        String s1 = new String(Base64.decode(s), StandardCharsets.UTF_8);
        JSONObject jsonObject = JSON.parseObject(s1);
        String script = jsonObject.getString("script");
        String proxy = jsonObject.getString("proxy");
        String driver = System.getProperty("user.dir") + "\\chromedriver.exe";
        try {
            ChromeUtil.runScript(script, driver, proxy);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("本地服务启动成功 端口：" + this.port);
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }
}
