package com.iceolive.selenium;

import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.pdf.codec.Base64;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author wangmianzhe
 */
@Slf4j
public class ChromeServer extends WebSocketServer {
    private Integer port;

    public ChromeServer(int port) {
        super(new InetSocketAddress(port));
        this.port = port;
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        log.info(webSocket.getRemoteSocketAddress() + "建立连接");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        log.info(webSocket.getRemoteSocketAddress() + "断开连接");
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        String s1 = new String(Base64.decode(s), StandardCharsets.UTF_8);
        JsonNode jsonObject = JsonUtil.parse(s1);
        String script = null;
        if (jsonObject.get("script") != null) {
            script = jsonObject.get("script").asText();
        }
        String proxy = null;
        if (jsonObject.get("proxy") != null) {
            proxy = jsonObject.get("proxy").asText();
        }
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
        log.info("本地服务启动成功 端口：" + this.port + "  当前版本：" + VersionUtil.getVersion());
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }
}
