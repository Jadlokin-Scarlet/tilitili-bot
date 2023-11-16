package com.tilitili.bot.socket.wrapper;

public interface BotWebSocketWrapperImp {
	void downBotBlocking();

	int getStatus();

	void upBotBlocking();
}
