package dev.iLnv_09.hwid;

import org.apache.commons.codec.digest.DigestUtils;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;
import java.util.Optional;

public class HWIDClient {
    // HWID相关常量
    private static final int ITERATIONS = 250_000;
    private static final int KEY_LENGTH = 256;
    private static final String PEPPER = "aK9bS2nL5gT8wR6fD1vY4zQ0jP3uE7xI";
    
    // 系统信息相关
    private static final SystemInfo SI = new SystemInfo();
    private static final HardwareAbstractionLayer HAL = SI.getHardware();
    private static final ComputerSystem CS = HAL.getComputerSystem();
    private static final OperatingSystem OS = SI.getOperatingSystem();
    
    // 网络通信相关
    private PrintWriter writer;
    private Socket socket;
    
    // 缓存
    private static String cachedHWID = null;
    
    /**
     * 获取硬件ID（HWID）
     */
    public static String getHWID() {
        if (cachedHWID != null) {
            return cachedHWID;
        }

        String hardwareFingerprint = String.join("|",
                getCPUInfo(),
                getComputerName(),
                getUsername(),
                getBaseboardInfo(),
                getTotalMemory(),
                getTotalDiskCapacity()
        );

        try {
            cachedHWID = generateSecureHWID(hardwareFingerprint + PEPPER);
            return cachedHWID;
        } catch (Exception e) {
            throw new RuntimeException("HWID generation failed", e);
        }
    }
    
    /**
     * 连接到验证服务器
     */
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

    /**
     * 发送HWID到服务器进行验证
     */
    public void sendHWID() {
        String message = getHWID();
        writer.println(message);
        System.out.println("Send: " + message);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String response = reader.readLine();
            System.out.println("Return: " + response);

            if (response != null && "SUCCESS".equals(response)) {
                System.out.println("验证成功，正在启动应用程序...");
                // 验证成功，继续执行
            } else {
                // 处理失败：明确的FAIL或格式错误的响应
                String reason = "未知错误";
                if (response != null && response.startsWith("FAIL:")) {
                    reason = response.substring("FAIL:".length());
                }
                showVerificationFailed(reason);
                System.exit(0);
            }
        } catch (IOException e) {
            System.out.println("Error reading from server: " + e.getMessage());
            showVerificationFailed("与服务器通信时出错");
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

    /**
     * 显示验证失败的对话框
     */
    private void showVerificationFailed(String reason) {
        String hwid = getHWID();
        boolean copied = copyToClipboard(hwid);
        String message;
        if (copied) {
            message = "硬件ID: " + hwid + "\n\n(已自动复制到剪贴板)\n失败原因: " + reason;
        } else {
            message = "硬件ID: " + hwid + "\n\n(未能自动复制到剪贴板)\n失败原因: " + reason;
        }
        JOptionPane.showMessageDialog(null, message, "验证失败", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 复制文本到剪贴板
     */
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
    
    /**
     * 生成安全的HWID
     */
    private static String generateSecureHWID(String input) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = DigestUtils.sha256Hex(input).substring(0, 32).getBytes();

        PBEKeySpec spec = new PBEKeySpec(
                input.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();

        String pbkdf2Result = String.format("%d:%s:%s",
                ITERATIONS,
                HexFormat.of().formatHex(salt),
                HexFormat.of().formatHex(hash)
        );

        return DigestUtils.sha256Hex(pbkdf2Result).toUpperCase();
    }

    /**
     * 获取CPU信息
     */
    private static String getCPUInfo() {
        try {
            CentralProcessor processor = HAL.getProcessor();
            CentralProcessor.ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();
            String vendor = processorIdentifier.getVendor();
            String model = processorIdentifier.getModel();
            String family = processorIdentifier.getFamily();
            String arch = processorIdentifier.getProcessorID();
            int logicalCores = processor.getLogicalProcessorCount();
            int physicalCores = processor.getPhysicalProcessorCount();
            return String.format("%s-%s-%s-%s-%d-%d", vendor, model, family, arch, physicalCores, logicalCores);
        } catch (Exception e) {
            return "UNKNOWN_CPU_INFO";
        }
    }

    /**
     * 获取计算机名称
     */
    private static String getComputerName() {
        return OS.getNetworkParams().getHostName();
    }

    /**
     * 获取用户名
     */
    private static String getUsername() {
        return System.getProperty("user.name");
    }

    /**
     * 获取主板信息
     */
    private static String getBaseboardInfo() {
        Baseboard baseboard = CS.getBaseboard();
        String manufacturer = Optional.ofNullable(baseboard.getManufacturer()).orElse("UNKNOWN_MANUFACTURER");
        String model = Optional.ofNullable(baseboard.getModel()).orElse("UNKNOWN_MODEL");
        String serial = Optional.ofNullable(baseboard.getSerialNumber()).orElse("UNKNOWN_SERIAL");
        return String.format("%s-%s-%s", manufacturer, model, serial);
    }

    /**
     * 获取总内存
     */
    private static String getTotalMemory() {
        return String.valueOf(HAL.getMemory().getTotal());
    }

    /**
     * 获取总磁盘容量
     */
    private static String getTotalDiskCapacity() {
        try {
            long totalCapacity = 0;
            for (HWDiskStore disk : HAL.getDiskStores()) {
                totalCapacity += disk.getSize();
            }
            return String.valueOf(totalCapacity);
        } catch (Exception e) {
            return "UNKNOWN_DISK_CAPACITY";
        }
    }
}