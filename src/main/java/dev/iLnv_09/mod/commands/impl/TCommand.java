package dev.iLnv_09.mod.commands.impl;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.core.impl.CommandManager;
import dev.iLnv_09.core.impl.ModuleManager;
import dev.iLnv_09.mod.commands.Command;
import dev.iLnv_09.mod.modules.Module;

import java.util.ArrayList;
import java.util.List;

public class TCommand extends Command {

	public TCommand() {
		super("t", "[module]");
	}

	@Override
	public void runCommand(String[] parameters) {
		if (parameters.length == 0) {
			sendUsage();
			return;
		}
		String moduleName = parameters[0];
		Module module = iLnv_09.MODULE.getModuleByName(moduleName);
		if (module == null) {
			CommandManager.sendChatMessage("§fUnknown module!");
			return;
		}
		module.toggle();
	}

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		if (count == 1) {
			String input = seperated.get(seperated.size() - 1).toLowerCase();
			ModuleManager cm = iLnv_09.MODULE;
			List<String> correct = new ArrayList<>();
			for (Module x : cm.modules) {
				if (input.equalsIgnoreCase(iLnv_09.PREFIX + "toggle") || x.getName().toLowerCase().startsWith(input)) {
					correct.add(x.getName());
				}
			}
			int numCmds = correct.size();
			String[] commands = new String[numCmds];

			int i = 0;
			for (String x : correct) {
				commands[i++] = x;
			}

			return commands;
		}
		return null;
	}
}
