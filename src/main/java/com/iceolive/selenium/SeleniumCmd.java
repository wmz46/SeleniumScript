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
    private String target;
    private String value;
    private Integer timeout;
    private List<SeleniumCmd> thenCommands;
    private List<SeleniumCmd> elseCommands;
    private List<SeleniumCmd> repeatCommands;
    private String statement;

    public boolean isCommand() {
        return this.command != null;
    }

    public boolean isSetCmd(){
        return "set".equals(this.command);
    }
    public boolean isExecCmd(){
        return "exec".equals(this.command);
    }
    public boolean isWaitCmd() {
        return "wait".equals(this.command);
    }
    public boolean isRepeatCmd(){
        return "repeat".equals(this.command);
    }
    public boolean isWhenCmd(){return "when".equals(this.command);}

    public SeleniumCmd(String line) {
        if(Pattern.matches("^\\s*//.*?",line)){
            return;
        }
        Pattern pattern = Pattern.compile("^\\s*([^\\s]+)(\\s+(('(.*?)')|([^\\s]+))|)(\\s+(('(.*?)')|([^\\s]+))|)(\\s+(('(.*?)')|([^\\s]+))|)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            this.command = matcher.group(1);
            if (matcher.groupCount() > 5) {
                if (matcher.group(6) == null) {
                    this.target = matcher.group(5);
                } else {
                    this.target = matcher.group(6);
                }
            }

            if (matcher.groupCount() > 10) {
                if (matcher.group(11) == null) {
                    this.value = matcher.group(10);
                } else {
                    this.value = matcher.group(11);
                }
            }
            this.timeout = 3;
            if (matcher.groupCount() > 15) {
                if (matcher.group(16) != null) {
                    this.timeout = Integer.parseInt(matcher.group(16));
                } else  if (matcher.group(15) != null) {
                    this.timeout = Integer.parseInt(matcher.group(15));
                }
            }
        }
    }
}
