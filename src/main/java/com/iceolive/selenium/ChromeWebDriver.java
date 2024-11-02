package com.iceolive.selenium;

import com.iceolive.util.ExcelUtil;
import com.iceolive.util.StringUtil;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.CaptureType;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.Connection;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author wangmianzhe
 */
@Slf4j
public class ChromeWebDriver implements WebDriver, JavascriptExecutor, TakesScreenshot, Interactive, HasCapabilities {

    private final String localStorePath = System.getProperty("user.dir") + File.separator + "localStore.json";
    private ChromiumDriver webDriver;

    private Map<String, Object> variableMap = new HashMap<>();

    private Map<String, Connection> connectionMap = new HashMap<>();

    BrowserMobProxy proxy;

    boolean stop = false;

    Actions builder = null;

    public ChromeWebDriver(String path, boolean headless, boolean guest) {
        if (path.endsWith("msedgedriver.exe")) {
            EdgeOptions edgeOptions = new EdgeOptions();
            if (headless) {
                edgeOptions.addArguments("headless");
            }
            if (guest) {
                edgeOptions.addArguments("guest");
            }
            //去除 window.navigator.webdriver
            edgeOptions.addArguments("disable-blink-features=AutomationControlled");

            Map<String, Object> prefs = new LinkedHashMap<>();
            prefs.put("user_experience_metrics.personalization_data_consent_enabled", Boolean.valueOf(true));
            edgeOptions.setExperimentalOption("prefs", prefs);
            webDriver = new EdgeDriver(edgeOptions);
        } else {
            File chromeDriverPath = new File(path);
            System.setProperty("webdriver.chrome.driver", chromeDriverPath.getAbsolutePath());
            ChromeOptions chromeOptions = new ChromeOptions();
            if (headless) {
                chromeOptions.addArguments("headless");
            }
            if (guest) {
                chromeOptions.addArguments("guest");
            }
            //去除 window.navigator.webdriver
            chromeOptions.addArguments("disable-blink-features=AutomationControlled");

            webDriver = new ChromeDriver(chromeOptions);
        }
    }

    public ChromeWebDriver(String path, boolean headless, boolean guest, BrowserMobProxy browserMobProxy) {
        this.proxy = browserMobProxy;
        proxy.enableHarCaptureTypes(CaptureType.REQUEST_HEADERS, CaptureType.REQUEST_CONTENT, CaptureType.REQUEST_BINARY_CONTENT, CaptureType.REQUEST_COOKIES, CaptureType.RESPONSE_HEADERS, CaptureType.RESPONSE_CONTENT, CaptureType.RESPONSE_BINARY_CONTENT, CaptureType.RESPONSE_COOKIES);
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(browserMobProxy);

        if (path.endsWith("msedgedriver.exe")) {
            EdgeOptions edgeOptions = new EdgeOptions();
            if (headless) {
                edgeOptions.addArguments("headless");
            }
            if (guest) {
                edgeOptions.addArguments("guest");
            }
            edgeOptions.setProxy(seleniumProxy);
            //去除 window.navigator.webdriver
            edgeOptions.addArguments("disable-blink-features=AutomationControlled");
            //忽略证书错误
            edgeOptions.addArguments("ignore-certificate-errors");
            //忽略证书访问
            edgeOptions.addArguments("ignore-urlfetcher-cert-requests");
            Map<String, Object> prefs = new LinkedHashMap<>();
            prefs.put("user_experience_metrics.personalization_data_consent_enabled", Boolean.valueOf(true));
            edgeOptions.setExperimentalOption("prefs", prefs);
            webDriver = new EdgeDriver(edgeOptions);
        } else {
            File chromeDriverPath = new File(path);
            System.setProperty("webdriver.chrome.driver", chromeDriverPath.getAbsolutePath());
            ChromeOptions chromeOptions = new ChromeOptions();
            if (headless) {
                chromeOptions.addArguments("headless");
            }
            if (guest) {
                chromeOptions.addArguments("guest");
            }
            chromeOptions.setProxy(seleniumProxy);
            //去除 window.navigator.webdriver
            chromeOptions.addArguments("disable-blink-features=AutomationControlled");
            //忽略证书错误
            chromeOptions.addArguments("ignore-certificate-errors");
            //忽略证书访问
            chromeOptions.addArguments("ignore-urlfetcher-cert-requests");
            webDriver = new ChromeDriver(chromeOptions);
        }

    }

    private Object getValueByKey(String exp, Object data) {
        if (data == null) {
            throw new NullPointerException();
        }
        if (!(data instanceof List) && !(data instanceof Map)) {
            throw new IllegalArgumentException();
        }
        Object value = null;
        if (exp.startsWith("[") && data instanceof List) {
            Matcher matcher = Pattern.compile("^\\[(\\d+)\\]").matcher(exp);
            if (matcher.find()) {
                String key = matcher.group(1);
                value = ((List<Object>) data).get(Integer.valueOf(key));
                if (exp.length() > key.length() + 2) {
                    value = getValueByKey(exp.substring(key.length() + 2), value);
                }
            }
        } else if (exp.startsWith(".")) {
            Matcher matcher = Pattern.compile("^\\.([^.\\[]+)").matcher(exp);
            if (matcher.find()) {
                String key = matcher.group(1);
                if (data instanceof List) {
                    value = ((List<Object>) data).get(Integer.valueOf(key));
                } else if (data instanceof Map) {
                    value = ((Map<String, Object>) data).get(key);
                }
                if (exp.length() > key.length() + 1) {
                    value = getValueByKey(exp.substring(key.length() + 1), value);
                }
            }
        } else {
            Matcher matcher = Pattern.compile("^([^.\\[]+)").matcher(exp);
            if (matcher.find()) {
                String key = matcher.group(1);
                if (data instanceof List) {
                    value = ((List<Object>) data).get(Integer.valueOf(key));
                    value = getValueByKey(exp.substring(key.length()), value);
                } else if (data instanceof Map) {
                    value = ((Map<String, Object>) data).get(key);
                    if (exp.length() > key.length()) {
                        value = getValueByKey(exp.substring(key.length()), value);
                    }
                }
            }

        }
        return value;
    }

    /**
     * 替换变量，替换%变量名%
     *
     * @param str
     * @return
     */
    private String replaceVariable(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        Matcher matcher = Pattern.compile("%(.*?)%").matcher(str);
        while (matcher.find()) {
            String key = matcher.group(1);
            str = str.replace("%" + key + "%", String.valueOf(getValueByKey(key, variableMap)));
        }
        return str;
    }

    private void run(List<SeleniumCmd> list) {
        if (stop) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            if (stop) {
                return;
            }
            SeleniumCmd item = list.get(i);
            log.info(item.toString());
            String command = item.getCommand();
            String target = replaceVariable(item.getArg1());
            String value = replaceVariable(item.getArg2());
            String timeout = replaceVariable(item.getArg3());
            String password = replaceVariable(item.getArg4());
            String statement = item.getStatement();
            String sqlStatement = item.getSqlStatement();
            if ("newStw".equals(command)) {
                variableMap.put(target, System.currentTimeMillis());
            } else if ("endStw".equals(command)) {
                long endTime = System.currentTimeMillis();
                if (variableMap.containsKey(target)) {
                    long startTime = (long) variableMap.get(target);
                    variableMap.put(target, endTime - startTime);
                    log.info("      -> " + target + "耗时" + (endTime - startTime) + "毫秒");
                } else {
                    variableMap.put(target, 0L);
                }
            } else if ("win32_getByTitle".equals(command)) {
                variableMap.put(target, Win32Api.getByTitle(value));
            } else if ("win32_getAllByPID".equals(command)) {
                variableMap.put(target, Win32Api.getAllByPID(Integer.parseInt(value)));
            } else if ("win32_getChildren".equals(command)) {
                variableMap.put(target, Win32Api.getChildren(Long.parseLong(value)));
            } else if ("win32_getTitle".equals(command)) {
                variableMap.put(target, Win32Api.getTitle(Long.parseLong(value)));
            } else if ("win32_setTopMost".equals(command)) {
                Win32Api.setTopMost(Long.parseLong(target));
            } else if ("win32_showWindow".equals(command)) {
                if (value.equals("normal")) {
                    value = "1";
                } else if (value.equals("min")) {
                    value = "2";
                } else if (value.equals("max")) {
                    value = "3";
                }
                Win32Api.showWindow(Long.parseLong(target), Integer.parseInt(value));
            } else if ("win32_getPID".equals(command)) {
                variableMap.put(target, Win32Api.getPID(Long.parseLong(value)));
            } else if ("win32_getDesktop".equals(command)) {
                variableMap.put(target, Win32Api.getDesktop());
            } else if ("win32_screenshot".equals(command)) {
                try {
                    BufferedImage screenShot = Win32Api.getScreenShot(Long.parseLong(target));
                    ImageIO.write(screenShot, "png", new File(value));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if ("cmd".equals(command)) {
                String filename = "cmd.bat";
                String script = statement;
                Matcher cmdMatcher = Pattern.compile("_\\$map\\.([_$a-zA-Z0-9]+)").matcher(statement);
                while (cmdMatcher.find()) {
                    String name = cmdMatcher.group(1);
                    if (variableMap.containsKey(name)) {
                        script = script.replaceAll("_\\$map\\.[_$a-zA-Z0-9]+", String.valueOf(variableMap.get(name)));
                    }
                }
                script = script.replace("\n", "\r\n");
                FileUtil.writeToFile(filename, script, "GBK");
                String s = CmdUtil.callCmd(filename);
                if (StringUtil.isNotEmpty(target)) {
                    variableMap.put(target, s);
                }
                new File(filename).delete();
            } else if ("wscript".equals(command)) {
                String filename = "wscript.js";
                String script = statement;
                Matcher cmdMatcher = Pattern.compile("_\\$map\\.([_$a-zA-Z0-9]+)").matcher(statement);
                while (cmdMatcher.find()) {
                    String name = cmdMatcher.group(1);
                    if (variableMap.containsKey(name)) {
                        script = script.replaceAll("_\\$map\\.[_$a-zA-Z0-9]+", String.valueOf(variableMap.get(name)));
                    }
                }
                script = script.replace("\n", "\r\n");
                FileUtil.writeToFile(filename, script, "GBK");
                String s = CmdUtil.callCmd("wscript", filename);
                if (StringUtil.isNotEmpty(target)) {
                    variableMap.put(target, s);
                }
                new File(filename).delete();
            } else if ("setConn".equals(command)) {
                connectionMap.put(target, SqlUtil.getConnection(value, timeout, password));
            } else if ("querySql".equals(command)) {
                if (StringUtil.isNotEmpty(statement)) {
                    statement = "var _$map = arguments[0];" + statement;
                    sqlStatement = (String) webDriver.executeScript(statement, variableMap);
                }
                List<Map<String, Object>> dataList = SqlUtil.querySql(connectionMap.get(value), sqlStatement, variableMap);
                variableMap.put(target, dataList);
                log.info("      -> 返回记录数：" + (dataList == null ? 0 : dataList.size()));
            } else if ("execSql".equals(command)) {
                if (StringUtil.isNotEmpty(statement)) {
                    statement = "var _$map = arguments[0];" + statement;
                    sqlStatement = (String) webDriver.executeScript(statement, variableMap);
                }
                SqlUtil.ExecResult execResult = SqlUtil.execSql(connectionMap.get(value), sqlStatement, variableMap);
                variableMap.put(target, execResult.getCount());
                if (StringUtil.isNotEmpty(timeout)) {
                    variableMap.put(timeout, execResult.getPrimaryKey());
                }
                log.info("      -> 受影响行数：" + execResult.getCount() + (execResult.getPrimaryKey() == null ? "" : " 数据主键：" + execResult.getPrimaryKey()));
            } else if ("screenshot".equals(command)) {
                try {
                    WebElement element1 = webDriver.findElement(By.cssSelector(target));
                    if (element1.isDisplayed()) {
                        Long fullWidth = (long) webDriver.executeScript("return document.documentElement.scrollWidth");
                        Long fullHeight = (long) webDriver.executeScript("return document.documentElement.scrollHeight");
                        Dimension dimension = new Dimension(fullWidth.intValue(), fullHeight.intValue());
                        Dimension originalSize = webDriver.manage().window().getSize();
                        webDriver.manage().window().setSize(dimension);
                        byte[] byteArray = webDriver.getScreenshotAs(OutputType.BYTES);
                        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
                        BufferedImage img = ImageIO.read(bais);
                        Rectangle rect = element1.getRect();
                        //从元素左上角坐标开始，按照元素的高宽对img进行裁剪为符合需要的图片
                        BufferedImage dest = img.getSubimage(rect.x, rect.y, rect.width, rect.height);
                        if (value.endsWith(".pdf")) {
                            com.lowagie.text.Rectangle rect2 = new com.lowagie.text.Rectangle(rect.x, rect.y, rect.width, rect.height);
                            Document document = new Document(rect2, 0, 0, 0, 0);
                            FileOutputStream fos = new FileOutputStream(new File(value));
                            PdfWriter.getInstance(document, fos);
                            document.open();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(dest, "png", baos);
                            Image image = Image.getInstance(baos.toByteArray());
                            document.add(image);
                            document.close();
                        } else {
                            ImageIO.write(dest, "png", new File(value));
                        }

                        webDriver.manage().window().setSize(originalSize);
                    } else {
                        log.error("元素不可见，无法截图：" + item.toString());
                    }

                } catch (IOException | BadElementException e) {
                    e.printStackTrace();
                } catch (DocumentException e) {
                    e.printStackTrace();
                }
            } else if ("set".equals(command)) {
                Object obj = null;
                if (value != null) {
                    obj = value;
                } else {
                    statement = "var _$map = arguments[0];" + statement;
                    obj = webDriver.executeScript(statement, variableMap);
                }
                log.info("      -> " + obj);
                variableMap.put(target, obj);
            } else if ("setStore".equals(command)) {
                String storeKey = target;
                String variableName = value;
                Object o = variableMap.get(variableName);
                Map<String, Object> localStore = JsonUtil.loadJson(localStorePath);
                localStore.put(storeKey, o);
                JsonUtil.saveJson(localStorePath, localStore);

            } else if ("getStore".equals(command)) {
                String storeKey = target;
                String variableName = value;
                String defaultValue = timeout;
                Map<String, Object> localStore = JsonUtil.loadJson(localStorePath);
                if (localStore.containsKey(storeKey)) {
                    variableMap.put(variableName, localStore.get(storeKey));
                } else {
                    variableMap.put(variableName, StringUtil.isEmpty(defaultValue) ? null : defaultValue);
                }
            } else if ("alert".equals(command)) {
                this.alert(target);
            } else if ("exec".equals(command)) {
                statement = "var _$map = arguments[0];" + statement;
                webDriver.executeScript(statement, variableMap);
            } else if ("execAsync".equals(command)) {
                statement = "var _$map = arguments[0]; var _$cb = arguments[arguments.length - 1];" + statement;
                webDriver.executeAsyncScript(statement, variableMap);
            } else if ("setAsync".equals(command)) {
                statement = "var _$map = arguments[0]; var _$cb = arguments[arguments.length - 1];" + statement;
                Object response = webDriver.executeAsyncScript(statement, variableMap);
                variableMap.put(target, response);
            } else if ("scroll".equals(command)) {
                webDriver.executeScript("window.scrollBy(" + target + ");");
            } else if ("switch".equals(command)) {
                if (target == null) {
                    webDriver.switchTo().defaultContent();
                } else if (Pattern.matches("^\\d+$", target)) {
                    webDriver.switchTo().frame(Integer.parseInt(target));
                } else if (target != null) {
                    webDriver.switchTo().frame(target);
                }
            } else if ("sleep".equals(command)) {
                try {
                    Thread.sleep((long) (Float.parseFloat(target) * 1000));
                } catch (InterruptedException e) {

                }
            } else if ("open".equals(command)) {
                try {
                    webDriver.get(target);
                } catch (Exception e) {
                    try {
                        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(3));
                        wait.until(ExpectedConditions.urlMatches(target));
                    } catch (TimeoutException e1) {
                        throw new RuntimeException("open超时", e1);
                    } catch (Exception e2) {
                        throw new RuntimeException("open异常", e2);
                    }
                }
            } else if ("clear".equals(command)) {
                webDriver.findElement(By.cssSelector(target)).clear();
            } else if ("type".equals(command)) {
                webDriver.findElement(By.cssSelector(target)).sendKeys(value);
            } else if ("enter".equals(command)) {
                webDriver.findElement(By.cssSelector(target)).sendKeys(Keys.ENTER);
            } else if ("click".equals(command)) {
                webDriver.findElement(By.cssSelector(target)).click();
            } else if ("drag".equals(command)) {
                WebElement element = webDriver.findElement(By.cssSelector(target));
                builder = new Actions(webDriver);
                builder.dragAndDropBy(element, Integer.parseInt(value.split(",")[0].trim()), Integer.parseInt(value.split(",")[1].trim())).perform();
            } else if ("repeat".equals(command)) {
                if (target != null) {
                    if (Pattern.matches("^\\d+$", target)) {
                        int count = Integer.parseInt(target);
                        int $index = 0;
                        while (count > 0) {
                            if (StringUtil.isNotEmpty(value)) {
                                variableMap.put(value, $index++);
                            }
                            if (item.getStatement() != null) {
                                String statement1 = "var _$map = arguments[0];" + item.getStatement();
                                if (!(Boolean) webDriver.executeScript(statement1, variableMap)) {
                                    break;
                                }
                            }
                            if (item.getRepeatCommands() != null && !item.getRepeatCommands().isEmpty()) {
                                run(item.getRepeatCommands());
                            }
                            count--;
                        }
                    } else {
                        List<Object> list2 = (List<Object>) getValueByKey(target, variableMap);
                        for (int j = 0; j < list2.size(); j++) {
                            if (StringUtil.isNotEmpty(value)) {
                                variableMap.put(value, j);
                            }
                            if (StringUtil.isNotEmpty(timeout)) {
                                variableMap.put(timeout, list2.get(j));
                            }
                            if (item.getStatement() != null) {
                                String statement1 = "var _$map = arguments[0];" + item.getStatement();
                                if (!(Boolean) webDriver.executeScript(statement1, variableMap)) {
                                    break;
                                }
                            }
                            if (item.getRepeatCommands() != null && !item.getRepeatCommands().isEmpty()) {
                                run(item.getRepeatCommands());
                            }
                        }
                    }

                } else {
                    while (true) {
                        if (item.getStatement() != null) {
                            String statement1 = "var _$map = arguments[0];" + item.getStatement();
                            if (!(Boolean) webDriver.executeScript(statement1, variableMap)) {
                                break;
                            }
                        } else {
                            log.error("repeat未设置最大循环次数，也未设置<script></script>,循环不执行");
                            break;
                        }
                        if (item.getRepeatCommands() != null && !item.getRepeatCommands().isEmpty()) {
                            run(item.getRepeatCommands());
                        }
                    }
                }

            } else if ("when".equals(command)) {
                statement = "var _$map = arguments[0];" + statement;
                if ((Boolean) webDriver.executeScript(statement, variableMap)) {
                    if (item.getThenCommands() != null && !item.getThenCommands().isEmpty()) {
                        run(item.getThenCommands());
                    }
                } else {
                    if (item.getElseCommands() != null && !item.getElseCommands().isEmpty()) {
                        run(item.getElseCommands());
                    }
                }
            } else if ("wait".equals(command)) {
                try {
                    if (StringUtil.isEmpty(timeout)) {
                        timeout = "3";
                    }
                    WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(Integer.parseInt(timeout)));
                    if ("visible".equals(value)) {
                        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(target)));
                    } else if ("url".equals(value)) {
                        wait.until(ExpectedConditions.urlMatches(target));
                    } else if ("invisible".equals(value)) {
                        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(target)));
                    }
                    if (item.getThenCommands() != null && !item.getThenCommands().isEmpty()) {
                        run(item.getThenCommands());
                    }
                } catch (TimeoutException e) {
                    if (item.getElseCommands() != null && !item.getElseCommands().isEmpty()) {
                        run(item.getElseCommands());
                    }
                    log.error("wait超时:" + item);
                }
            } else if ("saveJson".equals(command)) {
                download(target, value, "json", null);
            } else if ("saveCsv".equals(command)) {
                download(target, value, "csv", timeout);
            } else if ("log".equals(command)) {
                //输出日志
                log.info("      -> " + target);
            } else if ("stop".equals(command)) {
                //终止
                stop = true;
            } else if ("keydown".equals(command)) {
                Actions actions = new Actions(webDriver);
                switch (target.toLowerCase()) {
                    case "end":
                        actions.sendKeys(Keys.END).perform();
                        break;
                    case "home":
                        actions.sendKeys(Keys.HOME).perform();
                        break;
                    case "f5":
                        actions.sendKeys(Keys.F5).perform();
                        break;
                }
            } else if ("newHar".equals(command)) {
                if (this.proxy != null) {
                    proxy.newHar();
                }
            } else if ("endHar".equals(command)) {
                if (this.proxy != null && target != null) {
                    Har har = proxy.endHar();
                    List<Map<String, String>> requests = new ArrayList();
                    for (HarEntry entry : har.getLog().getEntries()) {
                        String method = entry.getRequest().getMethod();
                        if (method.equals("OPTIONS")) {
                            continue;
                        }
                        String url = entry.getRequest().getUrl();
                        String content = entry.getResponse().getContent().getText();
                        Map<String, String> map = new HashMap<>();
                        map.put("url", url);
                        map.put("method", method);
                        map.put("content", content);
                        requests.add(map);
                    }
                    variableMap.put(target, requests);
                }
            } else if ("maximize".equals(command)) {
                webDriver.manage().window().maximize();
            } else if ("loadExcel".equals(command)) {
                List<Map<String, String>> mapList = ExcelUtil.excel2List(target);
                log.info("      -> 加载记录数：" + mapList.size());
                variableMap.put(value, mapList);
            } else if ("prompt".equals(command)) {
                this.prompt(value);
                try {
                    if (StringUtil.isEmpty(timeout)) {
                        timeout = "3";
                    }
                    WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(Integer.parseInt(timeout)));
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("#__prompt__")));
                    String text = webDriver.findElement(By.cssSelector("#__prompt__ input")).getAttribute("value");
                    variableMap.put(target, text);
                    if (item.getThenCommands() != null && !item.getThenCommands().isEmpty()) {
                        run(item.getThenCommands());
                    }
                } catch (TimeoutException e) {
                    if (item.getElseCommands() != null && !item.getElseCommands().isEmpty()) {
                        run(item.getElseCommands());
                    }
                    log.error("prompt超时");
                }
            } else if ("resize".equals(command)) {
                String[] split = target.split(",");
                int w = Integer.parseInt(split[0]);
                int h = Integer.parseInt(split[1]);
                webDriver.manage().window().setSize(new Dimension(w, h));
            } else if ("pos".equals(command)) {
                String img1 = value;
                String img2 = timeout;
                Integer distance1 = ImageUtil.getDistance(img1, img2);
                variableMap.put(target, distance1);
                log.info("      -> 偏移量：" + distance1);
            } else if ("slowDrag".equals(command)) {
                WebElement element1 = webDriver.findElement(By.cssSelector(target));
                Integer dist = Integer.parseInt(value);
                if (StringUtil.isEmpty(timeout)) {
                    timeout = "3";
                }
                int time = (int) (Float.parseFloat(timeout) * 1000);
                Random random = new Random();
                int n = 3;
                builder = new Actions(webDriver);
                builder.clickAndHold(element1);
                for (int j = 0; j < n; j++) {
                    if (j < n - 1) {
                        int tempDist = random.nextInt(dist - 1) + 1;
                        int tempTime = random.nextInt(time - 1) + 1;
                        dist -= tempDist;
                        time -= tempTime;
                        builder.moveByOffset(tempDist, 0);
                        builder.pause(tempTime);
                    } else {
                        builder.moveByOffset(dist, 0);
                        builder.pause(time);
                    }
                }
                builder.release();
                builder.perform();

            } else if ("getWindowHandle".equals(command)) {
                String handle = webDriver.getWindowHandle();
                variableMap.put(target, handle);
            } else if ("getWindowHandles".equals(command)) {
                Set<String> handles = webDriver.getWindowHandles();
                //转list
                List<String> handlesList = new ArrayList<>();
                handles.forEach(handlesList::add);
                variableMap.put(target, handlesList);
            } else if ("switchWindow".equals(command)) {
                webDriver.switchTo().window(target);
            }
        }

    }

    /**
     * 获取所有的存储值
     *
     * @return
     */
    public Map<String, Object> getVariableMap() {
        return variableMap;
    }

    /**
     * 根据key获取存储值
     *
     * @param key
     * @return
     */
    public Object getVariable(String key) {
        return variableMap.get(key);
    }

    private List<SeleniumCmd> parse(String[] lines, int startLineNum) {
        List<SeleniumCmd> list = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            SeleniumCmd seleniumCmd = new SeleniumCmd(line, i + startLineNum + 1);
            if (seleniumCmd.isWaitCmd() || seleniumCmd.isPromptCmd()) {
                if (i + 1 < lines.length && "then".equals(lines[i + 1].trim())) {
                    //如果wait下一行是then，则需要设置then和else
                    int thenCount = 0;
                    Integer thenLineNum = i + 1;
                    Integer endLineNum = null;
                    Integer elseLineNum = null;
                    for (int j = i + 2; j < lines.length; j++) {
                        //从i+2开始找到第一个孤立的end,即没有then与之匹配
                        if (lines[j].trim().equals("then") || lines[j].trim().equals("begin")) {
                            thenCount++;
                        }
                        if (lines[j].trim().equals("else")) {
                            if (thenCount == 0) {
                                elseLineNum = j;
                            }
                        }
                        if (lines[j].trim().equals("end")) {
                            if (thenCount == 0) {
                                endLineNum = j;
                                break;
                            }
                            thenCount--;
                        }
                    }
                    if (thenLineNum != null) {
                        if (elseLineNum != null) {
                            String[] elseLines = new String[endLineNum - elseLineNum - 1];
                            System.arraycopy(lines, elseLineNum + 1, elseLines, 0, endLineNum - elseLineNum - 1);
                            seleniumCmd.setElseCommands(parse(elseLines, startLineNum + elseLineNum + 1));
                            String[] thenLines = new String[elseLineNum - thenLineNum - 1];
                            System.arraycopy(lines, thenLineNum + 1, thenLines, 0, elseLineNum - thenLineNum - 1);
                            seleniumCmd.setThenCommands(parse(thenLines, startLineNum + thenLineNum + 1));
                        } else {
                            String[] thenLines = new String[endLineNum - thenLineNum - 1];
                            System.arraycopy(lines, thenLineNum + 1, thenLines, 0, endLineNum - thenLineNum - 1);
                            seleniumCmd.setThenCommands(parse(thenLines, startLineNum + thenLineNum + 1));
                        }
                    }
                    list.add(seleniumCmd);
                    i = endLineNum;
                } else {
                    list.add(seleniumCmd);
                }
            } else if (seleniumCmd.isWhenCmd()) {
                if (i + 1 < lines.length && "<script>".equals(lines[i + 1].trim())) {
                    String statement = "";
                    for (int j = i + 2; j < lines.length; j++) {
                        if (lines[j].trim().equals("</script>")) {
                            i = j;
                            break;
                        } else {
                            statement += lines[j] + "\n";
                        }
                    }
                    seleniumCmd.setStatement(statement);
                    if (i + 1 < lines.length && "then".equals(lines[i + 1].trim())) {
                        //如果wait下一行是then，则需要设置then和else
                        int thenCount = 0;
                        Integer thenLineNum = i + 1;
                        Integer endLineNum = null;
                        Integer elseLineNum = null;
                        for (int j = i + 2; j < lines.length; j++) {
                            //从i+2开始找到第一个孤立的end,即没有then与之匹配
                            if (lines[j].trim().equals("then") || lines[j].trim().equals("begin")) {
                                thenCount++;
                            }
                            if (lines[j].trim().equals("else")) {
                                if (thenCount == 0) {
                                    elseLineNum = j;
                                }
                            }
                            if (lines[j].trim().equals("end")) {
                                if (thenCount == 0) {
                                    endLineNum = j;
                                    break;
                                }
                                thenCount--;
                            }
                        }
                        if (thenLineNum != null) {
                            if (elseLineNum != null) {
                                String[] elseLines = new String[endLineNum - elseLineNum - 1];
                                System.arraycopy(lines, elseLineNum + 1, elseLines, 0, endLineNum - elseLineNum - 1);
                                seleniumCmd.setElseCommands(parse(elseLines, startLineNum + elseLineNum + 1));
                                String[] thenLines = new String[elseLineNum - thenLineNum - 1];
                                System.arraycopy(lines, thenLineNum + 1, thenLines, 0, elseLineNum - thenLineNum - 1);
                                seleniumCmd.setThenCommands(parse(thenLines, startLineNum + thenLineNum + 1));
                            } else {
                                String[] thenLines = new String[endLineNum - thenLineNum - 1];
                                System.arraycopy(lines, thenLineNum + 1, thenLines, 0, endLineNum - thenLineNum - 1);
                                seleniumCmd.setThenCommands(parse(thenLines, startLineNum + thenLineNum + 1));
                            }
                        }
                        i = endLineNum;
                    }
                    list.add(seleniumCmd);
                }

            } else if (seleniumCmd.isRepeatCmd()) {
                if (i + 1 < lines.length && "<script>".equals(lines[i + 1].trim())) {
                    String statement = "";
                    for (int j = i + 2; j < lines.length; j++) {
                        if (lines[j].trim().equals("</script>")) {
                            i = j;
                            break;
                        } else {
                            statement += lines[j] + "\n";
                        }
                    }
                    seleniumCmd.setStatement(statement);

                }
                if (i + 1 < lines.length && "begin".equals(lines[i + 1].trim())) {
                    Integer beginLineNum = i + 1;
                    Integer endLineNum = null;
                    int thenCount = 0;
                    for (int j = i + 2; j < lines.length; j++) {
                        if (lines[j].trim().equals("then") || lines[j].trim().equals("begin")) {
                            thenCount++;
                        }
                        if (lines[j].trim().equals("end")) {
                            if (thenCount == 0) {
                                i = j;
                                endLineNum = j;
                                break;
                            }
                            thenCount--;
                        }
                    }
                    String[] repeatLines = new String[endLineNum - beginLineNum - 1];
                    System.arraycopy(lines, beginLineNum + 1, repeatLines, 0, endLineNum - beginLineNum - 1);

                    seleniumCmd.setRepeatCommands(parse(repeatLines, startLineNum + beginLineNum + 1));
                }
                list.add(seleniumCmd);
            } else if (seleniumCmd.isSetCmd() || seleniumCmd.isExecCmd() || seleniumCmd.isWinCmd() || seleniumCmd.isWscript()) {
                if (i + 1 < lines.length && "<script>".equals(lines[i + 1].trim())) {
                    String statement = "";
                    for (int j = i + 2; j < lines.length; j++) {
                        if (lines[j].trim().equals("</script>")) {
                            i = j;
                            break;
                        } else {
                            statement += lines[j] + "\n";
                        }
                    }
                    seleniumCmd.setStatement(statement);
                    list.add(seleniumCmd);
                } else if (seleniumCmd.getArg2() != null) {
                    list.add(seleniumCmd);
                }
            } else if (seleniumCmd.isQuerySql() || seleniumCmd.isExecSql()) {
                if (i + 1 < lines.length && "<sql>".equals(lines[i + 1].trim())) {
                    String statement = "";
                    for (int j = i + 2; j < lines.length; j++) {
                        if (lines[j].trim().equals("</sql>")) {
                            i = j;
                            break;
                        } else {
                            statement += lines[j] + "\n";
                        }
                    }
                    seleniumCmd.setSqlStatement(statement);
                    list.add(seleniumCmd);
                } else if (i + 1 < lines.length && "<script>".equals(lines[i + 1].trim())) {
                    String statement = "";
                    for (int j = i + 2; j < lines.length; j++) {
                        if (lines[j].trim().equals("</script>")) {
                            i = j;
                            break;
                        } else {
                            statement += lines[j] + "\n";
                        }
                    }
                    seleniumCmd.setStatement(statement);
                    list.add(seleniumCmd);
                } else {
                    list.add(seleniumCmd);
                }
            } else if (seleniumCmd.isCommand()) {
                list.add(seleniumCmd);
            }
        }
        return list;
    }


    /**
     * 同步执行脚本
     *
     * @param cmd 脚本
     */
    public void run(String cmd) {
        if (cmd != null) {
            String[] lines = cmd.split("\n");
            List<SeleniumCmd> list = parse(lines, 0);
            run(list);
        }
    }

    /**
     * 从文件执行脚本,文件编码utf-8
     *
     * @param path 文件路径
     */
    public void runFromFile(String path) {
        runFromFile(path, "utf-8");
    }

    /**
     * 从文件执行脚本
     *
     * @param path    文件路径
     * @param charset 文件编码
     */
    public void runFromFile(String path, String charset) {
        String s = FileUtil.readFromFile(path, charset);
        run(s);
    }

    /**
     * 异步执行脚本
     *
     * @param cmd param callback
     * @return
     */
    public Thread runAsync(String cmd, BiConsumer<Boolean, Exception> callback) {
        Thread t1 = new Thread(() -> {
            if (cmd != null) {
                String[] lines = cmd.split("\n");
                List<SeleniumCmd> list = parse(lines, 0);
                try {
                    run(list);
                    callback.accept(true, null);
                } catch (Exception e) {
                    callback.accept(false, e);
                }
            }
        });
        t1.start();
        return t1;
    }

    /**
     * 注册自定义事件，通过异步线程每100毫秒循环一次
     *
     * @param func 自定义事件，返回false则退出循环，返回true则继续循环
     * @return 返回线程
     */
    public Thread addWebDriverEvent(Supplier<Boolean> func) {
        Thread t1 = new Thread(() -> {
            while (true) {
                try {
                    if (!func.get()) {
                        break;
                    } else {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t1.start();
        return t1;
    }


    /**
     * 注册窗口关闭事件，通过异步线程每100毫秒循环一次
     *
     * @param func 关闭时触发
     * @return 返回线程
     */
    public Thread addWebDriverCloseEvent(Runnable func) {
        return addWebDriverEvent((() -> {
            try {
                webDriver.getWindowHandle();
                return true;
            } catch (UnhandledAlertException e) {
                //弹窗阻塞情况下不退出循环
                return true;
            } catch (WebDriverException e) {
                //窗口已关闭才会获取不到url，退出循环
                func.run();
                return false;
            }
        }));
    }

    /**
     * 注册Url改变事件，通过异步线程每100毫秒循环一次
     *
     * @param func 返回false则退出循环，返回true则继续循环
     * @return 返回线程
     */
    public Thread addWebDriverUrlChangeEvent(Function<String, Boolean> func) {
        final String[] oldUrl = {""};
        return addWebDriverEvent((() -> {
            try {
                String newUrl = webDriver.getCurrentUrl();
                if (!oldUrl[0].equals(newUrl)) {
                    if (!func.apply(newUrl)) {
                        return false;
                    }
                }
                oldUrl[0] = newUrl;
                return true;
            } catch (WebDriverException e) {
                //窗口已关闭才会获取不到url,退出循环
                return false;
            }
        }));
    }

    public void download(String key, String filename, String type, String columns) {
        if (!variableMap.containsKey(key)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        if ("csv".equals(type)) {
            sb.append("  const BOM = '\\uFEFF';\n");
            sb.append("  let columnDelimiter = ',';\n");
            sb.append("  let rowDelimiter = '\\r\\n';\n");
            sb.append("  let csv = '';\n");
            sb.append("  let rows = arguments[0]['" + key + "'];\n");
            sb.append("  if (Array.isArray(rows) && rows.length > 0) {\n");
            if (StringUtil.isEmpty(columns)) {
                sb.append("    let columns = Object.keys(rows[0]);\n");
            } else {
                sb.append("  let columns='" + columns + "'.split(',');\n");
            }
            sb.append("    csv = columns.reduce((previous, column) => {\n");
            sb.append("      return (previous ? previous + columnDelimiter : '') + column;\n");
            sb.append("    }, '');\n");
            sb.append("    csv = rows.reduce((previous, row) => {\n");
            sb.append("      let rowCsv = columns.reduce((pre, column) => {\n");
            sb.append("        if (row.hasOwnProperty(column)) {\n");
            sb.append("          let cell = row[column];\n");
            sb.append("          if (cell != null) {\n");
            sb.append("            if (cell != null) {\n");
            sb.append("              cell = cell.toString();\n");
            sb.append("              cell = cell.replace(/\"/g, '\"\"');\n");
            sb.append("              //cell = cell.replace(new RegExp(rowDelimiter, 'g'), ' ');\n");
            sb.append("              cell = (new RegExp(columnDelimiter).test(cell) || new RegExp('[\\r\\n\"]').test(cell)) ? `\"${cell}\"` : cell;\n");
            sb.append("              return pre ? pre + columnDelimiter + cell : pre + cell;\n");
            sb.append("            }\n");
            sb.append("          }\n");
            sb.append("          return pre ? pre + columnDelimiter : pre + \"\";\n");
            sb.append("        } else {\n");
            sb.append("          return pre ? pre + columnDelimiter : pre + \"\";\n");
            sb.append("        }\n");
            sb.append("      }, '');\n");
            sb.append("      return previous + rowDelimiter + rowCsv;\n");
            sb.append("    }, csv);\n");
            sb.append("  }\n");

            sb.append("  let blob = new Blob([BOM + csv], {\n");
            sb.append("    type: 'text/csv;charset=utf-8;'\n");
            sb.append("  });\n");
        } else {
            sb.append("  let blob = new Blob([JSON.stringify(arguments[0]['" + key + "'], null, 2)], {\n");
            sb.append("    type: 'application/json'\n");
            sb.append("  });\n");
        }
        if (filename != null && filename.length() > 0) {
            sb.append("  blob.name='" + filename + "';\n");
        }
        sb.append("  let link = document.createElement('a');\n");
        sb.append("  link.href = URL.createObjectURL(blob);\n");
        sb.append("  link.download = blob.name;\n");
        sb.append("  link.click();\n");
        sb.append("  URL.revokeObjectURL(link.href);\n");
        webDriver.executeScript(sb.toString(), variableMap);
    }

    /**
     * 在页面上弹窗提示
     *
     * @param msg 提示语
     */
    public void alert(String msg) {
        if (msg == null) {
            msg = "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(function(){\n");
        sb.append("  var div = document.createElement('div');\n");
        sb.append("  div.setAttribute('style','position: fixed;z-index:999999;font-size:16px;left:calc(50% - 200px);color:#fff;width:400px;top:calc(50% - 100px);height:200px;background-color:lightpink;text-align:center;');\n");

        sb.append("  var content = document.createElement('p');\n");
        sb.append("  content.setAttribute('style','margin-top:70px;overflow:auto;height:80px');\n");
        sb.append("  content.innerHTML = '" + msg.replace("'", "\\'").replace("\n", "<br>") + "';\n");
        sb.append("  div.appendChild(content);\n");

        sb.append("  var closeBtn = document.createElement('span');\n");
        sb.append("  closeBtn.setAttribute('style','position:absolute;top:5px;right:10px;cursor:pointer;');\n");
        sb.append("  closeBtn.innerText = '×';\n");
        sb.append("  closeBtn.onclick=function(){\n");
        sb.append("    document.body.removeChild(div);\n");
        sb.append("  };\n");
        sb.append("  div.appendChild(closeBtn);\n");

        sb.append("  var closeBtn2 = document.createElement('button');\n");
        sb.append("  closeBtn2.setAttribute('style','color:#fff;background-color:#FF9933;cursor:pointer;border:none;height:30px;width:50px;bottom:5px;right:5px;position:absolute;');\n");
        sb.append("  closeBtn2.innerText = '关闭';\n");
        sb.append("  closeBtn2.onclick=function(){\n");
        sb.append("    document.body.removeChild(div);\n");
        sb.append("  };\n");
        sb.append("  div.appendChild(closeBtn2);\n");

        sb.append("  document.body.appendChild(div);\n");
        sb.append("})()");

        webDriver.executeScript(sb.toString());
    }

    public void prompt(String msg) {
        if (msg == null) {
            msg = "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(function(){\n");
        sb.append("  var div = document.querySelector('__prompt__');\n");
        sb.append("  if(div){\n");
        sb.append("    document.body.removeChild(div);\n");
        sb.append("  };\n");
        sb.append("  div = document.createElement('div');\n");
        sb.append("  div.setAttribute('id','__prompt__');\n");
        sb.append("  div.setAttribute('style','position: fixed;z-index:999999;font-size:16px;left:calc(50% - 200px);color:#fff;width:400px;top:calc(50% - 100px);height:200px;background-color:lightpink;text-align:center;');\n");

        sb.append("  var content = document.createElement('p');\n");
        sb.append("  content.setAttribute('style','margin-top:50px;');\n");
        sb.append("  content.innerHTML = '" + msg.replace("'", "\\'").replace("\n", "<br>") + "';\n");
        sb.append("  var input = document.createElement('input');\n");
        sb.append("  input.style.width='80%';\n");
        sb.append("  div.appendChild(content);\n");
        sb.append("  div.appendChild(input);\n");

//        sb.append("  var closeBtn = document.createElement('span');\n");
//        sb.append("  closeBtn.setAttribute('style','position:absolute;top:5px;right:10px;cursor:pointer;');\n");
//        sb.append("  closeBtn.innerText = '×';\n");
//        sb.append("  closeBtn.onclick=function(){\n");
//        sb.append("    document.body.removeChild(div);\n");
//        sb.append("  };\n");
//        sb.append("  div.appendChild(closeBtn);\n");

//        sb.append("  var closeBtn2 = document.createElement('button');\n");
//        sb.append("  closeBtn2.setAttribute('style','color:#fff;background-color:#FF9933;cursor:pointer;border:none;height:30px;width:50px;bottom:5px;right:5px;position:absolute;');\n");
//        sb.append("  closeBtn2.innerText = '关闭';\n");
//        sb.append("  closeBtn2.onclick=function(){\n");
//        sb.append("    document.body.removeChild(div);\n");
//        sb.append("  };\n");
//        sb.append("  div.appendChild(closeBtn2);\n");

        sb.append("  var okBtn = document.createElement('button');\n");
        sb.append("  okBtn.setAttribute('style','color:#fff;background-color:#FF9933;cursor:pointer;border:none;height:30px;width:50px;bottom:5px;right:5px;position:absolute;');\n");
        sb.append("  okBtn.innerText = '确定';\n");
        sb.append("  okBtn.onclick=function(){\n");
        sb.append("    div.style.display='none';\n");
        sb.append("  };\n");
        sb.append("  div.appendChild(okBtn);\n");

        sb.append("  document.body.appendChild(div);\n");
        sb.append("})()");

        webDriver.executeScript(sb.toString());
    }


    @Override
    public Capabilities getCapabilities() {
        return webDriver.getCapabilities();
    }

    @Override
    public Object executeScript(String s, Object... objects) {
        return webDriver.executeScript(s, objects);
    }

    @Override
    public Object executeAsyncScript(String s, Object... objects) {
        return webDriver.executeAsyncScript(s, objects);
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> outputType) throws WebDriverException {
        return webDriver.getScreenshotAs(outputType);
    }

    @Override
    public void get(String s) {
        webDriver.get(s);
    }

    @Override
    public String getCurrentUrl() {
        return webDriver.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return webDriver.getTitle();
    }

    @Override
    public List<WebElement> findElements(By by) {
        return webDriver.findElements(by);
    }

    @Override
    public WebElement findElement(By by) {
        return webDriver.findElement(by);
    }

    @Override
    public String getPageSource() {
        return webDriver.getPageSource();
    }

    @Override
    public void close() {
        webDriver.close();
    }

    @Override
    public void quit() {
        webDriver.quit();
    }

    @Override
    public Set<String> getWindowHandles() {
        return webDriver.getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return webDriver.getWindowHandle();
    }

    @Override
    public TargetLocator switchTo() {
        return webDriver.switchTo();
    }

    @Override
    public Navigation navigate() {
        return webDriver.navigate();
    }

    @Override
    public Options manage() {
        return webDriver.manage();
    }


    @Override
    public void perform(Collection<Sequence> collection) {
        webDriver.perform(collection);
    }

    @Override
    public void resetInputState() {
        webDriver.resetInputState();
    }


}
