package com.tilitili.bot.component.fish;

import com.tilitili.bot.component.Game;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.FishPlayer;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.mapper.mysql.BotUserItemMappingMapper;
import com.tilitili.common.mapper.mysql.FishPlayerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class FishGame implements Game {
	private final Map<Long, FishPlayerTable> tableMap;

	private final BotManager botManager;
	private final FishPlayerMapper fishPlayerMapper;
	private final BotUserItemMappingMapper botUserItemMappingMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;

	public FishGame(BotManager botManager, FishPlayerMapper fishPlayerMapper, BotUserItemMappingMapper botUserItemMappingMapper, BotUserItemMappingManager botUserItemMappingManager) {
		this.botManager = botManager;
		this.fishPlayerMapper = fishPlayerMapper;
		this.botUserItemMappingMapper = botUserItemMappingMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;

		this.tableMap = new HashMap<>();
	}

	@Override
	public void run() {
		for (FishPlayerTable fishPlayerTable : tableMap.values()) {
			try {
				fishPlayerTable.run();
			} catch (Exception e) {
				log.error("钓鱼玩家异常", e);
			}
		}
	}

	public void newTableIfExited(BotMessageAction messageAction) {
		Long senderId = messageAction.getBotSender().getId();
		Long userId = messageAction.getBotUser().getId();
		FishPlayer fishPlayer = fishPlayerMapper.getFishPlayerByUserId(userId);
		if (fishPlayer == null) {
			fishPlayer = new FishPlayer().setSenderId(senderId).setUserId(userId);
			fishPlayerMapper.addFishPlayerSelective(fishPlayer);
		}
		if (!tableMap.containsKey(fishPlayer.getId())) {
			FishPlayerTable fishPlayerTable = new FishPlayerTable(fishPlayer, botManager, fishPlayerMapper, botUserItemMappingMapper, botUserItemMappingManager);
			this.tableMap.put(fishPlayerTable.getPlayerId(), fishPlayerTable);
		}
	}

	public void addOperate(BotMessageAction messageAction) {
		Long senderId = messageAction.getBotSender().getId();
		this.newTableIfExited(messageAction);
		FishPlayerTable fishPlayerTable = tableMap.get(senderId);
		fishPlayerTable.addOperate(messageAction);
	}

}
