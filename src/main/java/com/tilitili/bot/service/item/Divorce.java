package com.tilitili.bot.service.item;

import com.tilitili.common.entity.BotFavorite;
import com.tilitili.common.entity.BotItem;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserItemMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.mapper.mysql.BotFavoriteMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Divorce extends BaseItem {
	private final BotUserItemMappingManager botUserItemMappingManager;
	private final BotFavoriteMapper botFavoriteMapper;

	@Autowired
	public Divorce(BotUserItemMappingManager botUserItemMappingManager, BotFavoriteMapper botFavoriteMapper) {
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.botFavoriteMapper = botFavoriteMapper;
	}

	@Override
	public String getName() {
		return "休书";
	}

	@Override
	public boolean useItem(BotSender botSender, BotUserDTO botUser, BotItem botItem) {
		Long userId = botUser.getId();
		BotFavorite botFavorite = botFavoriteMapper.getBotFavoriteByUserId(userId);
		Asserts.notNull(botFavorite, "哪来的老婆");

		Integer cnt = botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(botItem.getId()).setNum(-1));
		Asserts.checkEquals(cnt, -1, "使用失败惹");

		botFavoriteMapper.deleteBotFavoriteByPrimary(botFavorite.getId());
		return true;
	}
}
