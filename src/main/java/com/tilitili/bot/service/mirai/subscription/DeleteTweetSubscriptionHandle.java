package com.tilitili.bot.service.mirai.subscription;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.Subscription;
import com.tilitili.common.entity.query.SubscriptionQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.twitter.user.TwitterUser;
import com.tilitili.common.manager.TwitterManager;
import com.tilitili.common.mapper.mysql.SubscriptionMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeleteTweetSubscriptionHandle extends ExceptionRespMessageHandle {
	private final SubscriptionMapper subscriptionMapper;
	private final TwitterManager twitterManager;

	@Autowired
	public DeleteTweetSubscriptionHandle(SubscriptionMapper subscriptionMapper, TwitterManager twitterManager) {
		this.subscriptionMapper = subscriptionMapper;
		this.twitterManager = twitterManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String name = messageAction.getParamOrDefault("name", messageAction.getValue());
		BotSender botSender = messageAction.getBotSender();

		Asserts.notBlank(name, "格式错啦(name)");

		TwitterUser user = twitterManager.getUserByUserName(name);
		Asserts.notNull(user, "没找到用户");
		String userId = user.getRestId();

		SubscriptionQuery subscriptionQuery = new SubscriptionQuery().setStatus(0).setType(2).setValue(userId).setSenderId(botSender.getId());
		List<Subscription> oldList = subscriptionMapper.getSubscriptionByCondition(subscriptionQuery);
		Asserts.notEmpty(oldList, "还没关注哦。");
		Asserts.checkEquals(oldList.size(), 1, "不太对劲。");
		Subscription old = oldList.get(0);

		subscriptionMapper.updateSubscriptionSelective(new Subscription().setId(old.getId()).setStatus(-1));

		return BotMessage.simpleTextMessage(String.format("取关%s成功", old.getName()));
	}
}
