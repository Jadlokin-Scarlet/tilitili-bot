package com.tilitili.bot.service.item;

import com.tilitili.common.entity.BotItem;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserItemMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.mapper.mysql.BotUserItemMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Viagra extends BaseItem {
	private final BotUserItemMappingMapper botUserItemMappingMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;
	private final RedisCache redisCache;

	@Autowired
	public Viagra(BotUserItemMappingMapper botUserItemMappingMapper, BotUserItemMappingManager botUserItemMappingManager, RedisCache redisCache) {
		this.botUserItemMappingMapper = botUserItemMappingMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.redisCache = redisCache;
	}

	@Override
	public String getName() {
		return "伟哥";
	}

	@Override
	public boolean useItem(BotSender botSender, BotUserDTO botUser, BotItem botItem) {
		Long userId = botUser.getId();
		BotUserItemMapping botUserItemMapping = botUserItemMappingMapper.getBotUserItemMappingByUserIdAndItemId(userId, botItem.getId());
		Asserts.notNull(botUserItemMapping, "你还没拥有他");
		Integer cnt = botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(botItem.getId()).setNum(1));
		Asserts.checkEquals(cnt, 1, "使用失败惹");

		String redisKey = String.format("CattleHandle-%s", userId);
		redisCache.delete(redisKey);
		return true;
	}
}
