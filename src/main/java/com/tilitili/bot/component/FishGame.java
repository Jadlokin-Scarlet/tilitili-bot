package com.tilitili.bot.component;

import com.tilitili.bot.entity.FishPlayer;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.BotUserItemMapping;
import com.tilitili.common.entity.query.BotUserItemMappingQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotUserItemMappingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class FishGame {
	private final List<FishPlayer> playerList = new ArrayList<>();
	private final Queue<BotMessageAction> operateQueue = new LinkedList<>();
	private final Random random = new Random(System.currentTimeMillis());
	private final BotManager botManager;
	private final BotUserItemMappingMapper botUserItemMappingMapper;

	@Autowired
	public FishGame(BotManager botManager, BotUserItemMappingMapper botUserItemMappingMapper) {
		this.botManager = botManager;
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

		List<BotUserItemMapping> itemMappingList = botUserItemMappingMapper.getBotUserItemMappingByCondition(new BotUserItemMappingQuery().setUserId(botUser.getId()));


		FishPlayer newPlayer = new FishPlayer(botSender, botUser).setStatus(FishPlayer.STATUS_FISHING);
		this.playerList.add(newPlayer);
	}

}
