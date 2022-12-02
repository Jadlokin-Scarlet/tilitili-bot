package com.tilitili.bot.service;

import com.tilitili.bot.service.item.BaseItem;
import com.tilitili.common.entity.BotItem;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BotItemService {
	private final Map<String, BaseItem> itemMap;

	@Autowired
	public BotItemService(List<BaseItem> itemList) {
		this.itemMap = new HashMap<>();
		for (BaseItem baseItem : itemList) {
			this.itemMap.put(baseItem.getName(), baseItem);
		}
	}

	public boolean useItem(BotSender botSender, BotUserDTO botUser, BotItem botItem) {
		BaseItem item = itemMap.get(botItem.getName());
		Asserts.notNull(item, "这个用不了吧。");
		return item.useItem(botSender, botUser, botItem);
	}
}
