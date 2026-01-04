package cn.loryu.client;

import javax.swing.*;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * @author Loryu
 * @date 2022/5/27 22:00
 * @description 客户端主类
 */
public class Main {

    public static void main(String[] args) {
        if (isDebugging() || HWID.isVirtualMachine()) {
            // Use a generic, non-descriptive error message to avoid giving clues to attackers.
            JOptionPane.showMessageDialog(null,
                    "Application integrity check failed. Error Code: CE-10825-5",
                    "Fatal Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(5); // Exit with a non-zero status code
        }
        Client client = new Client();
        client.connect();
    }

    /**
     * Checks if the JVM is running in debug mode by inspecting runtime arguments.
     * @return true if a debugger agent is detected, false otherwise.
     */
    private static boolean isDebugging() {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        // Check for common Java debugger agent strings
        for (String arg : jvmArgs) {
            if (arg.startsWith("-agentlib:jdwp") || arg.startsWith("-Xdebug") || arg.startsWith("-Xrunjdwp")) {
                return true;
            }
        }
        return false;
    }
}