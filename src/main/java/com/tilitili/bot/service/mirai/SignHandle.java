package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotItemConstant;
import com.tilitili.common.entity.BotIcePrice;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotItemDTO;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.BotUserRankDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotIcePriceManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotUserItemMappingMapper;
import com.tilitili.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SignHandle extends ExceptionRespMessageHandle {
	private final static String externalIdLockKey = "signHandle.externalIdLockKey";
	private final BotUserManager botUserManager;
	private final BotUserItemMappingMapper botUserItemMappingMapper;
	private final BotIcePriceManager botIcePriceManager;

	private List<String> noticeList;
	@Value("${SignHandle.noticeList:提说新开了一处钓场（河流），一起去看看吧！指令：（前往 河流）}")
	public void setNoticeListStr(String noticeListStr) {
		try {
			this.noticeList = Arrays.stream(noticeListStr.split("\n")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
			log.info("刷新noticeList成功，{}", this.noticeList);
		} catch (Exception e) {
			log.error("刷新noticeList异常", e);
		}
	}

	@Autowired
	public SignHandle(BotUserManager botUserManager, BotUserItemMappingMapper botUserItemMappingMapper, BotIcePriceManager botIcePriceManager) {
		this.botUserManager = botUserManager;
		this.botUserItemMappingMapper = botUserItemMappingMapper;
		this.botIcePriceManager = botIcePriceManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();

		switch (key) {
			case "签到": case "qd": return handleSignMessage(messageAction);
			case "积分排行": case "jfph": return handleQueryRankMessage(messageAction);
			case "积分查询": case "jfcx": return handleQueryScoreMessage(messageAction);
			case "v我": return handleBorrow(messageAction);
			default: return null;
		}
	}

	private BotMessage handleBorrow(BotMessageAction messageAction) {
		String scoreStr = messageAction.getValue();
		Asserts.isNumber(scoreStr, "格式错啦(积分)");

		return null;
	}

	private BotMessage handleQueryScoreMessage(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		Asserts.notNull(botUser, "啊嘞，似乎不对劲");
		Asserts.notNull(botUser.getScore(), "未绑定");
		List<BotItemDTO> itemDTOList = botUserItemMappingMapper.getItemListByUserId(botUser.getId());
		int itemScore = itemDTOList.stream()
				.filter(StreamUtil.isNotNull(BotItemDTO::getSellPrice)).filter(StreamUtil.isNotNull(BotItemDTO::getNum))
				.map(botItemDTO -> botItemDTO.getNum() * botItemDTO.getSellPrice()).mapToInt(Integer::intValue).sum();
		BotIcePrice botIcePrice = botIcePriceManager.getIcePrice();
		Integer icePrice = botIcePrice.getPrice() != null ? botIcePrice.getPrice() : botIcePrice.getBasePrice();
		Integer iceNum = itemDTOList.stream().filter(StreamUtil.isEqual(BotItemDTO::getName, BotItemConstant.ICE_NAME)).map(BotItemDTO::getNum).findFirst().orElse(0);
		int sumScore = botUser.getScore() + itemScore + icePrice * iceNum;
		return BotMessage.simpleTextMessage(String.format("当前可用积分为%s，总积分为%s分。", botUser.getScore(), sumScore)).setPrivateSend(true);
	}

	private BotMessage handleQueryRankMessage(BotMessageAction messageAction) {
		Long senderId = messageAction.getBotSender().getId();
		List<BotUserRankDTO> rankDTOList = botUserItemMappingMapper.getBotUserScoreRank(senderId, 5);
//		List<BotUser> userList = botUserMapper.getBotUserByCondition(new BotUserQuery().setStatus(0).setSorter("score").setSorted("desc").setPageSize(10));
//		if (userList.size() > 10) userList = userList.subList(0, 10);

		List<BotMessageChain> result = new ArrayList<>();
		result.add(BotMessageChain.ofPlain("排序:分数\t名称"));
		for (int index = 0; index < rankDTOList.size(); index++) {
			BotUserRankDTO rankDTO = rankDTOList.get(index);
//			List<BotItemDTO> itemDTOList = botUserItemMappingMapper.getItemListByUserId(botUser.getId());
//			int itemScore = itemDTOList.stream().map(BotItemDTO::getSellPrice).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
			if (rankDTO.getScore() <= 150) continue;
			result.add(BotMessageChain.ofPlain(String.format("\n%s:%s\t%s", index + 1, rankDTO.getScore(), rankDTO.getName())));
		}
		return BotMessage.simpleListMessage(result);
	}

	public BotMessage handleSignMessage(BotMessageAction messageAction) throws Exception {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotRobot bot = messageAction.getBot();
		BotMessage botMessage = messageAction.getBotMessage();
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		Asserts.notNull(botUser, "似乎有什么不对劲");
		Integer initScore = botUser.getScore();
		Asserts.notNull(initScore, "未绑定");
		Date now = new Date();

		int addScore = 150;
		try {
			if (! session.putIfAbsent(externalIdLockKey + botUser.getId(), "lock", Duration.ofSeconds(1))) {
				log.info("别签到刷屏");
				return null;
			}
			if (botUser.getLastSignTime() != null && botUser.getLastSignTime().after(DateUtils.getCurrentDay())) {
				return BotMessage.simpleTextMessage("已经签到过啦");
			}
			botUserManager.updateBotUserSelective(new BotUserDTO().setId(botUser.getId()).setLastSignTime(now));
			botUserManager.safeUpdateScore(botUser, addScore);
		} finally {
			session.remove(externalIdLockKey + botUser.getId());
		}

		String time = TimeUtil.getTimeTalk();

		int random = ThreadLocalRandom.current().nextInt(noticeList.size() + 1);
		String talk = random == noticeList.size()? "今天也是充满希望的一天": noticeList.get(random);
		String message1 = String.format("%s好，%s", time, talk);
		String message2 = String.format("(分数+%d)", addScore);
		String tips = "";//initScore == 0 && BotUserConstant.USER_TYPE_QQ != botUser.getType()? "（tips：有共同群聊最好先申请合体再游玩积分项目": "";
		String message = message1 + message2 + tips;
		return BotMessage.simpleTextMessage(message, botMessage).setQuote(messageAction.getMessageId()).setPrivateSend(true);
	}
}
