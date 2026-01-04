package dev.iLnv_09.mod.commands.impl;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.core.impl.CommandManager;
import dev.iLnv_09.core.impl.ConfigManager;
import dev.iLnv_09.mod.commands.Command;

import java.util.List;

public class ReloadCommand extends Command {

	public ReloadCommand() {
		super("reload", "");
	}

	@Override
	public void runCommand(String[] parameters) {
		CommandManager.sendChatMessage("§fReloading..");
		iLnv_09.CONFIG = new ConfigManager();
		iLnv_09.PREFIX = iLnv_09.CONFIG.getString("prefix", iLnv_09.PREFIX);
		iLnv_09.CONFIG.loadSettings();
		iLnv_09.XRAY.read();
		iLnv_09.TRADE.read();
		iLnv_09.FRIEND.read();
	}

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		return null;
	}
}
