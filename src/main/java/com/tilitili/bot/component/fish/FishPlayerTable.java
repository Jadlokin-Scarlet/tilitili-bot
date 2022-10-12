package com.tilitili.bot.component.fish;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.FishPlayer;
import com.tilitili.common.entity.dto.BotItemDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.mapper.mysql.BotUserItemMappingMapper;
import com.tilitili.common.mapper.mysql.FishPlayerMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

@Slf4j
public class FishPlayerTable {
	//	public static final Integer STATUS_WAIT = 0;
	public static final Integer STATUS_FISHING = 1;
	public static final Integer STATUS_COLLECT = 2;
	private final Queue<BotMessageAction> operateQueue;

	private final FishPlayer fishPlayer;
	private final BotManager botManager;
	private final FishPlayerMapper fishPlayerMapper;
	private final BotUserItemMappingMapper botUserItemMappingMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;

	public FishPlayerTable(FishPlayer fishPlayer, BotManager botManager, FishPlayerMapper fishPlayerMapper, BotUserItemMappingMapper botUserItemMappingMapper, BotUserItemMappingManager botUserItemMappingManager) {
		this.fishPlayer = fishPlayer;
		this.botManager = botManager;
		this.fishPlayerMapper = fishPlayerMapper;
		this.botUserItemMappingMapper = botUserItemMappingMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;

		this.operateQueue = new LinkedList<>();
	}

	public void run() {
		while (!operateQueue.isEmpty()) {
			BotMessageAction messageAction = operateQueue.poll();
			BotMessage botMessage = null;
			try {
				botMessage = this.handleOperate(messageAction);
			} catch (Exception e) {
				log.error("解析玩家指令异常", e);
			}
			// 只处理一个成功的
			if (botMessage != null) {
				try {
					botManager.sendMessage(botMessage.setSender(messageAction.getBotMessage()).setQuote(messageAction.getMessageId()));
				} catch (Exception e) {
					log.error("钓鱼提示发送失败", e);
				}
				break;
			}
		}
		operateQueue.clear();
	}

	private BotMessage handleOperate(BotMessageAction messageAction) {
		try {
			String key = messageAction.getKeyWithoutPrefix();
			String virtualKey = messageAction.getVirtualKey();
			switch (virtualKey != null ? virtualKey : key) {
				case "抛竿": return this.handleStart();
				case "收杆": return this.handleEnd();
				default: return null;
			}
		} catch (AssertException e) {
			return BotMessage.simpleTextMessage(e.getMessage());
		}
	}

	private BotMessage handleStart() {
		Long userId = this.fishPlayer.getUserId();

		List<BotMessageChain> resultList = new ArrayList<>();
		// 获取当前背包
		List<BotItemDTO> botItemList = botUserItemMappingMapper.getItemListByUserId(userId);
		List<Long> botItemIdList = botItemList.stream().map(BotItemDTO::getItemId).collect(Collectors.toList());

		// 兑换道具
		if (!botItemIdList.contains(BotItemDTO.FISH_TOOL)) {
			botUserItemMappingManager.safeBuyItem(userId, BotItemDTO.FISH_TOOL, 1);
			resultList.add(BotMessageChain.ofPlain("为您自动兑换鱼竿一把(-90)，谢谢惠顾。"));
		}

		if (!botItemIdList.contains(BotItemDTO.FISH_FOOD)) {
			botUserItemMappingManager.safeBuyItem(userId, BotItemDTO.FISH_FOOD, 10);
			resultList.add(BotMessageChain.ofPlain("为您自动兑换鱼饵10份(-10)，谢谢惠顾。"));
		}

		this.updateFishPayerStatus(STATUS_FISHING);
		resultList.add(BotMessageChain.ofPlain("抛竿成功，有动静我会再叫你哦。"));
		return BotMessage.simpleListMessage(resultList);
	}

	private BotMessage handleEnd() {
		Long userId = this.fishPlayer.getUserId();

		List<BotMessageChain> resultList = new ArrayList<>();

//		botUserItemMappingMapper.get
//		botUserItemMappingMapper.updateBotUserItemMappingSelective()
		return BotMessage.simpleListMessage(resultList);
	}

	private void updateFishPayerStatus(Integer status) {
		fishPlayerMapper.updateFishPlayerSelective(new FishPlayer().setId(fishPlayer.getId()).setStatus(status));
		this.fishPlayer.setStatus(status);
	}

	public Long getPlayerId() {
		if (fishPlayer == null) return null;
		return fishPlayer.getId();
	}

	public void addOperate(BotMessageAction messageAction) {
		operateQueue.add(messageAction);
	}
}
