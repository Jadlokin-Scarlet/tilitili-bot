package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.CalculateObject;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import com.tilitili.common.utils.MathUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class PlayTwentyFourHandle extends ExceptionRespMessageToSenderHandle {
	private final ScheduledExecutorService scheduled =  Executors.newSingleThreadScheduledExecutor();
	private final List<Integer> cardList = IntStream.rangeClosed(1, 13).flatMap(i -> IntStream.rangeClosed(1, 4).map((j)->i)).boxed().collect(Collectors.toList());
	private final static String numListKey = "playGameHandle.numListKey";
	private final static String lastSendTimeKey = "playGameHandle.last_send_time";
	private final static int waitTime = 1;

	private final BotManager botManager;

	@Autowired
	public PlayTwentyFourHandle(BotManager botManager) {
		this.botManager = botManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();
		switch (key) {
			case "玩24点": case "wyx": return startGame(messageAction);
			case "回答24点": case "yx": return handleGame(messageAction);
			default: return null;
		}
	}

	private BotMessage handleGame(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		String result = messageAction.getValue();
		Asserts.notBlank(result, "你想答什么。");
		String resultAfterReplace = result
				.replace("÷", "/")
				.replace("＋", "+")
				.replace("x", "*")
				.replace("X", "*")
				.replace("乘", "*")
				.replace("除", "/")
				.replace("加", "+")
				.replace("减", "-")
				.replace("减", "-")
				.replace("＝", "=")
				.replace("等于", "=");
		if (resultAfterReplace.contains("=")) resultAfterReplace = resultAfterReplace.split("=")[0];
		String resultAfterClean = resultAfterReplace.replaceAll("[^0-9+\\-*/()]", "");
		CalculateObject calculateObject = new CalculateObject(resultAfterClean);
		int resultNum = calculateObject.getResult();
		String calculateStr = calculateObject.toString();
		Asserts.checkEquals(resultNum, 24, "好像不对呢，你的回答是[%s]吗？", calculateStr);
		session.remove(numListKey);
		session.remove(lastSendTimeKey);
		return BotMessage.simpleTextMessage("恭喜你回答正确！").setQuote(messageAction.getMessageId());
	}

	private BotMessage startGame(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		String numListStr = session.get(numListKey);
		Asserts.checkNull(numListStr, "先玩完这一局吧："+numListStr);

		List<Integer> numList = MathUtil.getNThingFromList(cardList, 4);
		String newNumListStr = numList.stream().map(String::valueOf).collect(Collectors.joining(","));
		session.put(numListKey, newNumListStr);
		session.put(lastSendTimeKey, DateUtils.formatDateYMDHMS(new Date()));

		scheduled.schedule(() -> {
			String lastSendTime2Str = session.get(lastSendTimeKey);
			boolean needEnd = lastSendTime2Str == null || DateUtils.parseDateYMDHMS(lastSendTime2Str).before(getLimitDate());
			if (needEnd) {
				botManager.sendMessage(BotMessage.simpleTextMessage("时间到啦！没有人能答出来吗？", messageAction.getBotMessage()));
				session.remove(numListKey);
				session.remove(lastSendTimeKey);
			}
		}, waitTime, TimeUnit.MINUTES);
		return BotMessage.simpleTextMessage("试试看这道题吧("+newNumListStr+")，时限"+waitTime+"分钟哦~");
	}

	private Date getLimitDate() {
		Calendar calstart = Calendar.getInstance();
		calstart.setTime(new Date());
		calstart.add(Calendar.MINUTE, -waitTime);
		return calstart.getTime();
	}
}
