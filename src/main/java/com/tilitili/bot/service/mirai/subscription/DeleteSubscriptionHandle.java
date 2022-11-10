package com.tilitili.bot.service.mirai.subscription;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.Owner;
import com.tilitili.common.entity.Subscription;
import com.tilitili.common.entity.query.SubscriptionQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BilibiliManager;
import com.tilitili.common.mapper.mysql.SubscriptionMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeleteSubscriptionHandle extends ExceptionRespMessageHandle {
	private final SubscriptionMapper subscriptionMapper;
	private final BilibiliManager bilibiliManager;

	@Autowired
	public DeleteSubscriptionHandle(SubscriptionMapper subscriptionMapper, BilibiliManager bilibiliManager) {
		this.subscriptionMapper = subscriptionMapper;
		this.bilibiliManager = bilibiliManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String key = messageAction.getParamOrDefault("key", messageAction.getValue());
		BotSender botSender = messageAction.getBotSender();
		Long senderId = botSender.getId();

		Asserts.notBlank(key, "格式错啦(key)");

		Owner owner = bilibiliManager.getOwnerWithCacheByUidOrName(key);
		Long uid = owner.getUid();
		String name = owner.getName();

		SubscriptionQuery subscriptionQuery = new SubscriptionQuery().setStatus(0).setType(1).setValue(String.valueOf(uid)).setSenderId(senderId);
		List<Subscription> oldList = subscriptionMapper.getSubscriptionByCondition(subscriptionQuery);
		Asserts.notEmpty(oldList, "还没关注哦。");
		Asserts.checkEquals(oldList.size(), 1, "不太对劲。");
		Subscription old = oldList.get(0);

		subscriptionMapper.updateSubscriptionSelective(new Subscription().setId(old.getId()).setStatus(-1));

		return BotMessage.simpleTextMessage(String.format("取关%s成功", name));
	}
}
