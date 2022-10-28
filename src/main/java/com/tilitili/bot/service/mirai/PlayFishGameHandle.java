package com.tilitili.bot.service.mirai;

import com.tilitili.bot.component.fish.FishGame;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.FishPlayer;
import com.tilitili.common.entity.dto.BotItemDTO;
import com.tilitili.common.entity.dto.SafeTransactionDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.mapper.mysql.BotUserItemMappingMapper;
import com.tilitili.common.mapper.mysql.FishPlayerMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PlayFishGameHandle extends ExceptionRespMessageToSenderHandle {
	public static final Integer STATUS_WAIT = 0;
	public static final Integer STATUS_FISHING = 1;
	public static final Integer STATUS_COLLECT = 2;
	public static final Integer STATUS_FINALL = 3;
//	private final FishGame fishGame;
	private final FishPlayerMapper fishPlayerMapper;
	private final BotUserItemMappingMapper botUserItemMappingMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;

	@Autowired
	public PlayFishGameHandle(FishGame fishGame, FishPlayerMapper fishPlayerMapper, BotUserItemMappingMapper botUserItemMappingMapper, BotUserItemMappingManager botUserItemMappingManager) {
//		this.fishGame = fishGame;
		this.fishPlayerMapper = fishPlayerMapper;
		this.botUserItemMappingMapper = botUserItemMappingMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		switch (messageAction.getKey()) {
			case "抛竿": return handleStart(messageAction);
			default: throw new AssertException();
		}
//		fishGame.addOperate(messageAction);
	}

	private BotMessage handleStart(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		BotUser botUser = messageAction.getBotUser();
		Long senderId = botSender.getId();
		Long userId = botUser.getId();

		FishPlayer fishPlayer = fishPlayerMapper.getVaildFishPlayerByUserId(userId);
		if (fishPlayer == null) {
			fishPlayer = new FishPlayer();
			fishPlayer.setSenderId(senderId);
			fishPlayer.setUserId(userId);
			fishPlayer.setStatus(STATUS_WAIT);
			fishPlayerMapper.addFishPlayerSelective(fishPlayer);
		}
		List<BotMessageChain> resultList = new ArrayList<>();

		List<BotItemDTO> botItemList = botUserItemMappingMapper.getItemListByUserId(userId);
		List<Long> botItemIdList = botItemList.stream().map(BotItemDTO::getItemId).collect(Collectors.toList());

		// 兑换道具
		if (!botItemIdList.contains(BotItemDTO.FISH_TOOL)) {
			botUserItemMappingManager.safeBuyItem(new SafeTransactionDTO().setUserId(userId).setItemId(BotItemDTO.FISH_TOOL));
			resultList.add(BotMessageChain.ofPlain("为您自动兑换鱼竿一把(-90)，谢谢惠顾。"));
		}

		if (!botItemIdList.contains(BotItemDTO.FISH_FOOD)) {
			botUserItemMappingManager.safeBuyItem(new SafeTransactionDTO().setUserId(userId).setItemId(BotItemDTO.FISH_FOOD).setItemNum(10));
			resultList.add(BotMessageChain.ofPlain("为您自动兑换鱼饵10份(-10)，谢谢惠顾。"));
		}

		Integer updCnt = fishPlayerMapper.safeUpdateStatus(fishPlayer.getId(), fishPlayer.getStatus(), STATUS_FISHING);
		Asserts.checkEquals(updCnt, 1, "啊嘞，不对劲");
		resultList.add(BotMessageChain.ofPlain("抛竿成功，有动静我会再叫你哦。"));
		return BotMessage.simpleListMessage(resultList);
	}

}
