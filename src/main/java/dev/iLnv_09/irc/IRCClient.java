package dev.iLnv_09.irc;

import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class IRCClient {
    private final Socket socket;
    private final IRCHandler handler;
    public static Object transport;
    private final Map<String, String> userToIGNMap = new HashMap<>(); // 用于存储用户名与 IGN 的映射关系

    private volatile boolean running = true;
    private PrintWriter writer;
    private BufferedReader reader;

    public IRCClient(String host, int port, IRCHandler handler) throws IOException {
        this.handler = handler;
        this.socket = new Socket(host, port);
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        start();
    }

    // 启动客户端并监听消息
    public void start() {
        new Thread(() -> {
            try {
                // 连接成功时回调
                handler.onConnected();

                // 向服务器发送 "username:IGN"
                String username = handler.getUsername();
                String ign = handler.getInGameUsername();
                sendMessage(username + ":" + ign); // 发送用户名和IGN

                // 监听服务器发来的消息
                String line;
                while (running && (line = reader.readLine()) != null) {
                    // 解密
                    String message = new String(Base64.getDecoder().decode(line));
                    
                    // 处理映射关系消息
                    if (message.equals("GET_USERS_REQUEST")) {
                        sendAllUsers(); // 发送所有用户
                    } else {
                        // 检查消息格式，如果是 "username:some_ign" 这种格式，且some_ign看起来像游戏用户名，则认为是用户映射消息
                        String[] parts = message.split(":", 2); // 最多分割为2部分
                        if (parts.length == 2) {
                            String usernameReceived = parts[0];
                            String potentialIgn = parts[1];
                            
                            // 简单判断：如果第二部分较短（如16字符以内）且看起来像用户名，则认为是映射消息
                            if (potentialIgn.length() <= 16 && isValidUserName(potentialIgn)) {
                                userToIGNMap.put(usernameReceived, potentialIgn);  // 保存映射关系
                            }
                        }
                        
                        // 无论如何都要处理消息
                        handler.onMessage(message);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    handler.onDisconnected("错误: " + e.getMessage());
                }
            } finally {
                close();
            }
        }).start();  // 启动新线程
    }

    // 简单判断是否为有效的用户名（字母数字下划线，长度合理）
    private boolean isValidUserName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_]{2,16}$");
    }

    // 获取所有用户与IGN映射
    public void GetUsers() {
        sendMessage("GET_USERS_REQUEST"); // 向服务器请求用户映射
    }

    // 检查用户是否存在
    public boolean isUser(String ign) {
        GetUsers();
        return userToIGNMap.containsValue(ign);
    }

    // 发送所有用户与 IGN 映射
    private void sendAllUsers() {
        for (Map.Entry<String, String> entry : userToIGNMap.entrySet()) {
            sendMessage(entry.getKey() + ":" + entry.getValue());
        }
    }

    // 获取指定用户名的 IGN
    public String getName(String userName) {
        return userToIGNMap.get(userName);
    }

    // 发送消息
    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(Base64.getEncoder().encodeToString(message.getBytes()));
        }
    }

    // 关闭连接
    public void close() {
        if (!running) return;
        running = false;
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        handler.onDisconnected("连接已关闭");
    }
}