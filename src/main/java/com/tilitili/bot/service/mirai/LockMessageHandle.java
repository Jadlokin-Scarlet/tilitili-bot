package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class LockMessageHandle extends ExceptionRespMessageHandle {
	private final AtomicBoolean lockFlag = new AtomicBoolean(false);
	private final String lockRespMessage;

	protected LockMessageHandle(String lockRespMessage) {
		this.lockRespMessage = lockRespMessage;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		try {
			Asserts.isTrue(lockFlag.compareAndSet(false, true), lockRespMessage);
			return handleMessageAfterLock(messageAction);
		} finally {
			lockFlag.set(false);
		}
	}

	protected abstract BotMessage handleMessageAfterLock(BotMessageAction messageAction) throws Exception;
}
