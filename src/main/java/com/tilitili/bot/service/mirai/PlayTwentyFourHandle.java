package com.tilitili.bot.service.mirai;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.CalculateObject;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import com.tilitili.common.utils.MathUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class PlayTwentyFourHandle extends ExceptionRespMessageToSenderHandle {
	private final ScheduledExecutorService scheduled =  Executors.newSingleThreadScheduledExecutor();
	private final List<Integer> cardList = IntStream.rangeClosed(1, 13).flatMap(i -> IntStream.rangeClosed(1, 4).map((j)->i)).boxed().collect(Collectors.toList());
	private final static String numListKey = "playGameHandle.numListKey";
	private final static String lastSendTimeKey = "playGameHandle.last_send_time";
	private final static String lockKey = "playGameHandle.lock";
	private final static int waitTime = 3;
	public final Map<String, String> replaceMap;
	public final String calRegix;

	private final BotManager botManager;

	@Autowired
	public PlayTwentyFourHandle(BotManager botManager) {
		this.botManager = botManager;
		replaceMap = ImmutableMap.<String, String>builder()
				.put("÷", "/")
				.put("＋", "+")
				.put("×", "*")
				.put("x", "*")
				.put("X", "*")
				.put("乘", "*")
				.put("除", "/")
				.put("加", "+")
				.put("减", "-")
				.put("等于", "=")
				.put("＝", "=")
				.put("（", "(")
				.put("）", ")")
				.put(" ", "")
				.put("\t", "").build();
		Set<String> charSet = new HashSet<>(replaceMap.keySet());
		charSet.addAll(replaceMap.values());
		calRegix = String.format("([%s]+)", String.join("", charSet));
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();
		switch (key) {
			case "玩24点": case "w24": return startGame(messageAction);
			case "回答24点": case "hd24": return handleGame(messageAction);
			case "放弃24点": case "fq24": return endGame(messageAction);
			default: return null;
		}
	}

	@Override
	public String isThisTask(BotMessageAction botMessageAction) {
		String text = botMessageAction.getText();
		BotSessionService.MiraiSession session = botMessageAction.getSession();
		if (! session.containsKey(numListKey)) {
			return null;
		}
		if (Objects.equals(StringUtils.patten1(calRegix, text), text)) {
			if (StringUtils.pattenAll("(\\d+)", text).size() == 4) {
				return "回答24点";
			}
		}
		return null;
	}

	private BotMessage endGame(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		session.remove(numListKey);
		session.remove(lastSendTimeKey);
		return BotMessage.simpleTextMessage("游戏结束了！");
	}

	private BotMessage handleGame(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		String result = messageAction.getValue();
		if (Objects.equals(session.remove(lockKey), 0)) return null;
		try {
			if (!session.containsKey(numListKey)) return null;
			Asserts.notBlank(result, "你想答什么。");
			String resultAfterReplace = result;
			for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				resultAfterReplace = resultAfterReplace.replace(key, value);
			}
			if (resultAfterReplace.contains("=")) resultAfterReplace = resultAfterReplace.split("=")[0];
			String resultAfterClean = resultAfterReplace.replaceAll("[^0-9+\\-*/()]", "");
			CalculateObject calculateObject = new CalculateObject(resultAfterClean);
			int resultNum = calculateObject.getResult();
			String calculateStr = calculateObject.toString();
			Asserts.checkEquals(resultNum, 24, "好像不对呢，你的回答是[%s]吗？", calculateStr);

			String numListStr = session.get(numListKey);
			String[] numList = numListStr.split(",");
			String[] calNumList = StringUtils.pattenAll("(\\d+)", calculateStr).stream().filter(Predicate.isEqual("0").negate()).toArray(String[]::new);
			Arrays.sort(numList);
			Arrays.sort(calNumList);
			Asserts.isTrue(Arrays.equals(numList, calNumList), "题目是[%s]哦，不是[%s]", numListStr, String.join(",", calNumList));
		} finally {
			session.put(lockKey, "lock");
		}
		session.remove(lockKey);
		session.remove(numListKey);
		session.remove(lastSendTimeKey);
		return BotMessage.simpleTextMessage("恭喜你回答正确！").setQuote(messageAction.getMessageId());
	}

	private BotMessage startGame(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		String numListStr = session.get(numListKey);
		Asserts.checkNull(numListStr, "先玩完这一局吧："+numListStr);

		List<Integer> numList;
		String answer;
		while ((answer = this.hasAnswer(numList = MathUtil.getNThingFromList(cardList, 4))) == null) {
			String newNumListStr = numList.stream().map(String::valueOf).collect(Collectors.joining(","));
			log.debug("problem={} is haven't answer", newNumListStr);
		}
		String newNumListStr = numList.stream().map(String::valueOf).collect(Collectors.joining(","));
		session.put(numListKey, newNumListStr);
		session.put(lastSendTimeKey, DateUtils.formatDateYMDHMS(new Date()));
		session.put(lockKey, "lock");

		scheduled.schedule(() -> {
			String lastSendTime2Str = session.get(lastSendTimeKey);
			boolean needEnd = lastSendTime2Str != null && DateUtils.parseDateYMDHMS(lastSendTime2Str).before(getLimitDate());
			if (needEnd) {
				botManager.sendMessage(BotMessage.simpleTextMessage("时间到啦！没有人能答出来吗？", messageAction.getBotMessage()));
				session.remove(numListKey);
				session.remove(lastSendTimeKey);
				session.remove(lockKey);
			}
		}, waitTime, TimeUnit.MINUTES);
		return BotMessage.simpleTextMessage("试试看这道题吧("+newNumListStr+")，时限"+waitTime+"分钟哦~");
	}

	private String hasAnswer(List<Integer> numList) {
		return "null";
	}

	private Date getLimitDate() {
		Calendar calstart = Calendar.getInstance();
		calstart.setTime(new Date());
		calstart.add(Calendar.MINUTE, -waitTime);
		return calstart.getTime();
	}
}
