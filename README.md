# SeleniumScript
基于selenium-java的自定义脚本语言工具类
 
## 一、可执行jar包
releases提供了一个可执行jar包的下载，也可通过clone本项目，通过mvn package自行打包。

原先通过中央仓库jar包引用方式由于调用过于麻烦，已废弃。   

只支持windows下运行。
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
:: 默认websocket服务只能本机访问，如需暴露给其他机器使用，请添加-host
java -jar SeleniumScript.jar -ws -host
```

### 测试脚本
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
        return _$map.a +1;
    </script>
end
``` 

## 二、目录说明
- tests目录为测试脚本目录，里面附带了一个测试脚本       
## 三、开发背景
selenium是个强大的自动化测试工具，但是我还是想让它和js脚本语言一样，支持解释执行。并且我希望它的语法还能更简单一些。    
为了支持我在运行时修改脚本，无需重新编译，并且减少代码的编写量，所以开发了这样看起来有点蹩脚的脚本语言。
## 四、脚本语法说明：
- 第一个参数为指令
- 其他行内参数请用单引号包裹，如参数没有包含空格，也可省略单引号。
- js脚本请使用 \<script> 和\</script>包裹，且标签需独占一行。
- begin,then,else,end为包裹多条指令用，需独占一行，
- 行内除了第一个参数外，其他参数均可使用`%变量名%`动态赋值，本质都是替换字符串。支持变量路径，如%list[0].name%，返回值必须是基础类型，不能是对象或数组。
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
### 10.异步执行js
使用_$cb进行回调
```js
execAsync
<script>
 setTimeout(function(){
  _$cb()
 },1000)
</script>
```
### 11.存储值
必须有return，return的值将会存储在map中。    
如要在\<script>\</script>中获取设置过的值，可以通过_$map[key]（推荐）或argument[0][key]（不推荐）获取。
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
### 12.异步存储值
使用_$cb进行回调，参数值将会存储在map中，一般用于在页面fetch接口获取值后进行回调。
```js
setAsync a
<script>
  fetch('http://xxx.xx/api/xxx').then(m=>{return m.text()}).then(m=>{ 
   _$cb(m) 
  })
</script>
```
### 13.强制等待
```js
//第二个参数为等待的秒数,这里可以为小数，如0.5表示等待0.5秒
sleep 2
```
### 14.等待并执行操作（目前只支持三种等待）
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
### 15.逻辑判断
when后面必须紧跟着\<script>\</script>,然后才跟着then,else,end。else如果没有内容可省略。    
js脚本必须有return，且返回值为boolean型。
```js
set flag
<script>
  return true
</script>
when
<script>
    return _$map['flag']
</script>
//when里面为当true时执行的指令
then
    click '#btn'
//else里面为当false时执行的指令
else
    click '#btn2'
end
```
### 16.循环指令
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
    return _$map['flag']<5
</script>
begin
   click '#btn' 
   set flag
   <script>
     return _$map['flag']+1;
   </script>
end
```
- 支持下标及遍历列表

```js
//指定循环次数时获取下标，第一个参数为循环次数，第二个参数为下标变量名，下标从0开始
repeat 3 i
begin
//输出下标
log %i%
end

```
```js
//指定循环次数时获取下标，第一个参数为循环次数，第二个参数为下标变量名，第三个参数为起始坐标，当大于0时，循环次数会减去起始坐标。
repeat 3 i 1
begin
//输出下标
log %i%
end

```
```js
set a
<script>
return [1,2,3]
</script>
//第一个参数可以为列表的变量名，第二个参数为下标变量名，下标从0开始，第三个参数为遍历的元素变量名。
repeat a i b
begin
//输出下标
log  %i%
//输出元素
log %b%
end
```

### 17.页面提示语
```js
alert '提示内容'
```
### 18.网页下载存储值的文件，支持json和csv
```js
//第二个参数为存储值的key，值必须为对象数组，即[{a:1,b:2},{a:3,b:4}]。
//第三个参数为保存的文件名，文件名暂不支持变量
//第四个参数为列名，逗号隔开，列名与存储值的key必须一一对应。不传则自动生成，但顺序不固定。
saveCsv list list.csv '列1,列2'
//第二个参数为存储值的key，值可以为对象也可以为数组。
//第三个参数为保存的文件名，文件名暂不支持变量
saveJson obj obj.json
```
### 19.截图
```js
//第二个参数为要截图的元素
//第三个参数为保存的文件路径,图片格式应为png，也可保存为pdf
screenshot body 1.png
```
### 20.日志
```js
//第二个参数为日志内容
log '程序启动'
```
### 21.停止
```js
//不继续执行
stop
```
### 22.模拟按键
```js
//目前支持 f5,home,end
keydown f5
//一般用来触发下拉翻页
keydown end
keydown home
```
### 23.newHar
```js
//需写在open前，用于获取请求日志，只有当启动BrowserMobProxy代理才生效
newHar
```

### 24.endHar
```js
//需写在open后，获取请求日志，并赋值到logs中。格式[{url:'xxx',method:'get',content:'xxx'},...]
endHar logs
```
### 25.最大化
```js
maximize
```
### 26.读取excel
```js
//读取excel数据，并赋值到list,对象数组，对象所有字段类型均为字符串，excel首行为标题行
loadExcel 'D://1.xlsx' list
```
### 27.prompt
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
### 28.创建数据库连接 
第二个参数为自定义的连接名，和set变量是两套存储容器，不会引起变量名冲突    
第三个参数为数据库连接字符串，参考jdbc，目前支持h2,mysql,sqlserver和达梦    
第四个参数为用户名    
第五个参数为密码    
```js
setConn conn_a 'jdbc:mysql://127.0.0.1:3306/db?serverTimezone=UTC&useSSL=false&characterEncoding=utf-8' root 123456

```
### 29.查询数据库    
只允许执行一个select      
第二个参数为结果集存储的key，结果集类型为对象数组，为了和网页数据交互，时间类型字段会转为字符串，格式统一为"yyyy-MM-dd HH:mm:ss"        
第三个参数为连接名    
sql脚本通过`<sql></sql>`包裹，如有入参变量请使用#{}或${}包裹变量名     

```js
set id 1
querySql list conn_a 
<sql>
select * from tb1 where id = #{id} limit 1 
</sql>
```

如果是动态拼接sql请使用`<script></script>`代替`<sql></sql>`        
```js
set id 1
querySql list conn_a 
<script>
return 'select * from tb1 where id = '+_$map.id+' limit 1' 
</script>
```

### 30.执行sql
     
支持多条sql执行，多条语句请用分号隔开     
当只执行一条sql时，才会返回insert的自增主键           
第二个参数为更新记录数存储的key    
第三个参数为连接名    
第四个参数为insert返回的自增主键存储的key，非必要    
sql脚本通过`<sql></sql>`包裹，如有入参变量请使用#{}或${}包裹变量名    

```js
set name '张三'
execSql i conn_a id 
<sql>
insert into tb1(name) values(#{name})
</sql>
```

如果是动态拼接sql请使用`<script></script>`代替`<sql></sql>`     
```js
set name '张三'
execSql i conn_a id 
<script>
    return "insert into tb1(name) values('"+_$map.name+"')"
</scripit>
```
### 31.执行cmd脚本
第二个参数为结果存储的key，处理结果为批处理打印的字符串,非必要     
`<script></script>`包裹的是执行的命令，如果需调用外部变量，请使用`_$map.xxx`代替，本质为替换字符串
```js
set a www.baidu.com
cmd b
<script>    
ping _$map.a
log %b%
</script>
``` 
### 32.执行wsh脚本
第二个参数为结果存储的key，处理结果为批处理打印的字符串,非必要     
`<script></script>`包裹的是执行的命令，如果需调用外部变量，请使用`_$map.xxx`代替，本质为替换字符串
```js
set a 1
wscript
<script>
    WScript.Echo("_$map.a") 
</script>
```
### 33.win32
```js
//根据窗口标题查找窗口句柄(long)，并存储到第二个参数设置的key中。
win32_getByTitle hwnd 任务管理器
//根据进程pid获取所有窗口句柄(List<long>),并存储到第二个参数设置的key中。
win32_getAllByPID hwnds 11202
//根据窗口句柄获取所有子控件句柄(List<Long>)，并存储到第二个参数设置的key中
win32_getChildren hwnds 134642
//根据窗口句柄获取窗口标题,并存储到第二个参数的key中
win32_getTitle 134642
//根据窗口句柄设置窗口置顶
win32_setTopMost 134642
//控制窗口最大化最小化，第二个参数为窗口句柄，第三个参数 1 正常 2 最小化 3 最大化 或 normal min max
win32_showWindow 134642 3
//根据窗口句柄获取进程id,并存储到第二个参数设置的key中。
win32_getPID pid 134642
//获取桌面句柄（long），并存储到第二个参数的key中
win32_getDesktop hwnd
//根据窗口句柄截图并保存
win32_screenshot 134642 1.jpg
```
### 34.无浏览器窗口模式
启动时检查如果存在以下指令则启动无浏览器窗口模式
```js
#headless
```
### 35.计时器
StopWatch的缩写
```js
//开始计时,此时a存储的是当前的时间戳(long)
newStw a
//结束计时，此时a存储的是时间差，单位毫秒(long)
endStw a
```
### 36.调整浏览器窗口大小
```js
// 浏览器窗口调整为800*600
resize 800,600
````
### 37.访客模式
启动时检查如果存在以下指令则启动访客模式
```js
#guest
```
### 38.获取当前窗口句柄
```js
// 第二个参数为存储的变量名
getWindowHandle id
```
### 39.获取所有窗口句柄
```js
// 第二个参数为存储的变量名
getWindowHandles ids
```
### 40.切换窗口
```js
// 第二个参数为窗口句柄
switchWindow 7AEAE4DD9BA147414900601C0D4CFFCF
```
### 41.持久化变量
```js
//a为持久化的key
//b为变量的key
setStore a b
```
```js
//值需计算的持久化
//a 为存储的key
setStore a
<script>
    return 1;
</script>
```
### 42.获取持久化变量
```js
//a为持久化的key 
//b为变量的key
//c为可选参数，表示默认值。如果没有持久化数据，则返回默认值，只支持字符串默认值
getStore a b c
```
### 43.清除所有本地持久化
```js
clearStore
```
### 44.关闭浏览器
```js
close
```