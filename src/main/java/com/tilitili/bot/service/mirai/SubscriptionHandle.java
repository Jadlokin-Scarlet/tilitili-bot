package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.Owner;
import com.tilitili.common.entity.Subscription;
import com.tilitili.common.entity.Tweet;
import com.tilitili.common.entity.query.SubscriptionQuery;
import com.tilitili.common.entity.query.TweetQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.twitter.user.TwitterUser;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BilibiliManager;
import com.tilitili.common.manager.TwitterManager;
import com.tilitili.common.mapper.mysql.SubscriptionMapper;
import com.tilitili.common.mapper.mysql.TweetMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class SubscriptionHandle extends ExceptionRespMessageHandle {
    private final SubscriptionMapper subscriptionMapper;
    private final BilibiliManager bilibiliManager;
    private final TwitterManager twitterManager;
    private final TweetMapper tweetMapper;

    @Autowired
    public SubscriptionHandle(SubscriptionMapper subscriptionMapper, BilibiliManager bilibiliManager, TwitterManager twitterManager, TweetMapper tweetMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.bilibiliManager = bilibiliManager;
        this.twitterManager = twitterManager;
        this.tweetMapper = tweetMapper;
    }

    @Override
    protected List<BotMessage> mockMessageInWaiteSender(BotMessageAction messageAction) {
        switch (messageAction.getKeyWithoutPrefix()) {
            case "关注": {
                String key = messageAction.getBodyOrDefault("key", messageAction.getValue());
                if ("23210308".equals(key)) {
                    return Collections.singletonList(BotMessage.simpleTextMessage(String.format("关注%s成功！", "Jadlokin_Scarlet")));
                } else if ("1".equals(key)) {
                    return Collections.singletonList(BotMessage.simpleTextMessage(String.format("关注%s成功！", "bishi")));
                } else {
                    return Collections.singletonList(BotMessage.simpleTextMessage("格式错啦(key)"));
                }
            }
            case "取关": {
                String key = messageAction.getBodyOrDefault("key", messageAction.getValue());
                if ("23210308".equals(key)) {
                    return Collections.singletonList(BotMessage.simpleTextMessage(String.format("切割%s喵", "Jadlokin_Scarlet")));
                } else if ("1".equals(key)) {
                    return Collections.singletonList(BotMessage.simpleTextMessage(String.format("切割%s喵", "bishi")));
                } else {
                    return Collections.singletonList(BotMessage.simpleTextMessage("格式错啦(key)"));
                }
            }
            default: throw new AssertException();
        }
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        switch (messageAction.getKeyWithoutPrefix()) {
            case "关注b博": return handleAddDynamic(messageAction);
            case "取关b博": return handleDeleteDynamic(messageAction);
            case "关注": return handleAddSubscription(messageAction);
            case "取关": return handleDeleteSubscription(messageAction);
            case "关注推特": return handleAddTweet(messageAction);
            case "取关推特": return handleDeleteTweet(messageAction);
            case "ttoken": return handleTToken(messageAction);
            default: throw new AssertException();
        }
    }

    private BotMessage handleTToken(BotMessageAction messageAction) {
        String name = messageAction.getBodyOrDefault("name", messageAction.getValue());

        List<Subscription> subscriptionList = subscriptionMapper.getSubscriptionByCondition(new SubscriptionQuery().setName(name).setStatus(0).setType(2).setPageSize(1));
        Asserts.notEmpty(subscriptionList, "还没有被关注哦");
        String uid = subscriptionList.get(0).getValue();
        Asserts.notBlank(uid, "找不到uid");

        List<Tweet> tweetList = tweetMapper.getTweetByCondition(new TweetQuery().setUid(uid).setPageSize(1).setSorter("id").setSorted("desc"));
        Asserts.notEmpty(tweetList, "还没有数据");
        Tweet tweet = tweetList.get(0);

        return BotMessage.simpleTextMessage(tweet.getText());
    }

    private BotMessage handleDeleteTweet(BotMessageAction messageAction) {
        String name = messageAction.getBodyOrDefault("name", messageAction.getValue());
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

    private BotMessage handleAddTweet(BotMessageAction messageAction) {
        String name = messageAction.getBodyOrDefault("name", messageAction.getValue());
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

    private BotMessage handleDeleteSubscription(BotMessageAction messageAction) {
        String key = messageAction.getBodyOrDefault("key", messageAction.getValue());
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

        return BotMessage.simpleTextMessage(String.format("切割%s喵", name));
    }

    private BotMessage handleAddSubscription(BotMessageAction messageAction) {
        String key = messageAction.getBodyOrDefault("key", messageAction.getValue());
        BotSender botSender = messageAction.getBotSender();
        Long senderId = botSender.getId();

        Asserts.notBlank(key, "格式错啦(key)");

        Owner owner = bilibiliManager.getOwnerWithCacheByUidOrName(key);
        Long uid = owner.getUid();
        String name = owner.getName();
        Long roomId = owner.getRoomId();
        Asserts.notNull(roomId, "直播间不存在");

        SubscriptionQuery subscriptionQuery = new SubscriptionQuery().setStatus(0).setType(1).setValue(String.valueOf(uid)).setSenderId(senderId);
        int oldCount = subscriptionMapper.countSubscriptionByCondition(subscriptionQuery);
        Asserts.isTrue(oldCount == 0, "已经关注了哦。");

        Subscription add = new Subscription().setValue(String.valueOf(uid)).setType(1).setSenderId(senderId).setName(name).setRoomId(roomId);
        subscriptionMapper.addSubscriptionSelective(add);

        return BotMessage.simpleTextMessage(String.format("关注%s成功！", name));
    }

    private BotMessage handleDeleteDynamic(BotMessageAction messageAction) {
        String key = messageAction.getBodyOrDefault("key", messageAction.getValue());
        BotSender botSender = messageAction.getBotSender();
        Long senderId = botSender.getId();

        Asserts.notBlank(key, "格式错啦(key)");

        Owner owner = bilibiliManager.getOwnerWithCacheByUidOrName(key);
        Long uid = owner.getUid();
        String name = owner.getName();

        SubscriptionQuery subscriptionQuery = new SubscriptionQuery().setStatus(0).setType(3).setValue(String.valueOf(uid)).setSenderId(senderId);
        List<Subscription> oldList = subscriptionMapper.getSubscriptionByCondition(subscriptionQuery);
        Asserts.notEmpty(oldList, "还没关注哦。");
        Asserts.checkEquals(oldList.size(), 1, "不太对劲。");
        Subscription old = oldList.get(0);

        subscriptionMapper.updateSubscriptionSelective(new Subscription().setId(old.getId()).setStatus(-1));

        return BotMessage.simpleTextMessage(String.format("取关%s成功", name));
    }

    private BotMessage handleAddDynamic(BotMessageAction messageAction) {
        String key = messageAction.getBodyOrDefault("key", messageAction.getValue());
        BotSender botSender = messageAction.getBotSender();
        Long senderId = botSender.getId();

        Asserts.notBlank(key, "格式错啦(key)");

        Owner owner = bilibiliManager.getOwnerWithCacheByUidOrName(key);
        Long uid = owner.getUid();
        String name = owner.getName();
        Long roomId = owner.getRoomId();

        SubscriptionQuery subscriptionQuery = new SubscriptionQuery().setStatus(0).setType(3).setValue(String.valueOf(uid)).setSenderId(senderId);
        int oldCount = subscriptionMapper.countSubscriptionByCondition(subscriptionQuery);
        Asserts.isTrue(oldCount == 0, "已经关注了哦。");

        Subscription add = new Subscription().setValue(String.valueOf(uid)).setType(3).setSenderId(senderId).setName(name).setRoomId(roomId);
        subscriptionMapper.addSubscriptionSelective(add);

        return BotMessage.simpleTextMessage(String.format("关注%s成功！", name));
    }
}
