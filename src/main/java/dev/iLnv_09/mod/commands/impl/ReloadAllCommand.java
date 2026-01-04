package dev.iLnv_09.mod.commands.impl;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.core.impl.CommandManager;
import dev.iLnv_09.mod.commands.Command;

import java.util.List;

public class ReloadAllCommand extends Command {

	public ReloadAllCommand() {
		super("reloadall", "");
	}

	@Override
	public void runCommand(String[] parameters) {
		CommandManager.sendChatMessage("§fReloading..");
		iLnv_09.unload();
        try {
            iLnv_09.load();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		return null;
	}
}
