package dev.iLnv_09.hwid;

import org.apache.commons.codec.digest.DigestUtils;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;
import java.util.Optional;

public class HWID {
    private static final int ITERATIONS = 250_000;
    private static final int KEY_LENGTH = 256; // Output length
    private static final String PEPPER = "aK9bS2nL5gT8wR6fD1vY4zQ0jP3uE7xI";

    private static final SystemInfo SI = new SystemInfo();
    private static final HardwareAbstractionLayer HAL = SI.getHardware();
    private static final ComputerSystem CS = HAL.getComputerSystem();
    private static final OperatingSystem OS = SI.getOperatingSystem();

    private static String cachedHWID = null;

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

    private static String getComputerName() {
        return OS.getNetworkParams().getHostName();
    }

    private static String getUsername() {
        return System.getProperty("user.name");
    }

    private static String getBaseboardInfo() {
        Baseboard baseboard = CS.getBaseboard();
        String manufacturer = Optional.ofNullable(baseboard.getManufacturer()).orElse("UNKNOWN_MANUFACTURER");
        String model = Optional.ofNullable(baseboard.getModel()).orElse("UNKNOWN_MODEL");
        String serial = Optional.ofNullable(baseboard.getSerialNumber()).orElse("UNKNOWN_SERIAL");
        return String.format("%s-%s-%s", manufacturer, model, serial);
    }

    private static String getTotalMemory() {
        return String.valueOf(HAL.getMemory().getTotal());
    }

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
