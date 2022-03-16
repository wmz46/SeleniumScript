import CodeMirror from "codemirror";

"use strict";

CodeMirror.defineMode("seleniumscript", function (config, parserConfig) {
  var jsonldMode = parserConfig.jsonld;
  var isOperatorChar = /[+\-*&%=<>!?|~^@]/;

  function parseWords(str) {
    var obj = {},
      words = str.split(" ");
    for (var i = 0; i < words.length; ++i) obj[words[i]] = true;
    return obj;
  }

  // 关键字
  const list = ['open', 'type', 'clear', 'click', 'enter', 'drag', 'scroll', 'switch', 'exec', 'execAsync',
    'set', 'setAsync', 'sleep', 'wait', 'when', 'repeat', 'alert', 'saveCsv', 'saveJson',
    'log', 'screenshot', 'stop', 'keydown', 'newHar', 'endHar', 'maximize', 'loadExcel',
    'setConn', 'querySql', 'execSql', 'cmd', 'wscript', '#headless', 'newStw', 'endStw',
    'win32_getByTitle', 'win32_getAllByPID', 'win32_getChildren', 'win32_getTitle',
    'win32_setTopMost', 'win32_showWindow', 'win32_getPID', 'win32_getDesktop', 'win32_screenshot',
    'begin', 'then', 'else', 'end',
    '<script>', '</script>', '<sql>', '</sql>']
  var keywords = parseWords(list.join(' '));
  var type, content;

  function ret(tp, style, cont) {
    type = tp;
    content = cont;
    return style;
  }

  function tokenBase(stream, state) {
    var beforeParams = state.beforeParams;
    state.beforeParams = false;
    var ch = stream.next();

    if (ch == '"' || ch == "'") {
      state.tokenize = tokenString(ch);
      return state.tokenize(stream, state);
    } else if (ch == "." && stream.match(/^\d[\d_]*(?:[eE][+\-]?[\d_]+)?/)) {
      return ret("number", "number");
    } else if (ch == '[') {
      stream.skipTo(']');
      stream.eat(']');
      return ret("string", "string");
    } else if (/\d/.test(ch)) {
      stream.eatWhile(/[\w\.]/);
      return "number";
    } else {
      stream.eatWhile(/[\w\$_{}\xa1-\uffff]/);
      var word = stream.current();
      if (keywords && keywords.propertyIsEnumerable(word)) {
        state.beforeParams = true;
        return "keyword";
      }

      return null;
    }
  }

  function tokenString(quote) {
    return function (stream, state) {
      var escaped = false,
        next;
      if (jsonldMode && stream.peek() == "@" && stream.match(isJsonldKeyword)) {
        state.tokenize = tokenBase;
        return ret("jsonld-keyword", "meta");
      }
      while ((next = stream.next()) != null) {
        if (next == quote && !escaped) break;
        escaped = !escaped && next == "\\";
      }
      if (!escaped) state.tokenize = tokenBase;
      return ret("string", "string");
    };
  }

  return {
    startState: function () {
      return {
        tokenize: tokenBase,
        beforeParams: false,
        inParams: false
      };
    },
    token: function (stream, state) {
      if (stream.eatSpace()) return null;
      return state.tokenize(stream, state);
    }
  };

});
const getAllVariables = (editor, endLine) => {
  const list = []
  for (let i = endLine - 1; i >= 0; i--) {
    let line = editor.getLine(i)
    let variable = line.match(/^\s*(set|wscript|cmd|setAsync|endHar|endStw|newStw|querySql|execSql|win32_getByTitle|win32_getAllByPID|win32_getChildren|win32_getPID|win32_getDesktop)\s+([^\s]*)\s*/)?.[2]
    if (variable) {
      list.push(variable)
    }
    variable = line.match(/^\s*execSql\s+[^\s]+\s+[^\s]+\s+([^\s+]+)/)?.[1]
    if (variable) {
      list.push(variable)
    }
  }
  return list
}
CodeMirror.registerHelper("hint", "seleniumscript", function (editor) {

  const cursor = editor.getCursor();
  const end = cursor.ch;
  const line = editor.getLine(cursor.line);
  let inTag = false;
  let tagType = ''
  for (let i = cursor.line - 1; i >= 0; i--) {
    if (editor.getLine(i).match(/^\s*(<\/script>|<\/sql>)/)) {
      inTag = false;
      tagType = ''
      break;
    } else if (editor.getLine(i).match(/^\s*<script>/)) {
      inTag = true;
      tagType = 'script'
      break;
    } else if (editor.getLine(i).match(/^\s*<sql>/)) {
      inTag = true;
      tagType = 'sql'
      break;
    } else {
      tagType = ''
    }
  }
  if (!inTag) {
    let str = line.substring(0, end).match(/^\s*([#a-zA-Z]+)$/)?.[1]
    if (str) {
      //指令
      const start = end - str.length;
      const list = [
        'begin', 'then', 'else', 'end',
        'open', 'type', 'clear', 'click', 'enter', 'drag', 'scroll', 'switch', 'exec', 'execAsync',
        'set', 'setAsync', 'sleep', 'wait', 'when', 'repeat', 'alert', 'saveCsv', 'saveJson',
        'log', 'screenshot', 'stop', 'keydown', 'newHar', 'endHar', 'maximize', 'loadExcel',
        'setConn', 'querySql', 'execSql', 'cmd', 'wscript', '#headless', 'newStw', 'endStw',
        'win32_getByTitle', 'win32_getAllByPID', 'win32_getChildren', 'win32_getTitle',
        'win32_setTopMost', 'win32_showWindow', 'win32_getPID', 'win32_getDesktop', 'win32_screenshot'
      ]
      return { list: list.filter(m => m.toLowerCase().indexOf(str.toLowerCase()) == 0), from: CodeMirror.Pos(cursor.line, start), to: CodeMirror.Pos(cursor.line, end) }
    } else {
      //开始标签
      let str = line.substring(0, end).match(/^\s*(<[a-zA-Z]*)$/)?.[1]
      if (str) {
        const start = end - str.length;
        const list = ['<script>', '<sql>']
        return { list: list.filter(m => m.toLowerCase().indexOf(str.toLowerCase()) == 0), from: CodeMirror.Pos(cursor.line, start), to: CodeMirror.Pos(cursor.line, end) }
      }
      //变量
      str = line.substring(0, end).match(/(%[^\s%]*)$/)?.[1]
      if (str) {
        const list = getAllVariables(editor, cursor.line)
        for (let i = 0; i < list.length; i++) {
          list[i] = "%" + list[i] + "%"
        }
        const start = end - str.length;
        return { list: list.filter(m => m.indexOf(str) == 0), from: CodeMirror.Pos(cursor.line, start), to: CodeMirror.Pos(cursor.line, end) }
      }
      //连接变量
      str = line.substring(0, end).match(/^\s*(querySql|execSql)\s*[^\s]+\s+$/)
      if (str) {
        const list = [];
        const start = end;
        for (let i = cursor.line - 1; i >= 0; i--) {
          let line = editor.getLine(i)
          let variable = line.match(/^\s*setConn\s+([^\s]*)\s*/)?.[1]
          if (variable) {
            list.push(variable)
          }
        }
        return { list: list, from: CodeMirror.Pos(cursor.line, start), to: CodeMirror.Pos(cursor.line, end) }
      }
      return null;
    }
  } else {
    //结束标签
    let str = line.substring(0, end).match(/^\s*((<|<\/)[a-zA-Z]*)$/)?.[1]
    if (str) {
      const start = end - str.length;
      if (tagType == 'script') {
        return { list: ['<\/script>'], from: CodeMirror.Pos(cursor.line, start), to: CodeMirror.Pos(cursor.line, end) }
      } else if (tagType == 'sql') {
        return { list: ['<\/sql>'], from: CodeMirror.Pos(cursor.line, start), to: CodeMirror.Pos(cursor.line, end) }
      } else {
        return null
      }
    } else {
      if (tagType == 'script') {
        //变量
        let str = line.substring(0, end).match(/(_[\$a-z]*)$/)?.[1]
        if (str) {
          const start = end - str.length;
          const list = ['_$cb', '_$map']
          return { list: list.filter(m => m.indexOf(str) == 0), from: CodeMirror.Pos(cursor.line, start), to: CodeMirror.Pos(cursor.line, end) }
        }
        //变量
        let match = line.substring(0, end).match(/_\$map\.([_\$a-zA-Z0-9]*)$/)
        if (match) {
          let str = match?.[1] || ''
          const list = getAllVariables(editor, cursor.line)
          const start = end - str.length;
          return { list: list.filter(m => str.length == 0 || m.indexOf(str) == 0), from: CodeMirror.Pos(cursor.line, start), to: CodeMirror.Pos(cursor.line, end) }
        }
        //document
        match = line.substring(0, end).match(/document\.([a-zA-Z]*)$/)
        if (match) {
          let str = match?.[1] || ''
          const list = ['querySelector', 'querySelectorAll', 'getElementById']
          const start = end - str.length;
          return { list: list.filter(m => str.length == 0 || m.indexOf(str) == 0), from: CodeMirror.Pos(cursor.line, start), to: CodeMirror.Pos(cursor.line, end) }

        }
        return null;
      } else {
        return null;
      }
    }

  }
});

CodeMirror.defineMIME("text/seleniumscript", "seleniumscript");

