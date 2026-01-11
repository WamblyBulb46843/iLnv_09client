package dev.iLnv_09.hwid;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.Socket;

public class Client {
    PrintWriter writer;
    Socket socket;
    public void connect() {
        try {
            socket = new Socket("59.110.167.55", 5233);
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            System.out.println("Connect Success");
            sendHWID();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendHWID() {
        String message = HWID.getHWID();
        writer.println(message);
        System.out.println("Send: " + message);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String response = reader.readLine();
            System.out.println("Return: " + response);

            if (response != null && "SUCCESS".equals(response)) {
                System.out.println("验证成功，正在启动应用程序...");
                // 验证成功，继续执行
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