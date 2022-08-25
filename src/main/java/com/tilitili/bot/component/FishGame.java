package com.tilitili.bot.component;

import com.tilitili.bot.entity.FishPlayer;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.BotUserItemMapping;
import com.tilitili.common.entity.dto.BotItemDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotUserItemMappingMapper;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FishGame {
	private final List<FishPlayer> playerList = new ArrayList<>();
	private final Queue<BotMessageAction> operateQueue = new LinkedList<>();
	private final Random random = new Random(System.currentTimeMillis());
	private final BotManager botManager;
	private final BotUserMapper botUserMapper;
	private final BotUserItemMappingMapper botUserItemMappingMapper;

	@Autowired
	public FishGame(BotManager botManager, BotUserMapper botUserMapper, BotUserItemMappingMapper botUserItemMappingMapper) {
		this.botManager = botManager;
		this.botUserMapper = botUserMapper;
		this.botUserItemMappingMapper = botUserItemMappingMapper;
	}

	@Scheduled(fixedDelay = 100)
	public void run() {
		try {
			for (BotMessageAction messageAction : operateQueue) {
				try {
					String key = messageAction.getKeyWithoutPrefix();
					String virtualKey = messageAction.getVirtualKey();
					switch (virtualKey != null ? virtualKey : key) {
						case "抛竿": this.handleStart(messageAction);break;
						default: break;
					}
				} catch (AssertException e) {
					botManager.sendMessage(BotMessage.simpleTextMessage(e.getMessage(), messageAction.getBotMessage()));
				}
			}
			for (FishPlayer player : playerList) {
				int random = this.random.nextInt(100);
			}
		} catch (Exception e) {
			log.error("钓鱼异常", e);
		}
	}

	private void handleStart(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		BotUser botUser = messageAction.getBotUser();
		List<BotMessageChain> resultList = new ArrayList<>();
		// 获取当前背包
		List<BotItemDTO> botItemList = botUserItemMappingMapper.getItemListByUserId(botUser.getId());
		List<Long> botItemIdList = botItemList.stream().map(BotItemDTO::getItemId).collect(Collectors.toList());
		// 购买道具，失败则回滚
		List<Long> newItemMappingIdList = new ArrayList<>();
		int useScore = 0;
		try {
			if (!botItemIdList.contains(BotItemDTO.FISH_TOOL)) {
				Asserts.isTrue(botUser.getScore() > 90, "你没有积分兑换鱼竿(90)了，真遗憾。");
				resultList.add(BotMessageChain.ofPlain("为您自动兑换鱼竿一把(-90)，谢谢惠顾。"));
				BotUserItemMapping newUserItemMapping = new BotUserItemMapping().setItemId(BotItemDTO.FISH_TOOL).setUserId(botUser.getId());
				botUserItemMappingMapper.addBotUserItemMappingSelective(newUserItemMapping);
				newItemMappingIdList.add(newUserItemMapping.getId());
				useScore += 90;
			}
			if (!botItemIdList.contains(BotItemDTO.FISH_FOOD)) {
				Asserts.isTrue(botUser.getScore() > 90, "你没有积分兑换鱼饵(10)了，真遗憾。");
				useScore += 10;
				resultList.add(BotMessageChain.ofPlain("为您自动兑换鱼饵10份(-10)，谢谢惠顾。"));
				BotUserItemMapping newUserItemMapping = new BotUserItemMapping().setItemId(BotItemDTO.FISH_FOOD).setUserId(botUser.getId());
				botUserItemMappingMapper.addBotUserItemMappingSelective(newUserItemMapping);
				newItemMappingIdList.add(newUserItemMapping.getId());
			}
			if (useScore != 0) {
				botUserMapper.updateBotUserSelective(new BotUser().setId(botUser.getId()).setScore(botUser.getScore() - useScore));
			}
		} catch (Exception e) {
			log.error("自动购买道具异常，回滚", e);
			for (Long userItemMappingId : newItemMappingIdList) {
				botUserItemMappingMapper.deleteBotUserItemMappingByPrimary(userItemMappingId);
			}
			if (useScore != 0) {
				botUserMapper.updateBotUserSelective(new BotUser().setId(botUser.getId()).setScore(botUser.getScore() + useScore));
			}
		}

		FishPlayer newPlayer = new FishPlayer(botSender, botUser).setStatus(FishPlayer.STATUS_FISHING);
		this.playerList.add(newPlayer);


	}
}
