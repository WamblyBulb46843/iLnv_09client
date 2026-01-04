package dev.iLnv_09.mod.commands.impl;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.mod.commands.Command;
import dev.iLnv_09.core.impl.CommandManager;

import java.util.List;

public class PrefixCommand extends Command {

	public PrefixCommand() {
		super("prefix", "[prefix]");
	}

	@Override
	public void runCommand(String[] parameters) {
		if (parameters.length == 0) {
			sendUsage();
			return;
		}
		if (parameters[0].startsWith("/")) {
			CommandManager.sendChatMessage("§fPlease specify a valid §bprefix.");
			return;
		}
		iLnv_09.PREFIX = parameters[0];
		CommandManager.sendChatMessage("§bPrefix §fset to §e" + parameters[0]);
	}

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		return null;
	}
}
