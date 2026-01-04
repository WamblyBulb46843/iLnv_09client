package dev.iLnv_09.mod.commands.impl;

import dev.iLnv_09.core.Manager;
import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.core.impl.CommandManager;
import dev.iLnv_09.core.impl.ConfigManager;
import dev.iLnv_09.mod.commands.Command;

import java.util.List;

public class SaveCommand extends Command {

	public SaveCommand() {
		super("save", "");
	}

	@Override
	public void runCommand(String[] parameters) {
		if (parameters.length == 1) {
			CommandManager.sendChatMessage("§fSaving config named " + parameters[0]);
			ConfigManager.options = Manager.getFile(parameters[0] + ".cfg");
			iLnv_09.save();
			ConfigManager.options = Manager.getFile("options.txt");
		} else {
			CommandManager.sendChatMessage("§fSaving..");
		}
		iLnv_09.save();
	}

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		return null;
	}
}
