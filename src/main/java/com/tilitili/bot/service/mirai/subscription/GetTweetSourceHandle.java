package com.tilitili.bot.service.mirai.subscription;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.Subscription;
import com.tilitili.common.entity.Tweet;
import com.tilitili.common.entity.query.SubscriptionQuery;
import com.tilitili.common.entity.query.TweetQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.SubscriptionMapper;
import com.tilitili.common.mapper.mysql.TweetMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetTweetSourceHandle extends ExceptionRespMessageHandle {
	private final TweetMapper tweetMapper;
	private final SubscriptionMapper subscriptionMapper;

	@Autowired
	public GetTweetSourceHandle(TweetMapper tweetMapper, SubscriptionMapper subscriptionMapper) {
		this.tweetMapper = tweetMapper;
		this.subscriptionMapper = subscriptionMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String name = messageAction.getParamOrDefault("name", messageAction.getValue());

		List<Subscription> subscriptionList = subscriptionMapper.getSubscriptionByCondition(new SubscriptionQuery().setName(name).setStatus(0).setType(2).setPageSize(1));
		Asserts.notEmpty(subscriptionList, "还没有被关注哦");
		String uid = subscriptionList.get(0).getValue();
		Asserts.notBlank(uid, "找不到uid");

		List<Tweet> tweetList = tweetMapper.getTweetByCondition(new TweetQuery().setUid(uid).setPageSize(1).setSorter("id").setSorted("desc"));
		Asserts.notEmpty(tweetList, "还没有数据");
		Tweet tweet = tweetList.get(0);

		return BotMessage.simpleTextMessage(tweet.getText());
	}
}
