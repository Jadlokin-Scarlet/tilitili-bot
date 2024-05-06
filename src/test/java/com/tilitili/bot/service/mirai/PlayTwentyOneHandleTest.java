package com.tilitili.bot.service.mirai;

import com.tilitili.bot.StartApplication;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.MathUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
class PlayTwentyOneHandleTest {
	@Autowired
	private PlayTwentyOneHandle tester;
	@Autowired
	BotRobotCacheManager botRobotCacheManager;
	@Autowired
	BotSenderCacheManager botSenderCacheManager;
	@Autowired
	BotUserManager botUserManager;

	private BotMessage botMessage;

	@BeforeEach
	void init() {
		BotRobot bot = botRobotCacheManager.getValidBotRobotById(2L);
		BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(181L);
		BotSender botSender = botSenderCacheManager.getValidBotSenderById(3417L);
		botMessage = new BotMessage()
				.setBotUser(botUser)
				.setBotSender(botSender)
				.setBot(bot);
	}

	@Test
	void handleMessage() {
		HashMap<Integer, Integer> cntMap = new HashMap<>();
		ChooseMap chooseMap = new ChooseMap();
		for (int i = 0; i < 10000; i++) {
			BotMessage zbResp = testHandle("准备 10");

			try {
				while (true) {
					if (!zbResp.getBotMessageChainList().get(0).getText().contains("**")) {
						List<Integer> subScoreList = zbResp.getBotMessageChainList().stream().map(BotMessageChain::getText).filter(Objects::nonNull).map(
								text -> StringUtils.patten1("(-?\\d+)分", text)
						).filter(StringUtils::isNotBlank).map(Integer::parseInt).collect(Collectors.toList());
						int subScore = subScoreList.stream().mapToInt(Integer::intValue).sum();
						if (!cntMap.containsKey(subScore)) {
							cntMap.put(subScore, 1);
						} else {
							cntMap.put(subScore, cntMap.get(subScore) + 1);
						}
						int total = cntMap.entrySet().stream().mapToInt(e -> e.getKey() * e.getValue()).sum();
						log.info("--------------------result: {} total：{}  cnt：{}", subScoreList, total, cntMap);
						break;
					}

					int splitDiff = 0;
					while (zbResp.getBotMessageChainList().get(1 + splitDiff).getText().startsWith("\nJ")) {
						splitDiff++;
					}

					String botLine = zbResp.getBotMessageChainList().get(0).getText();
					String playerLine = zbResp.getBotMessageChainList().get(1 + splitDiff).getText();
					String allowChooseStr = zbResp.getBotMessageChainList().get(zbResp.getBotMessageChainList().size() - 1).getText();

					String botNumber = (StringUtils.patten1(",((A|\\d){1,2})", botLine));

					List<String> playerNumberList = StringUtils.patten1List("[：,]((A|\\d){1,2})", playerLine);
//			String playerNumber1 = (StringUtils.patten1("：((A|\\d){1,2})", playerLine));
//			String playerNumber2 = (StringUtils.patten1(",((A|\\d){1,2})", playerLine));
					List<String> allowChooseList = Arrays.asList(allowChooseStr.substring(allowChooseStr.indexOf("：")+1).split("、"));
					Asserts.notNull(botNumber);
					Asserts.isTrue(playerNumberList.size() > 1);
					Asserts.notEmpty(allowChooseList);
					boolean allowD = allowChooseList.contains("孤注一掷");
					boolean allowP = allowChooseList.contains("分家");
					boolean allowR = allowChooseList.contains("投降");

					String choose = chooseMap.getChoose(botNumber, playerNumberList, allowD, allowP, allowR);
					log.info("botNumber={}, playerNumberList={}, choose={}, allowD={}, allowP={}, allowR={}", botNumber, playerNumberList, choose, allowD, allowP, allowR);

					switch (choose) {
						case "D": zbResp = (testHandle("孤注一掷"));break;
						case "P": zbResp = (testHandle("分家"));break;
						case "R": zbResp = (testHandle("投降"));break;
						case "H": zbResp = (testHandle("进货"));break;
						case "S": zbResp = (testHandle("摆烂"));break;
						default: throw new AssertException();
					}
				}
			} catch (Exception e) {
				log.error("异常 resp"+zbResp, e);
				throw new AssertException(e);
			}
		}
	}

	private BotMessage testHandle(String message) {
		BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(botMessage.getBotUser().getId());
		botMessage.setBotUser(botUser).setBotMessageChainList(Collections.singletonList(BotMessageChain.ofPlain(message)));
		return tester.handleMessage(new BotMessageAction(botMessage, null));
	}

	static class ChooseMap {
		//					   2	 3		 4		 5		 6		 7		 8		 9		10		 A
		private final List<List<String>> hardMap = Arrays.asList(
				Arrays.asList("H",	"H",	"H",	"H",	"H",	"H",	"H",	"H",	"H",	"H"),	//4-8
				Arrays.asList("H",	"DH",	"DH",	"DH",	"DH",	"H",	"H",	"H",	"H",	"H"),	//9
				Arrays.asList("DH",	"DH",	"DH",	"DH",	"DH",	"DH",	"DH",	"DH",	"H",	"H"),	//10
				Arrays.asList("DH",	"DH",	"DH",	"DH",	"DH",	"DH",	"DH",	"DH",	"DH",	"DH"),	//11
				Arrays.asList("H",	"H",	"S",	"S",	"S",	"H",	"H",	"H",	"H",	"H"),	//12
				Arrays.asList("S",	"S",	"S",	"S",	"S",	"H",	"H",	"H",	"H",	"H"),	//13
				Arrays.asList("S",	"S",	"S",	"S",	"S",	"H",	"H",	"H",	"H",	"H"),	//14
				Arrays.asList("S",	"S",	"S",	"S",	"S",	"H",	"H",	"H",	"RH",	"RH"),	//15
				Arrays.asList("S",	"S",	"S",	"S",	"S",	"H",	"H",	"RH",	"RH",	"RH"),	//16
				Arrays.asList("S",	"S",	"S",	"S",	"S",	"S",	"S",	"S",	"S",	"RS"),	//17
				Arrays.asList("S",	"S",	"S",	"S",	"S",	"S",	"S",	"S",	"S",	"S")	//18+
		);
		private final List<List<String>> softMap = Arrays.asList(
				Arrays.asList("H",	"H",	"H",	"DH",	"DH",	"H",	"H",	"H",	"H",	"H"),	//13
				Arrays.asList("H",	"H",	"H",	"DH",	"DH",	"H",	"H",	"H",	"H",	"H"),	//14
				Arrays.asList("H",	"H",	"DH",	"DH",	"DH",	"H",	"H",	"H",	"H",	"H"),	//15
				Arrays.asList("H",	"H",	"DH",	"DH",	"DH",	"H",	"H",	"H",	"H",	"H"),	//16
				Arrays.asList("H",	"DH",	"DH",	"DH",	"DH",	"H",	"H",	"H",	"H",	"H"),	//17
				Arrays.asList("DS",	"DS",	"DS",	"DS",	"DS",	"S",	"S",	"H",	"H",	"H"),	//18
				Arrays.asList("S",	"S",	"S",	"S",	"DS",	"S",	"S",	"S",	"S",	"S"),	//19
				Arrays.asList("S",	"S",	"S",	"S",	"S",	"S",	"S",	"S",	"S",	"S")	//20+
		);
		private final List<List<String>> splitMap = Arrays.asList(
				Arrays.asList("PH",	"PH",	"P",	"P",	"P",	"P",	"H",	"H",	"H",	"H"),	//2,2
				Arrays.asList("PH",	"PH",	"P",	"P",	"P",	"P",	"H",	"H",	"H",	"H"),	//3,3
				Arrays.asList("H",	"H",	"H",	"PH",	"PH",	"H",	"H",	"H",	"H",	"H"),	//4,4
				Arrays.asList("PH",	"P",	"P",	"P",	"P",	"H",	"H",	"H",	"H",	"H"),	//6,6
				Arrays.asList("P",	"P",	"P",	"P",	"P",	"P",	"H",	"H",	"H",	"H"),	//7,7
				Arrays.asList("P",	"P",	"P",	"P",	"P",	"P",	"P",	"P",	"P",	"RP"),	//8,8
				Arrays.asList("P",	"P",	"P",	"P",	"P",	"S",	"P",	"P",	"S",	"S"),	//9,9
				Arrays.asList("P",	"P",	"P",	"P",	"P",	"P",	"P",	"P",	"P",	"P")	//A,A
		);

		public String getChoose(String botNumber, List<String> playerNumberList, boolean allowD, boolean allowP, boolean allowR) {
			String playerNumber1 = playerNumberList.get(0);
			int botIndex = "1".equals(botNumber)? 9: Integer.parseInt(botNumber) - 2;
			int playerNumber = 0;
			int playerNum = 0;
			for (String thePlayerNumber : playerNumberList) {
				if ("1".equals(thePlayerNumber)) {
					playerNum ++;
					playerNumber += 11;
				} else {
					playerNumber += Integer.parseInt(thePlayerNumber);
				}
				if (playerNumber > 21 && playerNum > 0) {
					playerNum --;
					playerNumber -= 10;
				}
			}

			String choose;
			if (allowP && !"5".equals(playerNumber1) && !"10".equals(playerNumber1)) {
				int firstPlayerNumber = "1".equals(playerNumber1)? 11: Integer.parseInt(playerNumber1);
				int playerIndex = firstPlayerNumber - 2 - firstPlayerNumber / 5;
				choose = splitMap.get(playerIndex).get(botIndex);
			} else if (playerNum > 0) {
				int playerIndex = MathUtil.range(0, playerNumber - 13, 7);
				choose = softMap.get(playerIndex).get(botIndex);
			} else {
				int playerIndex = MathUtil.range(0, playerNumber - 8, 10);
				choose = hardMap.get(playerIndex).get(botIndex);
			}
			if (choose.length() == 2) {
				switch (choose.charAt(0)) {
					case 'D': choose = allowD? "D": String.valueOf(choose.charAt(1));break;
					case 'P': choose = allowP? "P": String.valueOf(choose.charAt(1));break;
					case 'R': choose = allowR? "R": String.valueOf(choose.charAt(1));break;
					default: throw new AssertException();
				}
			}

			return choose;
		}
	}
}