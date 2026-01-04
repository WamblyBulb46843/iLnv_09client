package dev.iLnv_09.irc;

public interface IRCHandler {
    // 收到消息时调用
    void onMessage(String message);

    // 断开连接时调用
    void onDisconnected(String message);

    // 连接成功时调用
    void onConnected();

    // 获取游戏中的用户名
    String getInGameUsername();

    // 获取IRC的用户名
    String getUsername();
}