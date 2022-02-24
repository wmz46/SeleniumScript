package com.iceolive.selenium;

import lombok.Data;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wangmianzhe
 */
@Data
public class SeleniumCmd {
    private String command;
    private String arg1;
    private String arg2;
    private String arg3;
    private String arg4;
    private List<SeleniumCmd> thenCommands;
    private List<SeleniumCmd> elseCommands;
    private List<SeleniumCmd> repeatCommands;
    private String statement;
    private String sqlStatement;
    private String line;

    public boolean isCommand() {
        return this.command != null;
    }

    public boolean isSetCmd() {
        return "set".equals(this.command) || "setAsync".equals(this.command);
    }

    public boolean isExecCmd() {
        return "exec".equals(this.command) || "execAsync".equals(this.command);
    }
    public boolean isWinCmd(){
        return "cmd".equals(this.command);
    }

    public boolean isWaitCmd() {
        return "wait".equals(this.command);
    }

    public boolean isRepeatCmd() {
        return "repeat".equals(this.command);
    }
    public boolean isQuerySql(){
        return "querySql".equals(this.command);
    }
    public boolean isExecSql(){
        return "execSql".equals(this.command);
    }

    public boolean isWhenCmd() {
        return "when".equals(this.command);
    }

    public boolean isPromptCmd() {
        return "prompt".equals(this.command);
    }

    public SeleniumCmd(String line) {
        this.line = line;
        if (Pattern.matches("^\\s*//.*?", line)) {
            return;
        }
        // UTF-8 byte order mark (EF BB BF)
        if (line.startsWith("\uFEFF")) {
            line = line.substring(1);
        }
        Pattern pattern = Pattern.compile("^\\s*([^\\s]+)(\\s+(('(.*?)')|([^\\s]+))|)(\\s+(('(.*?)')|([^\\s]+))|)(\\s+(('(.*?)')|([^\\s]+))|)(\\s+(('(.*?)')|([^\\s]+))|)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            this.command = matcher.group(1);
            int g = 5;
            int i = 1;
            if (matcher.groupCount() > g*i) {
                if (matcher.group(g*i+1) == null) {
                    this.arg1 = matcher.group(g*i);
                } else {
                    this.arg1 = matcher.group(g*i+1);
                }
            }
            i++;
            if (matcher.groupCount() > g*i) {
                if (matcher.group(g*i+1) == null) {
                    this.arg2 = matcher.group(g*i);
                } else {
                    this.arg2 = matcher.group(g*i+1);
                }
            }
            i++;
            if (matcher.groupCount() > g*i) {
                if (matcher.group(g*i+1) == null) {
                    this.arg3 = matcher.group(g*i);
                } else {
                    this.arg3 = matcher.group(g*i+1);
                }
            }
            i++;
            if (matcher.groupCount() > g*i) {
                if (matcher.group(g*i+1) == null) {
                    this.arg4 = matcher.group(g*i);
                } else {
                    this.arg4 = matcher.group(g*i+1);
                }
            }
        }
    }

    @Override
    public String toString() {
        return this.line;
    }
}
