package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.SendMessageEvent;
import dev.iLnv_09.mod.modules.Module;

public class ChatAppend extends Module {
	public static ChatAppend INSTANCE;

	public ChatAppend() {
		super("ChatAppend", Category.Misc);
		setChinese("消息后缀");
		INSTANCE = this;
	}

	@EventHandler
	public void onSendMessage(SendMessageEvent event) {
		String Append = "\ud835\udc56\ud835\udc3f\ud835\udc5b\ud835\udc63_\ud835\udfe2\ud835\udfeb";
		if (nullCheck() || event.isCancelled() || AutoQueue.inQueue) return;
		String message = event.message;

		if (message.startsWith("/") || message.startsWith("!") || message.endsWith(Append)) {
			return;
		}
		String suffix = Append;
		message = message + " " + suffix;
		event.message = message;
	}
}
