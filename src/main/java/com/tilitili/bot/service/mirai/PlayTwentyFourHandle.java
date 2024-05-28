package com.tilitili.bot.service.mirai;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.CalculateObject;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import com.tilitili.common.utils.MathUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
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

	private final SendMessageManager sendMessageManager;
	@Autowired
	public PlayTwentyFourHandle(SendMessageManager sendMessageManager) {
		this.sendMessageManager = sendMessageManager;
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
	protected List<BotMessage> mockMessageInWaiteSender(BotMessageAction messageAction) {
		String key = messageAction.getKeyWithoutPrefix();
		String virtualKey = messageAction.getKey();
		switch (virtualKey != null? virtualKey: key) {
			case "玩24点": case "w24": return Collections.singletonList(BotMessage.simpleTextMessage("试试看这道题吧(1，4，3，8)，时限3分钟哦~"));
			case "回答24点": case "hd24": {
				String result = messageAction.getValue();
				if ("(8-3+1)*4".equals(result)) {
					return Collections.singletonList(BotMessage.simpleTextMessage("恭喜你回答正确！"));
				} else {
					return Collections.emptyList();
				}
			}
			default: return Collections.emptyList();
		}
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();
		String virtualKey = messageAction.getKey();
		switch (virtualKey != null? virtualKey: key) {
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
		String numListStr = session.get(numListKey);
		List<Integer> numList = Arrays.stream(numListStr.split("，")).map(Integer::valueOf).collect(Collectors.toList());
		String answer = this.hasAnswer(numList);
		session.remove(numListKey);
		session.remove(lastSendTimeKey);
		return BotMessage.simpleTextMessage("戳啦，答案是"+answer, messageAction.getBotMessage());
	}

	private BotMessage handleGame(BotMessageAction messageAction) {
		BotMessage botMessage = messageAction.getBotMessage();
		BotSessionService.MiraiSession session = messageAction.getSession();
		String result = messageAction.getValue();
		if (!session.containsKey(numListKey)) return null;
		try {
			Asserts.isTrue(session.tryTimeoutLock(lockKey, Duration.ofSeconds(1)), "猪脑过载，你先别急 Σ（ﾟдﾟlll）");
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
			Asserts.checkEquals(resultNum, 24, "好像不对呢，你的回答是[%s=%s]吗？", calculateStr, resultNum);

			String numListStr = session.get(numListKey);
			String[] numList = numListStr.split("，");
			String[] calNumList = StringUtils.pattenAll("(\\d+)", calculateStr).stream().filter(Predicate.isEqual("0").negate()).toArray(String[]::new);
			Arrays.sort(numList);
			Arrays.sort(calNumList);
			Asserts.isTrue(Arrays.equals(numList, calNumList), "题目是[%s]哦，不是[%s]", numListStr, String.join("，", calNumList));
		} finally {
			session.remove(lockKey);
		}
		session.remove(numListKey);
		session.remove(lastSendTimeKey);
		return BotMessage.simpleTextMessage("恭喜你回答正确！", botMessage).setQuote(messageAction.getMessageId());
	}

	private BotMessage startGame(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		String numListStr = session.get(numListKey);
		if (numListStr != null) {
			String lastSendTime2StrIfExited = session.get(lastSendTimeKey);
			boolean needEnd = lastSendTime2StrIfExited != null && DateUtils.parseDateYMDHMS(lastSendTime2StrIfExited).before(getLimitDate());
			if (needEnd) {
				session.remove(numListKey);
				session.remove(lastSendTimeKey);
			} else {
				throw new AssertException("先玩完这一局吧："+numListStr);
			}
		}

		List<Integer> numList;
		String answer;
		while ((answer = this.hasAnswer(numList = MathUtil.getNThingFromList(cardList, 4))) == null) {
			String newNumListStr = numList.stream().map(String::valueOf).collect(Collectors.joining("，"));
			log.debug("problem={} is haven't answer", newNumListStr);
		}
		String answerTmp = answer;
		String newNumListStr = numList.stream().map(String::valueOf).collect(Collectors.joining("，"));
		session.put(numListKey, newNumListStr);
		session.put(lastSendTimeKey, DateUtils.formatDateYMDHMS(new Date()));

		scheduled.schedule(() -> {
			String lastSendTime2Str = session.get(lastSendTimeKey);
			boolean needEnd = lastSendTime2Str != null && DateUtils.parseDateYMDHMS(lastSendTime2Str).before(getLimitDate());
			if (needEnd) {
				sendMessageManager.sendMessage(BotMessage.simpleTextMessage("戳啦，答案是"+answerTmp, messageAction.getBotMessage()));
				session.remove(numListKey);
				session.remove(lastSendTimeKey);
			}
		}, waitTime, TimeUnit.MINUTES);
		return BotMessage.simpleTextMessage("试试看这道题吧("+newNumListStr+")，时限"+waitTime+"分钟哦~");
	}

	private String hasAnswer(List<Integer> numList) {
		CalculateObject result = this.getResult(numList);
		if (result == null) return null;
		return result.toString();
	}

	private Date getLimitDate() {
		Calendar calstart = Calendar.getInstance();
		calstart.setTime(new Date());
		calstart.add(Calendar.MINUTE, -waitTime);
		return calstart.getTime();
	}

	private final List<String> protectTypeList = Arrays.asList("((%s%s%s)%s%s)%s%s", "%s%s(%s%s(%s%s%s))", "(%s%s%s)%s(%s%s%s)", "(%s%s(%s%s%s))%s%s", "%s%s((%s%s%s)%s%s)");
	private CalculateObject getResult(List<Integer> numList) {
		for (int index = 0; index < 256; index++) {
			List<Integer> indexList = Arrays.asList(
					index / 4 / 4 / 4,
					index / 4 / 4 % 4,
					index / 4 % 4,
					index % 4
			);
			if (new HashSet<>(indexList).size() != indexList.size()) {
				continue;
			}
			List<Integer> reIndexNumList = indexList.stream().map(numList::get).collect(Collectors.toList());
			List<String> opList = Arrays.asList("+", "-", "*", "/");
			// 0是全+，太简单了
			for (int jndex = 1; jndex < 64; jndex++) {
				List<Integer> jndexList = Arrays.asList(
						jndex / 4 / 4,
						jndex / 4 % 4,
						jndex % 4
				);
				List<String> reIndexOpList = jndexList.stream().map(opList::get).collect(Collectors.toList());

				for (String protectType : protectTypeList) {
					String calStr = String.format(protectType, reIndexNumList.get(0), reIndexOpList.get(0)
							, reIndexNumList.get(1), reIndexOpList.get(1)
							, reIndexNumList.get(2), reIndexOpList.get(2)
							, reIndexNumList.get(3));
					CalculateObject calculateObject = new CalculateObject(calStr);
					try {
						if (calculateObject.getResult() == 24) return calculateObject;
					} catch (AssertException e) {
						if (!e.getMessage().equals("好像便乘小数惹")) {
							log.warn("numList="+numList, e);
						}
					}
				}
			}
		}
		return null;
	}
}
