package dev.iLnv_09.core;

import dev.iLnv_09.iLnv_09;
import net.minecraft.client.MinecraftClient;

import java.io.File;

public class Manager {
    public static MinecraftClient mc = MinecraftClient.getInstance();

    public static File getFile(String s) {
        File folder = new File(mc.runDirectory.getPath() + File.separator + iLnv_09.NAME.toLowerCase());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, s);
    }
}
