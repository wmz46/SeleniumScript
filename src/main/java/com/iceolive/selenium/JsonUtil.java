package com.iceolive.selenium;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wangmianzhe
 */
public class JsonUtil {
    public static JsonNode parse(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(json);
        }  catch (JsonMappingException ex) {
            throw new RuntimeException(ex);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }
    public static Map<String,Object> loadJson(String filepath){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String,Object> map = objectMapper.readValue(new File(filepath), Map.class);
            return map;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    public static void saveJson(String filepath,Map<String,Object> data){
        File file = new File(filepath);
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        if(!file.exists()){
            file.delete();
        }
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            file.createNewFile();
            objectMapper.writeValue(file,data);
        } catch (StreamWriteException e) {
            throw new RuntimeException(e);
        } catch (DatabindException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
