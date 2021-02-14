# SeleniumScript
基于selenium-java的自定义脚本语言工具类
## 一、工具类介绍
ChromeWebDriver 
### 1.构造方法
只有一个入参，为chromedriver.exe的完整路径。请和本机的chrome版本相一致。    
下载地址：http://npm.taobao.org/mirrors/chromedriver
### 2.执行脚本
```
//执行外部脚本，path为文件路径
boolean runFromFile(String path);
//执行字符串脚本
boolean run(String cmd);
```
## 二、目录说明
- tests目录为测试脚本目录    
- webdriver为驱动程序目录，当前驱动为88.0.4324.96   
## 三、脚本语法说明：
- 第一个参数为指令
- 其他行内参数请用单引号包裹，如参数没有包含空格，也可省略单引号。
- js脚本请使用 \<script> 和\</script>包裹，且需独占一行。
- begin,then,else,end为包裹多条指令用，需独占一行，
### 1.打开页面
```
//第二个参数为网址
open 'http://www.baidu.com'
```
### 2.文本框输入内容
```
//第二个参数为css选择器，同document.querySelector
//第三个参数为输入的值，如需清除值，请使用clear命令
type '#username' 'wangmainzhe'
```
### 3.清除文本框内容
```
//第二个参数为css选择器，同document.querySelector
clear '#username'
```
### 4.点击按钮
```
//第二个参数为css选择器，同document.querySelector
click '#btn'
```
### 5.拖动元素
```
//第二个参数为css选择器，同document.querySelector
//第三个参数为拖动的位置
drag '#username' '300,0'
```
### 6.滚动条
```
//第二个参数为滚动的坐标位置。
scroll '0,1314'
```
### 7.切进iframe
如果你需要控制的元素在iframe里面，你必须执行切换才可操作他们。
```
//第二个参数为iframe的id或name，也可以用数字表示第几个。
//如果不带第二个参数,则切回主文档
switch 'frame';
```
### 8.执行js
```
exec
<script>
 alert(1);
</script>
```
### 9.存储值
必须有return，return的值将会存储在map中。    
如要在\<script>\</script>中获取设置过的值，可以通过argument[0][key]获取。

```
//name1 为存储的key
set name1
<script>
    return 1;
</script>
```
### 10.强制等待
```
//第二个参数为等待的秒数,这里可以为小数，如0.5表示等待0.5秒
sleep 2
```
### 11.等待并执行操作（目前只支持两种等待）
该指令不一定需要带then和else操作。
```
//等待元素可见
//第二个参数为css选择器，同document.querySelector
//第三个参数固定visible
//第四个参数为超时的秒数，必须为整数秒
wait '#username' visible 3
//then里面的指令为元素出现后才执行的操作
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
### 12.逻辑判断
when后面必须紧跟着\<script>\</script>,然后才跟着then,else,end。else如果没有内容可省略。    
js脚本必须有return，且返回值为boolean型。
```
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
### 13.循环指令
repeat后面必须紧跟着\<script>\</script>,然后才跟着begin,end。    
js脚本必须有return，且返回值为boolean型,返回值为false时退出循环    
```
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
### 14.页面提示语
```
alert '提示内容'
```
### 15.将网页下载存储值
```
//第二个参数为存储值的key，值必须为对象数组，即[{a:1,b:2},{a:3,b:4}]。
//第三个参数为保存的文件名，文件名暂不支持变量
saveCsv list list.csv
//第二个参数为存储值的key，值可以为对象也可以为数组。
//第三个参数为保存的文件名，文件名暂不支持变量
saveJson obj obj.json
```
