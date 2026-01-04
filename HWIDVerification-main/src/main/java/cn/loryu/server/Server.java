package cn.loryu.server;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private ServerSocket serverSocket;
    // Use a thread-safe Map for HWIDs and their notes for safe reloads
    private volatile Map<String, String> hwidMap = new ConcurrentHashMap<>();
    private WatchService watchService;
    private Thread fileWatcherThread;
    private static final String TOKEN_SECRET = "KzP6s9v$B&E)H@McQfTjWnZr4u7x!A%D*G-JaNdRgUkXp2s5v8y/B?E(H+KbPeSh"; // IMPORTANT: Change this to a long, random string
    private final ServerGUI gui; // Reference to the GUI

    public Server(ServerGUI gui) {
        this.gui = gui;
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);

            System.out.println("服务器已在端口启动: " + port);
            loadHwids();
            startFileWatcher();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    new ClientHandler((Socket) serverSocket.accept()).start();
                } catch (IOException e) {
                    // This exception is expected when the socket is closed by the stop() method
                    System.out.println("服务器套接字关闭或接受时出错。正在关闭。");
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("无法在端口 " + port + " 上启动服务器: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (gui != null) {
                gui.serverStopped();
            }
        }
    }

    public void stop() {
        System.out.println("正在停止服务器...");
        try {
            if (watchService != null) {
                watchService.close();
            }
            if (fileWatcherThread != null) {
                fileWatcherThread.interrupt();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("服务器已停止。");
        } catch (IOException e) {
            System.out.println("停止服务器时出错: " + e.getMessage());
        }
    }

    private void startFileWatcher() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get("HWIDServer").toAbsolutePath();
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            fileWatcherThread = new Thread(() -> {
                try {
                    WatchKey key;
                    while ((key = watchService.take()) != null && !Thread.currentThread().isInterrupted()) {
                        // Add a small delay to handle rapid/duplicate file system events
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        for (WatchEvent<?> event : key.pollEvents()) {
                            Path changed = (Path) event.context();
                            if (changed.toString().equals("HWID.txt")) {
                                System.out.println("检测到HWID.txt文件已修改。");
                                reloadHwids();
                            }
                        }
                        key.reset();
                    }
                } catch (InterruptedException | ClosedWatchServiceException e) {
                    System.out.println("文件监视服务已停止。");
                }
            });
            fileWatcherThread.setDaemon(true);
            fileWatcherThread.start();
            System.out.println("已启动对HWID.txt文件的自动热重载监控。");
        } catch (IOException e) {
            System.out.println("无法启动HWID文件监视器: " + e.getMessage());
        }
    }

    private synchronized void reloadHwids() {
        System.out.println("正在热重载HWID列表...");
        loadHwids();
        if (gui != null) {
            SwingUtilities.invokeLater(() -> gui.updateHwidList(getHwidMap()));
        }
    }

    public void loadHwids() {
        Map<String, String> newHwids = new ConcurrentHashMap<>();
        File hwidFile = new File("HWIDServer/HWID.txt");
        if (!hwidFile.exists()) {
            log("HWID.txt 文件不存在。正在创建一个空文件。");
            try {
                hwidFile.getParentFile().mkdirs();
                hwidFile.createNewFile();
            } catch (IOException e) {
                log("创建HWID.txt时出错: " + e.getMessage());
                return;
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(hwidFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split(",", 2);
                    String hwid = parts[0].trim().replaceAll("[^a-zA-Z0-9]", "");
                    String note = (parts.length > 1) ? parts[1].trim() : "";
                    if (!hwid.isEmpty()) {
                        newHwids.put(hwid, note);
                    }
                }
            }
            this.hwidMap = newHwids;
            log("已加载 " + hwidMap.size() + " 个硬件ID。");
        } catch (IOException e) {
            log("无法加载HWID列表。所有客户端的验证都将失败。");
        }
    }

    public synchronized void saveHwids(Map<String, String> hwids) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("HWIDServer/HWID.txt"))) {
            for (Map.Entry<String, String> entry : hwids.entrySet()) {
                writer.println(entry.getKey() + "," + entry.getValue());
            }
            log("HWID列表已保存。");
            reloadHwids(); // Reload to update the server's internal list and the GUI
        } catch (IOException e) {
            log("保存HWID列表时出错: " + e.getMessage());
        }
    }

    public Map<String, String> getHwidMap() {
        return hwidMap;
    }

    private void log(String message) {
        System.out.println(message);
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("HWIDServer/Log.txt", true)))) {
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateToken(String data) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(TOKEN_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            System.out.println("生成令牌时出错: " + e.getMessage());
            return null;
        }
    }

    private boolean validateToken(String data, String token) {
        if (data == null || token == null) {
            return false;
        }
        String expectedToken = generateToken(data);
        return token.equals(expectedToken);
    }

    private class ClientHandler extends Thread {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                clientSocket.setSoTimeout(20000); // 20 seconds timeout
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                String request = in.readLine();
                if (request == null) {
                    System.out.println("收到空请求。正在关闭连接。");
                    return;
                }

                if (request.startsWith("HEARTBEAT:")) {
                    // Handle heartbeat validation
                    System.out.println("收到心跳请求。");
                    String[] parts = request.split(":", 3);
                    if (parts.length == 3) {
                        String hwid = parts[1];
                        String token = parts[2];
                        if (validateToken(hwid, token)) {
                            System.out.println("心跳验证成功 for HWID: " + hwid.substring(0, Math.min(hwid.length(), 16)) + "...");
                            out.println("OK");
                        } else {
                            System.out.println("心跳验证失败 for HWID: " + hwid.substring(0, Math.min(hwid.length(), 16)) + "...");
                            out.println("FAIL");
                        }
                    } else {
                        System.out.println("格式错误的心跳请求。");
                        out.println("FAIL");
                    }
                } else {
                    // Handle initial login
                    String clientHwid = request;
                    log("收到原始请求: '" + clientHwid + "' (length: " + clientHwid.length() + ")");
                    if (clientHwid.length() > 200) { // Increased length for SHA3-512
                        log("收到无效的HWID。正在关闭连接。");
                        return;
                    }
                    log("收到初始登录请求 for HWID: " + clientHwid.substring(0, Math.min(clientHwid.length(), 16)) + "...");

                    if (hwidMap.containsKey(clientHwid)) {
                        String token = generateToken(clientHwid);
                        if (token != null) {
                            log("HWID已验证。正在发送令牌。");
                            out.println("SUCCESS:" + token);
                        } else {
                            log("HWID已验证，但令牌生成失败。");
                            out.println("FAIL:TOKEN_GENERATION_ERROR");
                        }
                    } else {
                        log("HWID验证失败。");
                        log("客户端 HWID: '" + clientHwid + "'");
                        log("服务器 HWID 列表: " + hwidMap.keySet().toString());
                        out.println("FAIL:INVALID_HWID");
                    }
                }

            } catch (IOException e) {
                System.out.println("客户端处理程序中出现异常 (可能是超时): " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("关闭客户端套接字时出现异常: " + e.getMessage());
                }
            }
        }
    }
}