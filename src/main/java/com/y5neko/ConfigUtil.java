package com.y5neko;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigUtil {
    static final String rootPath = System.getProperty("user.home") + "/.xfqconfig/";
    static final File CONFIG_FILE = new File(rootPath + "config.json");

    public static void saveToken(String token, String username, String password) {
        JSONObject obj = new JSONObject();
        obj.put("user_token", token);
        obj.put("username", username);
        obj.put("password", password);
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            writer.write(obj.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadToken() {
        if (!CONFIG_FILE.exists()) return null;
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JSONObject obj = JSON.parseObject(reader, JSONObject.class);
            return obj.getString("user_token");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String loadUsername() {
        if (!CONFIG_FILE.exists()) return null;
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JSONObject obj = JSON.parseObject(reader, JSONObject.class);
            return obj.getString("username");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String loadPassword() {
        if (!CONFIG_FILE.exists()) return null;
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JSONObject obj = JSON.parseObject(reader, JSONObject.class);
            return obj.getString("password");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
