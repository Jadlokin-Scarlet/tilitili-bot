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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AddTweetSubscriptionHandle extends ExceptionRespMessageHandle {
    private final SubscriptionMapper subscriptionMapper;
    private final TwitterManager twitterManager;

    @Autowired
    public AddTweetSubscriptionHandle(SubscriptionMapper subscriptionMapper, TwitterManager twitterManager) {
        this.subscriptionMapper = subscriptionMapper;
        this.twitterManager = twitterManager;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String name = messageAction.getParamOrDefault("name", messageAction.getValue());
        BotSender botSender = messageAction.getBotSender();
        Long senderId = botSender.getId();

        Asserts.notBlank(name, "格式错啦(name)");

        TwitterUser user = twitterManager.getUserByUserName(name);
        Asserts.notNull(user, "没找到用户");
        String userId = user.getRestId();
        String nike = user.getLegacy().getNike();

        SubscriptionQuery subscriptionQuery = new SubscriptionQuery().setStatus(0).setType(2).setValue(userId).setSenderId(senderId);
        int oldCount = subscriptionMapper.countSubscriptionByCondition(subscriptionQuery);
        Asserts.isTrue(oldCount == 0, "已经关注了哦。");

        Subscription add = new Subscription().setValue(userId).setType(2).setSenderId(senderId).setName(nike);
        subscriptionMapper.addSubscriptionSelective(add);

        return BotMessage.simpleTextMessage(String.format("关注%s成功！", nike));
    }
}
