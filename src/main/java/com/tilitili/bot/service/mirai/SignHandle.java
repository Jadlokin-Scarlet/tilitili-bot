package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotIcePrice;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.dto.BotItemDTO;
import com.tilitili.common.entity.dto.BotUserRankDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotIcePriceManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotUserItemMappingMapper;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import com.tilitili.common.utils.StreamUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component
public class SignHandle extends ExceptionRespMessageHandle {
	private final static String externalIdLockKey = "signHandle.externalIdLockKey";
	private final BotUserMapper botUserMapper;
	private final BotUserManager botUserManager;
	private final BotUserItemMappingMapper botUserItemMappingMapper;
	private final BotIcePriceManager botIcePriceManager;

	@Autowired
	public SignHandle(BotUserMapper botUserMapper, BotUserManager botUserManager, BotUserItemMappingMapper botUserItemMappingMapper, BotIcePriceManager botIcePriceManager) {
		this.botUserMapper = botUserMapper;
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
			default: return null;
		}
	}

	private BotMessage handleQueryScoreMessage(BotMessageAction messageAction) {
		BotUser botUser = messageAction.getBotUser();
		Asserts.notNull(botUser, "啊嘞，似乎不对劲");
		List<BotItemDTO> itemDTOList = botUserItemMappingMapper.getItemListByUserId(botUser.getId());
		int itemScore = itemDTOList.stream().map(BotItemDTO::getSellPrice).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
		BotIcePrice botIcePrice = botIcePriceManager.getIcePrice();
		Integer icePrice = botIcePrice.getPrice() != null ? botIcePrice.getPrice() : botIcePrice.getBasePrice();
		Integer iceNum = itemDTOList.stream().filter(StreamUtil.isEqual(BotItemDTO::getName, BotItemDTO.ICE_NAME)).map(BotItemDTO::getNum).findFirst().orElse(0);
		int sumScore = botUser.getScore() + itemScore + icePrice * iceNum;
		return BotMessage.simpleTextMessage(String.format("当前积分为%s分。", sumScore));
	}

	private BotMessage handleQueryRankMessage(BotMessageAction messageAction) {
		List<BotUserRankDTO> rankDTOList = botUserItemMappingMapper.getBotUserScoreRank(10);
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
		BotMessage botMessage = messageAction.getBotMessage();
		Long externalId = botMessage.getSendType().equals(SendTypeEmum.GUILD_MESSAGE_STR)? botMessage.getTinyId(): botMessage.getQq();
		Asserts.notNull(externalId, "似乎哪里不对劲");
		Date now = new Date();
		if (! session.putIfAbsent(externalIdLockKey + externalId, "lock")) {
			log.info("别签到刷屏");
			return null;
		}

		int addScore;
		try {
			BotUser botUser = botUserMapper.getBotUserByExternalId(externalId);
			Asserts.notNull(botUser, "似乎有什么不对劲");
			List<BotItemDTO> itemDTOList = botUserItemMappingMapper.getItemListByUserId(botUser.getId());
			int itemScore = itemDTOList.stream().map(BotItemDTO::getSellPrice).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
			BotIcePrice botIcePrice = botIcePriceManager.getIcePrice();
			Integer icePrice = botIcePrice.getPrice() != null ? botIcePrice.getPrice() : botIcePrice.getBasePrice();
			Integer iceNum = itemDTOList.stream().filter(StreamUtil.isEqual(BotItemDTO::getName, BotItemDTO.ICE_NAME)).map(BotItemDTO::getNum).findFirst().orElse(0);
			int sumScore = botUser.getScore() + itemScore + icePrice * iceNum;
			if (sumScore >= 150) {
				log.info("积分满了");
				return null;
			}
			if (botUser.getLastSignTime() != null && botUser.getLastSignTime().after(DateUtils.getCurrentDay())) {
				log.info("已经签到过了");
				return null;
			}
			addScore = Math.max(150 - sumScore, 0);
			botUserMapper.updateBotUserSelective(new BotUser().setId(botUser.getId()).setLastSignTime(now));
			if (addScore != 0) {
				botUserManager.safeUpdateScore(botUser.getId(), botUser.getScore(), addScore);
			}
		} finally {
			session.remove(externalIdLockKey + externalId);
		}


		int hour = Integer.parseInt(new SimpleDateFormat("HH", Locale.CHINESE).format(now));
		String time = "早上";
		if (hour > 9) time = "中午";
		if (hour > 12) time = "下午";
		if (hour > 18) time = "晚上";

		String talk = "今天也是充满希望的一天";
		String message1 = String.format("%s好，%s", time, talk);
		String message2 = String.format("(分数+%d)", addScore);
		String message = message1 + (addScore == 0? "": message2);
		return BotMessage.simpleTextMessage(message, botMessage).setQuote(messageAction.getMessageId());
	}
}
