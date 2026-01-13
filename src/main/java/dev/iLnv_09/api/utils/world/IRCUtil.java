package dev.iLnv_09.api.utils.world;

import dev.iLnv_09.irc.IRCClient;
import dev.iLnv_09.mod.modules.impl.client.IRCModule;

public class IRCUtil {
    private static IRCModule ircModule;

    public static void setIRCModule(IRCModule module) {
        ircModule = module;
    }

    public static boolean sendIRCMessage(String message) {
        if (ircModule != null && ircModule.client != null) {
            ircModule.client.sendMessage(message);
            return true;
        }
        return false;
    }

    public static IRCClient getIRCClient() {
        if (ircModule != null) {
            return ircModule.client;
        }
        return null;
    }

    public static boolean isIRCConnected() {
        return ircModule != null && ircModule.client != null;
    }
}