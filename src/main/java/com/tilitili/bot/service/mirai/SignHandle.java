package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.entity.BotIcePrice;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotItemDTO;
import com.tilitili.common.entity.dto.BotUserDTO;
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
import com.tilitili.common.utils.TimeUtil;
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
		Integer iceNum = itemDTOList.stream().filter(StreamUtil.isEqual(BotItemDTO::getName, BotItemDTO.ICE_NAME)).map(BotItemDTO::getNum).findFirst().orElse(0);
		int sumScore = botUser.getScore() + itemScore + icePrice * iceNum;
		return BotMessage.simpleTextMessage(String.format("当前可用积分为%s，总积分为%s分。", botUser.getScore(), sumScore));
	}

	private BotMessage handleQueryRankMessage(BotMessageAction messageAction) {
		List<BotUserRankDTO> rankDTOList = botUserItemMappingMapper.getBotUserScoreRank(5);
//		List<BotUser> userList = botUserMapper.getBotUserByCondition(new BotUserQuery().setStatus(0).setSorter("score").setSorted("desc").setPageSize(10));
//		if (userList.size() > 10) userList = userList.subList(0, 10);

		List<BotMessageChain> result = new ArrayList<>();
		result.add(BotMessageChain.ofPlain("排序:分数\t名称"));
		for (int index = 0; index < rankDTOList.size(); index++) {
			BotUserRankDTO rankDTO = rankDTOList.get(index);
//			List<BotItemDTO> itemDTOList = botUserItemMappingMapper.getItemListByUserId(botUser.getId());
//			int itemScore = itemDTOList.stream().map(BotItemDTO::getSellPrice).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
			if (rankDTO.getScore() <= 100) continue;
			result.add(BotMessageChain.ofPlain(String.format("\n%s:%s\t%s", index + 1, rankDTO.getScore(), rankDTO.getName())));
		}
		return BotMessage.simpleListMessage(result);
	}

	public BotMessage handleSignMessage(BotMessageAction messageAction) throws Exception {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotMessage botMessage = messageAction.getBotMessage();
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		Asserts.notNull(botUser, "似乎有什么不对劲");
		Integer initScore = botUser.getScore();
		Asserts.notNull(initScore, "未绑定");
		Date now = new Date();
		if (! session.putIfAbsent(externalIdLockKey + botUser.getId(), "lock")) {
			log.info("别签到刷屏");
			return null;
		}

		int addScore;
		try {
			List<BotItemDTO> itemDTOList = botUserItemMappingMapper.getItemListByUserId(botUser.getId());
			int itemScore = itemDTOList.stream().map(BotItemDTO::getSellPrice).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
			BotIcePrice botIcePrice = botIcePriceManager.getIcePrice();
			Integer icePrice = botIcePrice.getPrice() != null ? botIcePrice.getPrice() : botIcePrice.getBasePrice();
			Integer iceNum = itemDTOList.stream().filter(StreamUtil.isEqual(BotItemDTO::getName, BotItemDTO.ICE_NAME)).map(BotItemDTO::getNum).findFirst().orElse(0);
			int sumScore = initScore + itemScore + icePrice * iceNum;
			if (sumScore >= 100) {
				log.info("积分满了");
				return null;
			}
			if (botUser.getLastSignTime() != null && botUser.getLastSignTime().after(DateUtils.getCurrentDay())) {
				log.info("已经签到过了");
				return null;
			}
			addScore = Math.max(100 - sumScore, 0);
			botUserManager.updateBotUserSelective(botSender, new BotUserDTO().setId(botUser.getId()).setLastSignTime(now));
			if (addScore != 0) {
				botUserManager.safeUpdateScore(botUser, addScore);
			}
		} finally {
			session.remove(externalIdLockKey + botUser.getId());
		}


		String time = TimeUtil.getTimeTalk();

		String talk = "今天也是充满希望的一天";
		String message1 = String.format("%s好，%s", time, talk);
		String message2 = String.format("(分数+%d)", addScore);
		String tips = initScore == 0 && BotUserConstant.USER_TYPE_QQ != botUser.getType()? "（tips：有共同群聊最好先申请合体再游玩积分项目": "";
		String message = message1 + (addScore == 0? "": message2) + tips;
		return BotMessage.simpleTextMessage(message, botMessage).setQuote(messageAction.getMessageId());
	}
}
