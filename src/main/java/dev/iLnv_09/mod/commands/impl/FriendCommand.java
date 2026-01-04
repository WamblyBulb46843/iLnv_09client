package dev.iLnv_09.mod.commands.impl;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.mod.commands.Command;
import dev.iLnv_09.core.impl.CommandManager;

import java.util.ArrayList;
import java.util.List;

public class FriendCommand extends Command {

	public FriendCommand() {
		super("friend", "[name/reset/list] | [add/remove] [name]");
	}

	@Override
	public void runCommand(String[] parameters) {
		if (parameters.length == 0) {
			sendUsage();
			return;
		}
        switch (parameters[0]) {
            case "reset" -> {
                // 假设 fixedFriends 是固定好友列表，重置时跳过这些好友
                List<String> fixedFriends = List.of("iLnv_09", "WamblyBulb46843"); // 替换为实际固定好友名
                iLnv_09.FRIEND.friendList.removeIf(name -> !fixedFriends.contains(name));
                CommandManager.sendChatMessage("§f好友列表已重置，固定好友未被移除");
                return;
            }
            case "list" -> {
                if (iLnv_09.FRIEND.friendList.isEmpty()) {
                    CommandManager.sendChatMessage("§f好友列表为空");
                    return;
                }
                StringBuilder friends = new StringBuilder();
                int time = 0;
                boolean first = true;
                boolean start = true;
                for (String name : iLnv_09.FRIEND.friendList) {
                    if (!first) {
                        friends.append(", ");
                    }
                    friends.append(name);
                    first = false;
                    time++;
                    if (time > 3) {
                        CommandManager.sendChatMessage((start ? "§e朋友 §a" : "§a") + friends);
                        friends = new StringBuilder();
                        start = false;
                        first = true;
                        time = 0;
                    }
                }
                if (first) {
                    CommandManager.sendChatMessage("§a" + friends);
                }
                return;
            }
            case "add" -> {
                if (parameters.length == 2) {
                    iLnv_09.FRIEND.addFriend(parameters[1]);
                    CommandManager.sendChatMessage("§f" + parameters[1] + (iLnv_09.FRIEND.isFriend(parameters[1]) ? " §a已添加为好友" : " §c已移除为好友"));
                    return;
                }
                sendUsage();
                return;
            }
            case "remove" -> {
                if (parameters.length == 2) {
                    String name = parameters[1];
                    // 假设 fixedFriends 是固定好友列表
                    List<String> fixedFriends = List.of("iLnv_09", "WamblyBulb46843"); // 替换为实际固定好友名
                    if (fixedFriends.contains(name)) {
                        CommandManager.sendChatMessage("§c无法移除固定好友: " + name);
                        return;
                    }
                    iLnv_09.FRIEND.removeFriend(name);
                    CommandManager.sendChatMessage("§f" + name + (iLnv_09.FRIEND.isFriend(name) ? " §a已被加为好友" : " §c已被移除为好友"));
                    return;
                }
                sendUsage();
                return;
            }
        }

        if (parameters.length == 1) {
			CommandManager.sendChatMessage("§f" + parameters[0] + (iLnv_09.FRIEND.isFriend(parameters[0]) ? " §a已加为好友" : " §c未加为好友"));
			return;
		}

		sendUsage();
	}

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		if (count == 1) {
			String input = seperated.get(seperated.size() - 1).toLowerCase();
			List<String> correct = new ArrayList<>();
			List<String> list = List.of("add", "remove", "list", "reset");
			for (String x : list) {
				if (input.equalsIgnoreCase(iLnv_09.PREFIX + "friend") || x.toLowerCase().startsWith(input)) {
					correct.add(x);
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
