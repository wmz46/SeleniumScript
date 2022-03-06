<script setup lang="ts">
import {
  onMounted,
  ref,
  watch
} from 'vue'
import 'codemirror/lib/codemirror.css'
import './mode/seleniumscript.js'
import 'codemirror/addon/hint/show-hint.css';
import 'codemirror/addon/hint/show-hint.js';
import 'codemirror/addon/hint/anyword-hint.js';
import CodeMirror from 'codemirror'
const props = withDefaults(defineProps<{ modelValue?: string, height?: (string | number) }>(), {
  modelValue: '',
  height: '500px'
})
const emit = defineEmits(['update:modelValue', 'change'])
//组件id
const id = "ide"
//编辑器内容
const value = ref('')
//避免监听赋值造成死循环
let updateView: boolean = false
//编辑器内容变动触发事件
const changeValue = (newValue: string) => {
  value.value = newValue
  updateView = false
  emit('update:modelValue', value.value)
  emit('change', value.value)
}
//编辑器配置
const cmOptions = {
  mode: 'text/seleniumscript',
  hintOptions: {
    completeSingle: false
  },
  lineNumbers: true,
  indentUnit: 0,
  extraKeys: {
    'Ctrl-/': (editor: any) => {
      let content = editor.getValue()
      let arr = content.split('\n')
      let from = editor.listSelections()[0].from().line
      let to = editor.listSelections()[0].to().line
      const cursor = editor.getCursor()

      let comment = true
      for (let i = from; i <= to; i++) {
        if (!arr[i].trim().startsWith('//')) {
          comment = false
          break
        }
      }
      for (let i = from; i <= to; i++) {
        if (comment) {
          arr[i] = arr[i].trim().substring(2)
        } else {
          arr[i] = `//${arr[i]}`
        }
      }
      let text = arr.join('\n')
      editor.setValue(text)
      editor.setCursor({ line: cursor.line + 1, ch: cursor.ch })
      changeValue(text)
      let scrollInfo = editor.getScrollInfo()
      editor.scrollTo(scrollInfo.left, scrollInfo.top)
    }
  }
};
let editorView = null as any
//组件初始化
onMounted(() => {
  if (!editorView) {
    const dom = document.getElementById(id) as HTMLTextAreaElement
    if (dom) {
      const editor = CodeMirror.fromTextArea(dom, cmOptions)
      editor.on('change', () => {
        changeValue(editor.getValue())
      })
      editor.on('inputRead', () => {
        editor.closeHint()
        editor.showHint()
      })
      editor.setSize('auto', props.height)
      editorView = editor
    }
  }

})
//监听外部值变化
watch(() => props.modelValue, () => {
  value.value = props.modelValue
  const editor = editorView
  if (updateView && editor) {
    editor.setValue(props.modelValue || '')
    setTimeout(() => {
      editor.refresh()
    }, 1)
  }
  updateView = true
})
const fileRef = ref(null as any)
fileRef
const openFileHandle = () => {
  fileRef.value.click()
}
const fileChangeHandle = () => {
  var file = fileRef.value.files[0];
  var reader = new FileReader();
  reader.onloadend = function (ev: any) {
    if (ev.target.readyState == FileReader.DONE) {
      const text = ev.target.result
      editorView.setValue(text)
      changeValue(text)
    }
  };
  // 包含中文内容用utf-8编码
  reader.readAsText(file, 'utf-8');
}
const saveFileHandle = () => {
  const blob = new Blob([editorView.getValue()], {
    type: 'text/plain;charset=UTF-8'
  })
  const link = document.createElement('a')
  link.href = window.URL.createObjectURL(blob)
  link.download = 'script.txt' // 保存出来的文件名称
  link.click() // 触发内存数据存到文件的操作
  window.URL.revokeObjectURL(link.href) // 释放内存
}
</script>

<template>
  <div class="btn-group">
    <button type="button" @click="openFileHandle">打开脚本</button>
    <button type="button" @click="saveFileHandle">保存脚本</button>
    <input type="file" ref="fileRef" style="display:none" @change="fileChangeHandle()" />
  </div>
  <textarea :id="id" :value="modelValue" style="display:none"></textarea>
</template>

<style scoped>
.btn-group button:not(:first-child) {
  margin-left: 10px;
}
</style>