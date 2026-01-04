package cn.loryu.client;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Loryu
 * @date 2022/5/27 22:00
 * @description 客户端
 */
public class Client {
    PrintWriter writer;
    Socket socket;

    /**
     * @description 连接到服务器
     */
    public void connect() {
        try {
            socket = new Socket("127.0.0.1", 5233);
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            System.out.println("Connect Success");
            sendHWID();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @description 发送HWID到服务器
     */
    public void sendHWID() {
        String message = HWID.getHWID();
        writer.println(message);
        System.out.println("Send: " + message);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String response = reader.readLine();
            System.out.println("Return: " + response);

            if (response != null && response.startsWith("SUCCESS:")) {
                String token = response.substring("SUCCESS:".length());
                System.out.println("验证成功，正在启动应用程序...");
                launchApplication(token);
            } else {
                // Handle failure: either explicit FAIL or malformed response
                String reason = "未知错误";
                if (response != null && response.startsWith("FAIL:")) {
                    reason = response.substring("FAIL:".length());
                }
                showVerificationFailed(reason);
                System.exit(0);
            }
        } catch (IOException e) {
            System.out.println("Error reading from server: " + e.getMessage());
            showVerificationFailed("与服务器通信时出错"); // Show failure message on communication error
            System.exit(1);
        } finally {
            try {
                if (socket != null) socket.close();
                if (writer != null) writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void launchApplication(String token) {
        // This is where your protected application logic goes.
        // The token is now required to start this part of the application.
        // An attacker can't just remove System.exit(0) anymore.

        // Example: Create a main window and use the token in it.
        // This couples the token to the application's core functionality.
        JFrame frame = new JFrame("主程序 - 许可证: " + token.substring(0, 8) + "...");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        JLabel label = new JLabel("欢迎！您的应用程序已成功验证。", SwingConstants.CENTER);
        frame.getContentPane().add(label);

        // Center the frame on the screen
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Start the heartbeat to ensure the session remains valid
        startHeartbeat(token);
    }

    private void startHeartbeat(String token) {
        String hwid = HWID.getHWID();
        Timer timer = new Timer("HeartbeatTimer", true); // Run as a daemon thread

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("执行心跳验证...");
                try {
                    // Create a new connection for each heartbeat to ensure robustness
                    try (Socket heartbeatSocket = new Socket("127.0.0.1", 5233);
                         PrintWriter writer = new PrintWriter(new OutputStreamWriter(heartbeatSocket.getOutputStream(), "UTF-8"), true);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(heartbeatSocket.getInputStream()))) {

                        String request = "HEARTBEAT:" + hwid + ":" + token;
                        writer.println(request);

                        String response = reader.readLine();
                        if (!"OK".equals(response)) {
                            System.err.println("心跳验证失败。响应: " + response + ". 正在终止应用程序。");
                            JOptionPane.showMessageDialog(null, "会话已失效，应用程序将关闭。", "验证错误", JOptionPane.ERROR_MESSAGE);
                            System.exit(0);
                        } else {
                            System.out.println("心跳验证成功。");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("心跳检查期间发生错误: " + e.getMessage() + ". 正在终止应用程序。");
                    JOptionPane.showMessageDialog(null, "无法连接到验证服务器，应用程序将关闭。", "连接错误", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                }
            }
        }, 30 * 60 * 1000, 30 * 60 * 1000); // Start after 30 minutes, then repeat every 30 minutes
    }


    private void showVerificationFailed(String reason) {
        String hwid = HWID.getHWID();
        boolean copied = copyToClipboard(hwid);
        String message;
        if (copied) {
            message = "硬件ID: " + hwid + "\n\n(已自动复制到剪贴板)\n失败原因: " + reason;
        } else {
            message = "硬件ID: " + hwid + "\n\n(未能自动复制到剪贴板)\n失败原因: " + reason;
        }
        JOptionPane.showMessageDialog(null, message, "验证失败", JOptionPane.ERROR_MESSAGE);
    }

    private boolean copyToClipboard(String text) {
        try {
            StringSelection selection = new StringSelection(text);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
            System.out.println("内容已复制到剪贴板: " + text);
            return true;
        } catch (Exception e) {
            System.err.println("复制到剪贴板失败: " + e.getMessage());
            return false;
        }
    }
}