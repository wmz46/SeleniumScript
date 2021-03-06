# SeleniumScript
基于selenium-java的自定义脚本语言工具类
## 当前最新版本
```xml
 <dependency>
    <groupId>com.iceolive</groupId>
    <artifactId>selenium-script</artifactId>
    <version>0.0.9</version>
</dependency>
```
## 一、工具类介绍
ChromeWebDriver 
### 1.构造方法
只有一个入参，为chromedriver.exe的完整路径。请和本机的chrome版本相一致。    
chromedriver下载地址：http://npm.taobao.org/mirrors/chromedriver
### 2.主要方法
```
//执行外部脚本，path为文件路径
void runFromFile(String path);
//执行字符串脚本
void run(String cmd);
```
### 3.测试脚本
```js
set keyword www.iceolive.com
open http://www.baidu.com
type #kw %keyword%
click #su
sleep 2
click '.result a'
set a
<script>
    return 0
</script>
repeat 5
begin
    set a
    <script>
        return arguments[0]['a']+1;
    </script>
end
```
### 4.测试代码
```java
   //杀掉所有chromedriver进程
   ChromeUtil.killChromeDriver();
   ChromeWebDriver webDriver = new ChromeWebDriver(System.getProperty("user.dir") + "\\webdriver\\chromedriver.exe");
   try{
       webDriver.runFromFile(System.getProperty("user.dir") +"\\tests\\test.txt");   
       System.out.println("执行完毕");
   }catch(Exception e){
       System.out.println("执行出错:"+e.toString());
   }

   //打印所有的变量
   System.out.println(webDriver.getVariableMap());
   webDriver.close();
   webDriver.quit();

```
## 二、可执行jar包
releases提供了一个可执行jar包的下载，也可通过clone本项目，通过mvn package自行打包。
### 使用方法
下载SeleniumScript.jar后，命令行执行    
注意：脚本文件编码请使用utf-8，否则中文将会乱码          
```cmd
:: 脚本后缀不必是txt，这里只是举个例子
java -jar SeleniumScript.jar -s "D://你的测试脚本.txt"
:: 或者
java -jar SeleniumScript.jar -script "D://你的测试脚本.txt"
:: 指定驱动路径
java -jar SeleniumScript.jar -script "D://你的测试脚本.txt" -driver "D://chromedriver.exe"
:: 以websocket服务启动,当需执行脚本非本地文件存储时使用。
java -jar SeleniumScript.jar -ws
```

## 三、目录说明
- tests目录为测试脚本目录，里面附带了一个测试脚本       
## 四、开发背景
selenium是个强大的自动化测试工具，但是我还是想让它和js脚本语言一样，支持解释执行。并且我希望它的语法还能更简单一些。    
为了支持我在运行时修改脚本，无需重新编译，并且减少代码的编写量，所以开发了这样看起来有点蹩脚的脚本语言。
## 五、脚本语法说明：
- 第一个参数为指令
- 其他行内参数请用单引号包裹，如参数没有包含空格，也可省略单引号。
- js脚本请使用 \<script> 和\</script>包裹，且标签需独占一行。
- begin,then,else,end为包裹多条指令用，需独占一行，
- 行内除了第一个参数外，其他参数均可使用`%变量名%`动态赋值，本质都是替换字符串。
### 1.打开页面
```js
//第二个参数为网址
open 'http://www.baidu.com'
```
### 2.文本框输入内容
```js
//第二个参数为css选择器，同document.querySelector
//第三个参数为输入的值，如需清除值，请使用clear命令
type '#username' 'wangmainzhe'
```
### 3.清除文本框内容
```js
//第二个参数为css选择器，同document.querySelector
clear '#username'
```
### 4.点击按钮
```js
//第二个参数为css选择器，同document.querySelector
click '#btn'
```
### 5.触发回车
```js
enter '#username'
```
### 6.拖动元素
```js
//第二个参数为css选择器，同document.querySelector
//第三个参数为拖动的位置
drag '#username' '300,0'
```
### 7.滚动条
```js
//第二个参数为滚动的坐标位置。
scroll '0,1314'
```
### 8.切进iframe
如果你需要控制的元素在iframe里面，你必须执行切换才可操作他们。
```js
//第二个参数为iframe的id或name，也可以用数字表示第几个。
//如果不带第二个参数,则切回主文档
switch 'frame';
```
### 9.执行js
```js
exec
<script>
 alert(1);
</script>
```
### 10.存储值
必须有return，return的值将会存储在map中。    
如要在\<script>\</script>中获取设置过的值，可以通过argument[0][key]获取。
```js
//字符串赋值
//a 为存储的key
set a 123
```
```js
//复杂类型赋值，支持object,array,number,string
//a 为存储的key
set a
<script>
    return 1;
</script>
```
### 11.强制等待
```js
//第二个参数为等待的秒数,这里可以为小数，如0.5表示等待0.5秒
sleep 2
```
### 12.等待并执行操作（目前只支持三种等待）
该指令不一定需要带then和else操作。
```js
//等待元素可见
//第二个参数为css选择器，同document.querySelector
//第三个参数固定为visible 
//第四个参数为超时的秒数，必须为整数秒
wait '#username' visible 3
//then里面的指令为元素出现后才执行的操作
then
  click '#btn'
//else里面的指令为超时元素还未出现时才执行的操作
else
  click '#btn2'
end 

//等待元素可见
//第二个参数为css选择器，同document.querySelector
//第三个参数固定为invisible 
//第四个参数为超时的秒数，必须为整数秒
wait '#username' invisible 3
//then里面的指令为元素不可见后才执行的操作
then
  click '#btn'
//else里面的指令为超时元素还未出现时才执行的操作
else
  click '#btn2'
end 

//等待url跳转到指定网页
//第二个参数为url地址，可以为正则表达式
//第三个参数固定url
//第四个参数为超时的秒数，必须为整数秒
wait 'http://www.baidu.com' url 3
//then里面的指令为url跳转完成才执行的操作
then
  click '#btn'
//else里面的指令为超时url还未跳转到指定页面才执行的操作
else
  click '#btn2'
end
```
### 13.逻辑判断
when后面必须紧跟着\<script>\</script>,然后才跟着then,else,end。else如果没有内容可省略。    
js脚本必须有return，且返回值为boolean型。
```js
set flag
<script>
  return true
</script>
when
<script>
    return argument[0]['flag']
</script>
//when里面为当true时执行的指令
then
    click '#btn'
//else里面为当false时执行的指令
else
    click '#btn2'
end
```
### 14.循环指令
- 指定循环次数    
当指定循环次数时，可不添加\<script>\</script>控制脚本，当然也可以使用脚本，当脚本return false则可提前退出循环。    
注意：脚本判断在执行循环前执行。
```js
repeat 10
begin
  click #nextBtn
end
```
- 不指定循环次数
repeat后面必须紧跟着\<script>\</script>,然后才跟着begin,end。    
js脚本必须有return，且返回值为boolean型,返回值为false时退出循环     
注意：脚本判断在执行循环前执行。     
```js
set flag
<script>
  return 1
</script>
repeat
<script>
    return argument[0]['flag']<5
</script>
begin
   click '#btn' 
   set flag
   <script>
     return argument[0]['flag']+1;
   </script>
end
```
### 15.页面提示语
```js
alert '提示内容'
```
### 16.网页下载存储值的文件，支持json和csv
```js
//第二个参数为存储值的key，值必须为对象数组，即[{a:1,b:2},{a:3,b:4}]。
//第三个参数为保存的文件名，文件名暂不支持变量
saveCsv list list.csv
//第二个参数为存储值的key，值可以为对象也可以为数组。
//第三个参数为保存的文件名，文件名暂不支持变量
saveJson obj obj.json
```
### 17.截图
```js
//第二个参数为要截图的元素
//第三个参数为保存的文件路径,图片格式应为png，也可保存为pdf
screenshot body 1.png
```
### 18.日志
```js
//第二个参数为日志内容
log '程序启动'
```
### 19.停止
```js
//不继续执行
stop
```
### 20.模拟按键
```js
//目前支持 f5,home,end
keydown f5
//一般用来触发下拉翻页
keydown end
keydown home
```
### 21.newHar
```js
//需写在open前，用于获取请求日志，只有当启动BrowserMobProxy代理才生效
newHar
```

### 20.endHar
```js
//需写在open后，获取请求日志，并赋值到logs中。格式[{url:'xxx',method:'get',content:'xxx'},...]
endHar logs
```
### 21.最大化
```js
maximize
```
### 22.读取excel
```js
//读取excel数据，并赋值到list,对象数组，对象所有字段类型均为字符串，excel首行为标题行
loadExcel 'D://1.xlsx' list
```
### 23.prompt
else 为超时时的操作。
该指令不一定需要带then和else操作。
```js
//弹窗输入框，用户输入后，点击确定，将输入值赋给变量a，等待超时时间60秒 
prompt a 请输入网址 60
then
    alert hello
else
    alert timeout
end
```