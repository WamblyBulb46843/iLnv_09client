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
import java.util.List;
import java.util.Optional;

public class HWID {
    private static final int ITERATIONS = 200_000;
    private static final int KEY_LENGTH = 256; // Output length
    private static final String PEPPER = "xZ7pQ2wF5eD8sA1qW3cE6rT9yB4nH7mG";

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
                getSystemUUID(),
                getDiskSerial(),
                getBaseboardInfo(),
                getGpuId(),
                getCPUInfo(),
                getMemoryInfo(),
                getCpuCores(),
                getOsInfo(),
                getPowerSourceInfo(),
                getFirmwareInfo(),
                getSoundCardInfo(),
                getComputerSystemInfo()
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

    private static String getDiskSerial() {
        List<String> serials = new java.util.ArrayList<>();
        for (HWDiskStore disk : HAL.getDiskStores()) {
            serials.add(disk.getSerial().trim());
        }
        if (serials.isEmpty()) {
            return "UNKNOWN_DISK";
        }
        java.util.Collections.sort(serials);
        return String.join(";", serials);
    }



    private static String getSystemUUID() {
        return Optional.ofNullable(CS.getSerialNumber()).orElse("UNKNOWN_UUID");
    }

    private static String getCPUInfo() {
        try {
            CentralProcessor.ProcessorIdentifier processorIdentifier = HAL.getProcessor().getProcessorIdentifier();
            String vendor = processorIdentifier.getVendor();
            String model = processorIdentifier.getModel();
            String family = processorIdentifier.getFamily();
            String stepping = processorIdentifier.getStepping();
            String processorID = processorIdentifier.getProcessorID();
            return String.format("%s-%s-%s-%s-%s", vendor, model, family, stepping, processorID);
        } catch (Exception e) {
            return "UNKNOWN_CPU_INFO";
        }
    }

    private static String getGpuId() {
        List<String> gpuIds = new java.util.ArrayList<>();
        for (GraphicsCard gpu : HAL.getGraphicsCards()) {
            gpuIds.add(gpu.getDeviceId());
        }
        if (gpuIds.isEmpty()) {
            return "UNKNOWN_GPU";
        }
        java.util.Collections.sort(gpuIds);
        return String.join(";", gpuIds);
    }

    private static String getBaseboardInfo() {
        Baseboard baseboard = CS.getBaseboard();
        String manufacturer = Optional.ofNullable(baseboard.getManufacturer()).orElse("UNKNOWN_MANUFACTURER");
        String model = Optional.ofNullable(baseboard.getModel()).orElse("UNKNOWN_MODEL");
        String serial = Optional.ofNullable(baseboard.getSerialNumber()).orElse("UNKNOWN_BASEBOARD_SERIAL");
        return String.format("%s-%s-%s", manufacturer, model, serial);
    }

    private static String getMemoryInfo() {
        try {
            List<String> memoryDetails = new java.util.ArrayList<>();
            for (PhysicalMemory pm : HAL.getMemory().getPhysicalMemory()) {
                memoryDetails.add(
                        pm.getBankLabel().trim() + "-" +
                                pm.getManufacturer().trim() + "-" +
                                pm.getMemoryType().trim()
                );
            }
            if (memoryDetails.isEmpty()) {
                return "UNKNOWN_MEMORY_INFO";
            }
            java.util.Collections.sort(memoryDetails);
            return String.join(";", memoryDetails);
        } catch (Exception e) {
            return "UNKNOWN_MEMORY_INFO";
        }
    }

    private static String getCpuCores() {
        return String.valueOf(HAL.getProcessor().getLogicalProcessorCount());
    }

    private static String getOsInfo() {
        try {
            String family = OS.getFamily();
            String version = OS.getVersionInfo().getVersion();
            String build = OS.getVersionInfo().getBuildNumber();
            return String.format("%s-%s-%s", family, version, build);
        } catch (Exception e) {
            return "UNKNOWN_OS";
        }
    }

    private static String getPowerSourceInfo() {
        try {
            List<String> serials = new java.util.ArrayList<>();
            for (PowerSource powerSource : HAL.getPowerSources()) {
                serials.add(powerSource.getSerialNumber());
            }
            if (serials.isEmpty()) {
                return "NO_POWER_SOURCE";
            }
            java.util.Collections.sort(serials);
            return String.join(";", serials);
        } catch (Exception e) {
            return "UNKNOWN_POWER_SOURCE";
        }
    }

    private static String getFirmwareInfo() {
        try {
            Firmware firmware = CS.getFirmware();
            String manufacturer = Optional.ofNullable(firmware.getManufacturer()).orElse("UNKNOWN_FIRMWARE_MANUFACTURER");
            String version = Optional.ofNullable(firmware.getVersion()).orElse("UNKNOWN_FIRMWARE_VERSION");
            return String.format("%s-%s", manufacturer, version);
        } catch (Exception e) {
            return "UNKNOWN_FIRMWARE";
        }
    }

    private static String getSoundCardInfo() {
        try {
            List<String> soundCardDetails = new java.util.ArrayList<>();
            for (SoundCard soundCard : HAL.getSoundCards()) {
                soundCardDetails.add(
                        soundCard.getName() + "-" +
                                soundCard.getCodec() + "-" +
                                soundCard.getDriverVersion()
                );
            }
            if (soundCardDetails.isEmpty()) {
                return "UNKNOWN_SOUNDCARD";
            }
            java.util.Collections.sort(soundCardDetails);
            return String.join(";", soundCardDetails);
        } catch (Exception e) {
            return "UNKNOWN_SOUNDCARD";
        }
    }

    private static String getComputerSystemInfo() {
        try {
            String manufacturer = Optional.ofNullable(CS.getManufacturer()).orElse("UNKNOWN_CS_MANUFACTURER");
            String model = Optional.ofNullable(CS.getModel()).orElse("UNKNOWN_CS_MODEL");
            return String.format("%s-%s", manufacturer, model);
        } catch (Exception e) {
            return "UNKNOWN_COMPUTER_SYSTEM";
        }
    }

    public static boolean isVirtualMachine() {
        // Using a set for faster lookups
        final java.util.Set<String> vmManufacturers = new java.util.HashSet<>(java.util.Arrays.asList(
                "vmware", "innotek gmbh", "virtualbox", "qemu", "red hat", "proxmox", "hyper-v", "microsoft corporation", "xen"
        ));

        final java.util.Set<String> vmModels = new java.util.HashSet<>(java.util.Arrays.asList(
                "virtual", "vmware", "virtualbox", "qemu", "kvm"
        ));

        // 1. Check computer system manufacturer and model
        String computerManufacturer = CS.getManufacturer().toLowerCase(java.util.Locale.ROOT);
        if (vmManufacturers.stream().anyMatch(computerManufacturer::contains)) {
            return true;
        }

        String computerModel = CS.getModel().toLowerCase(java.util.Locale.ROOT);
        if (vmModels.stream().anyMatch(computerModel::contains)) {
            return true;
        }

        // 2. Check firmware manufacturer
        Firmware firmware = CS.getFirmware();
        if (firmware != null) {
            String firmwareManufacturer = firmware.getManufacturer().toLowerCase(java.util.Locale.ROOT);
            if (vmManufacturers.stream().anyMatch(firmwareManufacturer::contains)) {
                return true;
            }
        }

        // 3. Check network interfaces for known VM MAC addresses
        for (NetworkIF net : HAL.getNetworkIFs()) {
            if (net.isKnownVmMacAddr()) {
                return true;
            }
        }

        // 4. Check CPU Identifier for virtualization strings
        String cpuIdentifierName = HAL.getProcessor().getProcessorIdentifier().getName();
        if (cpuIdentifierName != null) {
            String lowerCpuName = cpuIdentifierName.toLowerCase(java.util.Locale.ROOT);
            if (lowerCpuName.contains("xen") || lowerCpuName.contains("kvm") || lowerCpuName.contains("hyper-v")) {
                return true;
            }
        }
        
        // 5. Check for specific disk names
        for (HWDiskStore disk : HAL.getDiskStores()) {
            String diskModel = disk.getModel().toLowerCase(java.util.Locale.ROOT);
            if (diskModel.contains("vbox") || diskModel.contains("virtual") || diskModel.contains("vmware")) {
                return true;
            }
        }

        return false;
    }
}
