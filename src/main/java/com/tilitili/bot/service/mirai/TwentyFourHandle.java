package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class TwentyFourHandle extends ExceptionRespMessageHandle {
	private final Random random = new Random(System.currentTimeMillis());
	private final List<Integer> cardList = IntStream.rangeClosed(1, 13).flatMap(i -> IntStream.rangeClosed(1, 4).map((j)->i)).boxed().collect(Collectors.toList());
	private final static String numListKey = "twentyFourHandle.numListKey";

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();
		switch (key) {
			case "玩24点": case "w24d": return startGame(messageAction);
			default: return null;
		}
	}

	private BotMessage startGame(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		String numListStr = session.get(numListKey);
		Asserts.notBlank(numListStr, "先玩完这一局吧："+numListStr);
		List<Integer> numList = Arrays.asList(
				cardList.get(random.nextInt(cardList.size())),
				cardList.get(random.nextInt(cardList.size())),
				cardList.get(random.nextInt(cardList.size())),
				cardList.get(random.nextInt(cardList.size()))
		);
		String newNumListStr = numList.stream().map(String::valueOf).collect(Collectors.joining("，"));
		session.put(numListKey, newNumListStr);
		return BotMessage.simpleTextMessage("试试看这道题吧("+newNumListStr+")");
	}
}
