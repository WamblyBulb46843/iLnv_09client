package dev.iLnv_09.mod.commands.impl;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.core.Manager;
import dev.iLnv_09.core.impl.CommandManager;
import dev.iLnv_09.core.impl.ConfigManager;
import dev.iLnv_09.mod.commands.Command;

import java.util.List;

public class LoadCommand extends Command {

	public LoadCommand() {
		super("load", "[config]");
	}

	@Override
	public void runCommand(String[] parameters) {
		if (parameters.length == 0) {
			sendUsage();
			return;
		}
		CommandManager.sendChatMessage("§fLoading..");
		ConfigManager.options = Manager.getFile(parameters[0] + ".cfg");
		iLnv_09.CONFIG = new ConfigManager();
		iLnv_09.PREFIX = iLnv_09.CONFIG.getString("prefix", iLnv_09.PREFIX);
		iLnv_09.CONFIG.loadSettings();
        ConfigManager.options = Manager.getFile("options.txt");
		iLnv_09.save();
	}

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		return null;
	}
}
