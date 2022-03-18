<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import "codemirror/lib/codemirror.css";
import "./mode/seleniumscript.js";
import "codemirror/addon/hint/show-hint.css";
import "codemirror/addon/hint/show-hint.js";
import "codemirror/addon/hint/anyword-hint.js";
import { ElMessageBox, ElMessage } from "element-plus";
import CodeMirror from "codemirror";
import { Base64 } from "js-base64";
import WS from "./ws/index";
const props = withDefaults(
  defineProps<{ modelValue?: string; height?: string | number }>(),
  {
    modelValue: "",
    height: "500px",
  }
);
const emit = defineEmits(["update:modelValue", "change"]);
//组件id
const id = "ide";
//编辑器内容
const value = ref("");
//避免监听赋值造成死循环
let updateView: boolean = true;
//编辑器内容变动触发事件
const changeValue = (newValue: string) => {
  value.value = newValue;
  updateView = false;
  emit("update:modelValue", value.value);
  emit("change", value.value);
};
//编辑器配置
const cmOptions = {
  mode: "text/seleniumscript",
  hintOptions: {
    completeSingle: false,
  },
  lineNumbers: true,
  indentUnit: 0,
  extraKeys: {
    "Tab": (editor: any) => {
      let content = editor.getValue();
      let arr = content.split("\n");
      let from = editor.listSelections()[0].from().line;
      let to = editor.listSelections()[0].to().line;
      const cursor = editor.getCursor();
      if (from < to) {
        let comment = true;
        for (let i = from; i <= to; i++) {
          arr[i] = '\t' + arr[i];
        }
        let text = arr.join("\n");
        editor.setValue(text);
        editor.setCursor({ line: cursor.line + 1, ch: cursor.ch });
        changeValue(text);
        let scrollInfo = editor.getScrollInfo();
        editor.scrollTo(scrollInfo.left, scrollInfo.top);
      } else {
        editor.replaceSelection('\t');
      }

    },
    "Ctrl-/": (editor: any) => {
      let content = editor.getValue();
      let arr = content.split("\n");
      let from = editor.listSelections()[0].from().line;
      let to = editor.listSelections()[0].to().line;
      const cursor = editor.getCursor();

      let comment = true;
      for (let i = from; i <= to; i++) {
        if (!arr[i].trim().startsWith("//")) {
          comment = false;
          break;
        }
      }
      for (let i = from; i <= to; i++) {
        if (comment) {
          arr[i] = arr[i].trim().substring(2);
        } else {
          arr[i] = `//${arr[i]}`;
        }
      }
      let text = arr.join("\n");
      editor.setValue(text);
      editor.setCursor({ line: cursor.line + 1, ch: cursor.ch });
      changeValue(text);
      let scrollInfo = editor.getScrollInfo();
      editor.scrollTo(scrollInfo.left, scrollInfo.top);
    },
    'Ctrl-Alt-L': (editor: any) => {
      let content = editor.getValue();
      let arr = content.split("\n");
      let indent = 0;
      let scriptIndent = 0;
      let inTag = false;
      for (let i = 0; i < arr.length; i++) {
        let cmd = arr[i].match(/^\s*([\/<a-zA-Z>]+)\s*/)?.[1]
        if (['end', '<\/script>', '<\/sql>'].indexOf(cmd) > -1) {
          if (['<\/script>', '<\/sql>'].indexOf(cmd) > -1) {
            inTag = false
          }
          if (indent > 0) {
            indent--
          }
        }
        if (inTag) {
          if (arr[i].trim().startsWith("}")) {
            if (scriptIndent > 0) {
              scriptIndent--
            }
          }
        }
        let j = 0;
        let str = ""
        while (j < indent) {
          str += "  "
          j++;
        }
        j = 0;
        while (j < scriptIndent) {
          str += '  '
          j++
        }
        arr[i] = str + arr[i].trimStart()
        if (['begin', 'then', 'else', '<script>', '<sql>'].indexOf(cmd) > -1) {
          indent++
        }
        if (['<script>', '<sql>'].indexOf(cmd) > -1) {
          inTag = true
        }
        if (inTag) {
          if (arr[i].trim().endsWith("{")) {
            scriptIndent++
          }
        }
      }
      let text = arr.join("\n");
      editor.setValue(text);
      changeValue(text);
      let scrollInfo = editor.getScrollInfo()
      editor.scrollTo(scrollInfo.left, scrollInfo.top)
    },
  },
};
let editorView = null as any;
//组件初始化
onMounted(() => {
  if (!editorView) {
    const dom = document.getElementById(id) as HTMLTextAreaElement;
    if (dom) {
      const editor = CodeMirror.fromTextArea(dom, cmOptions);
      editor.on("change", () => {
        changeValue(editor.getValue());
      });
      editor.on("inputRead", () => {
        editor.closeHint();
        editor.showHint();
      });
      editor.setSize("auto", props.height);
      editorView = editor;
    }
  }
});
//监听外部值变化
watch(
  () => props.modelValue,
  () => {
    value.value = props.modelValue;
    const editor = editorView;
    if (updateView && editor) {
      editor.setValue(props.modelValue || "");
      setTimeout(() => {
        editor.refresh();
      }, 1);
    }
    updateView = true;
  }
);
const fileRef = ref(null as any);
fileRef;
const openFileHandle = () => {
  fileRef.value.click();
};
const fileChangeHandle = () => {
  var file = fileRef.value.files[0];
  var reader = new FileReader();
  reader.onloadend = function (ev: any) {
    if (ev.target.readyState == FileReader.DONE) {
      const text = ev.target.result;
      editorView.setValue(text);
      changeValue(text);
    }
  };
  // 包含中文内容用utf-8编码
  reader.readAsText(file, "utf-8");
};
const saveFileHandle = () => {
  const blob = new Blob([editorView.getValue()], {
    type: "text/plain;charset=UTF-8",
  });
  const link = document.createElement("a");
  link.href = window.URL.createObjectURL(blob);
  link.download = "script.txt"; // 保存出来的文件名称
  link.click(); // 触发内存数据存到文件的操作
  window.URL.revokeObjectURL(link.href); // 释放内存
};
const openDocument = () => {
  window.open("https://github.com/wmz46/SeleniumScript", "_blank");
};
const ws = new WS("ws://127.0.0.1:10042");
const runScript = () => {
  if (value.value.trim()) {
    ws.open()
      .then(() => {
        ws.send(Base64.encode(JSON.stringify({ script: value.value, proxy: proxy.value ? proxy.value : null })))
        ElMessage.success({
          message: '已发送报文，请耐心等待'
        })

      })
      .catch(() => {
        ElMessageBox.alert(
          '未安装或运行客户端。<a href="https://github.com/wmz46/SeleniumScript/releases/download/v0.4.0/SeleniumScript.zip" target="_blank">点击下载客户端</a><br>下载解压后，请双击运行startup.bat。',
          {
            dangerouslyUseHTMLString: true,
          }
        );
      });
  } else {
    ElMessage.error({
      message: '请输入脚本'
    })
  }
};
const proxy = ref('')
</script>

<template>
  <el-container>
    <el-main>
      <el-form>
        <el-form-item label="代理地址">
          <el-input v-model="proxy" placeholder="代理地址" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="default" @click="openFileHandle">打开脚本</el-button>
          <el-button type="default" @click="saveFileHandle">保存脚本</el-button>
          <el-button type="default" @click="runScript">执行脚本</el-button>
          <el-button type="default" @click="openDocument">在线文档</el-button>
          <input type="file" ref="fileRef" style="display:none" @change="fileChangeHandle()" />
        </el-form-item>
      </el-form>

      <textarea :id="id" :value="modelValue" style="display:none"></textarea>
    </el-main>
  </el-container>
</template>

<style scoped>
</style>