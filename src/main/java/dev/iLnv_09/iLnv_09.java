package dev.iLnv_09;

import dev.iLnv_09.api.events.eventbus.EventBus;
import dev.iLnv_09.core.impl.*;
import dev.iLnv_09.hwid.Client;
import net.fabricmc.api.ModInitializer;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class iLnv_09 implements ModInitializer {
    public static final Identifier CUSTOM_SOUND_ID = new Identifier("ilnv_09", "custom_sound");
    public static SoundEvent CUSTOM_SOUND_EVENT = SoundEvent.of(CUSTOM_SOUND_ID);

    @Override
    public void onInitialize() {
        //Client client = new Client();   //验证启动
        //client.connect();
        load();
    }
    public static final String NAME = "iLnv_09";
    public static final String VERSION = "v2";
    public static String PREFIX = ";";
    public static final EventBus EVENT_BUS = new EventBus();
    public static ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    // Systems
    public static HoleManager HOLE;
    public static PlayerManager PLAYER;
    public static TradeManager TRADE;
    public static XrayManager XRAY;
    public static ModuleManager MODULE;
    public static CommandManager COMMAND;
    public static GuiManager GUI;
    public static ConfigManager CONFIG;
    public static RotationManager ROTATION;
    public static BreakManager BREAK;
    public static PopManager POP;
    public static FriendManager FRIEND;
    public static TimerManager TIMER;
    public static ShaderManager SHADER;
    public static FPSManager FPS;
    public static ServerManager SERVER;
    public static ThreadManager THREAD;
    public static boolean loaded = false;

    public static void load() {
        
        EVENT_BUS.registerLambdaFactory((lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        CONFIG = new ConfigManager();

        PREFIX = iLnv_09.CONFIG.getString("prefix", ";");
        THREAD = new ThreadManager();
        HOLE = new HoleManager();
        MODULE = new ModuleManager();
        COMMAND = new CommandManager();
        GUI = new GuiManager();
        FRIEND = new FriendManager();
        XRAY = new XrayManager();
        TRADE = new TradeManager();
        ROTATION = new RotationManager();
        BREAK = new BreakManager();
        PLAYER = new PlayerManager();

        POP = new PopManager();
        TIMER = new TimerManager();
        SHADER = new ShaderManager();
        FPS = new FPSManager();
        SERVER = new ServerManager();
        CONFIG.loadSettings();
        System.out.println("[" + iLnv_09.NAME + "] loaded");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (loaded) {
                save();
            }
        }));
        loaded = true;
    }

    public static void unload() {
        loaded = false;
        System.out.println("[" + iLnv_09.NAME + "] Unloading..");
        EVENT_BUS.listenerMap.clear();
        ConfigManager.resetModule();
        System.out.println("[" + iLnv_09.NAME + "] Unloaded");
    }

    public static void save() {
        System.out.println("[" + iLnv_09.NAME + "] Saving");
        CONFIG.saveSettings();
        FRIEND.save();
        XRAY.save();
        TRADE.save();
        System.out.println("[" + iLnv_09.NAME + "] Saved");
    }
}